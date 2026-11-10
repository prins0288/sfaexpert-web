package in.opt.sfa.tenant.hr.attendance;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import in.opt.sfa.tenant.hr.leave.LeaveService;
import in.opt.sfa.tenant.tp.service.HolidayService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** HR / admin marks attendance (a present day) or leave for several employees and dates in one go, with a reason - for people who do not submit their own. Advance dates are blocked. */
@Service
public class AttendanceMarkService {

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;
    private final LeaveService leaves;
    private final HolidayService holidays;

    private final TransactionTemplate tx;

    public AttendanceMarkService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access, LeaveService leaves, HolidayService holidays, @Qualifier("tenantTransactionManager") PlatformTransactionManager tm) {
        this.jdbc = jdbc; this.audit = audit; this.access = access; this.leaves = leaves; this.holidays = holidays; this.tx = new TransactionTemplate(tm);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> employees(String q) {
        access.require("ATTENDANCE_ADMIN");
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        return jdbc.queryForList("""
                SELECT e.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS name, d.designation, h.hq_name AS hq, s.state_name AS state
                FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation LEFT JOIN hq_master h ON h.oid = e.hq_id LEFT JOIN state_master s ON s.oid = e.state_id
                WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND (e.emp_name LIKE ? OR e.emp_code LIKE ?) ORDER BY e.emp_name LIMIT 500""", like, like);
    }

    /** One result row per employee (and date for attendance): DONE or SKIPPED with the reason, so a partly failed batch is clear. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> mark(Map<String, Object> f) {
        String mode = String.valueOf(f.getOrDefault("mode", ""));
        if (!List.of("ATTENDANCE", "LEAVE").contains(mode)) throw new IllegalArgumentException("Choose Attendance or Leave");
        access.require("ATTENDANCE".equals(mode) ? "ATTENDANCE_ADMIN" : "LEAVE_ADMIN");
        String reason = f.get("reason") == null ? "" : f.get("reason").toString().trim();
        if (reason.isEmpty()) throw new IllegalArgumentException("Please give the reason");
        LocalDate from = LocalDate.parse(String.valueOf(f.get("from"))), to = f.get("to") == null || f.get("to").toString().isBlank() ? from : LocalDate.parse(f.get("to").toString());
        if (to.isBefore(from)) throw new IllegalArgumentException("'To' date is before the 'From' date");
        if (to.isAfter(LocalDate.now())) throw new IllegalArgumentException("Advance dates are blocked - attendance / leave can be marked only up to today");
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 31) throw new IllegalArgumentException("Choose at most 32 days at a time");
        List<String> emps = ((List<Object>) f.getOrDefault("empIds", List.of())).stream().map(Object::toString).distinct().toList();
        if (emps.isEmpty()) throw new IllegalArgumentException("Choose at least one employee");
        if (emps.size() > 500) throw new IllegalArgumentException("Choose at most 500 employees at a time");
        List<Map<String, Object>> out = new ArrayList<>();
        for (String emp : emps) {
            Map<String, Object> e = jdbc.queryForList("SELECT emp_name, emp_code, designation, manager_id, date_of_joining, last_working_date FROM emp_detail WHERE emp_id = ?", emp).stream().findFirst().orElse(null);
            if (e == null) { out.add(row(emp, "-", null, "SKIPPED", "Employee not found")); continue; }
            String label = e.get("emp_name") + " (" + e.get("emp_code") + ")";
            try {
                tx.executeWithoutResult(st -> {   // one transaction per employee: a failure rolls back only that employee
                if ("LEAVE".equals(mode)) {
                    Map<String, Object> r = leaves.applyByAdmin(emp, Long.parseLong(String.valueOf(f.get("leaveTypeId"))), from, to, f.get("dayPart") == null ? "FULL" : f.get("dayPart").toString(), reason, !Boolean.FALSE.equals(f.get("shortfallAsLwp")));
                    out.add(row(emp, label, from + (to.equals(from) ? "" : " to " + to), "DONE", r.get("days") + " day(s) of leave" + (new java.math.BigDecimal(r.get("lwpDays").toString()).signum() > 0 ? ", " + r.get("lwpDays") + " as loss of pay" : "")));
                   
                } else {
                    Set<LocalDate> off = new HashSet<>();
                    holidays.forEmployee(emp, from, to).keySet().forEach(k -> off.add(LocalDate.parse(k)));
                    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                        String why = skipReason(emp, e, d, off);
                        if (why != null) { out.add(row(emp, label, d.toString(), "SKIPPED", why)); continue; }
                        markPresent(emp, e, d, reason);
                        out.add(row(emp, label, d.toString(), "DONE", "Attendance marked"));
                    }
                }
                });
            } catch (RuntimeException ex) { out.add(row(emp, label, from.toString(), "SKIPPED", ex.getMessage())); }
        }
        long done = out.stream().filter(r -> "DONE".equals(r.get("status"))).count(), skipped = out.size() - done;
        return Map.of("done", done, "skipped", skipped, "rows", out);
    }

    private String skipReason(String emp, Map<String, Object> e, LocalDate d, Set<LocalDate> off) {
        if (e.get("date_of_joining") != null && ((java.sql.Date) e.get("date_of_joining")).toLocalDate().isAfter(d)) return "Not joined yet on this date";
        if (e.get("last_working_date") != null && ((java.sql.Date) e.get("last_working_date")).toLocalDate().isBefore(d)) return "After the last working day";
        if (off.contains(d)) return "Holiday / weekly off - no attendance needed";
        String wt = jdbc.queryForList("SELECT work_type FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date = ? AND work_type NOT IN ('HOLIDAY', 'WEEKLY_OFF')", String.class, emp, d).stream().findFirst().orElse(null);
        if (wt != null) return "A DCR (" + wt + ") already exists for this date";
        return null;
    }

    private void markPresent(String emp, Map<String, Object> e, LocalDate d, String reason) {
        jdbc.update("DELETE FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date = ? AND work_type IN ('HOLIDAY', 'WEEKLY_OFF')", emp, d);
        String me = UserContext.get() == null ? null : UserContext.get().empId();
        String remarks = "Marked by HR: " + reason;
        jdbc.update("INSERT INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through, approved_by, approved_at, approval_remarks) VALUES (?,?, 'ATTENDANCE', ?, ?, ?, 2, NOW(), 2, ?, NOW(), 'Marked by HR')",
                emp, d, e.get("designation"), e.get("manager_id"), remarks.length() > 190 ? remarks.substring(0, 190) : remarks, me);
        audit.log("ATTENDANCE", d.toString(), emp, "CREATE", "attendance", null, "PRESENT (marked by HR)", reason);
    }

    private static Map<String, Object> row(String emp, String label, String date, String status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("empId", emp); m.put("employee", label); m.put("date", date); m.put("status", status); m.put("message", message);
        return m;
    }
}
