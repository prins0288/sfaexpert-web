package in.opt.sfa.tenant.master.empdetail.service;

import in.opt.sfa.common.entity.AppUser;
import in.opt.sfa.common.service.AppUserAdminService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    public EmpDetailService(EmpDetailRepository employees, AppUserAdminService credentials,
                            CodeGeneratorService codeGenerator, DivisionRepository divisions,
                            DesignationRepository designations) {
        this.employees = employees;
        this.credentials = credentials;
        this.codeGenerator = codeGenerator;
        this.divisions = divisions;
        this.designations = designations;
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
            String code = codeGenerator.nextEmpCode();
            e.setEmpId(code);
            e.setEmpCode(code);
            e.setActive(true);
            e.setEmpLevel(1);
            e.setConfirmed(false);
            e.setCreatedAt(LocalDateTime.now());
            e.setCreatedBy(currentEmpId());
        }

        if (!Strings.isBlank(req.getEmpName())) e.setEmpName(req.getEmpName().trim());
        if (req.getDivisionId() != null) e.setDivisionId(req.getDivisionId());
        if (req.getDesignationId() != null) e.setDesignationId(req.getDesignationId());
        if (req.getStateId() != null) e.setStateId(req.getStateId());
        if (req.getHqId() != null) e.setHqId(req.getHqId());
        if (req.getManagerId() != null) e.setManagerId(Strings.isBlank(req.getManagerId()) ? null : req.getManagerId());
        if (req.getMobile() != null) e.setMobile(req.getMobile().trim());
        if (req.getGender() != null) e.setGender(Strings.isBlank(req.getGender()) ? null : req.getGender());
        if (req.getDateOfJoining() != null) e.setDateOfJoining(parseDate(req.getDateOfJoining()));
        if (req.getReportingDate() != null) e.setReportingDate(parseDate(req.getReportingDate()));
        if (req.getOfficialEmail() != null) e.setOfficialEmail(Strings.isBlank(req.getOfficialEmail()) ? null : req.getOfficialEmail().trim());
        if (req.getDepartment() != null) e.setDepartment(Strings.isBlank(req.getDepartment()) ? null : req.getDepartment().trim());

        e.setProfileComplete(e.getGender() != null && e.getDateOfJoining() != null
                && e.getOfficialEmail() != null && e.getDepartment() != null);
        e.setUpdatedAt(LocalDateTime.now());
        e.setUpdatedBy(currentEmpId());
        employees.save(e);

        // Keep the COMMON-db login (user_login_master) in sync — blank username/password
        // on an update keeps the existing ones (see AppUserAdminService.upsert).
        credentials.upsert(TenantContext.getCompanyCode(), e.getEmpId(), req.getUsername(), req.getPassword(), e.isActive());

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
