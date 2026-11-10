package in.opt.sfa.tenant.hr.payroll;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Salary hold for chosen states / HQs / designations / employees, from a month to a month (or until released). Payroll puts matching slips on hold when it generates. */
@Service
public class HoldService {

    private static final List<String> TYPES = List.of("STATE", "HQ", "DESIGNATION", "EMP");
    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;

    public HoldService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access) { this.jdbc = jdbc; this.audit = audit; this.access = access; }

    private static String me() { return UserContext.get() == null ? null : UserContext.get().empId(); }

    /** The reason of the first active hold that covers the employee in that YYYYMM, else null. */
    public String reasonFor(String empId, int period) {
        List<String> r = jdbc.queryForList("""
                SELECT h.reason FROM hr_salary_hold h JOIN emp_detail e ON e.emp_id = ?
                WHERE h.is_active = 1 AND h.from_period <= ? AND (h.to_period IS NULL OR h.to_period >= ?) AND EXISTS (SELECT 1 FROM hr_salary_hold_target t WHERE t.hold_id = h.id AND (
                      (t.target_type = 'STATE' AND t.target_value = CAST(e.state_id AS CHAR)) OR (t.target_type = 'HQ' AND t.target_value = CAST(e.hq_id AS CHAR))
                   OR (t.target_type = 'DESIGNATION' AND t.target_value = CAST(e.designation AS CHAR)) OR (t.target_type = 'EMP' AND t.target_value = e.emp_id)))
                ORDER BY h.id LIMIT 1""", String.class, empId, period, period);
        return r.isEmpty() ? null : r.get(0);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> config() {
        access.require("PAYROLL_PROCESS");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("states", jdbc.queryForList("SELECT oid AS id, state_name AS name FROM state_master ORDER BY state_name"));
        m.put("hqs", jdbc.queryForList("SELECT oid AS id, hq_name AS name FROM hq_master ORDER BY hq_name"));
        m.put("designations", jdbc.queryForList("SELECT id, designation AS name FROM designation_master ORDER BY designation"));
        m.put("employees", jdbc.queryForList("SELECT emp_id AS id, CONCAT(emp_name, ' (', emp_code, ')') AS name FROM emp_detail WHERE is_active = 1 AND COALESCE(is_vacant,0) = 0 ORDER BY emp_name"));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() {
        access.require("PAYROLL_PROCESS");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, from_period AS fromPeriod, to_period AS toPeriod, reason, is_active AS active, created_at AS createdAt, released_at AS releasedAt FROM hr_salary_hold ORDER BY id DESC");
        for (Map<String, Object> r : rows) r.put("targets", jdbc.queryForList("""
                SELECT t.target_type AS targetType, t.target_value AS targetValue, CASE t.target_type WHEN 'STATE' THEN (SELECT state_name FROM state_master WHERE CAST(oid AS CHAR) = t.target_value)
                  WHEN 'HQ' THEN (SELECT hq_name FROM hq_master WHERE CAST(oid AS CHAR) = t.target_value) WHEN 'DESIGNATION' THEN (SELECT designation FROM designation_master WHERE CAST(id AS CHAR) = t.target_value)
                  ELSE (SELECT CONCAT(emp_name, ' (', emp_code, ')') FROM emp_detail WHERE emp_id = t.target_value) END AS targetName FROM hr_salary_hold_target t WHERE t.hold_id = ?""", r.get("id")));
        return rows;
    }

    @SuppressWarnings("unchecked")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> create(Map<String, Object> f) {
        access.require("PAYROLL_PROCESS");
        int from = period(f.get("fromPeriod"), true), to = f.get("toPeriod") == null || f.get("toPeriod").toString().isBlank() ? 0 : period(f.get("toPeriod"), false);
        if (to != 0 && to < from) throw new IllegalArgumentException("'To' month is before the 'From' month");
        String reason = f.get("reason") == null ? "" : f.get("reason").toString().trim();
        if (reason.isEmpty()) throw new IllegalArgumentException("Please give a reason for the hold");
        Map<String, Object> t = (Map<String, Object>) f.getOrDefault("targets", Map.of());
        List<String[]> targets = new ArrayList<>();
        for (String type : TYPES) for (Object v : (List<Object>) t.getOrDefault(type, List.of())) if (v != null && !v.toString().isBlank() && !"null".equalsIgnoreCase(v.toString())) targets.add(new String[]{type, v.toString()});
        if (targets.isEmpty()) throw new IllegalArgumentException("Choose at least one state, HQ, designation or employee");
        jdbc.update("INSERT INTO hr_salary_hold (from_period, to_period, reason, created_by) VALUES (?,?,?,?)", from, to == 0 ? null : to, reason, me());
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (String[] x : targets) jdbc.update("INSERT IGNORE INTO hr_salary_hold_target (hold_id, target_type, target_value) VALUES (?,?,?)", id, x[0], x[1]);
        // a draft month already generated: put the covered slips on hold now
        int slips = jdbc.update("""
                UPDATE hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id AND r.status = 'DRAFT' AND r.period_year * 100 + r.period_month >= ? AND (? = 0 OR r.period_year * 100 + r.period_month <= ?)
                JOIN emp_detail e ON e.emp_id = s.emp_id SET s.status = 'HELD', s.hold_reason = ?
                WHERE s.status <> 'HELD' AND EXISTS (SELECT 1 FROM hr_salary_hold_target t WHERE t.hold_id = ? AND (
                      (t.target_type = 'STATE' AND t.target_value = CAST(e.state_id AS CHAR)) OR (t.target_type = 'HQ' AND t.target_value = CAST(e.hq_id AS CHAR))
                   OR (t.target_type = 'DESIGNATION' AND t.target_value = CAST(e.designation AS CHAR)) OR (t.target_type = 'EMP' AND t.target_value = e.emp_id)))""", from, to, to, reason, id);
        audit.log("SALARY_HOLD", id, null, "CREATE", "hold", null, targets.size() + " target(s) from " + from + (to == 0 ? "" : " to " + to), reason);
        return Map.of("id", id, "heldNow", slips);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void release(long id, String reason) {
        access.require("PAYROLL_PROCESS");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please give a reason");
        if (jdbc.update("UPDATE hr_salary_hold SET is_active = 0, released_by = ?, released_at = NOW() WHERE id = ? AND is_active = 1", me(), id) == 0) throw new IllegalStateException("This hold is already released");
        audit.log("SALARY_HOLD", id, null, "UPDATE", "hold", "ACTIVE", "RELEASED", reason);
    }

    private static int period(Object o, boolean required) {
        String s = o == null ? "" : o.toString().trim();      // "2026-11" from a month input, or 202611
        if (s.isEmpty()) { if (required) throw new IllegalArgumentException("Choose the month the hold starts"); return 0; }
        s = s.replace("-", "");
        if (!s.matches("\\d{6}") || Integer.parseInt(s.substring(4)) < 1 || Integer.parseInt(s.substring(4)) > 12) throw new IllegalArgumentException("Month looks wrong: " + o);
        return Integer.parseInt(s);
    }
}
