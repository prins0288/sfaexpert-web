package in.opt.sfa.tenant.expense;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.mail.MailService;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Expense dashboard: every employee of the month with the state of his statement (not submitted / submitted / approved ...), for the admin (all) and the managers (their team); reminders by mail. */
@Service
public class ExpenseDashboardService {
    private final JdbcTemplate jdbc;
    private final TeamScope scope;
    private final ExpenseService expense;
    private final MailService mail;

    public ExpenseDashboardService(JdbcTemplate jdbc, TeamScope scope, ExpenseService expense, MailService mail) { this.jdbc = jdbc; this.scope = scope; this.expense = expense; this.mail = mail; }

    private static String monthName(int m) { return java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH); }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> dashboard(int month, int year) {
        if (!TeamScope.isAdmin() && !TeamScope.isManager()) throw new ForbiddenException("The expense dashboard is for the admin and the managers");
        YearMonth ym; try { ym = YearMonth.of(year, month); } catch (Exception e) { throw new IllegalArgumentException("Choose a valid month and year"); }
        LocalDate first = ym.atDay(1), last = ym.atEndOfMonth();
        List<Map<String, Object>> all = jdbc.queryForList("""
                SELECT e.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS empName, e.date_of_joining AS doj, sm.state_name AS state, hq.hq_name AS hq, mg.emp_name AS manager, e.official_email AS email,
                       s.id AS statementId, s.status, COALESCE(s.total_amount, 0) AS total,
                       (SELECT COUNT(*) FROM expense_statement_day d WHERE d.statement_id = s.id AND d.row_kind = 'WORK') AS workDays,
                       (SELECT COALESCE(SUM(d.visits), 0) FROM expense_statement_day d WHERE d.statement_id = s.id) AS visits,
                       (SELECT COALESCE(SUM(d.da), 0) FROM expense_statement_day d WHERE d.statement_id = s.id) AS da
                FROM emp_detail e LEFT JOIN state_master sm ON sm.oid = e.state_id LEFT JOIN hq_master hq ON hq.oid = e.hq_id LEFT JOIN emp_detail mg ON mg.emp_id = e.manager_id
                     LEFT JOIN expense_statement s ON s.emp_id = e.emp_id AND s.exp_month = ? AND s.exp_year = ?
                WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND (e.date_of_joining IS NULL OR e.date_of_joining <= ?) AND (e.last_working_date IS NULL OR e.last_working_date >= ?)
                ORDER BY e.emp_name""", month, year, last, first);
        List<Map<String, Object>> rows = new ArrayList<>(scope.filter(TeamScope.Scope.TEAM_SELF, all, r -> (String) r.get("empId")));
        BigDecimal claims = BigDecimal.ZERO; int pending = 0, notSub = 0, approved = 0;
        for (Map<String, Object> r : rows) {
            String st = (String) r.get("status"); String key = st == null ? "NOT_STARTED" : st; r.put("state", r.get("state")); r.put("status", key);
            Map<String, Object> p = expense.perms((String) r.get("empId"), st == null ? "DRAFT" : st);
            r.put("canApprove", st != null && Boolean.TRUE.equals(p.get("canApprove"))); r.put("canReject", st != null && Boolean.TRUE.equals(p.get("canReject"))); r.put("approveAs", p.get("approveAs"));
            claims = claims.add((BigDecimal) r.get("total"));
            switch (key) { case "SUBMITTED", "MGR_APPROVED" -> pending++; case "APPROVED" -> approved++; default -> notSub++; }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", rows); out.put("employees", rows.size()); out.put("pending", pending); out.put("notSubmitted", notSub); out.put("approved", approved); out.put("claims", claims);
        out.put("mailReady", safeReady());
        return out;
    }
    private boolean safeReady() { try { return mail.ready(); } catch (RuntimeException e) { return false; } }

    private void log(String emp, int month, int year, String kind, String to, boolean ok, String err) {
        try { jdbc.update("INSERT INTO expense_reminder_log (emp_id, exp_month, exp_year, kind, sent_to, ok, error, sent_by) VALUES (?,?,?,?,?,?,?,?)", emp, month, year, kind, to, ok ? 1 : 0, err == null ? null : err.substring(0, Math.min(290, err.length())), TeamScope.me()); }
        catch (org.springframework.dao.DataAccessException e) { /* migration 133 not run */ }
    }

    /** Mail the employees whose expense is not submitted (kind NOT_SUBMITTED), or the managers who have submitted statements waiting (kind PENDING). Only people of the caller's scope. */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> remind(int month, int year, String kind, List<String> empIds) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) dashboard(month, year).get("rows");
        if (!mail.ready()) throw new IllegalStateException("The mail server is not set up or not switched on - fill it in Expense Setup > Email (SMTP)");
        Set<String> only = empIds == null || empIds.isEmpty() ? null : new HashSet<>(empIds);
        String period = monthName(month) + " " + year; int sent = 0, noEmail = 0, failed = 0; List<String> errors = new ArrayList<>();
        if ("PENDING".equals(kind)) {
            Map<String, List<Map<String, Object>>> byMgr = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) if ("SUBMITTED".equals(r.get("status")) && (only == null || only.contains(r.get("empId")))) { String m = jdbc.queryForList("SELECT manager_id FROM emp_detail WHERE emp_id = ?", String.class, r.get("empId")).stream().findFirst().orElse(null); if (m != null) byMgr.computeIfAbsent(m, k -> new ArrayList<>()).add(r); }
            for (Map.Entry<String, List<Map<String, Object>>> e : byMgr.entrySet()) {
                Map<String, Object> m = jdbc.queryForList("SELECT emp_name, official_email FROM emp_detail WHERE emp_id = ?", e.getKey()).stream().findFirst().orElse(null);
                String to = m == null ? null : (String) m.get("official_email"); if (to == null || to.isBlank()) { noEmail++; continue; }
                String list = String.join("", e.getValue().stream().map(r -> "<li>" + esc(r.get("empName")) + " (" + esc(r.get("empCode")) + ")</li>").toList());
                try { mail.send(to, "Expense approval pending - " + period, "<p>Dear " + esc(m.get("emp_name")) + ",</p><p>The expense statements of <b>" + period + "</b> of the following employees are waiting for your approval. Please review and approve them as soon as possible.</p><ul>" + list + "</ul><p>Regards,<br>SFA</p>"); sent++; log(e.getKey(), month, year, kind, to, true, null); }
                catch (Exception ex) { failed++; errors.add(msg(ex)); log(e.getKey(), month, year, kind, to, false, msg(ex)); }
            }
        } else {
            for (Map<String, Object> r : rows) {
                String st = (String) r.get("status"); if (!List.of("NOT_STARTED", "DRAFT", "REJECTED").contains(st) || (only != null && !only.contains(r.get("empId")))) continue;
                String to = (String) r.get("email"); if (to == null || to.isBlank()) { noEmail++; continue; }
                try { mail.send(to, "Expense pending - please submit (" + period + ")", "<p>Dear " + esc(r.get("empName")) + ",</p><p>Your expense statement of <b>" + period + "</b> is still <b>pending</b>" + ("REJECTED".equals(st) ? " (it was rejected - please correct it and submit again)" : "") + ". Please submit it as soon as possible so that it can be approved.</p><p>Regards,<br>SFA</p>"); sent++; log((String) r.get("empId"), month, year, "NOT_SUBMITTED", to, true, null); }
                catch (Exception ex) { failed++; errors.add(msg(ex)); log((String) r.get("empId"), month, year, "NOT_SUBMITTED", to, false, msg(ex)); }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>(); out.put("sent", sent); out.put("noEmail", noEmail); out.put("failed", failed); out.put("error", errors.isEmpty() ? null : errors.get(0)); return out;
    }
    private static String msg(Exception e) { Throwable t = e.getCause() != null ? e.getCause() : e; return String.valueOf(t.getMessage()); }
    private static String esc(Object o) { return o == null ? "" : o.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
