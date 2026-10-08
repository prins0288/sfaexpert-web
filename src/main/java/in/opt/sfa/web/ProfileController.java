package in.opt.sfa.web;

import in.opt.sfa.common.service.AppUserAdminService;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.master.employee.entity.Employee;
import in.opt.sfa.tenant.master.employee.repository.EmployeeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The signed-in user's OWN profile. Identity always comes from the verified JWT
 * (UserContext) — a user can only read/change their own record. The profile
 * (name, contact, designation, photo...) lives in the TENANT employee row; the
 * password lives in the COMMON app_user row.
 */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "My Profile", description = "The signed-in user's own profile, photo and password")
public class ProfileController {

    private final EmployeeRepository employees;
    private final AppUserAdminService credentials;

    public ProfileController(EmployeeRepository employees, AppUserAdminService credentials) {
        this.employees = employees;
        this.credentials = credentials;
    }

    @GetMapping
    @Operation(summary = "Get my profile (login fields + tenant employee record + photo)")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> me() {
        UserContext.CurrentUser u = current();
        String empId = resolveEmpId(u);   // JWT emp_id, or derived from username
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("username", u.username());
        res.put("role", u.role());
        res.put("empId", empId);
        if (empId != null) {
            employees.findByEmpId(empId).ifPresent(e -> {
                res.put("name", e.getEmpName());
                res.put("designationOid", e.getDesignationOid());
                res.put("designation", e.getDesignationName());
                res.put("stateOid", e.getStateOid());
                res.put("state", e.getStateName());
                res.put("districtOid", e.getDistrictOid());
                res.put("district", e.getDistrictName());
                res.put("dob", e.getDob());
                res.put("mobile", e.getMobile());
                res.put("email", e.getEmail());
                res.put("photo", e.getPhoto());
                res.put("status", e.getStatus());
            });
        }
        return res;
    }

    @PostMapping
    @Operation(summary = "Update my profile fields (name, contact, designation, dob)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> update(@RequestBody Map<String, Object> body) {
        Employee e = employee();
        e.setEmpName(str(body.get("name")));
        e.setDesignationOid(lng(body.get("designationOid")));
        e.setStateOid(lng(body.get("stateOid")));
        e.setDistrictOid(lng(body.get("districtOid")));
        e.setMobile(str(body.get("mobile")));
        e.setEmail(str(body.get("email")));
        e.setDob(date(body.get("dob")));
        employees.save(e);
        return Map.of("saved", true);
    }

    @PostMapping("/photo")
    @Operation(summary = "Set my profile photo (base64 data URI in `image`)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> photo(@RequestBody Map<String, String> body) {
        Employee e = employee();
        e.setPhoto(emptyToNull(body.get("image")));   // null clears the photo
        employees.save(e);
        return Map.of("saved", true);
    }

    @PostMapping("/password")
    @Operation(summary = "Change my password (verifies the current one)")
    public Map<String, Object> password(@RequestBody Map<String, String> body) {
        credentials.changeOwnPassword(current().username(),
                body.get("currentPassword"), body.get("newPassword"));
        return Map.of("changed", true);
    }

    // ---- helpers ----
    private UserContext.CurrentUser current() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null || u.username() == null) throw new IllegalStateException("No authenticated user in context");
        return u;
    }

    /** The user's employee id from the JWT, or one derived from the username. */
    private String resolveEmpId(UserContext.CurrentUser u) {
        return (u.empId() != null && !u.empId().isBlank())
                ? u.empId()
                : u.username().toUpperCase().replaceAll("[^A-Z0-9]", "-");
    }

    /**
     * The current user's employee record. Logins without a linked employee (the
     * generic per-tenant accounts) get one created on first save/photo, derived
     * from the username, and the login is linked to it so later JWTs carry it.
     */
    private Employee employee() {
        UserContext.CurrentUser u = current();
        String empId = resolveEmpId(u);

        Employee e = employees.findByEmpId(empId).orElseGet(() -> {
            Employee ne = new Employee();
            ne.setEmpId(empId);
            ne.setUsername(u.username());
            ne.setEmpName(u.username());   // emp_name is NOT NULL — default to the username
            ne.setStatus("Y");
            return ne;
        });

        if (u.empId() == null || u.empId().isBlank()) {
            credentials.linkEmpId(u.username(), empId);   // best-effort link in the common db
        }
        return e;
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
    private static Long lng(Object o) {
        if (o == null || o.toString().isBlank()) return null;
        try { return Long.valueOf(o.toString()); } catch (NumberFormatException e) { return null; }
    }
    private static LocalDate date(Object o) {
        if (o == null || o.toString().isBlank()) return null;
        try { return LocalDate.parse(o.toString()); } catch (Exception e) { return null; }
    }
}
