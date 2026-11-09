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

    public TourPlanService(JdbcTemplate jdbc, TeamScope scope, DcrService dcr, DcrWorktypeService worktypes, HolidayService holidays, ApprovalEngine approval) {
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

    /** Everything the planner needs: day types, my areas (with area type), my clients per kind, seniors / juniors. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> options() {
        String me = me();
        Map<String, Object> o = dcr.options();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("me", me);
        m.put("workTypes", o.get("workTypes"));
        m.put("areas", o.get("areas"));
        m.put("clients", o.get("clients"));
        m.put("clientTypes", o.get("clientTypes"));
        m.put("tabs", o.get("tabs"));
        m.put("joint", o.get("joint"));
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
        } else out.put("approval", List.of());
        out.put("days", new ArrayList<>(days.values()));
        TourPlanRow row = p == null ? null : toRow(p);
        out.put("canApprove", row != null && approval.canApprove(row, APPROVAL, approval.actor(APPROVAL)));
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
        String me = me();
        LocalDate date;
        try { date = LocalDate.parse(String.valueOf(f.get("date"))); } catch (Exception e) { throw new IllegalStateException("Choose the date"); }
        int year = date.getYear(), month = date.getMonthValue();
        if (!plannable(date)) throw new IllegalStateException("A past date cannot be planned");
        editableParent(me, year, month);
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
            for (String w : withIds) if (!allowedWith.contains(w)) throw new IllegalStateException("You cannot plan to work with " + w);
        }
        long tm = ensureParent(me, year, month);
        jdbc.update("DELETE FROM tour_child WHERE tour_main = ? AND tour_date = ?", tm, date);
        for (String t : List.of("tour_emp_area", "tour_emp_clients", "tour_emp_visit_with")) jdbc.update("DELETE FROM " + t + " WHERE tour_main = ? AND tour_date = ?", tm, date);
        jdbc.update("INSERT INTO tour_child (tour_main, tour_date, worktype_code, stay_area_id, tour_remark) VALUES (?,?,?,?,?)", tm, date, wt, stay, remark);
        for (Long a : areaIds) jdbc.update("INSERT INTO tour_emp_area (tour_main, tour_date, area_id) VALUES (?,?,?)", tm, date, a);
        for (Map<String, Object> c : clientRows) jdbc.update("INSERT INTO tour_emp_clients (tour_main, tour_date, area_id, client_id) VALUES (?,?,?,?)", tm, date, c.get("areaId"), c.get("clientId"));
        for (String w : withIds) jdbc.update("INSERT INTO tour_emp_visit_with (tour_main, tour_date, visit_with_id) VALUES (?,?,?)", tm, date, w);
        jdbc.update("UPDATE tour_emp_parent SET updated_by = ? WHERE tour_main = ?", me, tm);
        return Map.of("tourMain", tm, "date", date.toString());
    }

    /** Take a day out of the plan. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void clearDay(LocalDate date) {
        String me = me();
        if (!plannable(date)) throw new IllegalStateException("A past date cannot be changed");
        Map<String, Object> p = editableParent(me, date.getYear(), date.getMonthValue());
        if (p == null) return;
        long tm = ((Number) p.get("tour_main")).longValue();
        jdbc.update("DELETE FROM tour_child WHERE tour_main = ? AND tour_date = ?", tm, date);
        for (String t : List.of("tour_emp_area", "tour_emp_clients", "tour_emp_visit_with")) jdbc.update("DELETE FROM " + t + " WHERE tour_main = ? AND tour_date = ?", tm, date);
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
        if (!missing.isEmpty()) throw new IllegalStateException("Plan these dates first (or they must be a holiday / weekly off): " + String.join(", ", missing.size() > 8 ? missing.subList(0, 8) : missing) + (missing.size() > 8 ? " …" : ""));
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
}
