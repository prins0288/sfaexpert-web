package in.opt.sfa.web.master;

import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.service.AppUserAdminService;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.tenant.entity.Employee;
import in.opt.sfa.tenant.repository.EmployeeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
@RestController
@RequestMapping("/api/master/employee")
public class EmployeeController {

    private final EmployeeRepository employees;
    private final AppUserAdminService credentials;

    public EmployeeController(EmployeeRepository employees, AppUserAdminService credentials) {
        this.employees = employees;
        this.credentials = credentials;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() {
        String tenant = TenantContext.getTenantId();
        // role + login-enabled come from the common app_user (by emp_id)
        Map<String, AppUser> byEmp = credentials.listByTenant(tenant).stream()
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
            m.put("role", u != null ? u.getRole() : null);
            m.put("loginEnabled", u != null && u.isEnabled());
            return m;
        }).toList();
    }

    @GetMapping("/{oid}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Employee get(@PathVariable Long oid) {
        return employees.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Employee not found: " + oid));
    }

    /** Create/update the employee profile (tenant) AND its login (common). */
    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Authz.requireRole("ADMIN", "MANAGER");   // only admins/managers can add or edit staff
        String tenant = TenantContext.getTenantId();
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
        credentials.upsert(tenant, empId, username, str(body.get("password")), role, true);

        return Map.of("empId", empId, "saved", true);
    }

    /** Soft delete / restore — also enables/disables the login. */
    @PostMapping("/{oid}/status")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> status(@PathVariable Long oid, @RequestParam String status) {
        Authz.requireRole("ADMIN");   // only admins can deactivate/restore staff (disables login)
        String tenant = TenantContext.getTenantId();
        Employee e = employees.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Employee not found: " + oid));
        e.setStatus(status);
        employees.save(e);
        credentials.setEnabled(tenant, e.getEmpId(), "Y".equals(status));
        return Map.of("oid", oid, "status", status);
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
