package in.opt.sfa.tenant.tp.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.approval.ApprovalEngine;
import in.opt.sfa.tenant.dcr.submit.service.DcrService;
import in.opt.sfa.tenant.dcr.submit.service.DcrWorktypeService;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Tour plan: one plan per employee per month (tour_emp_parent), a row per planned day (tour_child) with the planned areas,
 * clients and the seniors / juniors to work with. Submitted for approval (manager, then admin, per the company flow).
 */
@Service
public class TourPlanService {

    public static final ApprovalEngine.Config APPROVAL = new ApprovalEngine.Config("tourPlanApprovalFlow", "TP_APPROVE", "TP_APPROVE", "tour plan");
    private static final String DRAFT = "DRAFT";

    private final JdbcTemplate jdbc;
    private final TeamScope scope;
    private final DcrService dcr;
    private final DcrWorktypeService worktypes;
    private final HolidayService holidays;
    private final ApprovalEngine approval;
    private final in.opt.sfa.tenant.master.permission.service.PermissionService permissions;

    public TourPlanService(JdbcTemplate jdbc, TeamScope scope, DcrService dcr, DcrWorktypeService worktypes, HolidayService holidays, ApprovalEngine approval,
                           in.opt.sfa.tenant.master.permission.service.PermissionService permissions) {
        this.permissions = permissions;
        this.jdbc = jdbc;
        this.scope = scope;
        this.dcr = dcr;
        this.worktypes = worktypes;
        this.holidays = holidays;
        this.approval = approval;
    }

    private String me() {
        String me = TeamScope.me();
        if (me == null) throw new ForbiddenException("Your login is not linked to an employee");
        return me;
    }

    // =========================================================================================
    // options
    // =========================================================================================

    /**
     * The employee whose plan is being changed: myself, or - for a manager / admin whose approval the submitted plan is waiting for -
     * the plan's owner (he may correct any date while approving).
     */
    private String owner(Object empParam, int year, int month) {
        String me = me(), emp = empParam == null || String.valueOf(empParam).isBlank() ? me : String.valueOf(empParam).trim();
        if (emp.equals(me)) return me;
        Map<String, Object> p = parentRow(emp, year, month);
        if (p == null || !approval.canApprove(toRow(p), APPROVAL, approval.actor(APPROVAL))) throw new ForbiddenException("You can change another employee's plan only while it waits for your approval");
        return emp;
    }

    /** Everything the planner needs: day types, areas (with area type), clients per kind, seniors / juniors - of me, or of the plan's owner for an approver. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> options(String empParam) {
        String me = me();
        int y = LocalDate.now().getYear(), mo = LocalDate.now().getMonthValue();
        String emp = Strings.isBlank(empParam) || empParam.trim().equals(me) ? me : empParam.trim();
        if (!emp.equals(me)) scope.require(TeamScope.Scope.TEAM_SELF, emp, "This employee is not in your team");
        Map<String, Object> o = dcr.options();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("me", emp);
        m.put("workTypes", emp.equals(me) ? o.get("workTypes") : worktypes.dayTypes(emp));
        m.put("areas", emp.equals(me) ? o.get("areas") : dcr.areasFor(emp));
        m.put("clients", emp.equals(me) ? o.get("clients") : dcr.myClients(emp));
        m.put("clientTypes", o.get("clientTypes"));
        m.put("tabs", emp.equals(me) ? o.get("tabs") : worktypes.tabsFor(emp));
        m.put("joint", emp.equals(me) ? o.get("joint") : dcr.jointCandidates(emp));
        m.put("approvalFlow", approval.flow(APPROVAL));
        return m;
    }

    // =========================================================================================
    // read
    // =========================================================================================

    private static boolean plannable(LocalDate d) {
        LocalDate today = LocalDate.now();
        return !d.isBefore(today);
    }

    private Map<String, Object> parentRow(String empId, int year, int month) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM tour_emp_parent WHERE emp_id = ? AND tour_year = ? AND tour_month = ?", empId, year, month);
        return r.isEmpty() ? null : r.get(0);
    }

    /** One employee's plan of a month (own, or a team member's for a manager / admin) with its days, the calendar's holidays and the approval trail. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> get(int year, int month, String empId) {
        String me = me(), who = Strings.isBlank(empId) ? me : empId.trim();
        if (!who.equals(me)) scope.require(TeamScope.Scope.TEAM_SELF, who, "This employee is not in your team");
        YearMonth ym = checkMonth(year, month);
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> p = parentRow(who, year, month);
        Map<String, Object> emp = jdbc.queryForList("SELECT e.emp_name AS name, e.emp_code AS code, d.designation FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation WHERE e.emp_id = ?", who)
                .stream().findFirst().orElse(Map.of());
        out.put("empId", who); out.put("empName", emp.get("name")); out.put("empCode", emp.get("code")); out.put("designation", emp.get("designation"));
        out.put("year", year); out.put("month", month); out.put("mine", who.equals(me));
        String status = p == null ? DRAFT : (String) p.get("approval_status");
        out.put("status", status);
        out.put("tourMain", p == null ? null : p.get("tour_main"));
        out.put("monthGoal", p == null ? null : p.get("month_goal"));
        out.put("canEdit", who.equals(me) && (DRAFT.equals(status) || ApprovalEngine.REJECTED.equals(status)));
        out.put("holidays", holidays.forEmployee(who, ym.atDay(1), ym.atEndOfMonth()));
        Map<String, Map<String, Object>> days = new LinkedHashMap<>();
        if (p != null) {
            long tm = ((Number) p.get("tour_main")).longValue();
            for (Map<String, Object> d : jdbc.queryForList("SELECT tour_date AS d, worktype_code AS wt, stay_area_id AS stay, tour_remark AS remark FROM tour_child WHERE tour_main = ? ORDER BY tour_date", tm)) {
                Map<String, Object> day = new LinkedHashMap<>();
                String ds = d.get("d").toString().substring(0, 10);
                day.put("date", ds); day.put("worktypeCode", d.get("wt")); day.put("stayAreaId", d.get("stay")); day.put("remark", d.get("remark"));
                day.put("areas", new ArrayList<>()); day.put("clients", new ArrayList<>()); day.put("visitWith", new ArrayList<>());
                days.put(ds, day);
            }
            addAll(days, "SELECT a.tour_date AS d, ar.oid AS id, ar.area_name AS name, ar.area_type AS areaType FROM tour_emp_area a JOIN area_master ar ON ar.oid = a.area_id WHERE a.tour_main = ? ORDER BY ar.area_name", tm, "areas");
            addAll(days, """
                    SELECT c.tour_date AS d, c.client_id AS id, c.area_id AS areaId, CONCAT(COALESCE(CONCAT(m.salutation, ' '), ''), m.client_name) AS name, m.client_code AS code
                    FROM tour_emp_clients c JOIN client_master m ON m.id = c.client_id WHERE c.tour_main = ? ORDER BY m.client_name""", tm, "clients");
            addAll(days, "SELECT w.tour_date AS d, w.visit_with_id AS id, CONCAT(e.emp_name, ' (', e.emp_code, ')') AS name FROM tour_emp_visit_with w JOIN emp_detail e ON e.emp_id = w.visit_with_id WHERE w.tour_main = ? ORDER BY e.emp_name", tm, "visitWith");
            out.put("approval", jdbc.queryForList("""
                    SELECT step, action, status_after AS statusAfter, remarks, by_name AS byName, by_designation AS byDesignation, at_time AS at
                    FROM tour_emp_approval_log WHERE tour_main = ? ORDER BY id""", tm));
            out.put("rejectReason", p.get("reject_reason"));
            List<Map<String, Object>> chg = new ArrayList<>();
            try {
            for (Map<String, Object> c : jdbc.queryForList("SELECT tour_date AS d, change_action AS action, changed_by_name AS byName, changed_by_designation AS byDesignation, changed_at AS at, summary, before_day AS beforeDay, after_day AS afterDay FROM tour_emp_change_log WHERE tour_main = ? ORDER BY id", tm)) {
                Map<String, Object> x = new LinkedHashMap<>(c);
                x.put("date", c.get("d").toString().substring(0, 10)); x.remove("d");
                try { x.put("lines", c.get("summary") == null ? List.of() : JSON.readValue(c.get("summary").toString(), List.class)); } catch (Exception ex) { x.put("lines", List.of()); }
                for (String k : new String[]{"beforeDay", "afterDay"}) {   // the day as it was / as it became
                    try { x.put(k, c.get(k) == null ? Map.of() : JSON.readValue(c.get(k).toString(), Map.class)); } catch (Exception ex) { x.put(k, Map.of()); }
                }
                x.remove("summary"); chg.add(x);
            }
            } catch (org.springframework.dao.DataAccessException ex) { /* migration 97 (tour_emp_change_log) not run yet - the plan still opens */ }
            out.put("changes", chg);
        } else { out.put("approval", List.of()); out.put("changes", List.of()); }
        out.put("days", new ArrayList<>(days.values()));
        TourPlanRow row = p == null ? null : toRow(p);
        out.put("canApprove", row != null && approval.canApprove(row, APPROVAL, approval.actor(APPROVAL)));
        out.put("canUnapprove", p != null && canUnapprove(p));
        out.put("canDelete", p != null && !who.equals(me) && permissions.isAllowed(APPROVAL.addPermission()) && (TeamScope.isAdmin() || TeamScope.isManager()));
        out.put("canSubmit", who.equals(me) && (DRAFT.equals(status) || ApprovalEngine.REJECTED.equals(status)) && !days.isEmpty());
        return out;
    }

    private void addAll(Map<String, Map<String, Object>> days, String sql, long tm, String key) {
        for (Map<String, Object> r : jdbc.queryForList(sql, tm)) {
            Map<String, Object> day = days.get(r.get("d").toString().substring(0, 10));
            if (day == null) continue;
            Map<String, Object> x = new LinkedHashMap<>(r); x.remove("d");
            @SuppressWarnings("unchecked") List<Object> l = (List<Object>) day.get(key);
            l.add(x);
        }
    }

    private static YearMonth checkMonth(int year, int month) {
        if (month < 1 || month > 12 || year < 2000 || year > 2100) throw new IllegalStateException("Invalid month");
        return YearMonth.of(year, month);
    }

    /** Plans of a month for the approver's screen: own (manager), his team's, or everyone's (admin). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(int year, int month) {
        String me = me();
        checkMonth(year, month);
        Set<String> ids = new LinkedHashSet<>();
        if (TeamScope.isAdmin()) {
            jdbc.queryForList("SELECT emp_id FROM tour_emp_parent WHERE tour_year = ? AND tour_month = ?", year, month).forEach(r -> ids.add((String) r.get("emp_id")));
            ids.removeAll(scope.adminLevelIds());
        } else if (TeamScope.isManager()) {
            ids.addAll(scope.team(me));
        }
        ids.remove(me);
        if (ids.isEmpty()) return List.of();
        String in = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Object> args = new ArrayList<>(List.of(year, month)); args.addAll(ids);
        ApprovalEngine.Actor actor = approval.actor(APPROVAL);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.tour_main AS tourMain, p.emp_id AS empId, e.emp_name AS empName, e.emp_code AS empCode, d.designation, p.approval_status AS status, p.submitted_at AS submittedAt,
                       (SELECT COUNT(*) FROM tour_child c WHERE c.tour_main = p.tour_main) AS plannedDays
                FROM tour_emp_parent p JOIN emp_detail e ON e.emp_id = p.emp_id LEFT JOIN designation_master d ON d.id = e.designation
                WHERE p.tour_year = ? AND p.tour_month = ? AND p.emp_id IN (""" + in + ") AND p.approval_status <> 'DRAFT' ORDER BY FIELD(p.approval_status, 'PENDING_MANAGER', 'PENDING_ADMIN', 'REJECTED', 'APPROVED'), e.emp_name", args.toArray());
        for (Map<String, Object> r : rows) {
            Map<String, Object> full = jdbc.queryForList("SELECT * FROM tour_emp_parent WHERE tour_main = ?", r.get("tourMain")).get(0);
            r.put("canApprove", approval.canApprove(toRow(full), APPROVAL, actor));
        }
        return rows;
    }

    // =========================================================================================
    // plan a day
    // =========================================================================================

    private long ensureParent(String me, int year, int month) {
        Map<String, Object> p = parentRow(me, year, month);
        if (p != null) return ((Number) p.get("tour_main")).longValue();
        Map<String, Object> e = jdbc.queryForList("SELECT designation, manager_id FROM emp_detail WHERE emp_id = ?", me).get(0);
        jdbc.update("INSERT INTO tour_emp_parent (emp_id, tour_year, tour_month, designation_id, manager_id, approval_status, created_by) VALUES (?,?,?,?,?, 'DRAFT', ?)",
                me, year, month, e.get("designation"), e.get("manager_id"), me);
        return ((Number) parentRow(me, year, month).get("tour_main")).longValue();
    }

    private Map<String, Object> editableParent(String me, int year, int month) {
        Map<String, Object> p = parentRow(me, year, month);
        if (p != null) {
            String st = (String) p.get("approval_status");
            if (!DRAFT.equals(st) && !ApprovalEngine.REJECTED.equals(st)) throw new IllegalStateException("This tour plan is already submitted - it cannot be changed now");
        }
        return p;
    }

    /** Plan (replace) one day: work type, and for a field day the areas, clients per area and the seniors / juniors to work with. */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveDay(Map<String, Object> f) {
        LocalDate date;
        try { date = LocalDate.parse(String.valueOf(f.get("date"))); } catch (Exception e) { throw new IllegalStateException("Choose the date"); }
        int year = date.getYear(), month = date.getMonthValue();
        if (!plannable(date)) throw new IllegalStateException("A past date cannot be planned");
        String me = owner(f.get("empId"), year, month), actor = me();   // me = whose plan; actor = who is changing it
        if (me.equals(actor)) editableParent(me, year, month);
        String wt = f.get("worktypeCode") == null ? "" : String.valueOf(f.get("worktypeCode")).toUpperCase();
        if (!worktypes.isDayType(me, wt)) throw new IllegalStateException("Choose the work type");
        boolean field = worktypes.isFieldWork(me, wt);
        List<Long> areaIds = new ArrayList<>();
        for (Object a : (Collection<Object>) f.getOrDefault("areaIds", List.of())) { Long id = Long.valueOf(String.valueOf(a)); if (!areaIds.contains(id)) areaIds.add(id); }
        List<Map<String, Object>> clientRows = new ArrayList<>();
        Set<Long> seenClients = new HashSet<>();
        for (Object c : (Collection<Object>) f.getOrDefault("clients", List.of())) {
            Map<String, Object> cm = (Map<String, Object>) c;
            Long cid = Long.valueOf(String.valueOf(cm.get("clientId"))), aid = Long.valueOf(String.valueOf(cm.get("areaId")));
            if (seenClients.add(cid)) clientRows.add(Map.of("clientId", cid, "areaId", aid));
        }
        Set<String> withIds = new LinkedHashSet<>();
        for (Object w : (Collection<Object>) f.getOrDefault("visitWith", List.of())) withIds.add(String.valueOf(w));
        Long stay = f.get("stayAreaId") == null || String.valueOf(f.get("stayAreaId")).isBlank() ? null : Long.valueOf(String.valueOf(f.get("stayAreaId")));
        String remark = f.get("remark") == null || String.valueOf(f.get("remark")).isBlank() ? null : String.valueOf(f.get("remark")).trim();
        if (remark != null && remark.length() > 250) throw new IllegalStateException("Remark can be at most 250 characters");
        if (!field) {
            if (!areaIds.isEmpty() || !clientRows.isEmpty() || !withIds.isEmpty()) throw new IllegalStateException("Areas, clients and visit-with are only for a field working day");
            stay = null;
        } else {
            if (areaIds.isEmpty()) throw new IllegalStateException("Choose at least one area for a field day");
            Set<Long> myAreas = new HashSet<>();
            ((List<Map<String, Object>>) dcr.options().get("areas")).forEach(a -> myAreas.add(((Number) a.get("id")).longValue()));
            for (Long a : areaIds) if (!myAreas.contains(a)) throw new IllegalStateException("Area " + a + " is not one of your approved areas");
            if (stay != null && !myAreas.contains(stay)) throw new IllegalStateException("The stay area is not one of your areas");
            Map<Long, Map<String, Object>> mine = new HashMap<>();
            dcr.myClients(me).forEach(c -> mine.put(((Number) c.get("id")).longValue(), c));
            for (Map<String, Object> c : clientRows) {
                Long cid = (Long) c.get("clientId"), aid = (Long) c.get("areaId");
                Map<String, Object> cm = mine.get(cid);
                if (cm == null) throw new IllegalStateException("Client " + cid + " is not one of your approved, active clients");
                if (!areaIds.contains(aid)) throw new IllegalStateException("A client is picked under an area that is not planned for the day");
                if (cm.get("areaId") == null || ((Number) cm.get("areaId")).longValue() != aid) throw new IllegalStateException(cm.get("name") + " does not belong to that area");
            }
            Set<String> allowedWith = new HashSet<>();
            dcr.jointCandidates(me).forEach(j -> allowedWith.add((String) j.get("id")));
            for (String w : withIds) if (!allowedWith.contains(w)) throw new IllegalStateException("You cannot plan to work with " + empLabel(w));
        }
        long tm = ensureParent(me, year, month);
        Map<String, Object> before = me.equals(actor) ? null : daySnapshot(tm, date);
        jdbc.update("DELETE FROM tour_child WHERE tour_main = ? AND tour_date = ?", tm, date);
        for (String t : List.of("tour_emp_area", "tour_emp_clients", "tour_emp_visit_with")) jdbc.update("DELETE FROM " + t + " WHERE tour_main = ? AND tour_date = ?", tm, date);
        jdbc.update("INSERT INTO tour_child (tour_main, tour_date, worktype_code, stay_area_id, tour_remark) VALUES (?,?,?,?,?)", tm, date, wt, stay, remark);
        for (Long a : areaIds) jdbc.update("INSERT INTO tour_emp_area (tour_main, tour_date, area_id) VALUES (?,?,?)", tm, date, a);
        for (Map<String, Object> c : clientRows) jdbc.update("INSERT INTO tour_emp_clients (tour_main, tour_date, area_id, client_id) VALUES (?,?,?,?)", tm, date, c.get("areaId"), c.get("clientId"));
        for (String w : withIds) jdbc.update("INSERT INTO tour_emp_visit_with (tour_main, tour_date, visit_with_id) VALUES (?,?,?)", tm, date, w);
        jdbc.update("UPDATE tour_emp_parent SET updated_by = ? WHERE tour_main = ?", actor, tm);
        if (!me.equals(actor)) { editLog(tm, "Changed " + date + " (work type " + wt + ")"); changeLog(tm, date, before, daySnapshot(tm, date)); }
        return Map.of("tourMain", tm, "date", date.toString());
    }

    /** Take a day out of the plan. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void clearDay(LocalDate date, String empParam) {
        if (!plannable(date)) throw new IllegalStateException("A past date cannot be changed");
        String me = owner(empParam, date.getYear(), date.getMonthValue()), actor = me();
        Map<String, Object> p = me.equals(actor) ? editableParent(me, date.getYear(), date.getMonthValue()) : parentRow(me, date.getYear(), date.getMonthValue());
        if (p == null) return;
        long tm = ((Number) p.get("tour_main")).longValue();
        Map<String, Object> before = me.equals(actor) ? null : daySnapshot(tm, date);
        jdbc.update("DELETE FROM tour_child WHERE tour_main = ? AND tour_date = ?", tm, date);
        for (String t : List.of("tour_emp_area", "tour_emp_clients", "tour_emp_visit_with")) jdbc.update("DELETE FROM " + t + " WHERE tour_main = ? AND tour_date = ?", tm, date);
        if (!me.equals(actor)) { editLog(tm, "Removed " + date + " from the plan"); changeLog(tm, date, before, new LinkedHashMap<>()); }
    }

    private List<String> names(String sql, long tm, LocalDate d) {
        List<String> out = new ArrayList<>();
        jdbc.queryForList(sql, tm, d).forEach(r -> out.add(String.valueOf(r.values().iterator().next())));
        return out;
    }

    /** One planned day in readable form (names, not ids) - the "before" and "after" of an approver's change. */
    private Map<String, Object> daySnapshot(long tm, LocalDate d) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Map<String, Object>> day = jdbc.queryForList("SELECT c.worktype_code AS wt, c.tour_remark AS remark, (SELECT area_name FROM area_master WHERE oid = c.stay_area_id) AS stay FROM tour_child c WHERE c.tour_main = ? AND c.tour_date = ?", tm, d);
        if (day.isEmpty()) return m;
        m.put("worktype", day.get(0).get("wt")); m.put("remark", day.get(0).get("remark")); m.put("stay", day.get(0).get("stay"));
        m.put("areas", names("SELECT CONCAT(ar.area_name, IF(ar.area_type IS NULL OR ar.area_type = '', '', CONCAT(' [', ar.area_type, ']'))) FROM tour_emp_area a JOIN area_master ar ON ar.oid = a.area_id WHERE a.tour_main = ? AND a.tour_date = ? ORDER BY ar.area_name", tm, d));
        m.put("clients", names("SELECT CONCAT(m.client_name, ' (', m.client_code, ')') FROM tour_emp_clients c JOIN client_master m ON m.id = c.client_id WHERE c.tour_main = ? AND c.tour_date = ? ORDER BY m.client_name", tm, d));
        m.put("visitWith", names("SELECT CONCAT(e.emp_name, ' (', e.emp_code, ')') FROM tour_emp_visit_with w JOIN emp_detail e ON e.emp_id = w.visit_with_id WHERE w.tour_main = ? AND w.tour_date = ? ORDER BY e.emp_name", tm, d));
        return m;
    }

    @SuppressWarnings("unchecked")
    private static List<String> diff(Map<String, Object> b, Map<String, Object> a) {
        List<String> out = new ArrayList<>();
        if (b.isEmpty() && a.isEmpty()) return out;
        if (!Objects.equals(b.get("worktype"), a.get("worktype"))) out.add("~ Work type: " + (b.isEmpty() ? "-" : b.get("worktype")) + " → " + (a.isEmpty() ? "removed" : a.get("worktype")));
        for (String[] k : new String[][]{{"areas", "Area"}, {"clients", "Client"}, {"visitWith", "Visit with"}}) {
            Set<String> bs = new LinkedHashSet<>((List<String>) b.getOrDefault(k[0], List.of())), as = new LinkedHashSet<>((List<String>) a.getOrDefault(k[0], List.of()));
            for (String x : as) if (!bs.contains(x)) out.add("+ " + k[1] + " added: " + x);
            for (String x : bs) if (!as.contains(x)) out.add("- " + k[1] + " removed: " + x);
        }
        if (!Objects.equals(b.get("stay"), a.get("stay"))) out.add("~ Night stay: " + (b.get("stay") == null ? "-" : b.get("stay")) + " → " + (a.get("stay") == null ? "-" : a.get("stay")));
        if (!Objects.equals(b.get("remark"), a.get("remark"))) out.add("~ Remark: " + (b.get("remark") == null ? "-" : b.get("remark")) + " → " + (a.get("remark") == null ? "-" : a.get("remark")));
        return out;
    }

    /** What an approver changed on one date: who, when, the changed lines and the day before / after. */
    private void changeLog(long tm, LocalDate d, Map<String, Object> before, Map<String, Object> after) {
        String actor = me();
        Map<String, Object> e = jdbc.queryForList("SELECT e.emp_name AS name, d.designation FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation WHERE e.emp_id = ?", actor).stream().findFirst().orElse(Map.of());
        List<String> lines = diff(before, after);
        if (lines.isEmpty()) lines = List.of("No change");
        String action = after.isEmpty() ? "REMOVED" : before.isEmpty() ? "ADDED" : "CHANGED";
        try {
            jdbc.update("INSERT INTO tour_emp_change_log (tour_main, tour_date, change_action, changed_by, changed_by_name, changed_by_designation, summary, before_day, after_day) VALUES (?,?,?,?,?,?,?,?,?)",
                    tm, d, action, actor, e.get("name"), e.get("designation"), JSON.writeValueAsString(lines), JSON.writeValueAsString(before), JSON.writeValueAsString(after));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Could not record the change"); }
    }

    /** An approver's correction of a submitted plan goes into its approval trail. */
    private void editLog(long tm, String remarks) {
        String actor = me();
        Map<String, Object> e = jdbc.queryForList("SELECT e.emp_name AS name, d.designation FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation WHERE e.emp_id = ?", actor).stream().findFirst().orElse(Map.of());
        String status = (String) jdbc.queryForList("SELECT approval_status FROM tour_emp_parent WHERE tour_main = ?", tm).get(0).get("approval_status");
        jdbc.update("INSERT INTO tour_emp_approval_log (tour_main, step, action, status_after, remarks, by_emp_id, by_name, by_designation) VALUES (?,?,?,?,?,?,?,?)",
                tm, "EDIT", "EDITED", status, remarks, actor, e.get("name"), e.get("designation"));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveGoal(int year, int month, String goal) {
        String me = me();
        checkMonth(year, month);
        editableParent(me, year, month);
        long tm = ensureParent(me, year, month);
        jdbc.update("UPDATE tour_emp_parent SET month_goal = ?, updated_by = ? WHERE tour_main = ?", Strings.isBlank(goal) ? null : goal.trim(), me, tm);
    }

    // =========================================================================================
    // submit / approve
    // =========================================================================================

    private TourPlanRow toRow(Map<String, Object> p) {
        TourPlanRow r = new TourPlanRow();
        r.setTourMain(((Number) p.get("tour_main")).longValue());
        r.setEmpId((String) p.get("emp_id"));
        r.setApprovalStatus((String) p.get("approval_status"));
        r.setAddRequestedBy((String) p.get("add_requested_by"));
        r.setManagerApprovedBy((String) p.get("manager_approved_by"));
        return r;
    }

    private void persist(TourPlanRow r, boolean submitted) {
        jdbc.update("""
                UPDATE tour_emp_parent SET approval_status = ?, add_requested_by = ?, manager_approved_by = ?, manager_approved_on = ?, approved_by = ?, approved_on = ?,
                       rejected_by = ?, rejected_on = ?, reject_reason = ?, submitted_at = COALESCE(?, submitted_at) WHERE tour_main = ?""",
                r.getApprovalStatus(), r.getAddRequestedBy(), r.getManagerApprovedBy(), ts(r.getManagerApprovedOn()), r.getApprovedBy(), ts(r.getApprovedOn()),
                r.getRejectedBy(), ts(r.getRejectedOn()), r.getRejectReason(), submitted ? Timestamp.valueOf(LocalDateTime.now()) : null, r.getTourMain());
    }

    private static Timestamp ts(LocalDateTime t) { return t == null ? null : Timestamp.valueOf(t); }

    private void log(long tm, List<ApprovalEngine.LogRow> rows) {
        for (ApprovalEngine.LogRow l : rows) logRow(tm, l);
    }

    private void logRow(long tm, ApprovalEngine.LogRow l) {
        jdbc.update("INSERT INTO tour_emp_approval_log (tour_main, step, action, status_after, remarks, by_emp_id, by_name, by_designation) VALUES (?,?,?,?,?,?,?,?)",
                tm, l.step(), l.action(), l.statusAfter(), l.remarks(), l.by().empId, l.by().empName(), l.by().designationName());
    }

    /** Send the month's plan for approval: every working day (not a holiday / weekly off) from today on needs a work type. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> submit(int year, int month) {
        String me = me();
        YearMonth ym = checkMonth(year, month);
        Map<String, Object> p = editableParent(me, year, month);
        if (p == null) throw new IllegalStateException("Nothing is planned yet");
        long tm = ((Number) p.get("tour_main")).longValue();
        Set<String> planned = new HashSet<>();
        jdbc.queryForList("SELECT tour_date AS d FROM tour_child WHERE tour_main = ?", tm).forEach(r -> planned.add(r.get("d").toString().substring(0, 10)));
        if (planned.isEmpty()) throw new IllegalStateException("Plan at least one day before submitting");
        Map<String, List<Map<String, Object>>> off = holidays.forEmployee(me, ym.atDay(1), ym.atEndOfMonth());
        List<String> missing = new ArrayList<>();
        for (LocalDate d = ym.atDay(1); !d.isAfter(ym.atEndOfMonth()); d = d.plusDays(1)) {
            if (!plannable(d) || off.containsKey(d.toString()) || planned.contains(d.toString())) continue;
            missing.add(d.getDayOfMonth() + "");
        }
        if (!missing.isEmpty()) throw new IllegalStateException("The plan can be submitted only when every working day till the month end is planned (checked from today, holidays and weekly off excluded). " + missing.size() + " day(s) left: " + String.join(", ", missing.size() > 8 ? missing.subList(0, 8) : missing) + (missing.size() > 8 ? " …" : ""));
        TourPlanRow row = toRow(p);
        ApprovalEngine.Actor actor = approval.actor(APPROVAL);
        List<ApprovalEngine.LogRow> rows = approval.requestAdd(row, APPROVAL, actor);
        persist(row, true);
        log(tm, rows);
        return Map.of("tourMain", tm, "status", row.getApprovalStatus());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> approve(Long tourMain) {
        TourPlanRow row = load(tourMain);
        ApprovalEngine.LogRow l = approval.approve(row, APPROVAL, approval.actor(APPROVAL));
        persist(row, false);
        logRow(tourMain, l);
        return Map.of("tourMain", tourMain, "status", row.getApprovalStatus());
    }

    /**
     * May the signed-in user take a plan back (unapprove)? A manager of the employee or an admin, never for his own plan, only while the plan is
     * submitted or approved; a plan the admin gave the final approval to can be taken back by an admin only.
     */
    private boolean canUnapprove(Map<String, Object> p) {
        String me = me(), owner = (String) p.get("emp_id"), st = (String) p.get("approval_status");
        if (owner.equals(me) || !(ApprovalEngine.PENDING_MANAGER.equals(st) || ApprovalEngine.PENDING_ADMIN.equals(st) || ApprovalEngine.APPROVED.equals(st))) return false;
        if (!permissions.isAllowed(APPROVAL.addPermission())) return false;
        if (TeamScope.isAdmin()) return true;
        if (!TeamScope.isManager() || !approval.reach().reaches(owner)) return false;
        String by = (String) p.get("approved_by");
        return !(ApprovalEngine.APPROVED.equals(st) && by != null && scope.adminLevelIds().contains(by));
    }

    /** Take a submitted / approved plan back: it returns to the employee as editable (nothing is deleted) and has to be submitted again. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> unapprove(Long tourMain, String reason) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM tour_emp_parent WHERE tour_main = ?", tourMain);
        if (r.isEmpty()) throw new IllegalStateException("Tour plan not found");
        Map<String, Object> p = r.get(0);
        String owner = (String) p.get("emp_id"), me = me();
        if (!owner.equals(me)) scope.require(TeamScope.Scope.TEAM_SELF, owner, "This employee is not in your team");
        if (!canUnapprove(p)) throw new ForbiddenException("You cannot take this plan back");
        String why = Strings.isBlank(reason) ? null : reason.trim();
        if (why == null) throw new IllegalStateException("Give the reason for taking the plan back");
        if (why.length() > 255) throw new IllegalStateException("Reason can be at most 255 characters");
        jdbc.update("""
                UPDATE tour_emp_parent SET approval_status = 'REJECTED', rejected_by = ?, rejected_on = NOW(), reject_reason = ?,
                       approved_by = NULL, approved_on = NULL, manager_approved_by = NULL, manager_approved_on = NULL WHERE tour_main = ?""", me, why, tourMain);
        Map<String, Object> e = jdbc.queryForList("SELECT e.emp_name AS name, d.designation FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation WHERE e.emp_id = ?", me).stream().findFirst().orElse(Map.of());
        jdbc.update("INSERT INTO tour_emp_approval_log (tour_main, step, action, status_after, remarks, by_emp_id, by_name, by_designation) VALUES (?,?,?,?,?,?,?,?)",
                tourMain, "UNAPPROVE", "UNAPPROVED", "REJECTED", why, me, e.get("name"), e.get("designation"));
        return Map.of("tourMain", tourMain, "status", "REJECTED");
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    private static Object plain(Object o) {   // dates / timestamps as text, so the snapshot stays readable
        if (o instanceof Map<?, ?> m) { Map<String, Object> out = new LinkedHashMap<>(); m.forEach((k, v) -> out.put(String.valueOf(k), plain(v))); return out; }
        if (o instanceof Collection<?> c) return c.stream().map(TourPlanService::plain).toList();
        if (o == null || o instanceof Number || o instanceof Boolean || o instanceof String) return o;
        return String.valueOf(o);
    }

    /**
     * Delete a whole plan. An admin, or a manager of the employee (never his own plan), may do it - with a reason. Nothing is lost silently:
     * who deleted it, when, why and the complete plan (days, areas, clients, visit-with, approval trail) go to tour_emp_delete_log first.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> deletePlan(Long tourMain, String reason) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM tour_emp_parent WHERE tour_main = ?", tourMain);
        if (r.isEmpty()) throw new IllegalStateException("Tour plan not found");
        Map<String, Object> p = r.get(0);
        String owner = (String) p.get("emp_id"), me = me();
        if (owner.equals(me)) throw new ForbiddenException("You cannot delete your own tour plan");
        scope.require(TeamScope.Scope.TEAM_SELF, owner, "This employee is not in your team");
        if (!permissions.isAllowed(APPROVAL.addPermission()) || !(TeamScope.isAdmin() || TeamScope.isManager())) throw new ForbiddenException("Only a manager or an admin can delete a tour plan");
        String why = Strings.isBlank(reason) ? null : reason.trim();
        if (why == null) throw new IllegalStateException("Give the reason for deleting the plan");
        if (why.length() > 255) throw new IllegalStateException("Reason can be at most 255 characters");
        int year = ((Number) p.get("tour_year")).intValue(), month = ((Number) p.get("tour_month")).intValue();
        Map<String, Object> snap = new LinkedHashMap<>(get(year, month, owner));
        snap.remove("holidays");
        snap.put("plan", plain(p));
        String json;
        try { json = JSON.writeValueAsString(plain(snap)); } catch (Exception e) { throw new IllegalStateException("Could not record the plan before deleting it"); }
        Map<String, Object> who = jdbc.queryForList("SELECT e.emp_name AS name, d.designation FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation WHERE e.emp_id = ?", me).stream().findFirst().orElse(Map.of());
        int days = jdbc.queryForObject("SELECT COUNT(*) FROM tour_child WHERE tour_main = ?", Integer.class, tourMain);
        jdbc.update("""
                INSERT INTO tour_emp_delete_log (tour_main, emp_id, emp_name, tour_year, tour_month, status_at_delete, planned_days, deleted_by, deleted_by_name, deleted_by_designation, reason, snapshot)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""", tourMain, owner, snap.get("empName"), year, month, p.get("approval_status"), days, me, who.get("name"), who.get("designation"), why, json);
        jdbc.update("DELETE FROM tour_emp_parent WHERE tour_main = ?", tourMain);   // days, areas, clients, visit-with and the trail go with it (ON DELETE CASCADE)
        return Map.of("deleted", tourMain, "recorded", true);
    }

    /** Who deleted which plans, when and why (manager: his team's, admin: everyone's), newest first; {@code id} also returns the saved plan. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> deleted(Integer year, Integer month) {
        String me = me();
        if (!TeamScope.isAdmin() && !TeamScope.isManager()) return List.of();
        List<Object> args = new ArrayList<>(); StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (year != null) { where.append(" AND tour_year = ?"); args.add(year); }
        if (month != null) { where.append(" AND tour_month = ?"); args.add(month); }
        if (!TeamScope.isAdmin()) {
            Set<String> team = new LinkedHashSet<>(scope.team(me)); team.add(me);
            where.append(" AND emp_id IN (").append(String.join(",", Collections.nCopies(team.size(), "?"))).append(")"); args.addAll(team);
        }
        return jdbc.queryForList("""
                SELECT id, tour_main AS tourMain, emp_id AS empId, emp_name AS empName, tour_year AS year, tour_month AS month, status_at_delete AS status, planned_days AS plannedDays,
                       deleted_by AS deletedBy, deleted_by_name AS deletedByName, deleted_by_designation AS deletedByDesignation, deleted_at AS deletedAt, reason, snapshot
                FROM tour_emp_delete_log""" + where + " ORDER BY deleted_at DESC, id DESC LIMIT 500", args.toArray());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> reject(Long tourMain, String reason) {
        TourPlanRow row = load(tourMain);
        ApprovalEngine.LogRow l = approval.reject(row, reason, APPROVAL, approval.actor(APPROVAL));
        persist(row, false);
        logRow(tourMain, l);
        return Map.of("tourMain", tourMain, "status", row.getApprovalStatus());
    }

    private TourPlanRow load(Long tourMain) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM tour_emp_parent WHERE tour_main = ?", tourMain);
        if (r.isEmpty()) throw new IllegalStateException("Tour plan not found");
        TourPlanRow row = toRow(r.get(0));
        if (!row.getEmpId().equals(me())) scope.require(TeamScope.Scope.TEAM_SELF, row.getEmpId(), "This employee is not in your team");
        return row;
    }

    /** "Name (code)" of an employee for a message - never the internal employee id. */
    private String empLabel(String empId) {
        return jdbc.queryForList("SELECT CONCAT(emp_name, ' (', emp_code, ')') AS n FROM emp_detail WHERE emp_id = ?", empId).stream().findFirst().map(r -> String.valueOf(r.get("n"))).orElse("this employee");
    }
}
