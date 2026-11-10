package in.opt.sfa.tenant.hr.payroll;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.calc.AttendanceCalc;
import in.opt.sfa.tenant.hr.calc.RoleResolver;
import in.opt.sfa.tenant.hr.calc.SalaryCalc;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import in.opt.sfa.tenant.hr.setting.HrSettingService;
import in.opt.sfa.tenant.tp.service.HolidayService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Monthly payroll: generate payslips from the salary + days + leave, hold / release, approve (maker-checker), pay; employees read their own slips. */
@Service
public class PayrollService {

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;
    private final HrSettingService settings;
    private final HolidayService holidays;
    private final HoldService holds;

    public PayrollService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access, HrSettingService settings, HolidayService holidays, HoldService holds) {
        this.jdbc = jdbc; this.audit = audit; this.access = access; this.settings = settings; this.holidays = holidays; this.holds = holds;
    }

    private static String me() { return UserContext.get() == null ? null : UserContext.get().empId(); }
    private static BigDecimal bd(Object o) { return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString()); }
    private static boolean b(Object o) { return o instanceof Boolean x ? x : o != null && ("1".equals(o.toString()) || "true".equalsIgnoreCase(o.toString())); }

    static SalaryCalc.Head head(Map<String, Object> h) {
        return new SalaryCalc.Head((String) h.get("code"), (String) h.get("name"), (String) h.get("headType"), (String) h.get("calcType"), (String) h.get("calcBase"), b(h.get("prorata")), b(h.get("pfWage")), b(h.get("esiWage")), ((Number) h.get("sortOrder")).intValue());
    }

    static SalaryCalc.Params params(HrSettingService s, LocalDate on) {
        java.util.function.Function<String, BigDecimal> d = k -> s.decimal(k, on).orElseThrow(() -> new IllegalStateException("Setting " + k + " has no value on " + on + " - set it in HR Settings"));
        return new SalaryCalc.Params(d.apply("PF_WAGE_CEILING"), d.apply("PF_EMPLOYEE_RATE"), d.apply("PF_EMPLOYER_EPF_RATE"), d.apply("PF_EMPLOYER_EPS_RATE"), d.apply("PF_EDLI_RATE"), d.apply("PF_ADMIN_RATE"),
                d.apply("ESI_WAGE_LIMIT"), d.apply("ESI_EMPLOYEE_RATE"), d.apply("ESI_EMPLOYER_RATE"), s.text("MONEY_ROUNDING", on).orElse("NEAREST"));
    }

    // ---- loss of pay (attendance from the DCR) ---------------------------------------------------------------------
    /** What the month's attendance says about an employee: loss-of-pay days with a note, and whether any DCR was submitted at all. */
    record Attendance(BigDecimal lop, String note, boolean hasDcr) { }

    Attendance attendanceFor(String empId, YearMonth ym, Map<String, Object> emp) {
        LocalDate first = ym.atDay(1), last = ym.atEndOfMonth();
        Set<LocalDate> off = new HashSet<>();
        holidays.forEmployee(empId, first, last).keySet().forEach(k -> off.add(LocalDate.parse(k)));
        // days of leave: approved applications, per day, with the paid / unpaid kind of the leave type
        Map<LocalDate, AttendanceCalc.LeaveDay> leave = new HashMap<>();
        BigDecimal extraLwp = BigDecimal.ZERO;
        for (Map<String, Object> a : jdbc.queryForList("SELECT a.from_date, a.to_date, a.day_part, a.lwp_days, t.is_paid, t.sandwich FROM hr_leave_application a JOIN hr_leave_type t ON t.id = a.leave_type_id WHERE a.emp_id = ? AND a.status = 'APPROVED' AND a.from_date <= ? AND a.to_date >= ?", empId, last, first)) {
            LocalDate f = ((java.sql.Date) a.get("from_date")).toLocalDate(), t = ((java.sql.Date) a.get("to_date")).toLocalDate();
            boolean paid = b(a.get("is_paid")), sandwich = b(a.get("sandwich")), half = !"FULL".equals(a.get("day_part"));
            LocalDate firstWork = null, lastWork = null;
            for (LocalDate d = f; !d.isAfter(t); d = d.plusDays(1)) if (!off.contains(d)) { if (firstWork == null) firstWork = d; lastWork = d; }
            for (LocalDate d = f.isBefore(first) ? first : f; !d.isAfter(t.isAfter(last) ? last : t); d = d.plusDays(1)) {
                boolean working = !off.contains(d);
                boolean counted = working || (sandwich && firstWork != null && !d.isBefore(firstWork) && !d.isAfter(lastWork));
                if (counted) leave.put(d, new AttendanceCalc.LeaveDay(half ? new BigDecimal("0.5") : BigDecimal.ONE, paid));
            }
            if (paid && bd(a.get("lwp_days")).signum() > 0 && !f.isBefore(first)) extraLwp = extraLwp.add(bd(a.get("lwp_days")));   // the unpaid part sits at the start month of the application
        }
        // days with a submitted / approved DCR (a day of leave is not "worked")
        Set<LocalDate> worked = new HashSet<>();
        boolean any = false;
        for (Map<String, Object> r : jdbc.queryForList("SELECT dcr_date, work_type FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date BETWEEN ? AND ? AND status IN (1, 2)", empId, first, last)) {
            String wt = String.valueOf(r.get("work_type"));
            if ("HOLIDAY".equals(wt) || "WEEKLY_OFF".equals(wt)) continue;   // the automatic holiday / weekly-off rows are not work
            any = true;
            if (!"LEAVE".equals(r.get("work_type"))) worked.add(((java.sql.Date) r.get("dcr_date")).toLocalDate());
        }
        boolean useDcr = settings.bool("ATTENDANCE_FROM_DCR", last, true) && !b(emp.get("office"));
        LocalDate join = emp.get("doj") == null ? null : ((java.sql.Date) emp.get("doj")).toLocalDate();
        LocalDate lw = emp.get("lwd") == null ? null : ((java.sql.Date) emp.get("lwd")).toLocalDate();
        AttendanceCalc.Result r = AttendanceCalc.compute(new AttendanceCalc.Input(first, last, LocalDate.now(), off, join, lw, leave, worked, useDcr, extraLwp));
        return new Attendance(r.total().min(BigDecimal.valueOf(ym.lengthOfMonth())), r.note(), any);
    }

    // ---- generate ----------------------------------------------------------------------------------------------------
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> generate(int year, int month, boolean onlyMissing) {
        access.require("PAYROLL_PROCESS");
        YearMonth ym = YearMonth.of(year, month);
        if (ym.isAfter(YearMonth.now())) throw new IllegalArgumentException("Salary of a future month cannot be generated");
        Map<String, Object> run = jdbc.queryForList("SELECT id, status FROM hr_payroll_run WHERE company_id = 1 AND period_year = ? AND period_month = ?", year, month).stream().findFirst().orElse(null);
        long runId;
        if (run == null) {
            jdbc.update("INSERT INTO hr_payroll_run (period_year, period_month, status, generated_by, generated_at) VALUES (?,?, 'DRAFT', ?, NOW())", year, month, me());
            runId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            if ("PAID".equals(run.get("status"))) throw new IllegalStateException("This month is already paid - it cannot be generated again");
            runId = ((Number) run.get("id")).longValue();
            if ("DRAFT".equals(run.get("status")) && !onlyMissing) {   // a draft month is made again from scratch
                jdbc.update("DELETE FROM hr_payslip WHERE run_id = ?", runId);
                jdbc.update("UPDATE hr_payroll_run SET generated_by = ?, generated_at = NOW() WHERE id = ?", me(), runId);
            } else onlyMissing = true;   // an approved month: only the employees who have no slip yet are added; approved slips stay as they are
        }
        LocalDate first = ym.atDay(1), last = ym.atEndOfMonth();
        for (LocalDate d = first; !d.isAfter(last) && d.isBefore(LocalDate.now()); d = d.plusDays(1)) {   // catch up the automatic holiday / weekly-off DCRs of past days the 8 PM job may have missed
            try { jdbc.update("CALL sp_auto_holiday_dcr(?)", d); } catch (org.springframework.dao.DataAccessException e) { break; }   // migration 107 not run yet
        }
        SalaryCalc.Params p = params(settings, last);
        BigDecimal limitDisabled = settings.decimal("ESI_WAGE_LIMIT_DISABLED", last).orElse(p.esiLimit());
        int made = 0, noSalary = 0, held = 0, noDcr = 0;
        List<Map<String, Object>> emps = jdbc.queryForList("""
                SELECT e.emp_id, e.date_of_joining AS doj, e.last_working_date AS lwd, COALESCE(et.pf_applicable, 1) AS pf, COALESCE(et.esi_applicable, 1) AS esi, COALESCE(h.is_disabled, 0) AS disabled, COALESCE(e.is_office_staff, 0) AS office
                FROM emp_detail e LEFT JOIN hr_emp_employment h ON h.emp_id = e.emp_id LEFT JOIN hr_employee_type et ON et.id = h.employee_type_id
                WHERE COALESCE(e.is_vacant, 0) = 0 AND (e.is_active = 1 OR e.last_working_date >= ?) AND (e.date_of_joining IS NULL OR e.date_of_joining <= ?)""", first, last);
        for (Map<String, Object> e : emps) {
            String empId = (String) e.get("emp_id");
            Map<String, Object> sal = jdbc.queryForList("SELECT id, gross_monthly FROM hr_emp_salary WHERE emp_id = ? AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?) ORDER BY effective_from DESC LIMIT 1", empId, last, first).stream().findFirst().orElse(null);
            if (onlyMissing && jdbc.queryForObject("SELECT COUNT(*) FROM hr_payslip WHERE run_id = ? AND emp_id = ?", Integer.class, runId, empId) > 0) continue;
            if (sal == null) { noSalary++; continue; }
            Attendance att = attendanceFor(empId, ym, e);
            if (settings.bool("PAYROLL_REQUIRES_DCR", last, true) && !b(e.get("office")) && !att.hasDcr()) { noDcr++; continue; }   // no DCR submitted in the month: no salary
            if (insertSlip(runId, empId, ((Number) sal.get("id")).longValue(), bd(sal.get("gross_monthly")), ym, att.lop(), b(e.get("pf")), b(e.get("esi")),
                    new SalaryCalc.Params(p.pfCeiling(), p.pfEeRate(), p.pfErEpfRate(), p.pfErEpsRate(), p.pfEdliRate(), p.pfAdminRate(), b(e.get("disabled")) ? limitDisabled : p.esiLimit(), p.esiEeRate(), p.esiErRate(), p.rounding()), null)) held++;
            if (att.note() != null && !att.note().isEmpty()) jdbc.update("UPDATE hr_payslip SET lop_note = ? WHERE run_id = ? AND emp_id = ?", att.note(), runId, empId);
            made++;
        }
        if (made == 0 && onlyMissing) throw new IllegalStateException("Nothing to add: every employee who has a salary already has a slip in this month" + (noSalary > 0 ? " (" + noSalary + " employee(s) have no salary set)" : ""));
        if (made == 0) throw new IllegalStateException("No payslip could be made: " + noSalary + " employee(s) have no salary set and " + noDcr + " have no submitted DCR in the month. Set the salary in Employee Salary / get the DCRs submitted, then generate again");
        audit.log("PAYROLL", runId, null, "CREATE", "run", null, ym + ": " + made + " slips, " + held + " held, " + noSalary + " without salary, " + noDcr + " without a submitted DCR", null);
        return Map.of("runId", runId, "slips", made, "held", held, "withoutSalary", noSalary, "withoutDcr", noDcr);
    }

    /** Computes and stores one slip. Returns true when it was put on hold. */
    private boolean insertSlip(long runId, String empId, long salaryId, BigDecimal gross, YearMonth ym, BigDecimal lop, boolean pf, boolean esi, SalaryCalc.Params p, String keepHold) {
        List<SalaryCalc.Value> values = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT " + SalaryService.HEAD_COLS.replace("id,", "h.id,").replace(" code,", " h.code,").replace(" name,", " h.name,") + ", v.head_value AS hv FROM hr_emp_salary_head v JOIN hr_salary_head h ON h.id = v.head_id WHERE v.salary_id = ?", salaryId))
            values.add(new SalaryCalc.Value(head(r), bd(r.get("hv"))));
        SalaryCalc.Output o = SalaryCalc.compute(new SalaryCalc.Input(gross, values, ym.lengthOfMonth(), lop, pf, esi, p));
        String hold = keepHold != null ? keepHold : holds.reasonFor(empId, ym.getYear() * 100 + ym.getMonthValue());
        jdbc.update("INSERT INTO hr_payslip (run_id, generated_by, emp_id, salary_id, days_in_month, lop_days, paid_days, gross_earnings, total_deductions, net_pay, employer_cost, pf_wage, esi_wage, status, hold_reason) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                runId, me(), empId, salaryId, ym.lengthOfMonth(), lop, o.paidDays(), o.grossEarnings(), o.totalDeductions(), o.net(), o.employerCost(), o.pfWage(), o.esiWage(), hold == null ? "GENERATED" : "HELD", hold);
        long slipId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (SalaryCalc.Line l : o.lines()) jdbc.update("INSERT INTO hr_payslip_line (payslip_id, head_code, head_name, line_type, amount, sort_order) VALUES (?,?,?,?,?,?)", slipId, l.code(), l.name(), l.type(), l.amount(), l.sort());
        return hold != null;
    }

    // ---- runs and slips ----------------------------------------------------------------------------------------------
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> runs() {
        access.require("PAYROLL_REPORTS");
        return jdbc.queryForList("""
                SELECT r.id, r.period_year AS year, r.period_month AS month, r.status, r.generated_at AS generatedAt, g.emp_name AS generatedBy, r.approved_at AS approvedAt, a.emp_name AS approvedBy,
                       (SELECT COUNT(*) FROM hr_payslip s WHERE s.run_id = r.id) AS slips, (SELECT COUNT(*) FROM hr_payslip s WHERE s.run_id = r.id AND s.status = 'HELD') AS held, (SELECT COUNT(*) FROM hr_payslip s WHERE s.run_id = r.id AND s.approved_at IS NULL) AS pending,
                       (SELECT COALESCE(SUM(net_pay), 0) FROM hr_payslip s WHERE s.run_id = r.id) AS net, (SELECT COALESCE(SUM(net_pay), 0) FROM hr_payslip s WHERE s.run_id = r.id AND s.status = 'HELD') AS heldNet
                FROM hr_payroll_run r LEFT JOIN emp_detail g ON g.emp_id = r.generated_by LEFT JOIN emp_detail a ON a.emp_id = r.approved_by ORDER BY r.period_year DESC, r.period_month DESC""");
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> run(long id) {
        access.require("PAYROLL_REPORTS");
        Map<String, Object> run = jdbc.queryForList("SELECT id, period_year AS year, period_month AS month, status FROM hr_payroll_run WHERE id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Run not found"));
        run.put("slips", jdbc.queryForList("""
                SELECT s.id, s.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS name, d.designation, st.state_name AS state, h.hq_name AS hq, s.days_in_month AS daysInMonth, s.lop_days AS lopDays, s.lop_note AS lopNote, s.paid_days AS paidDays,
                       s.gross_earnings AS gross, s.total_deductions AS deductions, s.net_pay AS net, s.status, s.hold_reason AS holdReason, (s.approved_at IS NOT NULL) AS approved
                FROM hr_payslip s JOIN emp_detail e ON e.emp_id = s.emp_id LEFT JOIN designation_master d ON d.id = e.designation LEFT JOIN state_master st ON st.oid = e.state_id LEFT JOIN hq_master h ON h.oid = e.hq_id
                WHERE s.run_id = ? ORDER BY e.emp_name""", id));
        return run;
    }

    /** One slip with its lines and the employee / company facts the slip shows. Employees see only their own, released, approved slips. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> slip(long id) {
        Map<String, Object> s = jdbc.queryForList("""
                SELECT s.*, r.period_year AS py, r.period_month AS pm, r.status AS runStatus FROM hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id WHERE s.id = ?""", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Payslip not found"));
        boolean mine = s.get("emp_id").equals(me());
        boolean staff = access.has("PAYROLL_REPORTS") || access.has("SALARY_VIEW");
        if (!staff) {
            if (!mine) throw new ForbiddenException("This is not your payslip");
            if (s.get("approved_at") == null) throw new IllegalStateException("This month's salary is not approved yet");
            if ("HELD".equals(s.get("status"))) throw new IllegalStateException("Your salary of this month is on hold. Please contact HR");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id); out.put("year", s.get("py")); out.put("month", s.get("pm")); out.put("runStatus", s.get("runStatus")); out.put("status", s.get("status")); out.put("holdReason", s.get("hold_reason"));
        out.put("daysInMonth", s.get("days_in_month")); out.put("lopDays", s.get("lop_days")); out.put("lopNote", s.get("lop_note")); out.put("paidDays", s.get("paid_days")); out.put("gross", s.get("gross_earnings")); out.put("deductions", s.get("total_deductions")); out.put("net", s.get("net_pay")); out.put("employerCost", s.get("employer_cost"));
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT head_code AS code, head_name AS name, line_type AS type, amount FROM hr_payslip_line WHERE payslip_id = ? ORDER BY CASE line_type WHEN 'EARNING' THEN 0 WHEN 'DEDUCTION' THEN 1 ELSE 2 END, sort_order", id);
        // year-to-date (financial year up to this month, approved slips of the employee) per head
        int py = ((Number) s.get("py")).intValue(), pm = ((Number) s.get("pm")).intValue();
        int fyMonth = jdbc.queryForList("SELECT fy_start_month FROM hr_company WHERE id = 1", Integer.class).stream().findFirst().orElse(4);
        int fyStartYear = pm >= fyMonth ? py : py - 1;
        Map<String, BigDecimal> ytd = new HashMap<>();
        jdbc.query("""
                SELECT l.head_code, SUM(l.amount) FROM hr_payslip_line l JOIN hr_payslip x ON x.id = l.payslip_id JOIN hr_payroll_run r ON r.id = x.run_id
                WHERE x.emp_id = ? AND (x.approved_at IS NOT NULL OR x.id = ?) AND r.period_year * 12 + r.period_month BETWEEN ? AND ? GROUP BY l.head_code""",
                rs -> { ytd.put(rs.getString(1), rs.getBigDecimal(2)); }, s.get("emp_id"), id, fyStartYear * 12 + fyMonth, py * 12 + pm);
        for (Map<String, Object> l : lines) l.put("ytd", ytd.getOrDefault(l.get("code"), (BigDecimal) l.get("amount")));
        out.put("lines", lines);
        out.put("ytdGross", jdbc.queryForObject("SELECT COALESCE(SUM(x.gross_earnings), 0) FROM hr_payslip x JOIN hr_payroll_run r ON r.id = x.run_id WHERE x.emp_id = ? AND (x.approved_at IS NOT NULL OR x.id = ?) AND r.period_year * 12 + r.period_month BETWEEN ? AND ?", BigDecimal.class, s.get("emp_id"), id, fyStartYear * 12 + fyMonth, py * 12 + pm));
        out.put("ytdDeductions", jdbc.queryForObject("SELECT COALESCE(SUM(x.total_deductions), 0) FROM hr_payslip x JOIN hr_payroll_run r ON r.id = x.run_id WHERE x.emp_id = ? AND (x.approved_at IS NOT NULL OR x.id = ?) AND r.period_year * 12 + r.period_month BETWEEN ? AND ?", BigDecimal.class, s.get("emp_id"), id, fyStartYear * 12 + fyMonth, py * 12 + pm));
        out.put("fyLabel", fyStartYear + "-" + String.valueOf(fyStartYear + 1).substring(2));
        out.put("emp", jdbc.queryForList("""
                SELECT e.emp_code AS empCode, e.emp_name AS name, d.designation, e.date_of_joining AS doj, hq.hq_name AS hq, st.state_name AS state, dp.name AS department, br.name AS branch, gr.name AS grade,
                       sx.pan_no AS pan, sx.uan_no AS uan, sx.pf_no AS pfNo, sx.esi_no AS esiNo, bk.branch_name AS bank, bk.account_no AS accountNo, bk.ifsc_code AS ifsc
                FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation LEFT JOIN hq_master hq ON hq.oid = e.hq_id LEFT JOIN state_master st ON st.oid = e.state_id
                LEFT JOIN hr_emp_employment h ON h.emp_id = e.emp_id LEFT JOIN hr_department dp ON dp.id = h.department_id LEFT JOIN hr_branch br ON br.id = h.branch_id LEFT JOIN hr_grade gr ON gr.id = h.grade_id
                LEFT JOIN emp_statutory sx ON sx.emp_id = e.emp_id LEFT JOIN emp_bank_account bk ON bk.emp_id = e.emp_id AND bk.is_primary = 1 WHERE e.emp_id = ? LIMIT 1""", s.get("emp_id")).stream().findFirst().orElse(Map.of()));
        out.put("config", slipConfig());
        out.put("company", jdbc.queryForList("SELECT legal_name AS legalName, trade_name AS tradeName, address, pan, tan, pf_code AS pfCode, esi_code AS esiCode FROM hr_company WHERE id = 1").stream().findFirst().orElse(Map.of()));
        return out;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> mySlips() {
        return jdbc.queryForList("""
                SELECT s.id, r.period_year AS year, r.period_month AS month, s.net_pay AS net, s.status, r.status AS runStatus FROM hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id
                WHERE s.emp_id = ? AND s.approved_at IS NOT NULL ORDER BY r.period_year DESC, r.period_month DESC LIMIT 36""", me());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> setLop(long slipId, BigDecimal lop, String note) {
        access.require("PAYROLL_PROCESS");
        Map<String, Object> s = jdbc.queryForList("SELECT s.run_id, s.emp_id, s.salary_id, s.hold_reason, s.status, s.approved_at, r.status AS rs, r.period_year, r.period_month FROM hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id WHERE s.id = ?", slipId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Payslip not found"));
        if (s.get("approved_at") != null || "PAID".equals(s.get("rs"))) throw new IllegalStateException("An approved slip cannot be changed");
        if (lop == null || lop.signum() < 0) throw new IllegalArgumentException("Loss-of-pay days cannot be negative");
        if (note == null || note.isBlank()) throw new IllegalArgumentException("Please give a reason");
        YearMonth ym = YearMonth.of(((Number) s.get("period_year")).intValue(), ((Number) s.get("period_month")).intValue());
        if (lop.compareTo(BigDecimal.valueOf(ym.lengthOfMonth())) > 0) throw new IllegalArgumentException("More than the days of the month");
        String empId = (String) s.get("emp_id");
        Map<String, Object> e = jdbc.queryForMap("SELECT COALESCE(et.pf_applicable, 1) AS pf, COALESCE(et.esi_applicable, 1) AS esi, COALESCE(h.is_disabled, 0) AS disabled FROM emp_detail e LEFT JOIN hr_emp_employment h ON h.emp_id = e.emp_id LEFT JOIN hr_employee_type et ON et.id = h.employee_type_id WHERE e.emp_id = ?", empId);
        BigDecimal gross = jdbc.queryForObject("SELECT gross_monthly FROM hr_emp_salary WHERE id = ?", BigDecimal.class, s.get("salary_id"));
        SalaryCalc.Params p = params(settings, ym.atEndOfMonth());
        if (b(e.get("disabled"))) p = new SalaryCalc.Params(p.pfCeiling(), p.pfEeRate(), p.pfErEpfRate(), p.pfErEpsRate(), p.pfEdliRate(), p.pfAdminRate(), settings.decimal("ESI_WAGE_LIMIT_DISABLED", ym.atEndOfMonth()).orElse(p.esiLimit()), p.esiEeRate(), p.esiErRate(), p.rounding());
        BigDecimal old = jdbc.queryForObject("SELECT lop_days FROM hr_payslip WHERE id = ?", BigDecimal.class, slipId);
        String keep = "HELD".equals(s.get("status")) ? (String) s.get("hold_reason") : null;
        jdbc.update("DELETE FROM hr_payslip WHERE id = ?", slipId);
        insertSlip(((Number) s.get("run_id")).longValue(), empId, ((Number) s.get("salary_id")).longValue(), gross, ym, lop, b(e.get("pf")), b(e.get("esi")), p, keep);
        jdbc.update("UPDATE hr_payslip SET lop_note = ? WHERE run_id = ? AND emp_id = ?", note, s.get("run_id"), empId);
        audit.log("PAYROLL", s.get("run_id"), empId, "UPDATE", "lop_days", old.toPlainString(), lop.toPlainString(), note);
        return Map.of("ok", true, "slipId", jdbc.queryForObject("SELECT id FROM hr_payslip WHERE run_id = ? AND emp_id = ?", Long.class, s.get("run_id"), empId));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void holdSlip(long slipId, String reason) {
        access.require("PAYROLL_PROCESS");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please give a reason");
        Map<String, Object> s = jdbc.queryForList("SELECT s.emp_id, r.status FROM hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id WHERE s.id = ?", slipId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Payslip not found"));
        if ("PAID".equals(s.get("status"))) throw new IllegalStateException("This month is already paid");
        jdbc.update("UPDATE hr_payslip SET status = 'HELD', hold_reason = ?, released_by = NULL, released_at = NULL WHERE id = ?", reason, slipId);
        audit.log("PAYROLL", slipId, (String) s.get("emp_id"), "UPDATE", "status", null, "HELD", reason);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void releaseSlip(long slipId, String reason) {
        access.require("PAYROLL_PROCESS");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please give a reason");
        Map<String, Object> s = jdbc.queryForList("SELECT emp_id, status FROM hr_payslip WHERE id = ?", slipId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Payslip not found"));
        if (!"HELD".equals(s.get("status"))) throw new IllegalStateException("This salary is not on hold");
        jdbc.update("UPDATE hr_payslip SET status = 'RELEASED', released_by = ?, released_at = NOW() WHERE id = ?", me(), slipId);
        audit.log("PAYROLL", slipId, (String) s.get("emp_id"), "UPDATE", "status", "HELD", "RELEASED", reason);
    }

    /** Approves every slip of the month that is not approved yet (slips made later in an approved month need their own approval). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> approve(long runId, String overrideReason) {
        access.require("PAYROLL_APPROVE");
        Map<String, Object> r = jdbc.queryForList("SELECT status FROM hr_payroll_run WHERE id = ?", runId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Run not found"));
        if ("PAID".equals(r.get("status"))) throw new IllegalStateException("This month is already paid");
        int pending = jdbc.queryForObject("SELECT COUNT(*) FROM hr_payslip WHERE run_id = ? AND approved_at IS NULL", Integer.class, runId);
        if (pending == 0) throw new IllegalStateException("Nothing to approve - every slip of this month is already approved");
        boolean same = settings.bool("MAKER_CHECKER_ON", LocalDate.now(), true)
                && jdbc.queryForObject("SELECT COUNT(*) FROM hr_payslip WHERE run_id = ? AND approved_at IS NULL AND generated_by = ?", Integer.class, runId, me()) > 0;
        if (same) {
            // only a Super Admin may approve what they generated themselves, and only with a reason that stays in the audit log
            if (!access.myRoles().contains("SUPER_ADMIN")) throw new ForbiddenException("The person who generated the payroll cannot approve it - another Payroll Approver must");
            if (overrideReason == null || overrideReason.isBlank()) throw new ForbiddenException("SAME_PERSON: you generated this payroll yourself. As Super Admin you can still approve it - give a reason");
        }
        jdbc.update("UPDATE hr_payslip SET approved_by = ?, approved_at = NOW() WHERE run_id = ? AND approved_at IS NULL", me(), runId);
        jdbc.update("UPDATE hr_payroll_run SET status = 'APPROVED', approved_by = ?, approved_at = NOW() WHERE id = ?", me(), runId);
        audit.log("PAYROLL", runId, null, "UPDATE", "status", String.valueOf(r.get("status")), (same ? "APPROVED (Super Admin, same person as the maker)" : "APPROVED") + ", " + pending + " slip(s)", same ? overrideReason : null);
        return Map.of("approved", pending);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void markPaid(long runId) {
        access.require("PAYROLL_APPROVE");
        Map<String, Object> r = jdbc.queryForList("SELECT status FROM hr_payroll_run WHERE id = ?", runId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Run not found"));
        if (!"APPROVED".equals(r.get("status"))) throw new IllegalStateException("Approve the month first");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM hr_payslip WHERE run_id = ? AND approved_at IS NULL", Integer.class, runId) > 0) throw new IllegalStateException("Some slips of this month are not approved yet - approve them first");
        jdbc.update("UPDATE hr_payroll_run SET status = 'PAID', paid_at = NOW() WHERE id = ?", runId);
        audit.log("PAYROLL", runId, null, "UPDATE", "status", "APPROVED", "PAID", null);
    }

    /** Removes one employee's slip from a month that is not paid yet (approved or not; kept in the audit log). Generating the month again would make it again. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteSlip(long slipId, String reason) {
        access.require("PAYROLL_PROCESS");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please give a reason");
        Map<String, Object> s = jdbc.queryForList("SELECT s.emp_id, s.net_pay, s.run_id, s.approved_at, r.status FROM hr_payslip s JOIN hr_payroll_run r ON r.id = s.run_id WHERE s.id = ?", slipId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Payslip not found"));
        if ("PAID".equals(s.get("status"))) throw new IllegalStateException("This month is already paid - its slips cannot be deleted");
        jdbc.update("DELETE FROM hr_payslip WHERE id = ?", slipId);
        audit.log("PAYROLL", s.get("run_id"), (String) s.get("emp_id"), "DELETE", "payslip", String.valueOf(s.get("net_pay")), null, reason);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteRun(long runId, String reason) {
        access.require("PAYROLL_PROCESS");
        String st = jdbc.queryForList("SELECT status FROM hr_payroll_run WHERE id = ?", String.class, runId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Run not found"));
        if ("PAID".equals(st)) throw new IllegalStateException("This month is already paid - it cannot be deleted");
        if (!"DRAFT".equals(st) && (reason == null || reason.isBlank())) throw new IllegalArgumentException("Please give a reason for deleting an approved month");
        jdbc.update("DELETE FROM hr_payroll_run WHERE id = ?", runId);
        audit.log("PAYROLL", runId, null, "DELETE", "run", st, null, reason);
    }

    // ---- slip design --------------------------------------------------------------------------------------------------
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> slipConfig() {
        return jdbc.queryForMap("SELECT title, header_note AS headerNote, footer_note AS footerNote, show_logo AS showLogo, show_address AS showAddress, show_employer_contrib AS showEmployerContrib, show_bank AS showBank, show_statutory_ids AS showStatutoryIds, show_attendance AS showAttendance, show_net_in_words AS showNetInWords, accent_color AS accentColor, signatory_label AS signatoryLabel FROM hr_slip_config WHERE company_id = 1");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveSlipConfig(Map<String, Object> f) {
        access.require("SALARY_EDIT");
        Map<String, Object> before = slipConfig();
        String color = String.valueOf(f.getOrDefault("accentColor", "#d9441a"));
        if (!color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Colour must look like #d9441a");
        String title = String.valueOf(f.getOrDefault("title", "Salary Slip")).trim();
        if (title.isEmpty()) throw new IllegalArgumentException("Title is required");
        jdbc.update("UPDATE hr_slip_config SET title=?, header_note=?, footer_note=?, show_logo=?, show_address=?, show_employer_contrib=?, show_bank=?, show_statutory_ids=?, show_attendance=?, show_net_in_words=?, accent_color=?, signatory_label=?, updated_by=?, updated_at=NOW() WHERE company_id = 1",
                title, nz(f.get("headerNote")), nz(f.get("footerNote")), b(f.get("showLogo")) ? 1 : 0, b(f.get("showAddress")) ? 1 : 0, b(f.get("showEmployerContrib")) ? 1 : 0, b(f.get("showBank")) ? 1 : 0, b(f.get("showStatutoryIds")) ? 1 : 0, b(f.get("showAttendance")) ? 1 : 0, b(f.get("showNetInWords")) ? 1 : 0, color, nz(f.get("signatoryLabel")), me());
        audit.logDiff("SLIP_DESIGN", 1, null, "UPDATE", before, slipConfig(), null);
    }

    private static String nz(Object o) { return o == null || o.toString().isBlank() ? null : o.toString().trim(); }
}
