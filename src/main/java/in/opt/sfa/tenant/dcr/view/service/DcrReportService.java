package in.opt.sfa.tenant.dcr.view.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.team.TeamScope;
import in.opt.sfa.tenant.tp.service.HolidayService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * DCR report, entirely from the 2026 tables (dcr_parent_summary, dcr_client_summary, dcr_unlisted_client_summary, dcr_client_sample ...).
 * One row per employee per day: the DCR (web or app), or - when there is none - Absent, Holiday, Weekly off or "not started yet" (today).
 * A user sees his own days, a manager his team's, an admin everyone's (TeamScope).
 */
@Service
public class DcrReportService {

    public static final int MAX_DAYS = 366;
    private static final String[] STATUS = {"Draft", "Submitted", "Approved", "Rejected"};

    private final JdbcTemplate jdbc;
    private final TeamScope scope;
    private final HolidayService holidays;

    public DcrReportService(JdbcTemplate jdbc, TeamScope scope, HolidayService holidays) {
        this.jdbc = jdbc;
        this.scope = scope;
        this.holidays = holidays;
    }

    /** The report filter. */
    public record Filter(LocalDate from, LocalDate to, Long stateId, Long hqId, Integer designationId, String empId, Integer status, Integer source, boolean absent) { }

    // =========================================================================================
    // filters
    // =========================================================================================

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> filters() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("states", jdbc.queryForList("SELECT oid AS id, state_name AS name FROM state_master ORDER BY state_name"));
        m.put("hqs", jdbc.queryForList("SELECT oid AS id, hq_name AS name FROM hq_master ORDER BY hq_name"));
        m.put("designations", jdbc.queryForList("SELECT id, designation AS name FROM designation_master ORDER BY hierarchy_level, designation"));
        List<Map<String, Object>> emps = jdbc.queryForList("SELECT e.emp_id AS id, CONCAT(e.emp_name, ' (', e.emp_code, ')') AS name, e.state_id AS stateId, e.hq_id AS hqId, e.designation AS designationId FROM emp_detail e WHERE e.is_active = 1 ORDER BY e.emp_name");
        m.put("employees", scope.filter(TeamScope.Scope.TEAM_SELF, emps, r -> (String) r.get("id")));
        return m;
    }

    // =========================================================================================
    // summary: one row per employee per day
    // =========================================================================================

    private Filter checked(Filter f) {
        LocalDate to = f.to() == null ? LocalDate.now() : f.to(), from = f.from() == null ? to.withDayOfMonth(1) : f.from();
        if (from.isAfter(to)) throw new IllegalStateException("From date is after To date");
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) throw new IllegalStateException("Choose a period of at most " + MAX_DAYS + " days");
        String emp = Strings.isBlank(f.empId()) ? null : f.empId().trim();
        if (emp != null) scope.require(TeamScope.Scope.TEAM_SELF, emp, "This employee is not in your team");
        return new Filter(from, to, f.stateId(), f.hqId(), f.designationId(), emp, f.status(), f.source(), f.absent());
    }

    private static void empWhere(Filter f, StringBuilder w, List<Object> a) {
        if (f.stateId() != null) { w.append(" AND e.state_id = ?"); a.add(f.stateId()); }
        if (f.hqId() != null) { w.append(" AND e.hq_id = ?"); a.add(f.hqId()); }
        if (f.designationId() != null) { w.append(" AND e.designation = ?"); a.add(f.designationId()); }
        if (f.empId() != null) { w.append(" AND e.emp_id = ?"); a.add(f.empId()); }
    }

    private Map<String, String> workTypeNames() {
        Map<String, String> m = new HashMap<>();
        jdbc.queryForList("SELECT worktype_code AS c, display_name AS n FROM app_dcr_worktype WHERE emp_id IS NULL AND row_type IN ('wd', 'attn')").forEach(r -> m.put((String) r.get("c"), (String) r.get("n")));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> summary(Filter in) {
        Filter f = checked(in);
        LocalDate today = LocalDate.now();
        Map<String, String> wtNames = workTypeNames();
        // ---- the DCRs of the period ----
        StringBuilder w = new StringBuilder(" WHERE p.dcr_date BETWEEN ? AND ?");
        List<Object> a = new ArrayList<>(List.of(f.from(), f.to()));
        empWhere(f, w, a);
        if (f.status() != null) { w.append(" AND p.status = ?"); a.add(f.status()); }
        if (f.source() != null) { w.append(" AND p.submitted_through = ?"); a.add(f.source()); }
        List<Map<String, Object>> dcrs = jdbc.queryForList("""
                SELECT p.dcr_id AS dcrId, p.dcr_date AS date, p.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS empName, dg.designation, h.hq_name AS hq, st.state_name AS state,
                       p.work_type AS workType, p.time_in AS timeIn, p.time_out AS timeOut, sa.area_name AS startArea, COALESCE(p.start_area_type, sa.area_type) AS areaType, p.travel_mode AS travelMode,
                       p.listed_calls AS listedCalls, p.unlisted_calls AS unlistedCalls, p.pob_total AS pob, p.expense_total AS expense, p.status, p.submitted_at AS submittedAt,
                       p.submitted_through AS source, ap.emp_name AS approvedBy, p.approved_at AS approvedAt, p.remarks
                FROM dcr_parent_summary p JOIN emp_detail e ON e.emp_id = p.emp_id
                LEFT JOIN designation_master dg ON dg.id = e.designation LEFT JOIN hq_master h ON h.oid = e.hq_id LEFT JOIN state_master st ON st.oid = e.state_id
                LEFT JOIN area_master sa ON sa.oid = p.start_area_id LEFT JOIN emp_detail ap ON ap.emp_id = p.approved_by""" + w + " ORDER BY p.dcr_date, e.emp_name", a.toArray());
        dcrs = scope.filter(TeamScope.Scope.TEAM_SELF, dcrs, r -> (String) r.get("empId"));
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> have = new HashSet<>();
        for (Map<String, Object> r : dcrs) {
            r.put("kind", "DCR");
            r.put("workTypeName", wtNames.getOrDefault((String) r.get("workType"), (String) r.get("workType")));
            int st = ((Number) r.get("status")).intValue();
            r.put("statusName", STATUS[Math.max(0, Math.min(3, st))]);
            r.put("sourceName", ((Number) r.get("source")).intValue() == 1 ? "App" : "Web");
            r.put("calls", ((Number) r.get("listedCalls")).intValue() + ((Number) r.get("unlistedCalls")).intValue());
            have.add(r.get("empId") + "|" + String.valueOf(r.get("date")).substring(0, 10));
            r.put("date", String.valueOf(r.get("date")).substring(0, 10));
            rows.add(r);
        }
        // ---- the days without a DCR: absent / holiday / weekly off (only when no status / source filter narrows to DCRs) ----
        if (f.absent() && f.status() == null && f.source() == null) {
            StringBuilder ew = new StringBuilder(" WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND COALESCE(e.is_office_staff, 0) = 0");
            List<Object> ea = new ArrayList<>();
            empWhere(f, ew, ea);
            List<Map<String, Object>> emps = jdbc.queryForList("""
                    SELECT e.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS empName, dg.designation, h.hq_name AS hq, st.state_name AS state, e.date_of_joining AS joined, e.last_working_date AS lastDay
                    FROM emp_detail e LEFT JOIN designation_master dg ON dg.id = e.designation LEFT JOIN hq_master h ON h.oid = e.hq_id LEFT JOIN state_master st ON st.oid = e.state_id""" + ew + " ORDER BY e.emp_name", ea.toArray());
            emps = scope.filter(TeamScope.Scope.TEAM_SELF, emps, r -> (String) r.get("empId"));
            Set<String> admins = scope.adminLevelIds();
            for (Map<String, Object> e : emps) {
                String id = (String) e.get("empId");
                if (admins.contains(id)) continue;   // admin-level users do not fill a DCR
                LocalDate start = f.from(), end = f.to().isAfter(today) ? today : f.to();
                if (e.get("joined") != null) { LocalDate j = LocalDate.parse(e.get("joined").toString().substring(0, 10)); if (j.isAfter(start)) start = j; }
                if (e.get("lastDay") != null) { LocalDate l = LocalDate.parse(e.get("lastDay").toString().substring(0, 10)); if (l.isBefore(end)) end = l; }
                if (start.isAfter(end)) continue;
                Map<String, List<Map<String, Object>>> off = holidays.forEmployee(id, start, end);
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    String ds = d.toString();
                    if (have.contains(id + "|" + ds)) continue;
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("empId", id); r.put("empCode", e.get("empCode")); r.put("empName", e.get("empName")); r.put("designation", e.get("designation")); r.put("hq", e.get("hq")); r.put("state", e.get("state")); r.put("date", ds);
                    List<Map<String, Object>> o = off.get(ds);
                    if (o != null && !o.isEmpty()) {
                        boolean hol = o.stream().anyMatch(x -> "HOLIDAY".equals(x.get("kind")));
                        Map<String, Object> pick = o.stream().filter(x -> "HOLIDAY".equals(x.get("kind"))).findFirst().orElse(o.get(0));
                        r.put("kind", hol ? "HOLIDAY" : "WEEKLY_OFF"); r.put("note", hol ? pick.get("name") : "Weekly off" + (pick.get("remarks") != null ? " · " + pick.get("remarks") : ""));
                    } else if (d.equals(today)) { r.put("kind", "NOT_STARTED"); r.put("note", "Not started yet"); }
                    else { r.put("kind", "ABSENT"); r.put("note", "No DCR"); }
                    rows.add(r);
                }
            }
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("date")).thenComparing(r -> String.valueOf(r.get("empName"))));
        // ---- totals ----
        Map<String, Object> t = new LinkedHashMap<>();
        Set<String> emps = new LinkedHashSet<>();
        long calls = 0, pobCents = 0, expCents = 0; int dcrN = 0, absent = 0, holiday = 0, off = 0, web = 0, app = 0, sub = 0, appr = 0, rej = 0, draft = 0;
        for (Map<String, Object> r : rows) {
            emps.add((String) r.get("empId"));
            switch ((String) r.get("kind")) {
                case "ABSENT" -> absent++;
                case "HOLIDAY" -> holiday++;
                case "WEEKLY_OFF" -> off++;
                case "DCR" -> {
                    dcrN++; calls += ((Number) r.get("calls")).longValue();
                    pobCents += Math.round(((Number) r.get("pob")).doubleValue() * 100); expCents += Math.round(((Number) r.get("expense")).doubleValue() * 100);
                    if (((Number) r.get("source")).intValue() == 1) app++; else web++;
                    switch (((Number) r.get("status")).intValue()) { case 1 -> sub++; case 2 -> appr++; case 3 -> rej++; default -> draft++; }
                }
                default -> { }
            }
        }
        t.put("employees", emps.size()); t.put("dcrs", dcrN); t.put("absent", absent); t.put("holidays", holiday); t.put("weeklyOff", off); t.put("calls", calls);
        t.put("pob", pobCents / 100.0); t.put("expense", expCents / 100.0); t.put("web", web); t.put("app", app);
        t.put("draft", draft); t.put("submitted", sub); t.put("approved", appr); t.put("rejected", rej);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", f.from().toString()); out.put("to", f.to().toString()); out.put("rows", rows); out.put("totals", t);
        return out;
    }

    // =========================================================================================
    // one day, complete
    // =========================================================================================

    private static String s(Object o) { return o == null ? null : o.toString(); }

    private void own(String empId) {
        if (!empId.equals(TeamScope.me())) scope.require(TeamScope.Scope.TEAM_SELF, empId, "This employee is not in your team");
    }

    /** Everything of one DCR on one page: day facts, every call (listed / unlisted) with its items, leaflets, treatment and joint work, expenses, day end, approval. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> day(Long dcrId) {
        List<Map<String, Object>> ps = jdbc.queryForList("""
                SELECT p.*, e.emp_name AS empName, e.emp_code AS empCode, dg.designation AS designationName, h.hq_name AS hqName, st.state_name AS stateName, mg.emp_name AS managerName,
                       sa.area_name AS startAreaName, wa.area_name AS workingAreaName, da.area_name AS dayEndAreaName, ya.area_name AS stayAreaName, ap.emp_name AS approvedByName,
                       (SELECT display_name FROM app_dcr_worktype w WHERE w.emp_id IS NULL AND w.worktype_code = p.work_type LIMIT 1) AS workTypeName
                FROM dcr_parent_summary p JOIN emp_detail e ON e.emp_id = p.emp_id LEFT JOIN designation_master dg ON dg.id = e.designation
                LEFT JOIN hq_master h ON h.oid = e.hq_id LEFT JOIN state_master st ON st.oid = e.state_id LEFT JOIN emp_detail mg ON mg.emp_id = p.manager_id
                LEFT JOIN area_master sa ON sa.oid = p.start_area_id LEFT JOIN area_master wa ON wa.oid = p.working_area_id LEFT JOIN area_master da ON da.oid = p.day_end_area_id
                LEFT JOIN area_master ya ON ya.oid = p.stay_area_id LEFT JOIN emp_detail ap ON ap.emp_id = p.approved_by
                WHERE p.dcr_id = ?""", dcrId);
        if (ps.isEmpty()) throw new IllegalStateException("DCR not found");
        Map<String, Object> p = ps.get(0);
        own((String) p.get("emp_id"));
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("dcrId", dcrId); d.put("date", s(p.get("dcr_date")).substring(0, 10)); d.put("empId", p.get("emp_id")); d.put("empName", p.get("empName")); d.put("empCode", p.get("empCode"));
        d.put("designation", p.get("designationName")); d.put("hq", p.get("hqName")); d.put("state", p.get("stateName")); d.put("manager", p.get("managerName"));
        d.put("workType", p.get("workTypeName") != null ? p.get("workTypeName") : p.get("work_type"));
        d.put("timeIn", s(p.get("time_in"))); d.put("timeOut", s(p.get("time_out"))); d.put("startArea", p.get("startAreaName")); d.put("areaType", p.get("start_area_type")); d.put("workingArea", p.get("workingAreaName"));
        d.put("travelMode", p.get("travel_mode")); d.put("meterStart", p.get("meter_start")); d.put("meterEnd", p.get("meter_end")); d.put("remarks", p.get("remarks"));
        d.put("dayEndArea", p.get("dayEndAreaName")); d.put("dayEndAt", s(p.get("day_end_at"))); d.put("dayEndRemarks", p.get("day_end_remarks")); d.put("stayArea", p.get("stayAreaName")); d.put("stayHotel", p.get("stay_hotel"));
        int st = ((Number) p.get("status")).intValue();
        d.put("status", st); d.put("statusName", STATUS[Math.max(0, Math.min(3, st))]); d.put("source", ((Number) p.get("submitted_through")).intValue() == 1 ? "App" : "Web");
        d.put("submittedAt", s(p.get("submitted_at"))); d.put("approvedBy", p.get("approvedByName")); d.put("approvedAt", s(p.get("approved_at"))); d.put("approvalRemarks", p.get("approval_remarks"));
        d.put("pob", p.get("pob_total")); d.put("expense", p.get("expense_total")); d.put("listedCalls", p.get("listed_calls")); d.put("unlistedCalls", p.get("unlisted_calls"));
        d.put("startLat", p.get("start_lat")); d.put("startLng", p.get("start_lng")); d.put("startAddress", p.get("start_address"));   // where the day started (null before migration 111 / when not captured)
        boolean geoCols = !jdbc.queryForList("SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkin_address'").isEmpty();
        out.put("day", d);

        // items / leaflets / treatment / joint, grouped by (kind, ref)
        Map<String, List<Map<String, Object>>> items = group("""
                SELECT x.client_kind AS k, x.client_ref_id AS r, i.item_name AS name, t.item_type_name AS type, x.qty, x.pob_qty AS pobQty, x.rate, x.amount, x.is_detailed AS detailed
                FROM dcr_client_sample x JOIN item_master i ON i.id = x.item_id LEFT JOIN item_type_master t ON t.id = i.item_type_id WHERE x.dcr_id = ? ORDER BY i.item_name""", dcrId);
        Map<String, List<Map<String, Object>>> leaflets = group("SELECT l.client_kind AS k, l.client_ref_id AS r, i.item_name AS name, l.qty FROM dcr_leaflets_summary l JOIN item_master i ON i.id = l.item_id WHERE l.dcr_id = ?", dcrId);
        Map<String, List<Map<String, Object>>> treat = group("SELECT x.client_kind AS k, x.client_ref_id AS r, t.treatment_name AS name FROM dcr_client_treatment x JOIN treatment_master t ON t.id = x.treatment_id WHERE x.dcr_id = ?", dcrId);
        Map<String, List<Map<String, Object>>> joint = group("SELECT j.client_kind AS k, j.client_ref_id AS r, CONCAT(e.emp_name, ' (', e.emp_code, ')') AS name, dg.designation FROM dcr_sm_visits j JOIN emp_detail e ON e.emp_id = j.with_emp_id LEFT JOIN designation_master dg ON dg.id = e.designation WHERE j.dcr_id = ?", dcrId);

        List<Map<String, Object>> calls = new ArrayList<>();
        for (Map<String, Object> c : jdbc.queryForList("""
                SELECT s.id, s.client_id AS clientId, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS name, c.client_code AS code, ct.type_name AS clientType, cat.category_name AS category, sp.speciality_name AS speciality,
                       ar.area_name AS area, c.mobile_number_1 AS mobile, s.worktype_code AS tab, s.visit_type AS visitType, s.activity_type AS activityType, s.in_time AS inTime, s.out_time AS outTime,
                       s.is_refused AS refused, s.pob_value AS pob, s.remarks, s.next_visit AS nextVisit, s.next_visit_priority AS priority, s.next_visit_remarks AS nextRemarks,
                       s.checkin_lat AS lat, s.checkin_lng AS lng%GEO%
                FROM dcr_client_summary s JOIN client_master c ON c.id = s.client_id LEFT JOIN client_type ct ON ct.oid = c.client_type_id LEFT JOIN category_master cat ON cat.oid = c.category_id
                LEFT JOIN speciality_master sp ON sp.oid = c.speciality_id LEFT JOIN area_master ar ON ar.oid = COALESCE(s.area_id, c.city_id)
                WHERE s.dcr_id = ? ORDER BY s.in_time, s.id""".replace("%GEO%", geoCols ? ", s.checkin_address AS address, s.checkin_distance_m AS distanceM" : ""), dcrId)) {
            c.put("kind", "Listed"); attach(c, "1|" + c.get("clientId"), items, leaflets, treat, joint); c.remove("clientId"); calls.add(c);
        }
        for (Map<String, Object> c : jdbc.queryForList("""
                SELECT u.id, u.client_name AS name, ct.type_name AS clientType, cat.category_name AS category, sp.speciality_name AS speciality, ar.area_name AS area, u.mobile, u.qualification, u.contact_person AS contact, u.address,
                       u.worktype_code AS tab, u.visit_type AS visitType, u.activity_type AS activityType, u.in_time AS inTime, u.out_time AS outTime, u.is_refused AS refused, u.pob_value AS pob, u.remarks,
                       u.next_visit AS nextVisit, u.next_visit_priority AS priority, u.next_visit_remarks AS nextRemarks, u.checkin_lat AS lat, u.checkin_lng AS lng%GEO%
                FROM dcr_unlisted_client_summary u LEFT JOIN client_type ct ON ct.oid = u.client_type_id LEFT JOIN category_master cat ON cat.oid = u.category_id
                LEFT JOIN speciality_master sp ON sp.oid = u.speciality_id LEFT JOIN area_master ar ON ar.oid = u.area_id WHERE u.dcr_id = ? ORDER BY u.in_time, u.id""".replace("%GEO%", geoCols ? ", u.checkin_address AS address" : ""), dcrId)) {
            c.put("kind", "Unlisted"); attach(c, "2|" + c.get("id"), items, leaflets, treat, joint); calls.add(c);
        }
        calls.sort(Comparator.comparing(c -> String.valueOf(c.get("inTime"))));
        for (Map<String, Object> c : calls) { c.put("inTime", s(c.get("inTime"))); c.put("outTime", s(c.get("outTime"))); c.put("refused", flag(c.get("refused"))); c.put("nextVisit", s(c.get("nextVisit"))); }
        out.put("calls", calls);
        out.put("dayJoint", joint.getOrDefault("0|0", List.of()));
        out.put("expenses", jdbc.queryForList("""
                SELECT x.id, em.expense_name AS type, x.amount, x.description, fa.area_name AS fromArea, ta.area_name AS toArea, x.start_km AS startKm, x.end_km AS endKm, x.total_km AS totalKm, x.file_name AS fileName,
                       CASE x.client_kind WHEN 1 THEN (SELECT client_name FROM client_master WHERE id = x.client_ref_id) WHEN 2 THEN (SELECT client_name FROM dcr_unlisted_client_summary WHERE id = x.client_ref_id) END AS client
                FROM dcr_expense x JOIN expense_master em ON em.id = x.expense_id LEFT JOIN area_master fa ON fa.oid = x.from_area_id LEFT JOIN area_master ta ON ta.oid = x.to_area_id WHERE x.dcr_id = ? ORDER BY x.id""", dcrId));
        return out;
    }

    private static boolean flag(Object o) { return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() == 1; }

    private Map<String, List<Map<String, Object>>> group(String sql, Long dcrId) {
        Map<String, List<Map<String, Object>>> m = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(sql, dcrId)) {
            String key = r.get("k") + "|" + r.get("r");
            Map<String, Object> x = new LinkedHashMap<>(r); x.remove("k"); x.remove("r");
            if (x.containsKey("detailed")) x.put("detailed", flag(x.get("detailed")));
            m.computeIfAbsent(key, k -> new ArrayList<>()).add(x);
        }
        return m;
    }

    private static void attach(Map<String, Object> c, String key, Map<String, List<Map<String, Object>>> items, Map<String, List<Map<String, Object>>> leaflets,
                               Map<String, List<Map<String, Object>>> treat, Map<String, List<Map<String, Object>>> joint) {
        c.put("items", items.getOrDefault(key, List.of())); c.put("leaflets", leaflets.getOrDefault(key, List.of()));
        c.put("treatments", treat.getOrDefault(key, List.of()).stream().map(x -> x.get("name")).toList()); c.put("jointWith", joint.getOrDefault(key, List.of()));
    }

    // =========================================================================================
    // Excel
    // =========================================================================================

    @SuppressWarnings("unchecked")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public byte[] export(Filter in) throws IOException {
        Map<String, Object> sum = summary(in);
        Filter f = checked(in);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) sum.get("rows");
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CellStyle head = wb.createCellStyle(); Font bold = wb.createFont(); bold.setBold(true); head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex()); head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle rose = wb.createCellStyle(); rose.setFillForegroundColor(IndexedColors.ROSE.getIndex()); rose.setFillPattern(FillPatternType.SOLID_FOREGROUND);   // absent: a soft rose, not a harsh red
            CellStyle grey = wb.createCellStyle(); grey.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); grey.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet s1 = wb.createSheet("DCR Summary");
            String[] h1 = {"Date", "Employee code", "Employee", "Designation", "HQ", "State", "Day", "Work type", "Time in", "Time out", "Start area", "Area type", "Travel mode", "Listed calls", "Unlisted calls", "Total calls", "POB", "Expense", "Status", "Submitted from", "Submitted at", "Approved by", "Remarks"};
            headerRow(s1, h1, head);
            int r = 1;
            for (Map<String, Object> x : rows) {
                String kind = (String) x.get("kind");
                Row row = s1.createRow(r++);
                CellStyle sty = "ABSENT".equals(kind) ? rose : ("HOLIDAY".equals(kind) || "WEEKLY_OFF".equals(kind) || "NOT_STARTED".equals(kind)) ? grey : null;
                String[] vals = {s(x.get("date")), s(x.get("empCode")), s(x.get("empName")), s(x.get("designation")), s(x.get("hq")), s(x.get("state")),
                        java.time.LocalDate.parse(s(x.get("date"))).getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                        "DCR".equals(kind) ? s(x.get("workTypeName")) : label(kind, x), "DCR".equals(kind) ? s(x.get("timeIn")) : "", "DCR".equals(kind) ? s(x.get("timeOut")) : "",
                        s(x.get("startArea")), s(x.get("areaType")), s(x.get("travelMode"))};
                int c = 0;
                for (String v : vals) cell(row, c++, v, sty);
                if ("DCR".equals(kind)) {
                    num(row, c++, x.get("listedCalls"), sty); num(row, c++, x.get("unlistedCalls"), sty); num(row, c++, x.get("calls"), sty); num(row, c++, x.get("pob"), sty); num(row, c++, x.get("expense"), sty);
                    cell(row, c++, s(x.get("statusName")), sty); cell(row, c++, s(x.get("sourceName")), sty); cell(row, c++, s(x.get("submittedAt")), sty); cell(row, c++, s(x.get("approvedBy")), sty); cell(row, c, s(x.get("remarks")), sty);
                } else { for (; c < h1.length; c++) cell(row, c, "", sty); }
            }
            widths(s1, h1.length);

            // calls of the same period, one line per call with its items
            Sheet s2 = wb.createSheet("Calls");
            String[] h2 = {"Date", "Employee code", "Employee", "HQ", "Client", "Code / mobile", "Type", "Listed / Unlisted", "Category", "Speciality", "Area", "Visit type", "Activity", "In", "Out", "Refused", "POB", "Sample", "Input", "POB items", "Joint work", "Remarks", "Next visit"};
            headerRow(s2, h2, head);
            int r2 = 1;
            for (Map<String, Object> x : rows) {
                if (!"DCR".equals(x.get("kind"))) continue;
                Map<String, Object> det = day(((Number) x.get("dcrId")).longValue());
                for (Map<String, Object> c : (List<Map<String, Object>>) det.get("calls")) {
                    Row row = s2.createRow(r2++);
                    List<Map<String, Object>> its = (List<Map<String, Object>>) c.get("items");
                    int i = 0;
                    for (String v : new String[]{s(x.get("date")), s(x.get("empCode")), s(x.get("empName")), s(x.get("hq")), s(c.get("name")), s(c.get("code") != null ? c.get("code") : c.get("mobile")), s(c.get("clientType")), s(c.get("kind")),
                            s(c.get("category")), s(c.get("speciality")), s(c.get("area")), s(c.get("visitType")), s(c.get("activityType")), s(c.get("inTime")), s(c.get("outTime")), flag(c.get("refused")) ? "Yes" : ""}) cell(row, i++, v, null);
                    num(row, i++, c.get("pob"), null);
                    cell(row, i++, itemText(its, true, false), null); cell(row, i++, itemText(its, false, true), null); cell(row, i++, pobText(its), null);
                    cell(row, i++, String.join(", ", ((List<Map<String, Object>>) c.get("jointWith")).stream().map(j -> s(j.get("name"))).toList()), null);
                    cell(row, i++, s(c.get("remarks")), null); cell(row, i, s(c.get("nextVisit")), null);
                }
            }
            widths(s2, h2.length);

            Sheet s3 = wb.createSheet("Expenses");
            String[] h3 = {"Date", "Employee code", "Employee", "Expense", "Amount", "Description", "From", "To", "Km", "Client"};
            headerRow(s3, h3, head);
            int r3 = 1;
            for (Map<String, Object> x : rows) {
                if (!"DCR".equals(x.get("kind")) || ((Number) x.get("expense")).doubleValue() == 0) continue;
                for (Map<String, Object> e : (List<Map<String, Object>>) day(((Number) x.get("dcrId")).longValue()).get("expenses")) {
                    Row row = s3.createRow(r3++);
                    cell(row, 0, s(x.get("date")), null); cell(row, 1, s(x.get("empCode")), null); cell(row, 2, s(x.get("empName")), null); cell(row, 3, s(e.get("type")), null); num(row, 4, e.get("amount"), null);
                    cell(row, 5, s(e.get("description")), null); cell(row, 6, s(e.get("fromArea")), null); cell(row, 7, s(e.get("toArea")), null); num(row, 8, e.get("totalKm"), null); cell(row, 9, s(e.get("client")), null);
                }
            }
            widths(s3, h3.length);
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private static String itemText(List<Map<String, Object>> its, boolean sample, boolean input) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> i : its) {
            String t = String.valueOf(i.get("type")).toLowerCase();
            double q = i.get("qty") == null ? 0 : ((Number) i.get("qty")).doubleValue();
            if (q <= 0) continue;
            if ((sample && t.contains("sample")) || (input && t.contains("input"))) out.add(i.get("name") + " x" + (q == Math.floor(q) ? String.valueOf((long) q) : String.valueOf(q)));
        }
        return String.join(", ", out);
    }

    private static String pobText(List<Map<String, Object>> its) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> i : its) { double q = i.get("pobQty") == null ? 0 : ((Number) i.get("pobQty")).doubleValue(); if (q > 0) out.add(i.get("name") + " x" + (q == Math.floor(q) ? String.valueOf((long) q) : String.valueOf(q))); }
        return String.join(", ", out);
    }

    private static String label(String kind, Map<String, Object> x) {
        return switch (kind) { case "ABSENT" -> "ABSENT"; case "HOLIDAY" -> "Holiday: " + x.get("note"); case "WEEKLY_OFF" -> "Weekly off"; default -> "Not started yet"; };
    }

    private static void headerRow(Sheet s, String[] h, CellStyle st) { Row r = s.createRow(0); for (int i = 0; i < h.length; i++) { Cell c = r.createCell(i); c.setCellValue(h[i]); c.setCellStyle(st); } s.createFreezePane(0, 1); }
    private static void cell(Row r, int i, String v, CellStyle st) { Cell c = r.createCell(i); c.setCellValue(v == null ? "" : v); if (st != null) c.setCellStyle(st); }
    private static void num(Row r, int i, Object v, CellStyle st) { Cell c = r.createCell(i); if (v instanceof Number n) c.setCellValue(n.doubleValue()); else c.setCellValue(""); if (st != null) c.setCellStyle(st); }
    private static void widths(Sheet s, int n) { for (int i = 0; i < n; i++) s.setColumnWidth(i, i < 6 ? 5200 : 4200); }
}
