package in.opt.sfa.tenant.dcr.view.service;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Attendance Report: the sheet of every employee x every date of the period (what the DCR says, judged by the attendance rules), each employee's summary
 * and the employees who submitted no DCR at all. Days before the joining date, after the last working day and in the future are blank.
 * Built on {@link DcrReportService#summary} (same scope: self / team / all, holidays, weekly offs), so the two reports always agree.
 */
@Service
public class AttendanceReportService {

    private final JdbcTemplate jdbc;
    private final DcrReportService dcr;

    public AttendanceReportService(JdbcTemplate jdbc, DcrReportService dcr) { this.jdbc = jdbc; this.dcr = dcr; }

    private static String s(Object o) { return o == null ? null : o.toString(); }
    private static LocalDate day(Object o) { return o == null ? null : LocalDate.parse(o.toString().substring(0, 10)); }
    private static boolean flag(Object o) { return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() == 1; }

    // ---- rules --------------------------------------------------------------------------------------------------------------------------
    private AttendanceEval.Rule rule(Map<String, Object> r) {
        Set<String> types = new HashSet<>();
        for (String t : String.valueOf(r.get("apply_types")).split(",")) if (!t.isBlank()) types.add(t.trim().toUpperCase());
        return new AttendanceEval.Rule(types, ((Number) r.get("min_calls")).intValue(), s(r.get("calls_result")), flag(r.get("count_unlisted")), (BigDecimal) r.get("min_hours"), s(r.get("hours_result")),
                time(r.get("late_start_after")), s(r.get("late_result")), time(r.get("early_end_before")), s(r.get("early_result")), flag(r.get("need_approved")));
    }

    private static LocalTime time(Object o) { return o == null ? null : LocalTime.parse(o.toString().substring(0, 5)); }
    private static Integer asInt(Object o) { return o == null ? null : ((Number) o).intValue(); }

    // ---- the sheet ----------------------------------------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> sheet(DcrReportService.Filter in, String empStatus) {
        Map<String, Object> sum = dcr.summary(new DcrReportService.Filter(in.from(), in.to(), in.stateId(), in.hqId(), in.designationId(), in.empId(), null, null, true));
        LocalDate from = day(sum.get("from")), to = day(sum.get("to"));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) sum.get("rows");
        // employees found in the rows, with joining date / status / designation id / mobile
        List<String> ids = rows.stream().map(r -> s(r.get("empId"))).distinct().toList();
        Map<String, Map<String, Object>> meta = new HashMap<>();
        if (!ids.isEmpty()) {
            String q = String.join(",", Collections.nCopies(ids.size(), "?"));
            for (Map<String, Object> m : jdbc.queryForList("SELECT emp_id, date_of_joining, last_working_date, is_active, designation AS designation_id, mobile FROM emp_detail WHERE emp_id IN (" + q + ")", ids.toArray())) meta.put(s(m.get("emp_id")), m);
        }
        // rules: the designation's own rule wins over the general one
        Map<Integer, AttendanceEval.Rule> byDes = new HashMap<>();
        AttendanceEval.Rule general = AttendanceEval.Rule.none();
        try {
            for (Map<String, Object> r : jdbc.queryForList("SELECT * FROM attendance_rule WHERE is_active = 1 ORDER BY id")) {
                if (r.get("designation_id") == null) general = rule(r); else byDes.put(((Number) r.get("designation_id")).intValue(), rule(r));
            }
        } catch (org.springframework.dao.DataAccessException e) { /* migration 113 not run yet: no condition */ }
        Map<String, String> shortCodes = new LinkedHashMap<>();
        List<Map<String, Object>> legend = new ArrayList<>();
        try {
            for (Map<String, Object> w : jdbc.queryForList("SELECT worktype_code AS code, display_name AS name, short_code AS sc FROM app_dcr_worktype WHERE emp_id IS NULL AND is_enabled = 1 AND row_type IN ('wd', 'attn') ORDER BY sort_order, id")) {
                String code = s(w.get("code")), sc = w.get("sc") == null || s(w.get("sc")).isBlank() ? code.replace("_", "").substring(0, Math.min(3, code.replace("_", "").length())).toUpperCase() : s(w.get("sc"));
                shortCodes.put(code, sc); legend.add(Map.of("code", sc, "name", w.get("name")));
            }
        } catch (org.springframework.dao.DataAccessException e) { /* short_code column missing */ }

        List<String> dates = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) dates.add(d.toString());
        Map<String, List<Map<String, Object>>> byEmp = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) byEmp.computeIfAbsent(s(r.get("empId")), k -> new ArrayList<>()).add(r);

        List<Map<String, Object>> emps = new ArrayList<>(), notSubmitted = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : byEmp.entrySet()) {
            Map<String, Object> first = e.getValue().get(0), m = meta.getOrDefault(e.getKey(), Map.of());
            boolean active = flag(m.get("is_active"));
            if ("ACTIVE".equalsIgnoreCase(empStatus) && !active) continue;
            if ("RESIGNED".equalsIgnoreCase(empStatus) && active) continue;
            Integer desId = asInt(m.get("designation_id"));
            AttendanceEval.Rule rule = desId != null && byDes.containsKey(desId) ? byDes.get(desId) : general;
            Map<String, Map<String, Object>> cells = new HashMap<>();
            double present = 0, half = 0, leave = 0, off = 0, absent = 0, calls = 0, workMin = 0; boolean anyDcr = false;
            for (Map<String, Object> r : e.getValue()) {
                String date = s(r.get("date")), kind = s(r.get("kind")), wt = s(r.get("workType"));
                Map<String, Object> c = new LinkedHashMap<>();
                if ("DCR".equals(kind)) {
                    if ("HOLIDAY".equals(wt) || "WEEKLY_OFF".equals(wt)) { c.put("s", "HOLIDAY".equals(wt) ? "HL" : "WK"); off++; }
                    else if ("LEAVE".equals(wt) && ((Number) r.get("status")).intValue() >= 1) { c.put("s", "L"); c.put("t", shortCodes.getOrDefault("LEAVE", "L")); c.put("id", r.get("dcrId")); leave++; anyDcr = true; }
                    else {
                        anyDcr = true;
                        AttendanceEval.Outcome o = AttendanceEval.evaluate(new AttendanceEval.Day(((Number) r.get("status")).intValue(), wt, ((Number) r.get("listedCalls")).intValue(), ((Number) r.get("unlistedCalls")).intValue(),
                                asInt(r.get("workMin")), time(blankNull(r.get("dayStart"))), time(blankNull(r.get("dayEnd")))), rule);
                        c.put("s", o.result() == AttendanceEval.Result.PRESENT ? "P" : o.result() == AttendanceEval.Result.HALF ? "HD" : "A");
                        c.put("t", shortCodes.getOrDefault(wt, wt == null ? "" : wt.substring(0, Math.min(3, wt.length())).toUpperCase())); c.put("c", r.get("calls")); c.put("h", r.get("workHours")); c.put("id", r.get("dcrId")); c.put("in", r.get("dayStart")); c.put("out", r.get("dayEnd")); c.put("n", r.get("workTypeName"));
                        if (!o.reasons().isEmpty()) c.put("why", String.join("; ", o.reasons()));
                        switch (o.result()) { case PRESENT -> present++; case HALF -> half++; default -> absent++; }
                        calls += ((Number) r.get("calls")).doubleValue(); if (r.get("workMin") != null) workMin += ((Number) r.get("workMin")).doubleValue();
                    }
                } else if ("ABSENT".equals(kind)) { c.put("s", "A"); absent++; }
                else if ("HOLIDAY".equals(kind)) { c.put("s", "HL"); c.put("why", s(r.get("note"))); off++; }
                else if ("WEEKLY_OFF".equals(kind)) { c.put("s", "WK"); off++; }
                else continue;   // NOT_STARTED (today): blank
                cells.put(date, c);
            }
            List<Object> row = new ArrayList<>();
            for (String d : dates) row.add(cells.get(d));
            Map<String, Object> emp = new LinkedHashMap<>();
            emp.put("empCode", first.get("empCode")); emp.put("name", first.get("empName")); emp.put("designation", first.get("designation")); emp.put("hq", first.get("hq")); emp.put("state", first.get("state"));
            emp.put("doj", s(m.get("date_of_joining")) == null ? null : s(m.get("date_of_joining")).substring(0, 10)); emp.put("status", active ? "Active" : "Resigned"); emp.put("mobile", m.get("mobile"));
            emp.put("cells", row);
            emp.put("sum", Map.of("present", present, "half", half, "leave", leave, "off", off, "absent", absent + half * 0.5, "absentFull", absent, "calls", calls, "workMin", workMin));
            emps.add(emp);
            if (!anyDcr) notSubmitted.add(Map.of("empCode", first.get("empCode"), "name", first.get("empName"), "designation", s(first.get("designation")) == null ? "" : s(first.get("designation")), "hq", s(first.get("hq")) == null ? "" : s(first.get("hq")), "mobile", m.get("mobile") == null ? "" : m.get("mobile")));
        }
        emps.sort(Comparator.comparing(x -> String.valueOf(x.get("name"))));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from.toString()); out.put("to", to.toString()); out.put("dates", dates); out.put("employees", emps); out.put("notSubmitted", notSubmitted); out.put("legend", legend);
        return out;
    }

    private static Object blankNull(Object o) { return o == null || o.toString().isBlank() ? null : o; }

    // ---- rules (admin) ------------------------------------------------------------------------------------------------------------------
    private void admin() { if (!TeamScope.isAdmin()) throw new ForbiddenException("Only an admin can change the attendance rules"); }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> rules() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rules", jdbc.queryForList("""
                SELECT r.id, r.rule_name AS ruleName, r.designation_id AS designationId, d.designation AS designation, r.apply_types AS applyTypes, r.min_calls AS minCalls, r.calls_result AS callsResult, r.count_unlisted AS countUnlisted,
                       r.min_hours AS minHours, r.hours_result AS hoursResult, TIME_FORMAT(r.late_start_after, '%H:%i') AS lateStartAfter, r.late_result AS lateResult, TIME_FORMAT(r.early_end_before, '%H:%i') AS earlyEndBefore,
                       r.early_result AS earlyResult, r.need_approved AS needApproved, r.is_active AS active
                FROM attendance_rule r LEFT JOIN designation_master d ON d.id = r.designation_id ORDER BY (r.designation_id IS NOT NULL), d.designation, r.id"""));
        m.put("designations", jdbc.queryForList("SELECT id, designation AS name FROM designation_master ORDER BY hierarchy_level, designation"));
        m.put("workTypes", jdbc.queryForList("SELECT worktype_code AS code, display_name AS name FROM app_dcr_worktype WHERE emp_id IS NULL AND is_enabled = 1 AND row_type IN ('wd', 'attn') ORDER BY sort_order, id"));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveRule(Map<String, Object> f) {
        admin();
        String name = f.get("ruleName") == null || f.get("ruleName").toString().isBlank() ? "Rule" : f.get("ruleName").toString().trim();
        Integer des = f.get("designationId") == null || f.get("designationId").toString().isBlank() ? null : Integer.valueOf(f.get("designationId").toString());
        String types = String.valueOf(f.getOrDefault("applyTypes", "WORKING")).trim();
        if (types.isEmpty()) throw new IllegalArgumentException("Choose at least one work type the conditions apply to");
        int calls = f.get("minCalls") == null || f.get("minCalls").toString().isBlank() ? 0 : Integer.parseInt(f.get("minCalls").toString().split("\\.")[0]);
        BigDecimal hours = f.get("minHours") == null || f.get("minHours").toString().isBlank() ? BigDecimal.ZERO : new BigDecimal(f.get("minHours").toString());
        if (calls < 0 || hours.signum() < 0 || hours.compareTo(BigDecimal.valueOf(24)) > 0) throw new IllegalArgumentException("Calls cannot be negative and hours must be between 0 and 24");
        String cr = res(f.get("callsResult")), hr = res(f.get("hoursResult")), lr = res(f.get("lateResult")), er = res(f.get("earlyResult"));
        String late = tm(f.get("lateStartAfter")), early = tm(f.get("earlyEndBefore"));
        boolean active = !f.containsKey("active") || "true".equalsIgnoreCase(String.valueOf(f.get("active"))) || "1".equals(String.valueOf(f.get("active")));
        Long id = f.get("id") == null || f.get("id").toString().isBlank() ? null : Long.valueOf(f.get("id").toString());
        if (active && jdbc.queryForObject("SELECT COUNT(*) FROM attendance_rule WHERE is_active = 1 AND ((? IS NULL AND designation_id IS NULL) OR designation_id = ?) AND (? IS NULL OR id <> ?)", Integer.class, des, des, id, id) > 0)
            throw new IllegalArgumentException(des == null ? "There is already an active rule for all designations - edit it" : "There is already an active rule for this designation - edit it");
        String me = UserContext.get() == null ? null : UserContext.get().empId();
        boolean unl = !"false".equalsIgnoreCase(String.valueOf(f.get("countUnlisted"))) && !"0".equals(String.valueOf(f.get("countUnlisted"))), appr = "true".equalsIgnoreCase(String.valueOf(f.get("needApproved"))) || "1".equals(String.valueOf(f.get("needApproved")));
        if (id == null) {
            jdbc.update("INSERT INTO attendance_rule (rule_name, designation_id, apply_types, min_calls, calls_result, count_unlisted, min_hours, hours_result, late_start_after, late_result, early_end_before, early_result, need_approved, is_active, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    name, des, types, calls, cr, unl ? 1 : 0, hours, hr, late, lr, early, er, appr ? 1 : 0, active ? 1 : 0, me);
            return Map.of("id", jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        }
        jdbc.update("UPDATE attendance_rule SET rule_name=?, designation_id=?, apply_types=?, min_calls=?, calls_result=?, count_unlisted=?, min_hours=?, hours_result=?, late_start_after=?, late_result=?, early_end_before=?, early_result=?, need_approved=?, is_active=?, updated_by=?, updated_at=NOW() WHERE id=?",
                name, des, types, calls, cr, unl ? 1 : 0, hours, hr, late, lr, early, er, appr ? 1 : 0, active ? 1 : 0, me, id);
        return Map.of("id", id);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int deleteRules(List<Long> ids) {
        admin();
        int n = 0;
        for (Long id : ids) n += jdbc.update("DELETE FROM attendance_rule WHERE id = ?", id);
        return n;
    }

    private static String res(Object o) { String v = o == null ? "HALF" : o.toString().toUpperCase(); if (!List.of("ABSENT", "HALF").contains(v)) throw new IllegalArgumentException("Result must be Absent or Half day"); return v; }
    private static String tm(Object o) { if (o == null || o.toString().isBlank()) return null; LocalTime.parse(o.toString().substring(0, 5)); return o.toString().substring(0, 5) + ":00"; }
}
