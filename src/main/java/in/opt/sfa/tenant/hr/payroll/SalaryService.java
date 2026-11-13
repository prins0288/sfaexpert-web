package in.opt.sfa.tenant.hr.payroll;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.calc.SalaryCalc;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import in.opt.sfa.tenant.hr.setting.HrSettingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Salary head master and each employee's salary (revisions with an effective date). */
@Service
public class SalaryService {

    static final String HEAD_COLS = "id, code, name, head_type AS headType, calc_type AS calcType, calc_base AS calcBase, default_value AS defaultValue, prorata, pf_wage AS pfWage, esi_wage AS esiWage, taxable, show_in_slip AS showInSlip, sort_order AS sortOrder, is_active AS active";

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;
    private final HrSettingService settings;

    public SalaryService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access, HrSettingService settings) { this.jdbc = jdbc; this.audit = audit; this.access = access; this.settings = settings; }

    private static String me() { return UserContext.get() == null ? null : UserContext.get().empId(); }
    private static boolean b(Object o) { return o instanceof Boolean x ? x : o != null && ("1".equals(o.toString()) || "true".equalsIgnoreCase(o.toString())); }
    private static BigDecimal bd(Object o) { return o == null || o.toString().isBlank() ? BigDecimal.ZERO : new BigDecimal(o.toString()); }

    // ---- head master ----------------------------------------------------------------------------------------------
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> heads(boolean onlyActive) {
        access.require("SALARY_VIEW");
        return jdbc.queryForList("SELECT " + HEAD_COLS + " FROM hr_salary_head WHERE company_id = 1" + (onlyActive ? " AND is_active = 1" : "") + " ORDER BY head_type, sort_order, name");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveHead(Map<String, Object> f) {
        access.require("SALARY_EDIT");
        String code = String.valueOf(f.getOrDefault("code", "")).trim().toUpperCase(), name = String.valueOf(f.getOrDefault("name", "")).trim();
        if (!code.matches("[A-Z0-9_]{1,20}")) throw new IllegalArgumentException("Code: letters / digits / underscore, up to 20");
        if (name.isEmpty()) throw new IllegalArgumentException("Name is required");
        if (List.of("PF_EE", "ESI_EE", "PF_ER_EPF", "PF_ER_EPS", "PF_EDLI", "PF_ADMIN", "ESI_ER", "GROSS").contains(code)) throw new IllegalArgumentException(code + " is reserved - PF / ESI are worked out by the engine");
        String type = String.valueOf(f.getOrDefault("headType", "EARNING")), calc = String.valueOf(f.getOrDefault("calcType", "FIXED"));
        if (!List.of("EARNING", "DEDUCTION").contains(type)) throw new IllegalArgumentException("Type must be EARNING or DEDUCTION");
        if (!List.of("FIXED", "PERCENT", "BALANCE").contains(calc)) throw new IllegalArgumentException("Calculation must be FIXED, PERCENT or BALANCE");
        if ("BALANCE".equals(calc) && !"EARNING".equals(type)) throw new IllegalArgumentException("Only an earning can be the balance head");
        String base = f.get("calcBase") == null || f.get("calcBase").toString().isBlank() ? null : f.get("calcBase").toString().trim().toUpperCase();
        if ("PERCENT".equals(calc)) {
            if (base == null) base = "GROSS";
            if (!"GROSS".equals(base) && jdbc.queryForObject("SELECT COUNT(*) FROM hr_salary_head WHERE code = ? AND head_type = 'EARNING' AND company_id = 1", Integer.class, base) == 0) throw new IllegalArgumentException("Percent base must be GROSS or an earning head");
            if (base.equals(code)) throw new IllegalArgumentException("A head cannot be a percent of itself");
        } else base = null;
        boolean earning = "EARNING".equals(type);
        Object[] v = {code, name, type, calc, base, bd(f.get("defaultValue")), b(f.get("prorata")) ? 1 : 0, earning && b(f.get("pfWage")) ? 1 : 0, earning && b(f.get("esiWage")) ? 1 : 0, b(f.get("taxable")) ? 1 : 0, !f.containsKey("showInSlip") || b(f.get("showInSlip")) ? 1 : 0,
                f.get("sortOrder") == null || f.get("sortOrder").toString().isBlank() ? 0 : Integer.parseInt(f.get("sortOrder").toString()), f.containsKey("active") && !b(f.get("active")) ? 0 : 1};
        String sets = "code=?, name=?, head_type=?, calc_type=?, calc_base=?, default_value=?, prorata=?, pf_wage=?, esi_wage=?, taxable=?, show_in_slip=?, sort_order=?, is_active=?";
        try {
            if (f.get("id") == null || f.get("id").toString().isBlank()) {
                Object[] a = new Object[v.length + 1]; a[0] = me(); System.arraycopy(v, 0, a, 1, v.length);
                jdbc.update("INSERT INTO hr_salary_head SET company_id = 1, created_by = ?, " + sets, a);
                Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                audit.log("SALARY_HEAD", id, null, "CREATE", "head", null, code + " " + name, null);
                return Map.of("id", id);
            }
            long id = Long.parseLong(f.get("id").toString());
            Map<String, Object> before = jdbc.queryForMap("SELECT " + HEAD_COLS + " FROM hr_salary_head WHERE id = ?", id);
            Object[] a = new Object[v.length + 2]; a[0] = me(); System.arraycopy(v, 0, a, 1, v.length); a[a.length - 1] = id;
            jdbc.update("UPDATE hr_salary_head SET updated_by = ?, updated_at = NOW(), " + sets + " WHERE id = ?", a);
            audit.logDiff("SALARY_HEAD", id, null, "UPDATE", before, jdbc.queryForMap("SELECT " + HEAD_COLS + " FROM hr_salary_head WHERE id = ?", id), f.get("reason") == null ? null : f.get("reason").toString());
            return Map.of("id", id);
        } catch (org.springframework.dao.DuplicateKeyException e) { throw new IllegalArgumentException("That code already exists"); }
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int deleteHeads(List<Long> ids) {
        access.require("SALARY_EDIT");
        int n = 0;
        for (Long id : ids) {
            String code = jdbc.queryForList("SELECT code FROM hr_salary_head WHERE id = ?", String.class, id).stream().findFirst().orElse(null);
            if (code == null) continue;
            if (jdbc.queryForObject("SELECT COUNT(*) FROM hr_emp_salary_head WHERE head_id = ?", Integer.class, id) > 0) throw new IllegalArgumentException(code + " is used in employee salaries - switch it off instead");
            jdbc.update("DELETE FROM hr_salary_head WHERE id = ?", id);
            audit.log("SALARY_HEAD", id, null, "DELETE", "head", code, null, null);
            n++;
        }
        return n;
    }

    // ---- employee salary -------------------------------------------------------------------------------------------
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> employees(String q) {
        access.require("SALARY_VIEW");
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        return jdbc.queryForList("""
                SELECT e.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS name, d.designation, s.gross_monthly AS gross, s.effective_from AS since
                FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation
                LEFT JOIN hr_emp_salary s ON s.id = (SELECT x.id FROM hr_emp_salary x WHERE x.emp_id = e.emp_id AND x.effective_from <= CURDATE() AND (x.effective_to IS NULL OR x.effective_to >= CURDATE()) ORDER BY x.effective_from DESC LIMIT 1)
                WHERE e.is_active = 1 AND COALESCE(e.is_vacant, 0) = 0 AND (e.emp_name LIKE ? OR e.emp_code LIKE ?) ORDER BY e.emp_name LIMIT 300""", like, like);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> salaryOf(String empId) {
        access.require("SALARY_VIEW");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("emp", jdbc.queryForList("SELECT emp_id AS empId, emp_code AS empCode, emp_name AS name FROM emp_detail WHERE emp_id = ?", empId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Employee not found")));
        List<Map<String, Object>> revs = jdbc.queryForList("SELECT id, effective_from AS effectiveFrom, effective_to AS effectiveTo, gross_monthly AS gross, remarks FROM hr_emp_salary WHERE emp_id = ? ORDER BY effective_from DESC", empId);
        for (Map<String, Object> r : revs) r.put("values", jdbc.queryForList("SELECT h.id AS headId, h.code, h.name, v.head_value AS value FROM hr_emp_salary_head v JOIN hr_salary_head h ON h.id = v.head_id WHERE v.salary_id = ?", r.get("id")));
        out.put("revisions", revs); out.put("heads", heads(true));
        return out;
    }

    private List<SalaryCalc.Value> toValues(List<Map<String, Object>> vals) {
        List<SalaryCalc.Value> out = new ArrayList<>();
        for (Map<String, Object> v : vals) {
            Map<String, Object> h = jdbc.queryForList("SELECT " + HEAD_COLS + " FROM hr_salary_head WHERE id = ? AND is_active = 1", v.get("headId")).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown or inactive head"));
            out.add(new SalaryCalc.Value(PayrollService.head(h), bd(v.get("value"))));
        }
        return out;
    }

    /** Shows what a month of this salary would look like (30 days, no loss of pay) and warns about the wage rule - nothing is saved. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> preview(String empId, BigDecimal gross, List<Map<String, Object>> vals, LocalDate from) {
        access.require("SALARY_VIEW");
        if (gross == null || gross.signum() <= 0) throw new IllegalArgumentException("Gross must be more than zero");
        List<SalaryCalc.Value> values = toValues(vals);
        SalaryCalc.Output o = SalaryCalc.compute(new SalaryCalc.Input(gross, values, 30, BigDecimal.ZERO, true, true, PayrollService.params(settings, from)));
        List<String> warnings = new ArrayList<>();
        BigDecimal wage = o.lines().stream().filter(l -> "EARNING".equals(l.type()) && (l.code().equals("BASIC") || l.code().equals("DA"))).map(SalaryCalc.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal minPct = settings.decimal("WAGE_RULE_MIN_PERCENT", from).orElse(BigDecimal.ZERO);
        String mode = settings.text("WAGE_RULE_MODE", from).orElse("OFF");
        boolean breach = !"OFF".equals(mode) && wage.multiply(BigDecimal.valueOf(100)).compareTo(gross.multiply(minPct)) < 0;
        if (breach) warnings.add("Basic + DA is " + wage.multiply(BigDecimal.valueOf(100)).divide(gross, 1, java.math.RoundingMode.HALF_UP) + "% of the gross; the wage rule wants at least " + minPct.stripTrailingZeros().toPlainString() + "%");
        return Map.of("lines", o.lines(), "net", o.net(), "grossEarnings", o.grossEarnings(), "employerCost", o.employerCost(), "warnings", warnings, "blocked", breach && "BLOCK".equals(mode));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveSalary(String empId, LocalDate from, BigDecimal gross, List<Map<String, Object>> vals, String remarks) {
        access.require("SALARY_EDIT");
        if (from == null) throw new IllegalArgumentException("Effective from date is required");
        if (remarks == null || remarks.isBlank()) throw new IllegalArgumentException("Please give a reason for this salary");
        Map<String, Object> pv = preview(empId, gross, vals, from);   // also validates the heads
        if (Boolean.TRUE.equals(pv.get("blocked"))) throw new IllegalArgumentException(((List<?>) pv.get("warnings")).get(0).toString());
        Map<String, Object> old = jdbc.queryForList("SELECT id, gross_monthly FROM hr_emp_salary WHERE emp_id = ? AND effective_from <= ? ORDER BY effective_from DESC LIMIT 1", empId, from).stream().findFirst().orElse(null);
        jdbc.update("DELETE FROM hr_emp_salary WHERE emp_id = ? AND effective_from >= ?", empId, from);   // later or same-day revisions are superseded
        jdbc.update("UPDATE hr_emp_salary SET effective_to = ? WHERE emp_id = ? AND (effective_to IS NULL OR effective_to >= ?)", from.minusDays(1), empId, from);
        jdbc.update("INSERT INTO hr_emp_salary (company_id, emp_id, effective_from, gross_monthly, remarks, created_by) VALUES (1,?,?,?,?,?)", empId, from, gross, remarks, me());
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (Map<String, Object> v : vals) jdbc.update("INSERT INTO hr_emp_salary_head (salary_id, head_id, head_value) VALUES (?,?,?)", id, v.get("headId"), bd(v.get("value")));
        audit.log("SALARY", id, empId, old == null ? "CREATE" : "UPDATE", "gross@" + from, old == null ? null : String.valueOf(old.get("gross_monthly")), gross.toPlainString(), remarks);
        return Map.of("id", id, "warnings", pv.get("warnings"));
    }
}
