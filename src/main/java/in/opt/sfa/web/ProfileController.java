package in.opt.sfa.web;

import in.opt.sfa.common.service.AppUserAdminService;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.storage.FileStorageService;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.state.entity.State;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import in.opt.sfa.tenant.repository.DesignationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The signed-in user's OWN profile. Identity always comes from the verified JWT
 * (UserContext) — a user can only read/change their own record. The profile
 * (name, contact, designation, photo...) lives in the TENANT emp_detail row;
 * the password lives in the COMMON user_login_master row.
 *
 * Photo is stored as a FILE (FileStorageService, under the tenant's storage
 * folder — same convention as the company/app logos in BrandingController),
 * referenced by emp_detail.photo_path; the GET response still returns it as a
 * data: URI so the existing <img src> usage needs no change.
 */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "My Profile", description = "The signed-in user's own profile, photo and password")
public class ProfileController {

    private final EmpDetailRepository employees;
    private final AppUserAdminService credentials;
    private final DesignationRepository designations;
    private final StateRepository states;
    private final FileStorageService storage;

    public ProfileController(EmpDetailRepository employees, AppUserAdminService credentials,
                             DesignationRepository designations, StateRepository states,
                             FileStorageService storage) {
        this.employees = employees;
        this.credentials = credentials;
        this.designations = designations;
        this.states = states;
        this.storage = storage;
    }

    @GetMapping
    @Operation(summary = "Get my profile (login fields + tenant employee record + photo)")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> me() {
        UserContext.CurrentUser u = current();
        String empId = resolveEmpId(u);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("username", u.username());
        res.put("role", u.role());
        res.put("empId", empId);
        if (empId != null) {
            employees.findById(empId).ifPresent(e -> {
                res.put("name", e.getEmpName());
                res.put("designationOid", e.getDesignationId());
                res.put("designation", designationName(e.getDesignationId()));
                res.put("stateOid", e.getStateId());
                res.put("state", stateName(e.getStateId()));
                res.put("mobile", e.getMobile());
                res.put("email", e.getOfficialEmail());
                res.put("photo", photoDataUri(e.getPhotoPath()));
                res.put("status", e.isActive() ? "Y" : "N");
            });
        }
        return res;
    }

    @PostMapping
    @Operation(summary = "Update my profile fields (name, contact, designation)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> update(@RequestBody Map<String, Object> body) {
        EmpDetail e = employee();
        e.setEmpName(str(body.get("name")));
        e.setDesignationId(lng(body.get("designationOid")));
        e.setStateId(lng(body.get("stateOid")));
        e.setMobile(str(body.get("mobile")));
        e.setOfficialEmail(str(body.get("email")));
        employees.save(e);
        return Map.of("saved", true);
    }

    @PostMapping("/photo")
    @Operation(summary = "Set my profile photo (multipart file upload)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> photo(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose an image.");
        String companyCode = TenantContext.getCompanyCode();
        EmpDetail e = employee();

        String old = e.getPhotoPath();
        String ext = ext(file.getOriginalFilename());
        String filename = "emp-" + e.getEmpId() + "-" + UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);
        String stored = storage.store(companyCode, filename, file.getBytes());
        e.setPhotoPath(stored);
        employees.save(e);
        if (old != null && !old.isBlank()) storage.delete(companyCode, old);

        return Map.of("saved", true, "photo", photoDataUri(stored));
    }

    @DeleteMapping("/photo")
    @Operation(summary = "Remove my profile photo")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> removePhoto() {
        String companyCode = TenantContext.getCompanyCode();
        EmpDetail e = employee();
        String old = e.getPhotoPath();
        e.setPhotoPath(null);
        employees.save(e);
        if (old != null && !old.isBlank()) storage.delete(companyCode, old);
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
    private EmpDetail employee() {
        UserContext.CurrentUser u = current();
        String empId = resolveEmpId(u);

        EmpDetail e = employees.findById(empId).orElseGet(() -> {
            EmpDetail ne = new EmpDetail();
            ne.setEmpId(empId);
            ne.setEmpCode(empId);
            ne.setEmpName(u.username());   // emp_name is NOT NULL — default to the username
            ne.setActive(true);
            return ne;
        });

        if (u.empId() == null || u.empId().isBlank()) {
            credentials.linkEmpId(u.username(), empId);   // best-effort link in the common db
        }
        return e;
    }

    private String designationName(Long designationId) {
        if (designationId == null) return null;
        return designations.findById(designationId).map(Designation::getDesignationName).orElse(null);
    }

    private String stateName(Long stateId) {
        if (stateId == null) return null;
        return states.findById(stateId).map(State::getStateName).orElse(null);
    }

    /** Reads the stored photo file and returns it as a data: URI (keeps the existing <img src> contract). */
    private String photoDataUri(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) return null;
        try {
            byte[] bytes = storage.load(TenantContext.getCompanyCode(), photoPath);
            return "data:" + mimeType(photoPath) + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;   // file missing/unreadable -> frontend falls back to its placeholder
        }
    }

    private static String mimeType(String name) {
        String e = ext(name);
        return switch (e) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private static String ext(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static Long lng(Object o) {
        if (o == null || o.toString().isBlank()) return null;
        try { return Long.valueOf(o.toString()); } catch (NumberFormatException e) { return null; }
    }
}
