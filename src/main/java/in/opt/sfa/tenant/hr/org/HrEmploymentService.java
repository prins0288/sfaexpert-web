package in.opt.sfa.tenant.hr.org;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** HR facts of an employee (branch, department, grade, cost centre, type, probation, notice, tax regime). The person stays in emp_detail. */
@Service
public class HrEmploymentService {

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;

    public HrEmploymentService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access) { this.jdbc = jdbc; this.audit = audit; this.access = access; }

    private static final String SEL = "SELECT e.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS name, e.date_of_joining AS joiningDate, d.designation AS designation, m.emp_name AS manager, "
            + "h.branch_id AS branchId, h.department_id AS departmentId, h.grade_id AS gradeId, h.cost_center_id AS costCenterId, h.employee_type_id AS employeeTypeId, h.second_manager_id AS secondManagerId, "
            + "h.probation_months AS probationMonths, h.probation_end_date AS probationEndDate, h.notice_days AS noticeDays, COALESCE(h.tax_regime,'NEW') AS taxRegime, COALESCE(h.is_disabled,0) AS disabled, "
            + "h.pf_member_since AS pfMemberSince, COALESCE(h.prev_pf_member,0) AS prevPfMember, "
            + "b.name AS branchName, dp.name AS departmentName, g.name AS gradeName, cc.name AS costCenterName, et.name AS employeeTypeName, sm.emp_name AS secondManagerName "
            + "FROM emp_detail e LEFT JOIN hr_emp_employment h ON h.emp_id = e.emp_id LEFT JOIN designation_master d ON d.id = e.designation LEFT JOIN emp_detail m ON m.emp_id = e.manager_id "
            + "LEFT JOIN hr_branch b ON b.id = h.branch_id LEFT JOIN hr_department dp ON dp.id = h.department_id LEFT JOIN hr_grade g ON g.id = h.grade_id LEFT JOIN hr_cost_center cc ON cc.id = h.cost_center_id "
            + "LEFT JOIN hr_employee_type et ON et.id = h.employee_type_id LEFT JOIN emp_detail sm ON sm.emp_id = h.second_manager_id ";

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(String q) {
        access.require("HR_EMPLOYEE_VIEW");
        String like = "%" + (q == null ? "" : q.trim()) + "%";
        return jdbc.queryForList(SEL + "WHERE e.is_active = 1 AND COALESCE(e.is_vacant,0) = 0 AND (e.emp_name LIKE ? OR e.emp_code LIKE ?) ORDER BY e.emp_name LIMIT 300", like, like);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> get(String empId) {
        access.require("HR_EMPLOYEE_VIEW");
        return jdbc.queryForList(SEL + "WHERE e.emp_id = ?", empId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Employee not found"));
    }

    private static final Map<String, String> FIELDS = new LinkedHashMap<>();
    static {
        FIELDS.put("branchId", "branch_id"); FIELDS.put("departmentId", "department_id"); FIELDS.put("gradeId", "grade_id"); FIELDS.put("costCenterId", "cost_center_id");
        FIELDS.put("employeeTypeId", "employee_type_id"); FIELDS.put("secondManagerId", "second_manager_id"); FIELDS.put("probationMonths", "probation_months");
        FIELDS.put("probationEndDate", "probation_end_date"); FIELDS.put("noticeDays", "notice_days"); FIELDS.put("taxRegime", "tax_regime"); FIELDS.put("disabled", "is_disabled");
        FIELDS.put("pfMemberSince", "pf_member_since"); FIELDS.put("prevPfMember", "prev_pf_member");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void save(String empId, Map<String, Object> body, String reason) {
        access.require("HR_EMPLOYEE_EDIT");
        if (jdbc.queryForList("SELECT 1 FROM emp_detail WHERE emp_id = ?", Integer.class, empId).isEmpty()) throw new IllegalArgumentException("Employee not found");
        Map<String, Object> before = new LinkedHashMap<>(jdbc.queryForList("SELECT " + String.join(", ", FIELDS.values()) + " FROM hr_emp_employment WHERE emp_id = ?", empId).stream().findFirst().orElse(Map.of()));
        LinkedHashMap<String, Object> vals = new LinkedHashMap<>();
        for (Map.Entry<String, String> f : FIELDS.entrySet()) {
            if (!body.containsKey(f.getKey())) continue;
            Object v = body.get(f.getKey());
            String s = v == null ? "" : v.toString().trim();
            Object val;
            switch (f.getKey()) {
                case "branchId", "departmentId", "gradeId", "costCenterId", "employeeTypeId", "probationMonths", "noticeDays" -> val = s.isEmpty() ? null : Long.valueOf(s);
                case "secondManagerId" -> {
                    val = s.isEmpty() ? null : s;
                    if (val != null && val.equals(empId)) throw new IllegalArgumentException("Second approver cannot be the employee");
                }
                case "taxRegime" -> { if (!List.of("NEW", "OLD").contains(s)) throw new IllegalArgumentException("Tax regime must be NEW or OLD"); val = s; }
                case "disabled", "prevPfMember" -> val = ("true".equalsIgnoreCase(s) || "1".equals(s)) ? 1 : 0;
                default -> { if (s.isEmpty()) val = null; else { LocalDate.parse(s); val = s; } }   // dates
            }
            vals.put(f.getValue(), val);
        }
        if (vals.isEmpty()) return;
        String me = UserContext.get() == null ? null : UserContext.get().empId();
        boolean exists = !before.isEmpty() || jdbc.queryForObject("SELECT COUNT(*) FROM hr_emp_employment WHERE emp_id = ?", Integer.class, empId) > 0;
        if (!exists) jdbc.update("INSERT INTO hr_emp_employment (emp_id, created_by) VALUES (?,?)", empId, me);
        List<Object> args = new ArrayList<>(vals.values()); args.add(me); args.add(empId);
        jdbc.update("UPDATE hr_emp_employment SET " + String.join(", ", vals.keySet().stream().map(k -> k + " = ?").toList()) + ", updated_by = ?, updated_at = NOW() WHERE emp_id = ?", args.toArray());
        Map<String, Object> after = new LinkedHashMap<>(before); after.putAll(vals);
        audit.logDiff("EMPLOYMENT", empId, empId, exists ? "UPDATE" : "CREATE", before, after, reason);
    }
}
