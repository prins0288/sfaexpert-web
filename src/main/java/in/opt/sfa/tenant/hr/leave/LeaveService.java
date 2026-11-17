package in.opt.sfa.tenant.hr.leave;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.calc.LeaveRules;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import in.opt.sfa.tenant.team.TeamScope;
import in.opt.sfa.tenant.tp.service.HolidayService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Leave master (policy per type), balances, applications (from the DCR screen or My Leave), approval and manual adjustments. Rules live in {@link LeaveRules}. */
@Service
public class LeaveService {

    private static final String TYPE_COLS = "id, code, name, is_paid AS paid, annual_quota AS annualQuota, accrual, carry_forward_max AS carryForwardMax, allow_half_day AS allowHalfDay, max_consecutive AS maxConsecutive, "
            + "max_per_month AS maxPerMonth, min_notice_days AS minNoticeDays, backdate_days AS backdateDays, future_days AS futureDays, requires_reason AS requiresReason, sandwich, allow_negative AS allowNegative, "
            + "auto_approve AS autoApprove, min_service_months AS minServiceMonths, sort_order AS sortOrder, is_active AS active";
    private static final List<String> BOOLS = List.of("paid", "allowHalfDay", "requiresReason", "sandwich", "allowNegative", "autoApprove", "active");

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;
    private final HolidayService holidays;

    public LeaveService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access, HolidayService holidays) { this.jdbc = jdbc; this.audit = audit; this.access = access; this.holidays = holidays; }

    private static String me() { return TeamScope.me(); }
    private static BigDecimal bd(Object o) { return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString()); }
    private static boolean b(Object o) { return o instanceof Boolean x ? x : o != null && ("1".equals(o.toString()) || "true".equalsIgnoreCase(o.toString())); }

    // ---- leave master ------------------------------------------------------------------------------------------
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> types(boolean onlyActive) {
        return jdbc.queryForList("SELECT " + TYPE_COLS + " FROM hr_leave_type WHERE company_id = 1" + (onlyActive ? " AND is_active = 1" : "") + " ORDER BY sort_order, name");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveType(Map<String, Object> f) {
        access.require("LEAVE_ADMIN");
        String code = f.get("code") == null ? "" : f.get("code").toString().trim().toUpperCase(), name = f.get("name") == null ? "" : f.get("name").toString().trim();
        if (code.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("Code and name are required");
        if (!code.matches("[A-Z0-9_]{1,12}")) throw new IllegalArgumentException("Code: letters / digits, up to 12");
        String accrual = f.get("accrual") == null ? "UPFRONT" : f.get("accrual").toString();
        if (!List.of("UPFRONT", "MONTHLY").contains(accrual)) throw new IllegalArgumentException("Accrual must be UPFRONT or MONTHLY");
        BigDecimal quota = bd(f.get("annualQuota")), carry = bd(f.get("carryForwardMax")), perMonth = bd(f.get("maxPerMonth"));
        if (quota.signum() < 0 || carry.signum() < 0 || perMonth.signum() < 0) throw new IllegalArgumentException("Days cannot be negative");
        Object[] v = {code, name, b(f.get("paid")) ? 1 : 0, quota, accrual, carry, b(f.get("allowHalfDay")) ? 1 : 0, intOf(f.get("maxConsecutive")), perMonth, intOf(f.get("minNoticeDays")), intOf(f.get("backdateDays")),
                intOf(f.get("futureDays")), b(f.get("requiresReason")) ? 1 : 0, b(f.get("sandwich")) ? 1 : 0, b(f.get("allowNegative")) ? 1 : 0, b(f.get("autoApprove")) ? 1 : 0, intOf(f.get("minServiceMonths")),
                intOf(f.get("sortOrder")), f.containsKey("active") && !b(f.get("active")) ? 0 : 1};
        String sets = "code=?, name=?, is_paid=?, annual_quota=?, accrual=?, carry_forward_max=?, allow_half_day=?, max_consecutive=?, max_per_month=?, min_notice_days=?, backdate_days=?, future_days=?, requires_reason=?, sandwich=?, allow_negative=?, auto_approve=?, min_service_months=?, sort_order=?, is_active=?";
        try {
            if (f.get("id") == null || f.get("id").toString().isBlank()) {
                jdbc.update("INSERT INTO hr_leave_type SET company_id = 1, created_by = ?, " + sets, concat(new Object[]{me()}, v));
                Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                audit.log("LEAVE_TYPE", id, null, "CREATE", "type", null, code + " " + name, null);
                return Map.of("id", id);
            }
            long id = Long.parseLong(f.get("id").toString());
            Map<String, Object> before = jdbc.queryForMap("SELECT " + TYPE_COLS + " FROM hr_leave_type WHERE id = ?", id);
            jdbc.update("UPDATE hr_leave_type SET updated_by = ?, updated_at = NOW(), " + sets + " WHERE id = ?", concat(concat(new Object[]{me()}, v), new Object[]{id}));
            Map<String, Object> after = jdbc.queryForMap("SELECT " + TYPE_COLS + " FROM hr_leave_type WHERE id = ?", id);
            audit.logDiff("LEAVE_TYPE", id, null, "UPDATE", before, after, f.get("reason") == null ? null : f.get("reason").toString());
            return Map.of("id", id);
        } catch (org.springframework.dao.DuplicateKeyException e) { throw new IllegalArgumentException("That code already exists"); }
    }

    private static int intOf(Object o) { return o == null || o.toString().isBlank() ? 0 : Integer.parseInt(o.toString().split("\\.")[0]); }
    private static Object[] concat(Object[] a, Object[] c) { Object[] r = Arrays.copyOf(a, a.length + c.length); System.arraycopy(c, 0, r, a.length, c.length); return r; }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int deleteTypes(List<Long> ids) {
        access.require("LEAVE_ADMIN");
        int n = 0;
        for (Long id : ids) {
            String code = jdbc.queryForList("SELECT code FROM hr_leave_type WHERE id = ?", String.class, id).stream().findFirst().orElse(null);
            if (code == null) continue;
            if (jdbc.queryForObject("SELECT COUNT(*) FROM hr_leave_application WHERE leave_type_id = ?", Integer.class, id) > 0 || jdbc.queryForObject("SELECT COUNT(*) FROM hr_leave_adjustment WHERE leave_type_id = ?", Integer.class, id) > 0)
                throw new IllegalArgumentException(code + " has been used - switch it off instead of deleting");
            jdbc.update("DELETE FROM hr_leave_type WHERE id = ?", id);
            audit.log("LEAVE_TYPE", id, null, "DELETE", "type", code, null, null);
            n++;
        }
        return n;
    }

    // ---- balance --------------------------------------------------------------------------------------------------
    private LocalDate yearStart(LocalDate d) {
        Map<String, Object> c = jdbc.queryForList("SELECT fy_start_month, leave_year_mode FROM hr_company WHERE id = 1").stream().findFirst().orElse(Map.of("fy_start_month", 4, "leave_year_mode", "FINANCIAL"));
        return LeaveRules.yearStart(d, ((Number) c.get("fy_start_month")).intValue(), (String) c.get("leave_year_mode"));
    }

    private LocalDate joined(String empId) { List<java.sql.Date> l = jdbc.queryForList("SELECT date_of_joining FROM emp_detail WHERE emp_id = ?", java.sql.Date.class, empId); return l.isEmpty() || l.get(0) == null ? null : l.get(0).toLocalDate(); }

    private BigDecimal used(String empId, long typeId, LocalDate from, LocalDate to) {
        // days of PENDING + APPROVED applications that fall in [from, to] (an application is counted whole in the year its first day is in)
        return jdbc.queryForObject("SELECT COALESCE(SUM(days - lwp_days), 0) FROM hr_leave_application WHERE emp_id = ? AND leave_type_id = ? AND status IN ('PENDING','APPROVED') AND from_date BETWEEN ? AND ?", BigDecimal.class, empId, typeId, from, to);
    }

    private BigDecimal adjustments(String empId, long typeId, LocalDate ys) { return jdbc.queryForObject("SELECT COALESCE(SUM(days), 0) FROM hr_leave_adjustment WHERE emp_id = ? AND leave_type_id = ? AND leave_year = ?", BigDecimal.class, empId, typeId, ys.getYear()); }

    /** Balance of a type on a date: earned so far + carried in + adjustments - taken (pending counts as taken). */
    public Map<String, Object> balance(String empId, Map<String, Object> t, LocalDate on) {
        long typeId = ((Number) t.get("id")).longValue();
        LocalDate ys = yearStart(on), joined = joined(empId);
        BigDecimal quota = bd(t.get("annualQuota"));
        BigDecimal earned = LeaveRules.accrued(quota, (String) t.get("accrual"), ys, on, joined);
        BigDecimal carry = BigDecimal.ZERO;
        if (bd(t.get("carryForwardMax")).signum() > 0) {
            LocalDate prev = ys.minusYears(1);
            BigDecimal prevLeft = LeaveRules.accrued(quota, (String) t.get("accrual"), prev, ys.minusDays(1), joined).add(adjustments(empId, typeId, prev)).subtract(used(empId, typeId, prev, ys.minusDays(1)));
            carry = LeaveRules.carryForward(prevLeft, bd(t.get("carryForwardMax")));
        }
        BigDecimal adj = adjustments(empId, typeId, ys), taken = used(empId, typeId, ys, ys.plusYears(1).minusDays(1));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("typeId", typeId); m.put("code", t.get("code")); m.put("name", t.get("name")); m.put("earned", earned); m.put("carried", carry); m.put("adjusted", adj); m.put("taken", taken);
        m.put("balance", earned.add(carry).add(adj).subtract(taken)); m.put("paid", t.get("paid")); m.put("allowHalfDay", t.get("allowHalfDay")); m.put("requiresReason", t.get("requiresReason")); m.put("allowNegative", t.get("allowNegative"));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> balances(String empId, LocalDate on) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> t : types(true)) out.add(balance(empId, t, on));
        return out;
    }

    // ---- applying -----------------------------------------------------------------------------------------------
    private Map<String, Object> type(long id) { return jdbc.queryForList("SELECT " + TYPE_COLS + " FROM hr_leave_type WHERE id = ? AND is_active = 1", id).stream().findFirst().orElseThrow(() -> new IllegalStateException("Choose a leave type")); }

    private LeaveRules.Policy policy(Map<String, Object> t) {
        return new LeaveRules.Policy((String) t.get("code"), b(t.get("paid")), bd(t.get("annualQuota")), (String) t.get("accrual"), bd(t.get("carryForwardMax")), b(t.get("allowHalfDay")), intOf(t.get("maxConsecutive")), bd(t.get("maxPerMonth")),
                intOf(t.get("minNoticeDays")), intOf(t.get("backdateDays")), intOf(t.get("futureDays")), b(t.get("requiresReason")), b(t.get("sandwich")), b(t.get("allowNegative")), intOf(t.get("minServiceMonths")));
    }

    private LeaveRules.Result evaluate(String empId, long typeId, LocalDate from, LocalDate to, String part, String reason, Long ownDcrId) { return evaluate(empId, typeId, from, to, part, reason, ownDcrId, false, false); }

    /** {@code admin}: HR marks the leave for the employee - the notice / back-date / per-month / min-service / reason rules do not apply; {@code shortfallAsLwp}: days over the balance become loss of pay. */
    private LeaveRules.Result evaluate(String empId, long typeId, LocalDate from, LocalDate to, String part, String reason, Long ownDcrId, boolean admin, boolean shortfallAsLwp) {
        Map<String, Object> t = type(typeId);
        if (!"1".equals(String.valueOf(jdbc.queryForList("SELECT COALESCE((SELECT et.leave_applicable FROM hr_emp_employment h JOIN hr_employee_type et ON et.id = h.employee_type_id WHERE h.emp_id = ?), 1)", Integer.class, empId).get(0))))
            throw new IllegalStateException("Your employee type is not eligible for leave");
        Set<LocalDate> off = new HashSet<>();
        if (from != null && to != null && !to.isBefore(from) && java.time.temporal.ChronoUnit.DAYS.between(from, to) < 400)
            holidays.forEmployee(empId, from, to).keySet().forEach(k -> off.add(LocalDate.parse(k)));
        boolean overlap = from != null && to != null && (jdbc.queryForObject("SELECT COUNT(*) FROM hr_leave_application WHERE emp_id = ? AND status IN ('PENDING','APPROVED') AND from_date <= ? AND to_date >= ?", Integer.class, empId, to, from) > 0
                || jdbc.queryForObject("SELECT COUNT(*) FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date BETWEEN ? AND ? AND work_type <> 'LEAVE' AND (? IS NULL OR dcr_id <> ?)", Integer.class, empId, from, to, ownDcrId, ownDcrId) > 0);
        LocalDate ref = from == null ? LocalDate.now() : from;
        BigDecimal usedMonth = from == null ? BigDecimal.ZERO : jdbc.queryForObject("SELECT COALESCE(SUM(days), 0) FROM hr_leave_application WHERE emp_id = ? AND leave_type_id = ? AND status IN ('PENDING','APPROVED') AND YEAR(from_date) = ? AND MONTH(from_date) = ?",
                BigDecimal.class, empId, typeId, ref.getYear(), ref.getMonthValue());
        BigDecimal bal = (BigDecimal) balance(empId, t, ref).get("balance");
        LeaveRules.Policy pol = policy(t);
        if (admin) pol = new LeaveRules.Policy(pol.code(), pol.paid(), pol.quota(), pol.accrual(), pol.carryMax(), true, 0, BigDecimal.ZERO, 0, 36500, 0, false, pol.sandwich(), shortfallAsLwp || pol.allowNegative(), 0);
        return LeaveRules.check(pol, new LeaveRules.Request(from, to, part == null ? "FULL" : part, reason, LocalDate.now(), joined(empId), bal, usedMonth, off, overlap));
    }

    /** Throws with the policy messages when the request is not allowed; used by the DCR screen before it starts a leave day. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public LeaveRules.Result validate(String empId, long typeId, LocalDate from, LocalDate to, String part, String reason, Long ownDcrId) {
        LeaveRules.Result r = evaluate(empId, typeId, from, to, part, reason, ownDcrId);
        if (!r.ok()) throw new IllegalStateException(String.join(". ", r.errors()));
        return r;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> preview(long typeId, LocalDate from, LocalDate to, String part, String reason) {
        LeaveRules.Result r = evaluate(me(), typeId, from, to, part, reason, null);
        return Map.of("ok", r.ok(), "errors", r.errors(), "days", r.days(), "lwpDays", r.lwpDays());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> apply(String empId, long typeId, LocalDate from, LocalDate to, String part, String reason, String source, Long dcrId) {
        LeaveRules.Result r = validate(empId, typeId, from, to, part, reason, dcrId);
        Map<String, Object> t = type(typeId);
        String status = b(t.get("autoApprove")) ? "APPROVED" : "PENDING";
        jdbc.update("INSERT INTO hr_leave_application (emp_id, leave_type_id, from_date, to_date, day_part, days, lwp_days, reason, status, source, dcr_id, decided_at) VALUES (?,?,?,?,?,?,?,?,?,?,?, " + ("APPROVED".equals(status) ? "NOW()" : "NULL") + ")",
                empId, typeId, from, to, part == null ? "FULL" : part, r.days(), r.lwpDays(), reason == null || reason.isBlank() ? null : reason.trim(), status, source, dcrId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if ("APPROVED".equals(status)) createDcrDays(id);
        audit.log("LEAVE", id, empId, "CREATE", "application", null, t.get("code") + " " + from + (to.equals(from) ? "" : " to " + to) + " (" + r.days() + " d, " + status + ")", reason);
        return Map.of("id", id, "status", status, "days", r.days(), "lwpDays", r.lwpDays());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> applyMine(Map<String, Object> f) {
        return apply(me(), Long.parseLong(f.get("leaveTypeId").toString()), LocalDate.parse(f.get("fromDate").toString()), LocalDate.parse(f.get("toDate") == null || f.get("toDate").toString().isBlank() ? f.get("fromDate").toString() : f.get("toDate").toString()),
                f.get("dayPart") == null ? "FULL" : f.get("dayPart").toString(), f.get("reason") == null ? null : f.get("reason").toString(), "WEB", null);
    }



    /** HR / admin marks a leave for an employee who did not apply (approved at once; LEAVE DCR of every working day is made). Advance dates are blocked. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> applyByAdmin(String empId, long typeId, LocalDate from, LocalDate to, String part, String reason, boolean shortfallAsLwp) {
        access.require("LEAVE_ADMIN");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please give a reason");
        if (to.isAfter(LocalDate.now())) throw new IllegalStateException("Advance dates are blocked - leave can be marked only up to today");
        if (empId.equals(me())) throw new ForbiddenException("You cannot mark your own leave");
        LeaveRules.Result r = evaluate(empId, typeId, from, to, part, reason, null, true, shortfallAsLwp);
        if (!r.ok()) throw new IllegalStateException(String.join(". ", r.errors()));
        Map<String, Object> t = type(typeId);
        jdbc.update("INSERT INTO hr_leave_application (emp_id, leave_type_id, from_date, to_date, day_part, days, lwp_days, reason, status, source, decided_by, decided_at, decision_remarks) VALUES (?,?,?,?,?,?,?,?, 'APPROVED', 'ADMIN', ?, NOW(), ?)",
                empId, typeId, from, to, part == null ? "FULL" : part, r.days(), r.lwpDays(), reason.trim(), me(), "Marked by HR");
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        int made = createDcrDays(id);
        audit.log("LEAVE", id, empId, "CREATE", "application", null, t.get("code") + " " + from + (to.equals(from) ? "" : " to " + to) + " (" + r.days() + " d, marked by HR, " + made + " DCR day(s))", reason);
        return Map.of("id", id, "days", r.days(), "lwpDays", r.lwpDays());
    }

    // ---- DCR rows of an approved leave ---------------------------------------------------------------------------
    private static String tag(long appId) { return "LEAVE-APP-" + appId; }

    /** Approved leave => a LEAVE DCR (approved) for every working day of it, so reports show leave, not absent. Existing DCRs are left alone. */
    private int createDcrDays(long appId) {
        Map<String, Object> a = jdbc.queryForList("SELECT a.emp_id, a.from_date, a.to_date, a.dcr_id, t.code FROM hr_leave_application a JOIN hr_leave_type t ON t.id = a.leave_type_id WHERE a.id = ?", appId).get(0);
        String emp = (String) a.get("emp_id");
        LocalDate from = ((java.sql.Date) a.get("from_date")).toLocalDate(), to = ((java.sql.Date) a.get("to_date")).toLocalDate();
        if (a.get("dcr_id") != null)   // the leave day started from the DCR screen: approve it too
            jdbc.update("UPDATE dcr_parent_summary SET status = 2, submitted_at = COALESCE(submitted_at, NOW()), approved_by = ?, approved_at = NOW(), approval_remarks = ? WHERE dcr_id = ? AND work_type = 'LEAVE'", me(), "Leave approved", a.get("dcr_id"));
        Set<LocalDate> off = new HashSet<>();
        holidays.forEmployee(emp, from, to).keySet().forEach(k -> off.add(LocalDate.parse(k)));
        Map<String, Object> e = jdbc.queryForList("SELECT designation, manager_id FROM emp_detail WHERE emp_id = ?", emp).get(0);
        int made = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (off.contains(d)) continue;
            if (jdbc.queryForObject("SELECT COUNT(*) FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date = ?", Integer.class, emp, d) > 0) continue;
            jdbc.update("INSERT INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, remarks, status, submitted_at, submitted_through, approved_by, approved_at, approval_remarks) VALUES (?,?, 'LEAVE', ?, ?, ?, 2, NOW(), 2, ?, NOW(), 'Leave approved')",
                    emp, d, e.get("designation"), e.get("manager_id"), tag(appId) + " " + a.get("code"), me());
            made++;
        }
        return made;
    }

    /** Leave rejected / cancelled => the LEAVE DCRs made for it go away (a draft leave day from the DCR screen too). */
    private void removeDcrDays(long appId) {
        List<Long> ids = jdbc.queryForList("SELECT dcr_id FROM dcr_parent_summary WHERE work_type = 'LEAVE' AND (remarks LIKE ? OR dcr_id = (SELECT dcr_id FROM hr_leave_application WHERE id = ?))", Long.class, tag(appId) + "%", appId);
        for (Long id : ids) {
            if (id == null) continue;
            for (String t : List.of("dcr_client_sample", "dcr_leaflets_summary", "dcr_client_treatment", "dcr_sm_visits", "dcr_expense", "dcr_area_summary", "dcr_client_summary", "dcr_unlisted_client_summary"))
                jdbc.update("DELETE FROM " + t + " WHERE dcr_id = ?", id);
            jdbc.update("DELETE FROM dcr_parent_summary WHERE dcr_id = ?", id);
        }
    }

    // ---- lists / decisions ---------------------------------------------------------------------------------------
    private static final String APP_SEL = "SELECT a.id, a.emp_id AS empId, e.emp_name AS empName, e.emp_code AS empCode, t.code AS typeCode, t.name AS typeName, a.from_date AS fromDate, a.to_date AS toDate, a.day_part AS dayPart, a.days, a.lwp_days AS lwpDays, "
            + "a.reason, a.status, a.source, a.applied_at AS appliedAt, d.emp_name AS decidedByName, a.decided_at AS decidedAt, a.decision_remarks AS remarks FROM hr_leave_application a JOIN hr_leave_type t ON t.id = a.leave_type_id "
            + "JOIN emp_detail e ON e.emp_id = a.emp_id LEFT JOIN emp_detail d ON d.emp_id = a.decided_by ";

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> mine() {
        LocalDate today = LocalDate.now();
        return Map.of("balances", balances(me(), today), "applications", jdbc.queryForList(APP_SEL + "WHERE a.emp_id = ? ORDER BY a.from_date DESC, a.id DESC LIMIT 100", me()), "types", types(true));
    }

    private boolean canDecide(String applicant) {
        if (applicant.equals(me())) return false;   // never decide your own leave
        if (TeamScope.isAdmin() || access.has("LEAVE_ADMIN")) return true;
        return jdbc.queryForObject("SELECT COUNT(*) FROM emp_detail e LEFT JOIN hr_emp_employment h ON h.emp_id = e.emp_id WHERE e.emp_id = ? AND (e.manager_id = ? OR h.second_manager_id = ?)", Integer.class, applicant, me(), me()) > 0;
    }

    /** Applications waiting for me (my team's; an admin sees everyone's). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> toApprove(boolean all) {
        List<Map<String, Object>> rows = jdbc.queryForList(APP_SEL + (all ? "" : "WHERE a.status = 'PENDING' ") + "ORDER BY a.from_date DESC LIMIT 300");
        rows.removeIf(r -> !canDecide((String) r.get("empId")));
        return rows;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void decide(long id, boolean approve, String remarks) {
        Map<String, Object> a = jdbc.queryForList("SELECT emp_id, status, from_date FROM hr_leave_application WHERE id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!canDecide((String) a.get("emp_id"))) throw new ForbiddenException("You cannot decide this leave");
        if (!"PENDING".equals(a.get("status"))) throw new IllegalStateException("Already " + a.get("status").toString().toLowerCase());
        if (!approve && (remarks == null || remarks.isBlank())) throw new IllegalArgumentException("Please give a reason for rejecting");
        jdbc.update("UPDATE hr_leave_application SET status = ?, decided_by = ?, decided_at = NOW(), decision_remarks = ? WHERE id = ?", approve ? "APPROVED" : "REJECTED", me(), remarks, id);
        int made = approve ? createDcrDays(id) : 0;
        if (!approve) removeDcrDays(id);
        audit.log("LEAVE", id, (String) a.get("emp_id"), "UPDATE", "status", "PENDING", approve ? "APPROVED (" + made + " DCR day(s) made)" : "REJECTED", remarks);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void cancel(long id, String reason) {
        Map<String, Object> a = jdbc.queryForList("SELECT emp_id, status, from_date, dcr_id FROM hr_leave_application WHERE id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Application not found"));
        boolean mine = a.get("emp_id").equals(me());
        if (!mine && !(TeamScope.isAdmin() || access.has("LEAVE_ADMIN"))) throw new ForbiddenException("Only the employee or HR can cancel");
        if (!List.of("PENDING", "APPROVED").contains(a.get("status"))) throw new IllegalStateException("Already " + a.get("status").toString().toLowerCase());
        if (a.get("dcr_id") != null) throw new IllegalStateException("This leave is part of a DCR day - delete that DCR to cancel it");
        if (mine && "APPROVED".equals(a.get("status")) && ((java.sql.Date) a.get("from_date")).toLocalDate().isBefore(LocalDate.now())) throw new IllegalStateException("An approved leave of a past date can be cancelled only by HR");
        jdbc.update("UPDATE hr_leave_application SET status = 'CANCELLED', decided_by = ?, decided_at = NOW(), decision_remarks = ? WHERE id = ?", me(), reason, id);
        removeDcrDays(id);
        audit.log("LEAVE", id, (String) a.get("emp_id"), "UPDATE", "status", (String) a.get("status"), "CANCELLED", reason);
    }

    /** Called by the DCR screen when a leave DCR is deleted. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void cancelForDcr(long dcrId) {
        for (Map<String, Object> a : jdbc.queryForList("SELECT id, emp_id, status FROM hr_leave_application WHERE dcr_id = ? AND status IN ('PENDING','APPROVED')", dcrId)) {
            jdbc.update("UPDATE hr_leave_application SET status = 'CANCELLED', decided_by = ?, decided_at = NOW(), decision_remarks = 'DCR deleted' WHERE id = ?", me(), a.get("id"));
            removeDcrDays(((Number) a.get("id")).longValue());
            audit.log("LEAVE", a.get("id"), (String) a.get("emp_id"), "UPDATE", "status", (String) a.get("status"), "CANCELLED", "DCR deleted");
        }
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void adjust(String empId, long typeId, BigDecimal days, String reason) {
        access.require("LEAVE_ADMIN");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Reason is required");
        if (days == null || days.signum() == 0) throw new IllegalArgumentException("Days cannot be zero");
        LocalDate ys = yearStart(LocalDate.now());
        jdbc.update("INSERT INTO hr_leave_adjustment (emp_id, leave_type_id, leave_year, days, reason, by_emp_id) VALUES (?,?,?,?,?,?)", empId, typeId, ys.getYear(), days, reason, me());
        audit.log("LEAVE_BALANCE", typeId, empId, "UPDATE", "adjustment", null, days.toPlainString(), reason);
    }
}
