package in.opt.sfa.tenant.master.employee.service;

import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.service.AppUserAdminService;
import in.opt.sfa.common.util.RoleLevelMapper;
import in.opt.sfa.tenant.master.employee.dto.EmployeeDto;
import in.opt.sfa.tenant.master.employee.entity.Employee;
import in.opt.sfa.tenant.master.employee.mapper.EmployeeMapper;
import in.opt.sfa.tenant.master.employee.repository.EmployeeRepository;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Employee master. The profile row lives in the TENANT db (employee); the login
 * row lives in the COMMON db (app_user). Add/update writes BOTH (linked by
 * emp_id), so a new employee can log in immediately. Soft-delete disables the
 * login too.
 *
 * Note: the two writes use different transaction managers (tenant + common), so
 * they are not one atomic XA transaction — employee is written first, then the
 * credential. Good enough here; a failure leaves a profile without a login,
 * which a re-save fixes.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final AppUserAdminService credentials;
    private final EmployeeMapper mapper;

    public EmployeeService(EmployeeRepository employees, AppUserAdminService credentials, EmployeeMapper mapper) {
        this.employees = employees;
        this.credentials = credentials;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() {
        String companyCode = TenantContext.getCompanyCode();
        // login-enabled comes from the common app_user (by emp_id); role is derived from emp_level
        Map<String, AppUser> byEmp = credentials.listByCompanyCode(companyCode).stream()
                .filter(u -> u.getEmpId() != null)
                .collect(Collectors.toMap(AppUser::getEmpId, Function.identity(), (a, b) -> a));

        return employees.findAllByOrderByOidAsc().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("oid", e.getOid());
            m.put("empId", e.getEmpId());
            m.put("name", e.getEmpName());
            m.put("username", e.getUsername());
            m.put("designationOid", e.getDesignationOid());
            m.put("designation", e.getDesignationName());
            m.put("stateOid", e.getStateOid());
            m.put("state", e.getStateName());
            m.put("districtOid", e.getDistrictOid());
            m.put("district", e.getDistrictName());
            m.put("dob", e.getDob());
            m.put("mobile", e.getMobile());
            m.put("email", e.getEmail());
            m.put("status", e.getStatus());
            AppUser u = byEmp.get(e.getEmpId());
            m.put("role", RoleLevelMapper.roleFor(e.getEmpLevel()));
            m.put("loginEnabled", u != null && u.isActive());
            return m;
        }).toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public EmployeeDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    /** Create/update the employee profile (tenant) AND its login (common). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(Map<String, Object> body) {
        Authz.requireRole("ADMIN", "MANAGER");   // only admins/managers can add or edit staff
        String companyCode = TenantContext.getCompanyCode();
        String username = str(body.get("username"));
        String empId = str(body.get("empId"));
        if (isBlank(empId)) {
            empId = isBlank(username) ? null : username.toUpperCase().replaceAll("[^A-Z0-9]", "-");
        }
        if (isBlank(empId) || isBlank(username)) {
            throw new IllegalStateException("Username is required");
        }

        Employee e = employees.findByEmpId(empId).orElseGet(Employee::new);
        String role = str(body.get("role"));
        e.setEmpId(empId);
        e.setEmpName(str(body.get("name")));
        e.setUsername(username);
        e.setDesignationOid(lng(body.get("designationOid")));
        e.setStateOid(lng(body.get("stateOid")));
        e.setDistrictOid(lng(body.get("districtOid")));
        e.setMobile(str(body.get("mobile")));
        e.setEmail(str(body.get("email")));
        e.setDob(parseDate(body.get("dob")));
        e.setEmpLevel("ADMIN".equals(role) ? 9 : "MANAGER".equals(role) ? 5 : 1);
        e.setStatus("Y");
        employees.save(e);

        // login (common db) — password optional on update, required on create
        credentials.upsert(companyCode, empId, username, str(body.get("password")), true);

        return Map.of("empId", empId, "saved", true);
    }

    /** Soft delete / restore — also enables/disables the login. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> updateStatus(Long oid, String status) {
        Authz.requireRole("ADMIN");   // only admins can deactivate/restore staff (disables login)
        String companyCode = TenantContext.getCompanyCode();
        Employee e = find(oid);
        e.setStatus(status);
        employees.save(e);
        credentials.setEnabled(companyCode, e.getEmpId(), "Y".equals(status));
        return Map.of("oid", oid, "status", status);
    }

    private Employee find(Long oid) {
        return employees.findById(oid).orElseThrow(() -> new IllegalStateException("Employee not found: " + oid));
    }

    // ---- helpers ----
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static Long lng(Object o) {
        if (o == null || o.toString().isBlank()) return null;
        try { return Long.valueOf(o.toString()); } catch (NumberFormatException e) { return null; }
    }
    private static LocalDate parseDate(Object o) {
        if (o == null || o.toString().isBlank()) return null;
        try { return LocalDate.parse(o.toString()); } catch (Exception e) { return null; }
    }
}
