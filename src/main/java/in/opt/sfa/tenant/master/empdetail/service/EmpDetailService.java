package in.opt.sfa.tenant.master.empdetail.service;

import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.service.AppUserAdminService;
import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.common.util.CodeGeneratorService;
import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.Authz;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.master.division.entity.Division;
import in.opt.sfa.tenant.master.division.repository.DivisionRepository;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.repository.DesignationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * "Create Employee" — single quick-create / update form + bulk Excel upload.
 * Writes emp_detail (tenant db) AND keeps the matching login row in
 * user_login_master (common db, via AppUserAdminService) in sync, linked by
 * emp_id. Passwords are stored in PLAIN TEXT (no hashing) — see AuthService.
 */
@Service
public class EmpDetailService {

    private final EmpDetailRepository employees;
    private final AppUserAdminService credentials;
    private final CodeGeneratorService codeGenerator;
    private final DivisionRepository divisions;
    private final DesignationRepository designations;
    private final CompanySettingStore settings;

    private static final SecureRandom RANDOM = new SecureRandom();
    /** Username: 3-50 chars, letters / digits / . _ @ - (no spaces). */
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._@-]{3,50}$");
    /** Same rule as the emp_detail chk_emp_mobile constraint. */
    private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmpDetailService(EmpDetailRepository employees, AppUserAdminService credentials,
                            CodeGeneratorService codeGenerator, DivisionRepository divisions,
                            DesignationRepository designations, CompanySettingStore settings) {
        this.employees = employees;
        this.credentials = credentials;
        this.codeGenerator = codeGenerator;
        this.divisions = divisions;
        this.designations = designations;
        this.settings = settings;
    }

    // ---- login suggestions for the "Add Employee" form -----------------------

    /**
     * A ready-to-use login for a new employee: username = company prefix +
     * 8 random digits (e.g. ACME48203917, re-rolled until it is free across all
     * companies), password = 8 random digits. The prefix is the company
     * setting "username.prefix", else the company code in capitals.
     */
    public Map<String, Object> suggestLogin() {
        Authz.requireRole("ADMIN", "MANAGER");
        String companyCode = TenantContext.getCompanyCode();
        String prefix = usernamePrefix();
        String username = null;
        for (int i = 0; i < 20 && username == null; i++) {
            String candidate = prefix + eightDigits();
            if (credentials.isUsernameAvailable(candidate, companyCode, null)) username = candidate;
        }
        if (username == null) throw new IllegalStateException("Could not generate a free username, please try again");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", username);
        m.put("password", eightDigits());
        return m;
    }

    /** Live check for the form: is this username valid and not taken (by anyone but this employee)? */
    public Map<String, Object> usernameAvailability(String username, String empId) {
        Authz.requireRole("ADMIN", "MANAGER");
        String u = username == null ? "" : username.trim();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", u);
        if (!USERNAME.matcher(u).matches()) {
            m.put("available", false);
            m.put("message", "Use 3-50 letters, digits or . _ @ - (no spaces)");
        } else if (!credentials.isUsernameAvailable(u, TenantContext.getCompanyCode(), Strings.isBlank(empId) ? null : empId)) {
            m.put("available", false);
            m.put("message", "Username already exists");
        } else {
            m.put("available", true);
            m.put("message", "Username is available");
        }
        return m;
    }

    private String usernamePrefix() {
        String companyCode = TenantContext.getCompanyCode();
        String p = settings.value("username.prefix", companyCode == null ? "" : companyCode);
        p = p == null ? "" : p.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return p.length() > 20 ? p.substring(0, 20) : p;
    }

    /** 8 random digits, never starting with 0 (so Excel / phones don't drop it). */
    private static String eightDigits() {
        return String.valueOf(10_000_000 + RANDOM.nextInt(90_000_000));
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() {
        String companyCode = TenantContext.getCompanyCode();
        Map<String, AppUser> byEmp = credentials.listByCompanyCode(companyCode).stream()
                .collect(Collectors.toMap(AppUser::getEmpId, Function.identity(), (a, b) -> a));
        Map<Long, String> designationNames = designations.findAll().stream()
                .collect(Collectors.toMap(Designation::getOid, Designation::getDesignationName, (a, b) -> a));
        Map<Long, String> divisionNames = divisions.findAll().stream()
                .collect(Collectors.toMap(Division::getOid, Division::getDivisionName, (a, b) -> a));

        return employees.findAllByOrderByEmpNameAsc().stream()
                .map(e -> toRow(e, byEmp.get(e.getEmpId()), designationNames, divisionNames))
                .toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> get(String empId) {
        EmpDetail e = find(empId);
        String companyCode = TenantContext.getCompanyCode();
        AppUser u = credentials.listByCompanyCode(companyCode).stream()
                .filter(a -> empId.equals(a.getEmpId())).findFirst().orElse(null);
        return toRow(e, u, null, null);
    }

    /**
     * Create (empId blank) or update (empId given) an employee. On create, only
     * empName/divisionId/designationId/username/password are required; the rest
     * are optional and can be filled in later via another save() (update) —
     * profileComplete flips to true once gender/DOJ/email/department are all set.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(in.opt.sfa.tenant.master.empdetail.dto.EmpDetailSaveRequest req) {
        Authz.requireRole("ADMIN", "MANAGER");
        boolean isNew = Strings.isBlank(req.getEmpId());
        EmpDetail e = isNew ? new EmpDetail() : find(req.getEmpId());

        if (isNew) {
            if (Strings.isBlank(req.getEmpName())) throw new IllegalStateException("Employee name is required");
            if (req.getDesignationId() == null) throw new IllegalStateException("Designation is required");
            if (Strings.isBlank(req.getUsername())) throw new IllegalStateException("Username is required");
            if (Strings.isBlank(req.getPassword())) throw new IllegalStateException("Password is required for a new employee");
        }
        validate(req, e, isNew);

        if (isNew) {
            String code = codeGenerator.nextEmpCode();
            e.setEmpId(code);
            e.setEmpCode(code);
            e.setActive(true);
            e.setConfirmed(false);
            e.setCreatedAt(LocalDateTime.now());
            e.setCreatedBy(currentEmpId());
        }

        if (!Strings.isBlank(req.getEmpName())) e.setEmpName(req.getEmpName().trim());
        if (req.getDivisionId() != null) e.setDivisionId(req.getDivisionId());
        if (req.getDesignationId() != null) {
            Designation d = designations.findById(req.getDesignationId())
                    .orElseThrow(() -> new IllegalStateException("Designation not found: " + req.getDesignationId()));
            e.setDesignationId(d.getOid());
            // emp_level drives the role in the JWT (RoleLevelMapper) — keep it in step with the designation
            e.setEmpLevel(d.getEmpLevel() == null ? 1 : d.getEmpLevel());
        }
        if (req.getStateId() != null) e.setStateId(req.getStateId());
        if (req.getHqId() != null) e.setHqId(req.getHqId());
        if (req.getManagerId() != null) e.setManagerId(Strings.isBlank(req.getManagerId()) ? null : req.getManagerId());
        if (req.getMobile() != null) e.setMobile(Strings.isBlank(req.getMobile()) ? null : req.getMobile().trim());
        if (req.getGender() != null) e.setGender(Strings.isBlank(req.getGender()) ? null : req.getGender());
        if (req.getDateOfJoining() != null) e.setDateOfJoining(parseDate(req.getDateOfJoining()));
        if (req.getReportingDate() != null) e.setReportingDate(parseDate(req.getReportingDate()));
        if (req.getOfficialEmail() != null) e.setOfficialEmail(Strings.isBlank(req.getOfficialEmail()) ? null : req.getOfficialEmail().trim());
        if (req.getDepartment() != null) e.setDepartment(Strings.isBlank(req.getDepartment()) ? null : req.getDepartment().trim());
        if (req.getOfficeStaff() != null) e.setOfficeStaff(req.getOfficeStaff());
        if (req.getConfirmed() != null) e.setConfirmed(req.getConfirmed());
        if (req.getConfirmationDate() != null) e.setConfirmationDate(parseDate(req.getConfirmationDate()));
        if (req.getResignationDate() != null) e.setResignationDate(parseDate(req.getResignationDate()));
        if (req.getLastWorkingDate() != null) e.setLastWorkingDate(parseDate(req.getLastWorkingDate()));
        if (!e.isConfirmed()) e.setConfirmationDate(null);

        e.setProfileComplete(e.getGender() != null && e.getDateOfJoining() != null
                && e.getOfficialEmail() != null && e.getDepartment() != null);
        e.setUpdatedAt(LocalDateTime.now());
        e.setUpdatedBy(currentEmpId());
        employees.save(e);

        // Keep the COMMON-db login (user_login_master) in sync — blank username/password
        // on an update keeps the existing ones (see AppUserAdminService.upsert).
        credentials.upsert(TenantContext.getCompanyCode(), e.getEmpId(),
                Strings.isBlank(req.getUsername()) ? null : req.getUsername().trim(), req.getPassword(), e.isActive());

        return get(e.getEmpId());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> updateStatus(String empId, boolean active) {
        Authz.requireRole("ADMIN");
        EmpDetail e = find(empId);
        e.setActive(active);
        employees.save(e);
        credentials.setEnabled(TenantContext.getCompanyCode(), empId, active);
        return get(empId);
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("Employees",
                List.of("emp_name", "mobile", "division_code", "designation_code", "username", "password", "official_email", "department"),
                List.of("Jane Doe", "9876543210", "DIV-NORTH", "DSG-MR", "jane.doe", "Welcome@123", "jane.doe@company.com", "Sales"));
    }

    /**
     * Bulk upload = INSERT only (each row creates a brand-new employee + login —
     * emp_code/emp_id are always freshly generated, never taken from the sheet).
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        Authz.requireRole("ADMIN", "MANAGER");
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        String companyCode = TenantContext.getCompanyCode();
        int inserted = 0, skipped = 0;
        List<String> errors = new java.util.ArrayList<>();
        int rowNum = 1;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            rowNum++;
            String empName = row.get("emp_name"), mobile = row.get("mobile");
            String divisionCode = row.get("division_code"), designationCode = row.get("designation_code");
            String username = row.get("username"), password = row.get("password");
            if (Strings.isBlank(empName) || Strings.isBlank(divisionCode) || Strings.isBlank(designationCode)
                    || Strings.isBlank(username) || Strings.isBlank(password)) {
                skipped++; errors.add("Row " + rowNum + ": missing required field(s)"); continue;
            }
            Division division = divisions.findByDivisionCode(divisionCode.trim()).orElse(null);
            Designation designation = designations.findByDesignationCode(designationCode.trim()).orElse(null);
            if (division == null) { skipped++; errors.add("Row " + rowNum + ": unknown division_code '" + divisionCode + "'"); continue; }
            if (designation == null) { skipped++; errors.add("Row " + rowNum + ": unknown designation_code '" + designationCode + "'"); continue; }

            String code = codeGenerator.nextEmpCode();
            EmpDetail e = new EmpDetail();
            e.setEmpId(code);
            e.setEmpCode(code);
            e.setEmpName(empName.trim());
            e.setMobile(mobile == null ? null : mobile.trim());
            e.setDivisionId(division.getOid());
            e.setDesignationId(designation.getOid());
            e.setEmpLevel(designation.getEmpLevel() == null ? 1 : designation.getEmpLevel());
            String email = row.get("official_email"), dept = row.get("department");
            e.setOfficialEmail(Strings.isBlank(email) ? null : email.trim());
            e.setDepartment(Strings.isBlank(dept) ? null : dept.trim());
            e.setActive(true);
            e.setConfirmed(false);
            e.setProfileComplete(false);
            e.setCreatedAt(LocalDateTime.now());
            e.setCreatedBy(currentEmpId());
            e.setUpdatedAt(LocalDateTime.now());
            e.setUpdatedBy(currentEmpId());
            employees.save(e);

            try {
                credentials.upsert(companyCode, code, username.trim(), password, true);
                inserted++;
            } catch (Exception ex) {
                employees.delete(e);
                skipped++; errors.add("Row " + rowNum + ": " + ex.getMessage());
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("inserted", inserted);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return res;
    }

    // ---- helpers --------------------------------------------------------

    /** Field rules, so the user gets a clear message instead of a DB constraint error. */
    private void validate(in.opt.sfa.tenant.master.empdetail.dto.EmpDetailSaveRequest req, EmpDetail e, boolean isNew) {
        String self = isNew ? null : e.getEmpId();
        if (!Strings.isBlank(req.getUsername())) {
            String u = req.getUsername().trim();
            if (!USERNAME.matcher(u).matches()) throw new IllegalStateException("Username: use 3-50 letters, digits or . _ @ - (no spaces)");
            if (!credentials.isUsernameAvailable(u, TenantContext.getCompanyCode(), self)) {
                throw new IllegalStateException("Username already exists: " + u);
            }
        }
        if (!Strings.isBlank(req.getPassword()) && req.getPassword().trim().length() < 4) {
            throw new IllegalStateException("Password must be at least 4 characters");
        }
        if (!Strings.isBlank(req.getMobile()) && !MOBILE.matcher(req.getMobile().trim()).matches()) {
            throw new IllegalStateException("Mobile must be 10 digits starting with 6-9");
        }
        if (!Strings.isBlank(req.getGender()) && !List.of("MALE", "FEMALE", "OTHER").contains(req.getGender())) {
            throw new IllegalStateException("Gender must be MALE, FEMALE or OTHER");
        }
        if (!Strings.isBlank(req.getOfficialEmail())) {
            String mail = req.getOfficialEmail().trim();
            if (!EMAIL.matcher(mail).matches()) throw new IllegalStateException("Official email is not valid");
            employees.findByOfficialEmail(mail)
                    .filter(other -> !other.getEmpId().equals(self))
                    .ifPresent(other -> { throw new IllegalStateException("Official email already used by " + other.getEmpName()); });
        }
        if (self != null && self.equals(req.getManagerId())) {
            throw new IllegalStateException("An employee cannot be their own manager");
        }
        LocalDate doj = parseDate(req.getDateOfJoining()) != null ? parseDate(req.getDateOfJoining()) : e.getDateOfJoining();
        LocalDate lwd = parseDate(req.getLastWorkingDate());
        if (doj != null && lwd != null && lwd.isBefore(doj)) {
            throw new IllegalStateException("Last working date cannot be before the date of joining");
        }
    }

    private EmpDetail find(String empId) {
        return employees.findById(empId).orElseThrow(() -> new IllegalStateException("Employee not found: " + empId));
    }

    private Map<String, Object> toRow(EmpDetail e, AppUser user, Map<Long, String> designationNames, Map<Long, String> divisionNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("empId", e.getEmpId());
        m.put("empCode", e.getEmpCode());
        m.put("empName", e.getEmpName());
        m.put("mobile", e.getMobile());
        m.put("officialEmail", e.getOfficialEmail());
        m.put("department", e.getDepartment());
        m.put("gender", e.getGender());
        m.put("dateOfJoining", e.getDateOfJoining());
        m.put("reportingDate", e.getReportingDate());
        m.put("divisionId", e.getDivisionId());
        m.put("divisionName", divisionNames == null ? null : divisionNames.get(e.getDivisionId()));
        m.put("designationId", e.getDesignationId());
        m.put("designationName", designationNames == null ? null : designationNames.get(e.getDesignationId()));
        m.put("stateId", e.getStateId());
        m.put("hqId", e.getHqId());
        m.put("managerId", e.getManagerId());
        m.put("empLevel", e.getEmpLevel());
        m.put("officeStaff", e.isOfficeStaff());
        m.put("confirmed", e.isConfirmed());
        m.put("confirmationDate", e.getConfirmationDate());
        m.put("resignationDate", e.getResignationDate());
        m.put("lastWorkingDate", e.getLastWorkingDate());
        m.put("active", e.isActive());
        m.put("profileComplete", e.isProfileComplete());
        m.put("username", user == null ? null : user.getUsername());
        m.put("loginActive", user != null && user.isActive());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        return m;
    }

    private static LocalDate parseDate(String s) {
        if (Strings.isBlank(s)) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    private static String currentEmpId() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.empId() == null) ? "system" : u.empId();
    }
}
