package in.opt.sfa.tenant.master.clientmaster.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.storage.FileUploadService;
import in.opt.sfa.storage.StoredFile;
import in.opt.sfa.tenant.entity.District;
import in.opt.sfa.tenant.master.area.entity.Area;
import in.opt.sfa.tenant.master.area.repository.AreaRepository;
import in.opt.sfa.tenant.master.category.entity.CategoryMaster;
import in.opt.sfa.tenant.master.category.repository.CategoryMasterRepository;
import in.opt.sfa.tenant.master.clientmaster.dto.ClientMasterDto;
import in.opt.sfa.tenant.master.clientmaster.entity.ClientMaster;
import in.opt.sfa.tenant.master.clientmaster.entity.ClientMasterChild;
import in.opt.sfa.tenant.master.clientmaster.repository.ClientMasterChildRepository;
import in.opt.sfa.tenant.master.clientmaster.repository.ClientMasterRepository;
import in.opt.sfa.tenant.master.clienttype.entity.ClientType;
import in.opt.sfa.tenant.master.clienttype.repository.ClientTypeRepository;
import in.opt.sfa.tenant.master.degree.entity.DegreeMaster;
import in.opt.sfa.tenant.master.degree.repository.DegreeMasterRepository;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpNameResolver;
import in.opt.sfa.tenant.master.permission.service.PermissionService;
import in.opt.sfa.tenant.master.speciality.entity.SpecialityMaster;
import in.opt.sfa.tenant.master.speciality.repository.SpecialityMasterRepository;
import in.opt.sfa.tenant.master.state.entity.State;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import in.opt.sfa.tenant.repository.DistrictRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Client Master (doctors, chemists, stockists ...) — like Create Employee:
 * a form with the essentials and optional sections, photo, approval and
 * delete requests, and bulk Excel upload.
 *
 * Code: typed by the user, or generated from the client type (first 2 letters of
 * its code + 5 digits, e.g. DR00001; no type = CL00001).
 * Approval follows the company setting clientApprovalFlow (Settings > General):
 *   AUTO           approved at once
 *   MANAGER        a manager of the client's employee approves -> APPROVED
 *   ADMIN          an admin approves -> APPROVED
 *   MANAGER_ADMIN  manager approves (PENDING_ADMIN), then an admin -> APPROVED  (default)
 * Roles come from the emp level (5-8 manager, 9-20 admin, 21+ super admin). A
 * manager acts only for employees in his reporting chain (emp_detail.manager_id);
 * an admin cannot skip the manager step unless the employee has no manager above
 * him; the super admin can approve any step. A new client starts at the first
 * step above its creator (a manager's own client waits only for the admin).
 * Rejected clients go back into approval when they are edited. The client's
 * "owner" for these rules is its mapped employee, else whoever created it.
 * A delete request only flags the client; whoever gives the last approval
 * step of the flow accepts it (client deactivated) or rejects it.
 */
@Service
public class ClientMasterService {

    public static final String PHOTO_MODULE = "clientPhoto";
    public static final String APPROVE = "CLIENT_MASTER_APPROVE";
    public static final String FLOW_KEY = "clientApprovalFlow";
    public static final List<String> FLOWS = List.of("AUTO", "MANAGER", "ADMIN", "MANAGER_ADMIN");
    public static final String DEFAULT_FLOW = "MANAGER_ADMIN";
    public static final String PENDING_MANAGER = "PENDING_MANAGER", PENDING_ADMIN = "PENDING_ADMIN",
            APPROVED = "APPROVED", REJECTED = "REJECTED";
    public static final List<String> LOCALITY_TYPES = List.of("Urban", "Semi-Urban", "Rural");
    public static final List<String> GENDERS = List.of("Male", "Female", "Other");
    public static final List<String> VISIT_DAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    public static final List<String> FREQUENCIES = List.of("Weekly", "Fortnightly", "Monthly", "Bi-monthly", "Quarterly");

    private static final Pattern CODE = Pattern.compile("[A-Z0-9][A-Z0-9 ._/-]*");
    private static final Pattern MOBILE = Pattern.compile("[6-9][0-9]{9}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Pattern PIN = Pattern.compile("[1-9][0-9]{5}");
    private static final Pattern PAN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
    private static final Pattern GST = Pattern.compile("[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]");
    private static final Pattern AADHAAR = Pattern.compile("[2-9][0-9]{11}");

    private final ClientMasterRepository clients;
    private final ClientMasterChildRepository children;
    private final ClientTypeRepository clientTypes;
    private final CategoryMasterRepository categories;
    private final SpecialityMasterRepository specialities;
    private final DegreeMasterRepository degrees;
    private final StateRepository states;
    private final DistrictRepository districts;
    private final AreaRepository areas;
    private final EmpDetailRepository employees;
    private final PermissionService permissions;
    private final FileUploadService uploads;
    private final EmpNameResolver employeeNames;
    private final in.opt.sfa.common.service.CompanySettingStore settings;
    private final in.opt.sfa.tenant.master.salutation.service.SalutationMasterService salutations;

    public ClientMasterService(ClientMasterRepository clients, ClientMasterChildRepository children,
                               ClientTypeRepository clientTypes, CategoryMasterRepository categories,
                               SpecialityMasterRepository specialities, DegreeMasterRepository degrees,
                               StateRepository states, DistrictRepository districts, AreaRepository areas,
                               EmpDetailRepository employees, PermissionService permissions,
                               FileUploadService uploads, EmpNameResolver employeeNames,
                               in.opt.sfa.common.service.CompanySettingStore settings,
                               in.opt.sfa.tenant.master.salutation.service.SalutationMasterService salutations) {
        this.settings = settings;
        this.salutations = salutations;
        this.clients = clients;
        this.children = children;
        this.clientTypes = clientTypes;
        this.categories = categories;
        this.specialities = specialities;
        this.degrees = degrees;
        this.states = states;
        this.districts = districts;
        this.areas = areas;
        this.employees = employees;
        this.permissions = permissions;
        this.uploads = uploads;
        this.employeeNames = employeeNames;
    }

    // ---- read --------------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ClientMasterDto> list() {
        Names n = names();
        Me me = me(n);
        Map<Long, ClientMasterChild> social = children.findAll().stream()
                .collect(Collectors.toMap(ClientMasterChild::getClientId, Function.identity(), (a, b) -> a));
        return clients.findAllByOrderByClientNameAsc().stream().map(c -> rights(toDto(c, social.get(c.getId()), n), c, me)).toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ClientMasterDto get(Long id) {
        ClientMaster c = find(id);
        Names n = names();
        return rights(toDto(c, children.findByClientId(id).orElse(null), n), c, me(n));
    }

    /** Everything the form needs in one call (dropdowns with their state for filtering, fixed lists). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> options() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("salutations", salutations.activeValues());
        m.put("clientTypes", clientTypes.findByIsActiveOrderByTypeNameAsc(true).stream().map(t -> item(t.getOid(), t.getTypeName(), null)).toList());
        m.put("categories", categories.findByStatusOrderByCategoryNameAsc(true).stream().map(t -> item(t.getOid(), t.getCategoryName(), null)).toList());
        m.put("specialities", specialities.findByStatusOrderBySpecialityNameAsc(true).stream().map(t -> item(t.getOid(), t.getSpecialityName(), null)).toList());
        m.put("degrees", degrees.findByIsActiveOrderByDisplayOrderAscDegreeNameAsc(true).stream().map(t -> item(t.getId(), t.getDegreeName(), null)).toList());
        m.put("states", states.findByStatusOrderByStateNameAsc("Y").stream().map(t -> item(t.getOid(), t.getStateName(), null)).toList());
        m.put("districts", districts.findByStatusOrderByDistrictNameAsc("Y").stream().map(t -> item(t.getOid(), t.getDistrictName(), t.getStateOid())).toList());
        m.put("cities", areas.findByStatusOrderByAreaNameAsc("Y").stream().map(a -> {
            Map<String, Object> i = item(a.getOid(), a.getAreaName() + (Strings.isBlank(a.getCity()) ? "" : " (" + a.getCity() + ")"), a.getStateOid());
            i.put("pincode", a.getPincode());
            return i;
        }).toList());
        m.put("employees", employees.findByActiveTrueOrderByEmpNameAsc().stream()
                .map(e -> item(e.getEmpId(), e.getEmpName() + " (" + e.getEmpCode() + ")", e.getStateId())).toList());
        m.put("localityTypes", LOCALITY_TYPES);
        m.put("genders", GENDERS);
        m.put("visitDays", VISIT_DAYS);
        m.put("frequencies", FREQUENCIES);
        m.put("approvalFlow", flow());
        return m;
    }

    private static Map<String, Object> item(Object oid, String label, Long stateId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("oid", oid);
        m.put("label", label);
        if (stateId != null) m.put("stateId", stateId);
        return m;
    }

    /** Preview of the code a new client of this type gets (taken for real on save). */
    // not readOnly: the code query is a locking read (FOR UPDATE)
    @Transactional(transactionManager = "tenantTransactionManager")
    public String nextCode(Long clientTypeId) {
        return formatCode(prefix(clientTypeId == null ? null : clientTypes.findById(clientTypeId).orElse(null)));
    }

    // ---- save ----------------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientMasterDto save(ClientMasterDto form) {
        boolean isNew = form.getId() == null;
        ClientMaster c = isNew ? new ClientMaster() : find(form.getId());
        Names n = names();
        apply(c, form, n, true);
        if (isNew && Strings.isBlank(form.getClientCode())) {
            c.setClientCode(formatCode(prefix(c.getClientTypeId() == null ? null : n.types.get(c.getClientTypeId()))));
        } else {
            c.setClientCode(checkCode(form.getClientCode()));
        }
        clients.findByClientCode(c.getClientCode()).filter(o -> !o.getId().equals(c.getId()))
                .ifPresent(o -> { throw new IllegalStateException("Client code " + c.getClientCode() + " already exists (" + o.getClientName() + ")"); });
        String me = currentUser();
        if (isNew) {
            c.setCreatedBy(me);
            c.setActive(true);
            startApproval(c);
        } else if (REJECTED.equals(c.getApprovalStatus())) {
            startApproval(c);   // corrected after a rejection: goes for approval again
        }
        c.setUpdatedBy(me);
        clients.saveAndFlush(c);
        saveSocial(c, form, true);
        return rights(toDto(c, children.findByClientId(c.getId()).orElse(null), n), c, me(n));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientMasterDto updateStatus(Long id, boolean active) {
        ClientMaster c = find(id);
        c.setActive(active);
        c.setUpdatedBy(currentUser());
        clients.save(c);
        return get(id);
    }

    /** Approve the step the client is waiting for (manager step or final admin step). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientMasterDto approve(Long id) {
        ClientMaster c = find(id);
        Names n = names();
        Me me = me(n);
        String why = whyNotApprove(c, me);
        if (why != null) throw new IllegalStateException(why);
        LocalDateTime now = LocalDateTime.now();
        String flow = flow();
        if (PENDING_MANAGER.equals(c.getApprovalStatus())) {
            c.setManagerApprovedBy(me.empId);
            c.setManagerApprovedOn(now);
            // a manager's approval is final unless the flow also needs an admin
            boolean needsAdmin = (MANAGER_ADMIN_FLOW(flow) || "ADMIN".equals(flow)) && me.rank < ADMIN_RANK;
            if (needsAdmin) c.setApprovalStatus(PENDING_ADMIN);
            else finalApprove(c, me.empId, now);
        } else {
            finalApprove(c, me.empId, now);
        }
        c.setUpdatedBy(me.empId);
        clients.save(c);
        return get(id);
    }

    /** Reject at the current step (reason required). Editing the client sends it for approval again. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientMasterDto reject(Long id, String reason) {
        ClientMaster c = find(id);
        Names n = names();
        Me me = me(n);
        String why = whyNotApprove(c, me);
        if (why != null) throw new IllegalStateException(why.replace("approve", "reject"));
        String r = Strings.isBlank(reason) ? null : reason.trim();
        if (r == null) throw new IllegalStateException("Please give the reason for rejecting this client");
        if (r.length() > 255) throw new IllegalStateException("Reason can be at most 255 characters");
        c.setApprovalStatus(REJECTED);
        c.setApproved(false);
        c.setRejectedBy(me.empId);
        c.setRejectedOn(LocalDateTime.now());
        c.setRejectReason(r);
        c.setUpdatedBy(me.empId);
        clients.save(c);
        return get(id);
    }

    /** Field user asks for the client to be removed; an approver decides. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientMasterDto requestDelete(Long id, String reason) {
        ClientMaster c = find(id);
        if (!Boolean.TRUE.equals(c.getActive())) throw new IllegalStateException("Client is already inactive");
        String r = Strings.isBlank(reason) ? null : reason.trim();
        if (r == null) throw new IllegalStateException("Please give the reason for deleting this client");
        if (r.length() > 255) throw new IllegalStateException("Reason can be at most 255 characters");
        c.setDeleteRequested(true);
        c.setDeleteRequestedBy(currentUser());
        c.setDeleteRequestReason(r);
        clients.save(c);
        return get(id);
    }

    /** accept = deactivate the client; reject = keep it. Either way the request is closed. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientMasterDto decideDelete(Long id, boolean accept) {
        ClientMaster c = find(id);
        if (!Boolean.TRUE.equals(c.getDeleteRequested())) throw new IllegalStateException("There is no delete request for this client");
        if (!canDecideDelete(c, me(names()))) throw new IllegalStateException("Only the final approver of this client can decide its delete request");
        if (accept) c.setActive(false);
        c.setDeleteRequested(false);
        c.setUpdatedBy(currentUser());
        clients.save(c);
        return get(id);
    }

    /** Set (file) or remove (null) the client's photo — images only. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientMasterDto setPhoto(Long id, MultipartFile file) throws Exception {
        ClientMaster c = find(id);
        String[] old = { c.getPhotoPath(), c.getPhotoThumbnailPath() };
        if (file != null && !file.isEmpty()) {
            StoredFile f = uploads.upload(PHOTO_MODULE, file, FileUploadService.Kind.IMAGE, true);
            afterRollback(() -> uploads.delete(f.path(), f.thumbnailPath()));
            c.setPhotoPath(f.path());
            c.setPhotoThumbnailPath(f.thumbnailPath());
        } else {
            c.setPhotoPath(null);
            c.setPhotoThumbnailPath(null);
        }
        clients.save(c);
        afterCommit(() -> uploads.delete(old));
        return get(id);
    }

    // ---- bulk upload ---------------------------------------------------------------

    public static final List<String> BULK_COLUMNS = List.of("client_code", "salutation", "client_name", "client_type", "category", "sub_category", "speciality", "sub_speciality", "degree", "qualification", "designation", "is_prescriber", "experience_years", "mapped_emp_code", "state", "district", "city", "locality_type", "pin_code", "address", "landmark", "latitude", "longitude", "sitting_place", "property_name", "star_rating", "visit_frequency", "visit_days", "preferred_visit_time", "patients_per_day", "no_of_machines", "is_multi_location", "is_pool_client", "mapped_campaign_code", "mobile_number_1", "mobile_number_2", "email", "contact_person_1_name", "contact_person_1_mobile", "contact_person_2_name", "contact_person_2_mobile", "gender", "date_of_birth", "anniversary_date", "spouse_date_of_birth", "gst_number", "pan_number", "aadhaar_number", "drug_license_no", "medical_registration_no", "remarks", "extra_info_1", "extra_info_2", "extra_info_3", "is_active", "has_fund_support",
            "instagram_handle", "facebook_handle", "twitter_handle");

    public byte[] template() throws Exception {
        Map<String, String> sample = new LinkedHashMap<>();
        BULK_COLUMNS.forEach(k -> sample.put(k, ""));
        sample.put("salutation", "Dr.");
        sample.put("client_name", "Anil Sharma");
        sample.put("client_type", "Doctor");
        sample.put("speciality", "");
        sample.put("is_prescriber", "Y");
        sample.put("mobile_number_1", "9876543210");
        sample.put("locality_type", "Urban");
        sample.put("pin_code", "800001");
        sample.put("address", "12 Boring Road");
        sample.put("visit_frequency", "Monthly");
        sample.put("visit_days", "MON,THU");
        sample.put("gender", "Male");
        sample.put("date_of_birth", "1975-06-20");
        sample.put("star_rating", "4");
        sample.put("is_active", "Y");
        return ExcelUtil.template(List.of(
                new ExcelUtil.SheetSpec("Clients", BULK_COLUMNS, List.of(new ArrayList<>(sample.values()))),
                new ExcelUtil.SheetSpec("Help", List.of("Notes"), List.of(
                        List.of("One row per client. client_code blank = new client with a generated code (e.g. DR00001); an existing client_code updates that client (blank cell = keep the stored value)."),
                        List.of("client_type, category, speciality, degree, state, district, city: code or name from the masters (city = Area master)."),
                        List.of("salutation: Dr. / Mr. / Mrs. ... as in the Salutation master (the name itself without it)."),
                        List.of("mapped_emp_code: employee code. Yes/No columns: Y or N. Dates: 2026-04-01 or 01-04-2026."),
                        List.of("locality_type: Urban / Semi-Urban / Rural. gender: Male / Female / Other. visit_days: MON,TUE,... (comma separated)."),
                        List.of("Mobile numbers: 10 digits starting 6-9. PAN: ABCDE1234F. GST: 15 characters. Aadhaar: 12 digits. PIN: 6 digits.")))));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose an Excel file");
        List<Map<String, String>> rows;
        try (var in = file.getInputStream()) {
            Map<String, List<Map<String, String>>> sheets = ExcelUtil.readAll(in);
            rows = sheets.containsKey("clients") ? sheets.get("clients") : sheets.values().stream().findFirst().orElse(List.of());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the file — use the .xlsx template");
        }
        Names n = names();
        String me = currentUser();
        int created = 0, updated = 0, skipped = 0, r = 1;
        List<String> errors = new ArrayList<>(), codes = new ArrayList<>();
        {
            for (Map<String, String> raw : rows) {
                r++;
                Cells x = new Cells(raw);
                String code = x.get("client_code") == null ? null : x.get("client_code").toUpperCase(Locale.ROOT);
                try {
                    ClientMaster c = code == null ? null : clients.findByClientCode(code).orElse(null);
                    boolean isNew = c == null;
                    if (isNew) c = new ClientMaster();
                    ClientMasterDto f = new ClientMasterDto();
                    f.setSalutation(x.get("salutation"));
                    f.setClientName(x.get("client_name"));
                    f.setClientTypeId(x.has("client_type") ? n.id(n.typeKeys, x.get("client_type"), "client_type") : null);
                    f.setCategoryId(x.has("category") ? n.id(n.categoryKeys, x.get("category"), "category") : null);
                    f.setSubCategory(x.get("sub_category"));
                    f.setSpecialityId(x.has("speciality") ? n.id(n.specialityKeys, x.get("speciality"), "speciality") : null);
                    f.setSubSpeciality(x.get("sub_speciality"));
                    f.setDegreeId(x.has("degree") ? n.id(n.degreeKeys, x.get("degree"), "degree") : null);
                    f.setQualification(x.get("qualification"));
                    f.setDesignation(x.get("designation"));
                    f.setPrescriber(x.yesNo("is_prescriber"));
                    f.setExperienceYears(x.get("experience_years"));
                    f.setMappedEmpCode(x.has("mapped_emp_code") ? n.empId(x.get("mapped_emp_code")) : null);
                    f.setStateId(x.has("state") ? n.id(n.stateKeys, x.get("state"), "state") : null);
                    f.setDistrictId(x.has("district") ? n.id(n.districtKeys, x.get("district"), "district") : null);
                    f.setCityId(x.has("city") ? n.id(n.cityKeys, x.get("city"), "city") : null);
                    f.setLocalityType(x.get("locality_type"));
                    f.setPinCode(x.get("pin_code"));
                    f.setAddress(x.get("address"));
                    f.setLandmark(x.get("landmark"));
                    f.setLatitude(x.decimal("latitude"));
                    f.setLongitude(x.decimal("longitude"));
                    f.setSittingPlace(x.get("sitting_place"));
                    f.setPropertyName(x.get("property_name"));
                    f.setStarRating(x.integer("star_rating"));
                    f.setVisitFrequency(x.get("visit_frequency"));
                    f.setVisitDays(x.get("visit_days"));
                    f.setPreferredVisitTime(x.get("preferred_visit_time"));
                    f.setPatientsPerDay(x.integer("patients_per_day"));
                    f.setNoOfMachines(x.integer("no_of_machines"));
                    f.setMultiLocation(x.yesNo("is_multi_location"));
                    f.setPoolClient(x.yesNo("is_pool_client"));
                    f.setMappedCampaignCode(x.get("mapped_campaign_code"));
                    f.setMobileNumber1(x.get("mobile_number_1"));
                    f.setMobileNumber2(x.get("mobile_number_2"));
                    f.setEmail(x.get("email"));
                    f.setContactPerson1Name(x.get("contact_person_1_name"));
                    f.setContactPerson1Mobile(x.get("contact_person_1_mobile"));
                    f.setContactPerson2Name(x.get("contact_person_2_name"));
                    f.setContactPerson2Mobile(x.get("contact_person_2_mobile"));
                    f.setGender(x.get("gender"));
                    f.setDateOfBirth(x.date("date_of_birth"));
                    f.setAnniversaryDate(x.date("anniversary_date"));
                    f.setSpouseDateOfBirth(x.date("spouse_date_of_birth"));
                    f.setGstNumber(x.get("gst_number"));
                    f.setPanNumber(x.get("pan_number"));
                    f.setAadhaarNumber(x.get("aadhaar_number"));
                    f.setDrugLicenseNo(x.get("drug_license_no"));
                    f.setMedicalRegistrationNo(x.get("medical_registration_no"));
                    f.setRemarks(x.get("remarks"));
                    f.setExtraInfo1(x.get("extra_info_1"));
                    f.setExtraInfo2(x.get("extra_info_2"));
                    f.setExtraInfo3(x.get("extra_info_3"));
                    f.setFundSupport(x.yesNo("has_fund_support"));
                    f.setInstagramHandle(x.get("instagram_handle"));
                    f.setFacebookHandle(x.get("facebook_handle"));
                    f.setTwitterHandle(x.get("twitter_handle"));
                    apply(c, f, n, isNew);   // update: blank cell keeps the stored value
                    Boolean active = x.yesNo("is_active");
                    if (isNew) {
                        c.setClientCode(code != null ? checkCode(code)
                                : formatCode(prefix(c.getClientTypeId() == null ? null : n.types.get(c.getClientTypeId()))));
                        ClientMaster self = c;
                        clients.findByClientCode(c.getClientCode()).ifPresent(o -> { throw new IllegalStateException("client_code " + self.getClientCode() + " already exists"); });
                        c.setCreatedBy(me);
                        c.setActive(active == null || active);
                        startApproval(c);
                    } else if (active != null) {
                        c.setActive(active);
                    }
                    c.setUpdatedBy(me);
                    clients.saveAndFlush(c);
                    saveSocial(c, f, isNew);
                    if (isNew) { created++; if (code == null) codes.add("Row " + r + ": " + c.getClientCode() + " = " + c.getClientName()); }
                    else updated++;
                } catch (IllegalStateException e) {
                    skipped++;
                    errors.add("Row " + r + (code == null ? "" : " (" + code + ")") + ": " + e.getMessage());
                }
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("created", created);
        res.put("updated", updated);
        res.put("skipped", skipped);
        res.put("errors", errors);
        res.put("generatedCodes", codes);
        return res;
    }

    // ---- rules ---------------------------------------------------------------------

    /**
     * Copy + check the editable fields. full = form save / new client: every field
     * is taken as sent (blank clears); otherwise (bulk update) blank keeps the value.
     */
    private void apply(ClientMaster e, ClientMasterDto f, Names n, boolean full) {
        trimAll(f);
        if (f.getClientName() != null || full) {
            if (f.getClientName() == null) throw new IllegalStateException("Client name is required");
            if (f.getClientName().length() > 150) throw new IllegalStateException("Client name can be at most 150 characters");
        }
        if (f.getSalutation() != null) f.setSalutation(salutations.normalise(f.getSalutation()));   // master's spelling
        if (full || f.getSalutation() != null) e.setSalutation(f.getSalutation());
        exists(f.getClientTypeId(), n.types, "Client type");
        exists(f.getCategoryId(), n.categories, "Category");
        exists(f.getSpecialityId(), n.specialities, "Speciality");
        exists(f.getDegreeId(), n.degrees, "Degree");
        exists(f.getStateId(), n.states, "State");
        exists(f.getDistrictId(), n.districts, "District");
        exists(f.getCityId(), n.cities, "City / Area");
        if (f.getMappedEmpCode() != null && !n.employees.containsKey(f.getMappedEmpCode())) {
            throw new IllegalStateException("Employee not found: " + f.getMappedEmpCode());
        }
        Long state = f.getStateId() != null ? f.getStateId() : (full ? null : e.getStateId());
        if (state != null && f.getDistrictId() != null) {
            District d = n.districts.get(f.getDistrictId());
            if (d.getStateOid() != null && !d.getStateOid().equals(state)) throw new IllegalStateException("District " + d.getDistrictName() + " is not in the chosen state");
        }
        if (state != null && f.getCityId() != null) {
            Area a = n.cities.get(f.getCityId());
            if (a.getStateOid() != null && !a.getStateOid().equals(state)) throw new IllegalStateException("City / Area " + a.getAreaName() + " is not in the chosen state");
        }
        if (f.getLocalityType() != null) f.setLocalityType(oneOf(f.getLocalityType(), LOCALITY_TYPES, "Locality type"));
        if (f.getGender() != null) f.setGender(oneOf(f.getGender(), GENDERS, "Gender"));
        if (f.getVisitDays() != null) {
            List<String> days = new ArrayList<>();
            for (String d : f.getVisitDays().toUpperCase(Locale.ROOT).split("[,\\s]+")) {
                if (d.isBlank()) continue;
                String day = d.length() > 3 ? d.substring(0, 3) : d;
                if (!VISIT_DAYS.contains(day)) throw new IllegalStateException("Visit day '" + d + "' — use MON, TUE, WED, THU, FRI, SAT, SUN");
                if (!days.contains(day)) days.add(day);
            }
            days.sort(Comparator.comparingInt(VISIT_DAYS::indexOf));
            f.setVisitDays(days.isEmpty() ? null : String.join(",", days));
        }
        for (String[] m : new String[][]{ { f.getMobileNumber1(), "Mobile" }, { f.getMobileNumber2(), "Mobile 2" },
                { f.getContactPerson1Mobile(), "Contact person 1 mobile" }, { f.getContactPerson2Mobile(), "Contact person 2 mobile" } }) {
            if (m[0] != null && !MOBILE.matcher(m[0]).matches()) throw new IllegalStateException(m[1] + " must be 10 digits starting with 6-9");
        }
        if (f.getEmail() != null && !EMAIL.matcher(f.getEmail()).matches()) throw new IllegalStateException("Email is not valid");
        if (f.getPinCode() != null && !PIN.matcher(f.getPinCode()).matches()) throw new IllegalStateException("PIN code must be 6 digits");
        if (f.getPanNumber() != null) {
            f.setPanNumber(f.getPanNumber().toUpperCase(Locale.ROOT));
            if (!PAN.matcher(f.getPanNumber()).matches()) throw new IllegalStateException("PAN must look like ABCDE1234F");
        }
        if (f.getGstNumber() != null) {
            f.setGstNumber(f.getGstNumber().toUpperCase(Locale.ROOT));
            if (!GST.matcher(f.getGstNumber()).matches()) throw new IllegalStateException("GST number must be 15 characters like 10ABCDE1234F1Z5");
        }
        if (f.getAadhaarNumber() != null) {
            f.setAadhaarNumber(f.getAadhaarNumber().replaceAll("[\\s-]", ""));
            if (!AADHAAR.matcher(f.getAadhaarNumber()).matches()) throw new IllegalStateException("Aadhaar must be 12 digits");
        }
        BigDecimal lat = f.getLatitude(), lng = f.getLongitude();
        if (lat != null && (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0)) throw new IllegalStateException("Latitude must be between -90 and 90");
        if (lng != null && (lng.compareTo(BigDecimal.valueOf(-180)) < 0 || lng.compareTo(BigDecimal.valueOf(180)) > 0)) throw new IllegalStateException("Longitude must be between -180 and 180");
        if (full && (lat == null) != (lng == null)) throw new IllegalStateException("Give both latitude and longitude (or neither)");
        if (lat != null) f.setLatitude(lat.setScale(Math.min(7, Math.max(lat.scale(), 0)), java.math.RoundingMode.HALF_UP));
        if (lng != null) f.setLongitude(lng.setScale(Math.min(7, Math.max(lng.scale(), 0)), java.math.RoundingMode.HALF_UP));
        if (f.getStarRating() != null && (f.getStarRating() < 0 || f.getStarRating() > 5)) throw new IllegalStateException("Star rating must be 0 to 5");
        if (f.getPatientsPerDay() != null && (f.getPatientsPerDay() < 0 || f.getPatientsPerDay() > 65535)) throw new IllegalStateException("Patients per day must be 0 to 65535");
        if (f.getNoOfMachines() != null && (f.getNoOfMachines() < 0 || f.getNoOfMachines() > 65535)) throw new IllegalStateException("No. of machines must be 0 to 65535");
        LocalDate today = LocalDate.now();
        if (f.getDateOfBirth() != null && f.getDateOfBirth().isAfter(today)) throw new IllegalStateException("Date of birth cannot be in the future");
        if (f.getSpouseDateOfBirth() != null && f.getSpouseDateOfBirth().isAfter(today)) throw new IllegalStateException("Spouse date of birth cannot be in the future");
        max(f.getSubCategory(), 100, "Sub category"); max(f.getSubSpeciality(), 100, "Sub speciality");
        max(f.getQualification(), 150, "Qualification"); max(f.getDesignation(), 100, "Designation");
        max(f.getExperienceYears(), 100, "Experience"); max(f.getAddress(), 500, "Address"); max(f.getLandmark(), 200, "Landmark");
        max(f.getSittingPlace(), 100, "Sitting place"); max(f.getPropertyName(), 150, "Clinic / property name");
        max(f.getVisitFrequency(), 50, "Visit frequency"); max(f.getPreferredVisitTime(), 100, "Preferred visit time");
        max(f.getMappedCampaignCode(), 100, "Campaign code"); max(f.getEmail(), 120, "Email");
        max(f.getContactPerson1Name(), 150, "Contact person 1"); max(f.getContactPerson2Name(), 150, "Contact person 2");
        max(f.getDrugLicenseNo(), 80, "Drug licence"); max(f.getMedicalRegistrationNo(), 100, "Medical registration");
        max(f.getExtraInfo1(), 255, "Extra info 1"); max(f.getExtraInfo2(), 255, "Extra info 2"); max(f.getExtraInfo3(), 255, "Extra info 3");
        max(f.getRemarks(), 5000, "Remarks");
        max(f.getInstagramHandle(), 150, "Instagram"); max(f.getFacebookHandle(), 150, "Facebook"); max(f.getTwitterHandle(), 150, "Twitter / X");

        if (full || f.getClientName() != null) e.setClientName(f.getClientName());
        if (full || f.getClientTypeId() != null) e.setClientTypeId(f.getClientTypeId());
        if (full || f.getCategoryId() != null) e.setCategoryId(f.getCategoryId());
        if (full || f.getSubCategory() != null) e.setSubCategory(f.getSubCategory());
        if (full || f.getSpecialityId() != null) e.setSpecialityId(f.getSpecialityId());
        if (full || f.getSubSpeciality() != null) e.setSubSpeciality(f.getSubSpeciality());
        if (full || f.getDegreeId() != null) e.setDegreeId(f.getDegreeId());
        if (full || f.getQualification() != null) e.setQualification(f.getQualification());
        if (full || f.getDesignation() != null) e.setDesignation(f.getDesignation());
        if (full || f.getPrescriber() != null) e.setPrescriber(f.getPrescriber());
        if (full || f.getExperienceYears() != null) e.setExperienceYears(f.getExperienceYears());
        if (full || f.getMappedEmpCode() != null) e.setMappedEmpCode(f.getMappedEmpCode());
        if (full || f.getStateId() != null) e.setStateId(f.getStateId());
        if (full || f.getDistrictId() != null) e.setDistrictId(f.getDistrictId());
        if (full || f.getCityId() != null) e.setCityId(f.getCityId());
        if (full || f.getLocalityType() != null) e.setLocalityType(f.getLocalityType());
        if (full || f.getPinCode() != null) e.setPinCode(f.getPinCode());
        if (full || f.getAddress() != null) e.setAddress(f.getAddress());
        if (full || f.getLandmark() != null) e.setLandmark(f.getLandmark());
        if (full || f.getLatitude() != null) e.setLatitude(f.getLatitude());
        if (full || f.getLongitude() != null) e.setLongitude(f.getLongitude());
        if (full || f.getSittingPlace() != null) e.setSittingPlace(f.getSittingPlace());
        if (full || f.getPropertyName() != null) e.setPropertyName(f.getPropertyName());
        if (full || f.getStarRating() != null) e.setStarRating(f.getStarRating());
        if (full || f.getVisitFrequency() != null) e.setVisitFrequency(f.getVisitFrequency());
        if (full || f.getVisitDays() != null) e.setVisitDays(f.getVisitDays());
        if (full || f.getPreferredVisitTime() != null) e.setPreferredVisitTime(f.getPreferredVisitTime());
        if (full || f.getPatientsPerDay() != null) e.setPatientsPerDay(f.getPatientsPerDay());
        if (full || f.getNoOfMachines() != null) e.setNoOfMachines(f.getNoOfMachines());
        if (full || f.getMultiLocation() != null) e.setMultiLocation(f.getMultiLocation());
        if (full || f.getPoolClient() != null) e.setPoolClient(f.getPoolClient());
        if (full || f.getMappedCampaignCode() != null) e.setMappedCampaignCode(f.getMappedCampaignCode());
        if (full || f.getMobileNumber1() != null) e.setMobileNumber1(f.getMobileNumber1());
        if (full || f.getMobileNumber2() != null) e.setMobileNumber2(f.getMobileNumber2());
        if (full || f.getEmail() != null) e.setEmail(f.getEmail());
        if (full || f.getContactPerson1Name() != null) e.setContactPerson1Name(f.getContactPerson1Name());
        if (full || f.getContactPerson1Mobile() != null) e.setContactPerson1Mobile(f.getContactPerson1Mobile());
        if (full || f.getContactPerson2Name() != null) e.setContactPerson2Name(f.getContactPerson2Name());
        if (full || f.getContactPerson2Mobile() != null) e.setContactPerson2Mobile(f.getContactPerson2Mobile());
        if (full || f.getGender() != null) e.setGender(f.getGender());
        if (full || f.getDateOfBirth() != null) e.setDateOfBirth(f.getDateOfBirth());
        if (full || f.getAnniversaryDate() != null) e.setAnniversaryDate(f.getAnniversaryDate());
        if (full || f.getSpouseDateOfBirth() != null) e.setSpouseDateOfBirth(f.getSpouseDateOfBirth());
        if (full || f.getGstNumber() != null) e.setGstNumber(f.getGstNumber());
        if (full || f.getPanNumber() != null) e.setPanNumber(f.getPanNumber());
        if (full || f.getAadhaarNumber() != null) e.setAadhaarNumber(f.getAadhaarNumber());
        if (full || f.getDrugLicenseNo() != null) e.setDrugLicenseNo(f.getDrugLicenseNo());
        if (full || f.getMedicalRegistrationNo() != null) e.setMedicalRegistrationNo(f.getMedicalRegistrationNo());
        if (full || f.getRemarks() != null) e.setRemarks(f.getRemarks());
        if (full || f.getExtraInfo1() != null) e.setExtraInfo1(f.getExtraInfo1());
        if (full || f.getExtraInfo2() != null) e.setExtraInfo2(f.getExtraInfo2());
        if (full || f.getExtraInfo3() != null) e.setExtraInfo3(f.getExtraInfo3());
        if (full || f.getFundSupport() != null) e.setFundSupport(f.getFundSupport());
        // numbers / yes-no columns are NOT NULL: a cleared value means 0 / No
        if (e.getStarRating() == null) e.setStarRating(0);
        if (e.getPatientsPerDay() == null) e.setPatientsPerDay(0);
        if (e.getNoOfMachines() == null) e.setNoOfMachines(0);
        if (e.getPrescriber() == null) e.setPrescriber(false);
        if (e.getMultiLocation() == null) e.setMultiLocation(false);
        if (e.getPoolClient() == null) e.setPoolClient(false);
        if (e.getFundSupport() == null) e.setFundSupport(false);
        e.setHasGeoTag(e.getLatitude() != null && e.getLongitude() != null);
    }

    private void saveSocial(ClientMaster c, ClientMasterDto f, boolean full) {
        boolean any = f.getInstagramHandle() != null || f.getFacebookHandle() != null || f.getTwitterHandle() != null;
        ClientMasterChild ch = children.findByClientId(c.getId()).orElse(null);
        if (ch == null && !any) return;
        if (ch == null) {
            ch = new ClientMasterChild();
            ch.setClientId(c.getId());
            ch.setCreatedBy(currentUser());
        }
        if (full || f.getInstagramHandle() != null) ch.setInstagramHandle(f.getInstagramHandle());
        if (full || f.getFacebookHandle() != null) ch.setFacebookHandle(f.getFacebookHandle());
        if (full || f.getTwitterHandle() != null) ch.setTwitterHandle(f.getTwitterHandle());
        ch.setMappedEmpCode(c.getMappedEmpCode());
        ch.setActive(true);
        children.save(ch);
    }

    // ---- approval rules ------------------------------------------------------------

    private static final int USER_RANK = 0, MANAGER_RANK = 1, ADMIN_RANK = 2, SUPER_RANK = 3;

    private static boolean MANAGER_ADMIN_FLOW(String flow) { return "MANAGER_ADMIN".equals(flow); }

    /** The company's flow (Settings > General > Client approval). */
    public String flow() {
        String f = settings.value(FLOW_KEY, DEFAULT_FLOW);
        return FLOWS.contains(f) ? f : DEFAULT_FLOW;
    }

    private static int rank(String role) {
        if (role == null) return USER_RANK;
        return switch (role) {
            case "SUPER_ADMIN" -> SUPER_RANK;
            case "ADMIN" -> ADMIN_RANK;
            case "MANAGER" -> MANAGER_RANK;
            default -> USER_RANK;
        };
    }

    /** First status for a new (or resubmitted) client: the steps its creator already covers are done. */
    private void startApproval(ClientMaster c) {
        UserContext.CurrentUser u = UserContext.get();
        int r = rank(u == null ? null : u.role());
        String who = currentUser();
        LocalDateTime now = LocalDateTime.now();
        c.setManagerApprovedBy(null); c.setManagerApprovedOn(null);
        c.setApprovedByEmpCode(null); c.setApprovedOn(null);
        c.setRejectedBy(null); c.setRejectedOn(null); c.setRejectReason(null);
        c.setApproved(false);
        switch (flow()) {
            case "AUTO" -> finalApprove(c, who, now);
            case "MANAGER" -> {
                if (r >= MANAGER_RANK) { c.setManagerApprovedBy(who); c.setManagerApprovedOn(now); finalApprove(c, who, now); }
                else c.setApprovalStatus(PENDING_MANAGER);
            }
            case "ADMIN" -> {
                if (r >= ADMIN_RANK) finalApprove(c, who, now);
                else c.setApprovalStatus(PENDING_ADMIN);
            }
            default -> {   // MANAGER_ADMIN
                if (r >= MANAGER_RANK) { c.setManagerApprovedBy(who); c.setManagerApprovedOn(now); }
                if (r >= ADMIN_RANK) finalApprove(c, who, now);
                else c.setApprovalStatus(r >= MANAGER_RANK ? PENDING_ADMIN : PENDING_MANAGER);
            }
        }
    }

    private static void finalApprove(ClientMaster c, String who, LocalDateTime when) {
        c.setApprovalStatus(APPROVED);
        c.setApproved(true);
        c.setApprovedByEmpCode(who);
        c.setApprovedOn(when);
    }

    /** null = this user may approve (or reject) the client's current step; else the reason why not. */
    private String whyNotApprove(ClientMaster c, Me me) {
        String st = c.getApprovalStatus();
        if (!Boolean.TRUE.equals(c.getActive())) return "This client is inactive";
        if (APPROVED.equals(st)) return "This client is already approved";
        if (REJECTED.equals(st)) return "This client was rejected — it goes for approval again when it is edited";
        if (!me.allowed) return "You are not allowed to approve clients (permission CLIENT_MASTER_APPROVE)";
        if (me.rank >= SUPER_RANK) return null;
        String owner = owner(c);
        if (PENDING_ADMIN.equals(st)) {
            return me.rank >= ADMIN_RANK ? null : "Waiting for an admin's final approval";
        }
        // PENDING_MANAGER
        if (me.rank == MANAGER_RANK) {
            return owner == null || me.team.contains(owner) ? null
                    : "Only the manager of " + me.name(owner) + " can approve this client";
        }
        if (me.rank >= ADMIN_RANK) {
            // Manager -> Admin flow: the manager step belongs to the manager; the admin takes it
            // only when nobody of manager level is above the employee. Other flows: an admin may stand in.
            if (!MANAGER_ADMIN_FLOW(flow())) return null;
            return owner == null || !me.hasManagerAbove(owner) ? null
                    : "Waiting for the manager's approval first (then the admin gives the final approval)";
        }
        return "Only a manager or an admin can approve clients";
    }

    /** Delete requests are decided at the flow's last step. */
    private boolean canDecideDelete(ClientMaster c, Me me) {
        if (!Boolean.TRUE.equals(c.getDeleteRequested()) || !me.allowed) return false;
        if (me.rank >= ADMIN_RANK) return true;
        String flow = flow();
        if (me.rank == MANAGER_RANK && ("MANAGER".equals(flow) || "AUTO".equals(flow))) {
            String owner = owner(c);
            return owner == null || me.team.contains(owner);
        }
        return false;
    }

    private ClientMasterDto rights(ClientMasterDto d, ClientMaster c, Me me) {
        d.setCanApprove(whyNotApprove(c, me) == null);
        d.setCanDecideDelete(canDecideDelete(c, me));
        return d;
    }

    /** The employee the client belongs to: mapped employee, else its creator (when that is an employee). */
    private static String owner(ClientMaster c) {
        return c.getMappedEmpCode() != null ? c.getMappedEmpCode() : c.getCreatedBy();
    }

    private Me me(Names n) {
        UserContext.CurrentUser u = UserContext.get();
        return new Me(u == null ? null : u.empId(), rank(u == null ? null : u.role()), permissions.isAllowed(APPROVE), n);
    }

    /** The signed-in user: rank, the employees reporting to them (any depth), and who has a manager above. */
    private static final class Me {
        final String empId;
        final int rank;
        final boolean allowed;
        final Set<String> team = new HashSet<>();
        private final Names n;

        Me(String empId, int rank, boolean allowed, Names n) {
            this.empId = empId;
            this.rank = rank;
            this.allowed = allowed;
            this.n = n;
            if (empId == null) return;
            Map<String, List<String>> reports = new HashMap<>();
            n.employees.values().forEach(e -> { if (e.getManagerId() != null) reports.computeIfAbsent(e.getManagerId(), k -> new ArrayList<>()).add(e.getEmpId()); });
            Deque<String> todo = new ArrayDeque<>(reports.getOrDefault(empId, List.of()));
            while (!todo.isEmpty()) {
                String id = todo.pop();
                if (team.add(id)) todo.addAll(reports.getOrDefault(id, List.of()));
            }
        }

        /** Is anyone of manager level (5-8) above this employee in the reporting chain? */
        boolean hasManagerAbove(String empId) {
            Set<String> seen = new HashSet<>();
            EmpDetail e = n.employees.get(empId);
            while (e != null && e.getManagerId() != null && seen.add(e.getEmpId())) {
                EmpDetail m = n.employees.get(e.getManagerId());
                if (m == null) return false;
                int lvl = m.getEmpLevel() == null ? 0 : m.getEmpLevel();
                if (lvl >= 5 && lvl <= 8 && m.isActive()) return true;
                e = m;
            }
            return false;
        }

        String name(String empId) {
            EmpDetail e = n.employees.get(empId);
            return e == null ? empId : e.getEmpName();
        }
    }

    private static String checkCode(String raw) {
        String code = Strings.isBlank(raw) ? null : raw.trim().toUpperCase(Locale.ROOT);
        if (code == null) throw new IllegalStateException("Client code is required");
        if (code.length() > 30) throw new IllegalStateException("Client code can be at most 30 characters");
        if (!CODE.matcher(code).matches()) throw new IllegalStateException("Client code may use letters, digits, space and . _ / - only");
        return code;
    }

    /** First 2 letters/digits of the client type's code (DOC -> DO, CHEM -> CH), else CL. */
    private static String prefix(ClientType t) {
        String code = t == null || t.getTypeCode() == null ? "" : t.getTypeCode().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return code.length() >= 2 ? code.substring(0, 2) : "CL";
    }

    private String formatCode(String prefix) {
        Long max = clients.maxCodeNumber(prefix.length() + 1, "^" + prefix + "[0-9]+$");
        long next = (max == null ? 0 : max) + 1;
        return prefix + String.format("%05d", next);
    }

    private static void exists(Object id, Map<?, ?> master, String label) {
        if (id != null && !master.containsKey(id)) throw new IllegalStateException(label + " not found: " + id);
    }

    private static String oneOf(String v, List<String> allowed, String label) {
        for (String a : allowed) if (a.equalsIgnoreCase(v.trim())) return a;
        throw new IllegalStateException(label + " must be one of " + String.join(" / ", allowed));
    }

    private static void max(String v, int max, String label) {
        if (v != null && v.length() > max) throw new IllegalStateException(label + " can be at most " + max + " characters");
    }

    /** Blank strings -> null, others trimmed (every String property of the form). */
    private static void trimAll(ClientMasterDto f) {
        for (var pd : org.springframework.beans.BeanUtils.getPropertyDescriptors(ClientMasterDto.class)) {
            if (pd.getPropertyType() != String.class || pd.getReadMethod() == null || pd.getWriteMethod() == null) continue;
            try {
                String v = (String) pd.getReadMethod().invoke(f);
                if (v != null) pd.getWriteMethod().invoke(f, v.isBlank() ? null : v.trim());
            } catch (ReflectiveOperationException ignored) { }
        }
    }

    // ---- mapping -------------------------------------------------------------------

    private ClientMasterDto toDto(ClientMaster e, ClientMasterChild ch, Names n) {
        ClientMasterDto d = new ClientMasterDto();
        d.setId(e.getId());
        d.setSalutation(e.getSalutation());
        d.setClientCode(e.getClientCode());
        d.setClientName(e.getClientName());
        d.setClientTypeId(e.getClientTypeId());
        d.setCategoryId(e.getCategoryId());
        d.setSubCategory(e.getSubCategory());
        d.setSpecialityId(e.getSpecialityId());
        d.setSubSpeciality(e.getSubSpeciality());
        d.setDegreeId(e.getDegreeId());
        d.setQualification(e.getQualification());
        d.setDesignation(e.getDesignation());
        d.setPrescriber(e.getPrescriber());
        d.setExperienceYears(e.getExperienceYears());
        d.setMappedEmpCode(e.getMappedEmpCode());
        d.setStateId(e.getStateId());
        d.setDistrictId(e.getDistrictId());
        d.setCityId(e.getCityId());
        d.setLocalityType(e.getLocalityType());
        d.setPinCode(e.getPinCode());
        d.setAddress(e.getAddress());
        d.setLandmark(e.getLandmark());
        d.setLatitude(e.getLatitude());
        d.setLongitude(e.getLongitude());
        d.setHasGeoTag(e.getHasGeoTag());
        d.setSittingPlace(e.getSittingPlace());
        d.setPropertyName(e.getPropertyName());
        d.setStarRating(e.getStarRating());
        d.setVisitFrequency(e.getVisitFrequency());
        d.setVisitDays(e.getVisitDays());
        d.setPreferredVisitTime(e.getPreferredVisitTime());
        d.setPatientsPerDay(e.getPatientsPerDay());
        d.setNoOfMachines(e.getNoOfMachines());
        d.setMultiLocation(e.getMultiLocation());
        d.setPoolClient(e.getPoolClient());
        d.setMappedCampaignCode(e.getMappedCampaignCode());
        d.setMobileNumber1(e.getMobileNumber1());
        d.setMobileNumber2(e.getMobileNumber2());
        d.setEmail(e.getEmail());
        d.setContactPerson1Name(e.getContactPerson1Name());
        d.setContactPerson1Mobile(e.getContactPerson1Mobile());
        d.setContactPerson2Name(e.getContactPerson2Name());
        d.setContactPerson2Mobile(e.getContactPerson2Mobile());
        d.setGender(e.getGender());
        d.setDateOfBirth(e.getDateOfBirth());
        d.setAnniversaryDate(e.getAnniversaryDate());
        d.setSpouseDateOfBirth(e.getSpouseDateOfBirth());
        d.setGstNumber(e.getGstNumber());
        d.setPanNumber(e.getPanNumber());
        d.setAadhaarNumber(e.getAadhaarNumber());
        d.setDrugLicenseNo(e.getDrugLicenseNo());
        d.setMedicalRegistrationNo(e.getMedicalRegistrationNo());
        d.setPhotoPath(e.getPhotoPath());
        d.setPhotoThumbnailPath(e.getPhotoThumbnailPath());
        d.setRemarks(e.getRemarks());
        d.setExtraInfo1(e.getExtraInfo1());
        d.setExtraInfo2(e.getExtraInfo2());
        d.setExtraInfo3(e.getExtraInfo3());
        d.setActive(e.getActive());
        d.setDeleteRequested(e.getDeleteRequested());
        d.setDeleteRequestedBy(e.getDeleteRequestedBy());
        d.setDeleteRequestReason(e.getDeleteRequestReason());
        d.setApproved(e.getApproved());
        d.setApprovedByEmpCode(e.getApprovedByEmpCode());
        d.setApprovedOn(e.getApprovedOn());
        d.setFundSupport(e.getFundSupport());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedBy(e.getUpdatedBy());
        d.setCreatedOn(e.getCreatedOn());
        d.setUpdatedOn(e.getUpdatedOn());
        if (ch != null) {
            d.setInstagramHandle(ch.getInstagramHandle());
            d.setFacebookHandle(ch.getFacebookHandle());
            d.setTwitterHandle(ch.getTwitterHandle());
        }
        ClientType t = e.getClientTypeId() == null ? null : n.types.get(e.getClientTypeId());
        d.setClientTypeName(t == null ? null : t.getTypeName());
        CategoryMaster cat = e.getCategoryId() == null ? null : n.categories.get(e.getCategoryId());
        d.setCategoryName(cat == null ? null : cat.getCategoryName());
        SpecialityMaster sp = e.getSpecialityId() == null ? null : n.specialities.get(e.getSpecialityId());
        d.setSpecialityName(sp == null ? null : sp.getSpecialityName());
        DegreeMaster dg = e.getDegreeId() == null ? null : n.degrees.get(e.getDegreeId());
        d.setDegreeName(dg == null ? null : dg.getDegreeName());
        State st = e.getStateId() == null ? null : n.states.get(e.getStateId());
        d.setStateName(st == null ? null : st.getStateName());
        District di = e.getDistrictId() == null ? null : n.districts.get(e.getDistrictId());
        d.setDistrictName(di == null ? null : di.getDistrictName());
        Area ar = e.getCityId() == null ? null : n.cities.get(e.getCityId());
        d.setCityName(ar == null ? null : ar.getAreaName());
        d.setMappedEmpName(e.getMappedEmpCode() == null ? null : employeeNames.resolve(n.empNames, e.getMappedEmpCode()));
        d.setApprovedByName(e.getApprovedByEmpCode() == null ? null : employeeNames.resolve(n.empNames, e.getApprovedByEmpCode()));
        d.setApprovalStatus(e.getApprovalStatus());
        d.setManagerApprovedBy(e.getManagerApprovedBy());
        d.setManagerApprovedOn(e.getManagerApprovedOn());
        d.setManagerApprovedByName(e.getManagerApprovedBy() == null ? null : employeeNames.resolve(n.empNames, e.getManagerApprovedBy()));
        d.setRejectedBy(e.getRejectedBy());
        d.setRejectedOn(e.getRejectedOn());
        d.setRejectedByName(e.getRejectedBy() == null ? null : employeeNames.resolve(n.empNames, e.getRejectedBy()));
        d.setRejectReason(e.getRejectReason());
        d.setCreatedByName(e.getCreatedBy() == null ? null : employeeNames.resolve(n.empNames, e.getCreatedBy()));
        d.setUpdatedByName(e.getUpdatedBy() == null ? null : employeeNames.resolve(n.empNames, e.getUpdatedBy()));
        return d;
    }

    private ClientMaster find(Long id) {
        return clients.findById(id).orElseThrow(() -> new IllegalStateException("Client not found: " + id));
    }

    private Names names() {
        return new Names(clientTypes.findAll(), categories.findAll(), specialities.findAll(), degrees.findAll(), states.findAll(),
                districts.findAll(), areas.findAll(), employees.findAll(), employeeNames.nameMap());
    }

    /** Masters by id, and code / name (lower case) -> id for the upload. */
    private static final class Names {
        final Map<Long, ClientType> types;
        final Map<Long, CategoryMaster> categories;
        final Map<Long, SpecialityMaster> specialities;
        final Map<Long, DegreeMaster> degrees;
        final Map<Long, State> states;
        final Map<Long, District> districts;
        final Map<Long, Area> cities;
        final Map<String, EmpDetail> employees;
        final Map<String, String> empNames;
        final Map<String, Long> typeKeys = new HashMap<>(), categoryKeys = new HashMap<>(), specialityKeys = new HashMap<>(),
                degreeKeys = new HashMap<>(), stateKeys = new HashMap<>(), districtKeys = new HashMap<>(), cityKeys = new HashMap<>();
        final Map<String, String> empKeys = new HashMap<>();

        Names(List<ClientType> t, List<CategoryMaster> c, List<SpecialityMaster> s, List<DegreeMaster> d, List<State> st,
              List<District> di, List<Area> ar, List<EmpDetail> emps, Map<String, String> empNames) {
            types = t.stream().collect(Collectors.toMap(ClientType::getOid, Function.identity()));
            categories = c.stream().collect(Collectors.toMap(CategoryMaster::getOid, Function.identity()));
            specialities = s.stream().collect(Collectors.toMap(SpecialityMaster::getOid, Function.identity()));
            degrees = d.stream().collect(Collectors.toMap(DegreeMaster::getId, Function.identity()));
            states = st.stream().collect(Collectors.toMap(State::getOid, Function.identity()));
            districts = di.stream().collect(Collectors.toMap(District::getOid, Function.identity()));
            cities = ar.stream().collect(Collectors.toMap(Area::getOid, Function.identity()));
            employees = emps.stream().collect(Collectors.toMap(EmpDetail::getEmpId, Function.identity()));
            this.empNames = empNames;
            t.forEach(x -> { key(typeKeys, x.getTypeName(), x.getOid()); key(typeKeys, x.getTypeCode(), x.getOid()); });
            c.forEach(x -> { key(categoryKeys, x.getCategoryName(), x.getOid()); key(categoryKeys, x.getCategoryCode(), x.getOid()); });
            s.forEach(x -> { key(specialityKeys, x.getSpecialityName(), x.getOid()); key(specialityKeys, x.getSpecialityCode(), x.getOid()); });
            d.forEach(x -> { key(degreeKeys, x.getDegreeName(), x.getId()); key(degreeKeys, x.getDegreeCode(), x.getId()); });
            st.forEach(x -> { key(stateKeys, x.getStateName(), x.getOid()); key(stateKeys, x.getStateCode(), x.getOid()); });
            di.forEach(x -> { key(districtKeys, x.getDistrictName(), x.getOid()); key(districtKeys, x.getDistrictCode(), x.getOid()); });
            ar.forEach(x -> { key(cityKeys, x.getAreaName(), x.getOid()); key(cityKeys, x.getAreaCode(), x.getOid()); });
            emps.forEach(x -> {
                if (x.getEmpCode() != null) empKeys.put(x.getEmpCode().trim().toLowerCase(Locale.ROOT), x.getEmpId());
                empKeys.put(x.getEmpId().trim().toLowerCase(Locale.ROOT), x.getEmpId());
            });
        }

        private static void key(Map<String, Long> m, String k, Long id) {
            if (k != null) m.put(k.trim().toLowerCase(Locale.ROOT), id);
        }

        Long id(Map<String, Long> m, String v, String what) {
            Long id = m.get(v.trim().toLowerCase(Locale.ROOT));
            if (id == null) throw new IllegalStateException(what + " '" + v + "' not found in the master");
            return id;
        }

        String empId(String v) {
            String id = empKeys.get(v.trim().toLowerCase(Locale.ROOT));
            if (id == null) throw new IllegalStateException("mapped_emp_code '" + v + "' is not an employee");
            return id;
        }
    }

    /** One Excel row: header names matched without case, spaces or "*". */
    private static final class Cells {
        private static final DateTimeFormatter[] DATES = {
                DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT) };
        private final Map<String, String> cells = new HashMap<>();

        Cells(Map<String, String> raw) {
            raw.forEach((k, v) -> cells.put(k.replace("*", "").trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_"), v));
        }

        String get(String col) {
            String v = cells.get(col);
            return v == null || v.isBlank() ? null : v.trim();
        }

        boolean has(String col) { return get(col) != null; }

        BigDecimal decimal(String col) {
            String v = get(col);
            if (v == null) return null;
            try { return new BigDecimal(v.replace(",", "")); }
            catch (NumberFormatException e) { throw new IllegalStateException(col + " '" + v + "' is not a number"); }
        }

        Integer integer(String col) {
            BigDecimal d = decimal(col);
            if (d == null) return null;
            try { return d.intValueExact(); }
            catch (ArithmeticException e) { throw new IllegalStateException(col + " must be a whole number"); }
        }

        LocalDate date(String col) {
            String v = get(col);
            if (v == null) return null;
            for (DateTimeFormatter f : DATES) {
                try { return LocalDate.parse(v, f); } catch (Exception ignored) { }
            }
            throw new IllegalStateException(col + " '" + v + "' is not a valid date (use 2026-04-01 or 01-04-2026)");
        }

        Boolean yesNo(String col) {
            String v = get(col);
            if (v == null) return null;
            return switch (v.toLowerCase(Locale.ROOT)) {
                case "y", "yes", "1", "true" -> true;
                case "n", "no", "0", "false" -> false;
                default -> throw new IllegalStateException(col + " must be Y or N");
            };
        }
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        String who = (u == null || u.empId() == null) ? "system" : u.empId();
        return who.length() > 40 ? who.substring(0, 40) : who;
    }

    private static void afterCommit(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { r.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { r.run(); }
        });
    }

    private static void afterRollback(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) r.run(); }
        });
    }
}
