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
import in.opt.sfa.tenant.master.empdetail.dto.EmpDetailSaveRequest;
import in.opt.sfa.tenant.master.empdetail.dto.EmpProfileDto;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.repository.DesignationRepository;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import in.opt.sfa.tenant.master.hq.repository.HqRepository;
import in.opt.sfa.tenant.master.bank.repository.BankMasterRepository;
import in.opt.sfa.tenant.master.degree.repository.DegreeMasterRepository;
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
    private final EmpProfileService profiles;
    private final StateRepository states;
    private final HqRepository hqs;
    private final BankMasterRepository banks;
    private final DegreeMasterRepository degrees;

    private static final SecureRandom RANDOM = new SecureRandom();
    /** Username: 3-50 chars, letters / digits / . _ @ - (no spaces). */
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._@-]{3,50}$");
    /** Same rule as the emp_detail chk_emp_mobile constraint. */
    private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmpDetailService(EmpDetailRepository employees, AppUserAdminService credentials,
                            CodeGeneratorService codeGenerator, DivisionRepository divisions,
                            DesignationRepository designations, CompanySettingStore settings,
                            EmpProfileService profiles, StateRepository states, HqRepository hqs,
                            BankMasterRepository banks, DegreeMasterRepository degrees) {
        this.employees = employees;
        this.credentials = credentials;
        this.codeGenerator = codeGenerator;
        this.divisions = divisions;
        this.designations = designations;
        this.settings = settings;
        this.profiles = profiles;
        this.states = states;
        this.hqs = hqs;
        this.banks = banks;
        this.degrees = degrees;
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
    public Map<String, Object> save(EmpDetailSaveRequest req) {
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
        profiles.check(isNew ? "" : e.getEmpId(), req.getProfile());   // before ANY write

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
        // flush now: the profile rows below are plain JDBC inserts with an FK to emp_detail
        employees.saveAndFlush(e);
        profiles.save(e.getEmpId(), req.getProfile());

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

    // ---- bulk upload ------------------------------------------------------

    /**
     * Template columns, in sheet order. One row = one employee. emp_code blank
     * -> a NEW employee; emp_code of an existing employee -> UPDATE it, where a
     * blank cell keeps the stored value. Codes (division_code, state_code, …)
     * must match the masters; relationships are names (Father, Mother, …).
     * The first 8 columns of the old template still work on their own.
     */
    static final List<String> BULK_COLUMNS = List.of(
            "emp_code", "emp_name", "mobile", "division_code", "designation_code", "state_code", "hq_code",
            "manager_emp_code", "gender", "date_of_joining", "reporting_date", "official_email", "department",
            "office_staff", "confirmed", "confirmation_date", "username", "password",
            "dob", "father_name", "mother_name", "marital_status", "anniversary_date", "spouse_name",
            "qualification_code", "qualification_detail", "blood_group", "personal_email", "landline_no",
            "total_experience_yrs", "shirt_size",
            "present_address", "present_city", "present_district", "present_state_code", "present_pincode",
            "permanent_address", "permanent_city", "permanent_district", "permanent_state_code", "permanent_pincode",
            "bank_code", "ifsc_code", "account_no", "branch_name",
            "nominee_type", "nominee_name", "nominee_relationship", "nominee_dob", "nominee_contact", "nominee_share_pct",
            "emergency_name", "emergency_relationship", "emergency_phone1", "emergency_phone2",
            "pan_no", "pf_no", "uan_no", "esi_no", "mediclaim_policy_no");

    public byte[] template() throws Exception {
        List<String> sample = List.of(
                "", "Jane Doe", "9876543210", "DIV-NORTH", "DSG-MR", "ST-BR", "HQ-PATNA",
                "", "FEMALE", "2024-04-01", "2024-04-01", "jane.doe@company.com", "Sales",
                "N", "N", "", "", "",
                "1995-08-15", "Ram Doe", "Sita Doe", "SINGLE", "", "",
                "BPHARM", "B.Pharm", "B+", "jane@gmail.com", "",
                "3.5", "38",
                "12 MG Road", "Patna", "Patna", "ST-BR", "800001",
                "Village X", "Siwan", "Siwan", "ST-BR", "841226",
                "SBI", "SBIN0001234", "123456789012", "Patna Main",
                "PF", "Ram Doe", "Father", "1965-01-01", "9876500000", "100",
                "Ram Doe", "Father", "9876500000", "",
                "ABCDE1234F", "", "", "", "");
        return ExcelUtil.template("Employees", BULK_COLUMNS, sample);
    }

    /**
     * Bulk upload: new rows create an employee + login (username / password
     * auto-generated when blank, like the form), rows with an existing emp_code
     * update that employee. Every row goes through the same save() as the form,
     * so the rules are identical; a bad row is reported and skipped.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        Authz.requireRole("ADMIN", "MANAGER");
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        List<String> errors = new java.util.ArrayList<>();
        List<Map<String, Object>> created = new java.util.ArrayList<>();
        int rowNum = 1;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            rowNum++;
            try {
                Row r = new Row(row);
                String empCode = r.get("emp_code");
                EmpDetail existing = empCode == null ? null : employees.findByEmpCode(empCode)
                        .orElseThrow(() -> new IllegalStateException("unknown emp_code '" + empCode + "' (leave it blank to create a new employee)"));
                boolean isNew = existing == null;

                EmpDetailSaveRequest req = new EmpDetailSaveRequest();
                if (!isNew) req.setEmpId(existing.getEmpId());
                req.setEmpName(r.get("emp_name"));
                req.setMobile(r.get("mobile"));
                if (r.get("division_code") != null) req.setDivisionId(divisions.findByDivisionCode(r.get("division_code"))
                        .orElseThrow(() -> new IllegalStateException("unknown division_code '" + r.get("division_code") + "'")).getOid());
                if (r.get("designation_code") != null) req.setDesignationId(designations.findByDesignationCode(r.get("designation_code"))
                        .orElseThrow(() -> new IllegalStateException("unknown designation_code '" + r.get("designation_code") + "'")).getOid());
                req.setStateId(stateId(r.get("state_code"), "state_code"));
                if (r.get("hq_code") != null) req.setHqId(hqs.findByHqCode(r.get("hq_code"))
                        .orElseThrow(() -> new IllegalStateException("unknown hq_code '" + r.get("hq_code") + "'")).getOid());
                if (r.get("manager_emp_code") != null) req.setManagerId(employees.findByEmpCode(r.get("manager_emp_code"))
                        .orElseThrow(() -> new IllegalStateException("unknown manager_emp_code '" + r.get("manager_emp_code") + "'")).getEmpId());
                req.setGender(r.upper("gender"));
                req.setDateOfJoining(r.date("date_of_joining"));
                req.setReportingDate(r.date("reporting_date"));
                req.setOfficialEmail(r.get("official_email"));
                req.setDepartment(r.get("department"));
                req.setOfficeStaff(r.bool("office_staff"));
                req.setConfirmed(r.bool("confirmed"));
                req.setConfirmationDate(r.date("confirmation_date"));

                String username = r.get("username"), password = r.get("password");
                boolean generated = false;
                if (isNew && (username == null || password == null)) {
                    Map<String, Object> login = suggestLogin();
                    if (username == null) { username = (String) login.get("username"); generated = true; }
                    if (password == null) { password = (String) login.get("password"); generated = true; }
                }
                req.setUsername(username);
                req.setPassword(password);
                req.setProfile(bulkProfile(r, isNew ? null : profiles.load(existing.getEmpId())));

                Map<String, Object> saved = save(req);
                if (isNew) {
                    inserted++;
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("row", rowNum);
                    c.put("empCode", saved.get("empCode"));
                    c.put("empName", saved.get("empName"));
                    c.put("username", username);
                    c.put("password", password);
                    c.put("generated", generated);
                    created.add(c);
                } else {
                    updated++;
                }
            } catch (Exception ex) {
                skipped++;
                errors.add("Row " + rowNum + ": " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("inserted", inserted);
        res.put("updated", updated);
        res.put("skipped", skipped);
        res.put("errors", errors);
        res.put("created", created);   // logins of the new employees, to hand out
        return res;
    }

    /**
     * Profile sections from one sheet row, laid OVER what is stored (existing,
     * null for a new employee): a blank cell keeps the stored value; a section
     * with no filled cell is left out (= untouched). The single bank / nominee /
     * emergency contact in the row is matched to a stored one (same account no.,
     * same type + name, same phone) and updated, else added.
     */
    private EmpProfileDto bulkProfile(Row r, EmpProfileDto existing) {
        EmpProfileDto p = new EmpProfileDto();
        EmpProfileDto old = existing == null ? new EmpProfileDto() : existing;

        if (r.any("dob", "father_name", "mother_name", "marital_status", "anniversary_date", "spouse_name", "qualification_code",
                "qualification_detail", "blood_group", "personal_email", "landline_no", "total_experience_yrs", "shirt_size")) {
            var x = old.getPersonal() != null ? old.getPersonal() : new EmpProfileDto.Personal();
            if (r.get("dob") != null) x.setDob(r.date("dob"));
            if (r.get("father_name") != null) x.setFatherName(r.get("father_name"));
            if (r.get("mother_name") != null) x.setMotherName(r.get("mother_name"));
            if (r.get("marital_status") != null) x.setMaritalStatus(r.upper("marital_status"));
            if (r.get("anniversary_date") != null) x.setAnniversaryDate(r.date("anniversary_date"));
            if (r.get("spouse_name") != null) x.setSpouseName(r.get("spouse_name"));
            if (r.get("qualification_code") != null) x.setQualificationId(degrees.findByDegreeCode(r.get("qualification_code"))
                    .orElseThrow(() -> new IllegalStateException("unknown qualification_code '" + r.get("qualification_code") + "'")).getId());
            if (r.get("qualification_detail") != null) x.setQualificationDetail(r.get("qualification_detail"));
            if (r.get("blood_group") != null) x.setBloodGroup(r.upper("blood_group"));
            if (r.get("personal_email") != null) x.setPersonalEmail(r.get("personal_email"));
            if (r.get("landline_no") != null) x.setLandlineNo(r.get("landline_no"));
            if (r.get("total_experience_yrs") != null) x.setTotalExperienceYrs(r.decimal("total_experience_yrs"));
            if (r.get("shirt_size") != null) x.setShirtSize(r.decimal("shirt_size").intValue());
            p.setPersonal(x);
        }
        p.setPresentAddress(bulkAddress(r, "present", old.getPresentAddress()));
        p.setPermanentAddress(bulkAddress(r, "permanent", old.getPermanentAddress()));

        if (r.get("account_no") != null) {
            var list = new java.util.ArrayList<>(old.getBankAccounts() == null ? List.<EmpProfileDto.BankAccount>of() : old.getBankAccounts());
            var b = list.stream().filter(x -> r.get("account_no").equals(x.getAccountNo())).findFirst().orElse(null);
            if (b == null) { b = new EmpProfileDto.BankAccount(); b.setAccountNo(r.get("account_no")); list.add(b); }
            if (r.get("bank_code") != null) b.setBankId(banks.findByBankCode(r.get("bank_code"))
                    .orElseThrow(() -> new IllegalStateException("unknown bank_code '" + r.get("bank_code") + "'")).getId());
            if (r.get("ifsc_code") != null) b.setIfscCode(r.upper("ifsc_code"));
            if (r.get("branch_name") != null) b.setBranchName(r.get("branch_name"));
            b.setActive(true);
            for (var other : list) other.setPrimary(other == b);   // the sheet's account becomes the primary one
            p.setBankAccounts(list);
        }

        if (r.get("nominee_name") != null) {
            var list = new java.util.ArrayList<>(old.getNominees() == null ? List.<EmpProfileDto.Nominee>of() : old.getNominees());
            String type = r.upper("nominee_type");
            var n = list.stream().filter(x -> r.get("nominee_name").equalsIgnoreCase(x.getNomineeName())
                    && (type == null || type.equals(x.getNomineeType()))).findFirst().orElse(null);
            if (n == null) { n = new EmpProfileDto.Nominee(); n.setNomineeName(r.get("nominee_name")); list.add(n); }
            if (type != null) n.setNomineeType(type);
            if (r.get("nominee_relationship") != null) n.setRelationshipId(relationship(r.get("nominee_relationship")));
            if (r.get("nominee_dob") != null) n.setNomineeDob(r.date("nominee_dob"));
            if (r.get("nominee_contact") != null) n.setContactNo(r.get("nominee_contact"));
            if (r.get("nominee_share_pct") != null) n.setSharePct(r.decimal("nominee_share_pct"));
            p.setNominees(list);
        }

        if (r.any("emergency_name", "emergency_phone1", "emergency_phone2", "emergency_relationship")) {
            var list = new java.util.ArrayList<>(old.getEmergencyContacts() == null ? List.<EmpProfileDto.EmergencyContact>of() : old.getEmergencyContacts());
            String phone = r.get("emergency_phone1");
            var c = list.stream().filter(x -> phone != null ? phone.equals(x.getContactNo1())
                    : r.get("emergency_name") != null && r.get("emergency_name").equalsIgnoreCase(x.getContactName())).findFirst().orElse(null);
            if (c == null) { c = new EmpProfileDto.EmergencyContact(); list.add(c); }
            if (r.get("emergency_name") != null) c.setContactName(r.get("emergency_name"));
            if (r.get("emergency_relationship") != null) c.setRelationshipId(relationship(r.get("emergency_relationship")));
            if (phone != null) c.setContactNo1(phone);
            if (r.get("emergency_phone2") != null) c.setContactNo2(r.get("emergency_phone2"));
            p.setEmergencyContacts(list);
        }

        if (r.any("pan_no", "pf_no", "uan_no", "esi_no", "mediclaim_policy_no")) {
            var s = old.getStatutory() != null ? old.getStatutory() : new EmpProfileDto.Statutory();
            if (r.get("pan_no") != null) s.setPanNo(r.upper("pan_no"));
            if (r.get("pf_no") != null) s.setPfNo(r.get("pf_no"));
            if (r.get("uan_no") != null) s.setUanNo(r.get("uan_no"));
            if (r.get("esi_no") != null) s.setEsiNo(r.get("esi_no"));
            if (r.get("mediclaim_policy_no") != null) s.setMediclaimPolicyNo(r.get("mediclaim_policy_no"));
            p.setStatutory(s);
        }
        return p;
    }

    private EmpProfileDto.Address bulkAddress(Row r, String prefix,
            EmpProfileDto.Address old) {
        if (!r.any(prefix + "_address", prefix + "_city", prefix + "_district", prefix + "_state_code", prefix + "_pincode")) return null;
        var a = old != null ? old : new EmpProfileDto.Address();
        if (r.get(prefix + "_address") != null) a.setAddressLine(r.get(prefix + "_address"));
        if (r.get(prefix + "_city") != null) a.setCity(r.get(prefix + "_city"));
        if (r.get(prefix + "_district") != null) a.setDistrict(r.get(prefix + "_district"));
        if (r.get(prefix + "_state_code") != null) a.setStateId(stateId(r.get(prefix + "_state_code"), prefix + "_state_code"));
        if (r.get(prefix + "_pincode") != null) a.setPincode(r.get(prefix + "_pincode"));
        return a;
    }

    private Long stateId(String code, String column) {
        if (code == null) return null;
        return states.findByStateCode(code)
                .orElseThrow(() -> new IllegalStateException("unknown " + column + " '" + code + "'")).getOid();
    }

    /** "Father" / "father" / "1" -> 1 (see EmpProfileService.RELATIONSHIPS). */
    private static Integer relationship(String value) {
        for (var e : EmpProfileService.RELATIONSHIPS.entrySet()) {
            if (e.getValue().equalsIgnoreCase(value) || String.valueOf(e.getKey()).equals(value)) return e.getKey();
        }
        throw new IllegalStateException("unknown relationship '" + value + "' (use " + String.join(", ", EmpProfileService.RELATIONSHIPS.values()) + ")");
    }

    /** One sheet row: trimmed cell text, blank = null, plus typed readers with readable errors. */
    private static final class Row {
        private static final List<java.time.format.DateTimeFormatter> DATE_FORMATS = List.of(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
                strict("d-M-uuuu"), strict("d/M/uuuu"), strict("d.M.uuuu"));

        /** STRICT: 31/02/2020 is an error, not silently 29 Feb. */
        private static java.time.format.DateTimeFormatter strict(String pattern) {
            return java.time.format.DateTimeFormatter.ofPattern(pattern).withResolverStyle(java.time.format.ResolverStyle.STRICT);
        }
        private final Map<String, String> cells;

        Row(Map<String, String> cells) { this.cells = cells; }

        String get(String col) {
            String v = cells.get(col);
            return Strings.isBlank(v) ? null : v.trim();
        }

        String upper(String col) {
            String v = get(col);
            return v == null ? null : v.toUpperCase();
        }

        boolean any(String... cols) {
            for (String c : cols) if (get(c) != null) return true;
            return false;
        }

        /** ISO yyyy-MM-dd from an Excel date cell or text like 25-12-2024 / 25/12/2024 / 2024-12-25. */
        String date(String col) {
            String v = get(col);
            if (v == null) return null;
            for (var f : DATE_FORMATS) {
                try { return LocalDate.parse(v, f).toString(); } catch (Exception ignored) { }
            }
            throw new IllegalStateException(col + ": '" + v + "' is not a date (use 2024-12-25 or 25-12-2024)");
        }

        Boolean bool(String col) {
            String v = get(col);
            if (v == null) return null;
            return switch (v.toUpperCase()) {
                case "Y", "YES", "TRUE", "1" -> true;
                case "N", "NO", "FALSE", "0" -> false;
                default -> throw new IllegalStateException(col + ": use Y or N (got '" + v + "')");
            };
        }

        java.math.BigDecimal decimal(String col) {
            String v = get(col);
            if (v == null) return null;
            try { return new java.math.BigDecimal(v); }
            catch (Exception e) { throw new IllegalStateException(col + ": '" + v + "' is not a number"); }
        }
    }

    // ---- helpers --------------------------------------------------------

    /** Field rules, so the user gets a clear message instead of a DB constraint error. */
    private void validate(EmpDetailSaveRequest req, EmpDetail e, boolean isNew) {
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
