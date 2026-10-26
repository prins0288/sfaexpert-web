package in.opt.sfa.tenant.master.area.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Numbers;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.approval.ApprovalEngine;
import in.opt.sfa.tenant.team.TeamScope;
import in.opt.sfa.tenant.master.area.dto.AreaDto;
import in.opt.sfa.tenant.master.area.entity.Area;
import in.opt.sfa.tenant.master.area.entity.AreaMasterApproval;
import in.opt.sfa.tenant.master.area.mapper.AreaMapper;
import in.opt.sfa.tenant.master.area.repository.AreaMasterApprovalRepository;
import in.opt.sfa.tenant.master.area.repository.AreaRepository;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.hq.repository.HqRepository;
import in.opt.sfa.tenant.master.routearea.entity.RouteAreaMap;
import in.opt.sfa.tenant.master.routearea.repository.RouteAreaMapRepository;
import in.opt.sfa.tenant.master.state.repository.StateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Area master (area_master). Every area belongs to an employee (emp_id):
 *   admin   sees / edits / deletes every area, and picks the employee
 *   manager his own areas and his team's (everyone reporting to him, any depth)
 *   user    only his own
 * A manager / user always adds areas for himself. Adding and deleting go through
 * approval exactly like Client Master (ApprovalEngine, Settings > General > Area
 * approval): a new area is inactive until approved, a delete request keeps it
 * active until approved; an admin's request is applied directly.
 */
@Service
public class AreaService {

    public static final ApprovalEngine.Config APPROVAL =
            new ApprovalEngine.Config("areaApprovalFlow", "AREA_ADD_APPROVE", "AREA_DELETE_APPROVE", "area");

    private final AreaRepository areas;
    private final RouteAreaMapRepository maps;
    private final HqRepository hqs;
    private final StateRepository states;
    private final AreaMapper mapper;
    private final ApprovalEngine approval;
    private final AreaMasterApprovalRepository history;
    private final EmpDetailRepository employees;
    private final TeamScope scope;

    public AreaService(AreaRepository areas, RouteAreaMapRepository maps, HqRepository hqs, StateRepository states,
                       AreaMapper mapper, ApprovalEngine approval, AreaMasterApprovalRepository history,
                       EmpDetailRepository employees, TeamScope scope) {
        this.scope = scope;
        this.areas = areas;
        this.maps = maps;
        this.hqs = hqs;
        this.states = states;
        this.mapper = mapper;
        this.approval = approval;
        this.history = history;
        this.employees = employees;
    }

    // ---- read ----------------------------------------------------------------

    /** The areas the signed-in user may see (admin all, manager self + team, user own). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<AreaDto> list() {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        Map<Long, String> routes = new HashMap<>();
        maps.findAll().stream().filter(m -> "Y".equals(m.getStatus()))
                .forEach(m -> routes.merge(m.getAreaOid(), m.getRouteName(), (x, y) -> x + ", " + y));
        return scope.filter(TeamScope.Scope.TEAM_SELF, areas.findAllByOrderByOidAsc(), Area::getEmpId).stream()
                .map(a -> { a.setRoutes(routes.get(a.getOid())); return toDto(a, me); })
                .toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public AreaDto get(Long oid) {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        Area a = reachable(oid, me);
        a.setRoutes(maps.findByAreaOidAndStatus(oid, "Y").stream().map(RouteAreaMap::getRouteName).collect(Collectors.joining(", ")));
        return toDto(a, me);
    }

    /** What the page needs: can the user pick the employee (admin), the employees to pick from, the flow. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> options() {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pickEmployee", me.isAdmin());
        m.put("me", me.empId);
        m.put("meName", me.name(me.empId));
        Set<String> adminIds = scope.adminLevelIds();   // admin-level users never appear in a dropdown
        m.put("employees", !me.isAdmin() ? List.of() : employees.findByActiveTrueOrderByEmpNameAsc().stream()
                .filter(e -> !adminIds.contains(e.getEmpId()))
                .map(e -> Map.of("oid", e.getEmpId(), "label", e.getEmpName() + (e.getEmpCode() == null ? "" : " (" + e.getEmpCode() + ")")))
                .toList());
        m.put("approvalFlow", approval.flow(APPROVAL));
        return m;
    }

    // ---- write ---------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager")
    public AreaDto save(AreaDto form) {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        boolean isNew = form.getOid() == null;
        Area a = isNew ? new Area() : reachable(form.getOid(), me);
        a.setEmpId(owner(form.getEmpId(), isNew ? null : a.getEmpId(), isNew, me));
        apply(a, form);
        saveWithRequest(a, isNew, me);
        return toDto(a, me);
    }

    /** Add Multiple — each row is a new area of its employee (blank / duplicate / invalid rows skipped with the reason). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<AreaDto> forms) {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        int saved = 0, skipped = 0, row = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (AreaDto f : forms) {
            row++;
            try {
                String code = Strings.isBlank(f.getAreaCode()) ? null : f.getAreaCode().trim();
                if (code != null && !seen.add(code.toUpperCase(Locale.ROOT))) throw new IllegalStateException("code " + code + " repeated in this list");
                Area a = new Area();
                a.setEmpId(owner(f.getEmpId(), null, true, me));
                apply(a, f);
                saveWithRequest(a, true, me);
                saved++;
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Row " + row + ": " + e.getMessage());
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("saved", saved);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return res;
    }

    /** Activate / deactivate. An area becomes active only once its add request is approved. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public AreaDto updateStatus(Long oid, String status) {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        Area a = reachable(oid, me);
        if (!"Y".equals(status) && !"N".equals(status)) throw new IllegalStateException("Status must be Y or N");
        if ("Y".equals(status) && !ApprovalEngine.isApproved(a)) {
            throw new IllegalStateException("This area is not approved yet — it becomes active when its add request is approved");
        }
        a.setStatus(status);
        a.setUpdatedBy(me.empId);
        areas.save(a);
        return toDto(a, me);
    }

    /** Ask for the area to be deleted (made inactive); it stays active until the request is finally approved. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public AreaDto requestDelete(Long oid, String reason) {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        Area a = reachable(oid, me);
        List<ApprovalEngine.LogRow> rows = approval.requestDelete(a, reason, APPROVAL, me);
        a.setUpdatedBy(me.empId);
        areas.save(a);
        store(a.getOid(), rows);
        return toDto(a, me);
    }

    /** Approve the step the area's open request (add or delete) is waiting for. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public AreaDto approve(Long oid) {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        Area a = reachable(oid, me);
        ApprovalEngine.LogRow row = approval.approve(a, APPROVAL, me);
        a.setUpdatedBy(me.empId);
        areas.save(a);
        store(a.getOid(), List.of(row));
        return toDto(a, me);
    }

    /** Reject the open request (reason required). A rejected new area goes for approval again when edited. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public AreaDto reject(Long oid, String reason) {
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        Area a = reachable(oid, me);
        ApprovalEngine.LogRow row = approval.reject(a, reason, APPROVAL, me);
        a.setUpdatedBy(me.empId);
        areas.save(a);
        store(a.getOid(), List.of(row));
        return toDto(a, me);
    }

    /** Every request / approval / rejection of the area, oldest first. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<AreaMasterApproval> history(Long oid) {
        reachable(oid, approval.actor(APPROVAL));
        return history.findByAreaIdOrderByActionAtAscIdAsc(oid);
    }

    // ---- bulk upload -----------------------------------------------------------

    public byte[] template() throws Exception {
        return ExcelUtil.template("Area",
                List.of("area_code", "area_name", "city", "hq", "state", "pincode", "area_type", "distance_km", "emp_code"),
                List.of("AR-XXX", "Sample Area", "Mumbai", "HQ-MUM", "MH", "400001", "Core", "5", "(admin only) employee code"));
    }

    /**
     * Bulk upload = upsert by area_code; hq/state resolved by code or name. An
     * existing code updates that area (only if it is within the user's reach); a
     * new code adds an area of the row's emp_code (admin) or of the user himself,
     * which then goes for approval.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        ApprovalEngine.Actor me = approval.actor(APPROVAL);
        int inserted = 0, updated = 0, skipped = 0, r = 1;
        List<String> errors = new ArrayList<>();
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            r++;
            String code = row.get("area_code");
            try {
                if (Strings.isBlank(code) && Strings.isBlank(row.get("area_name"))) continue;
                Area a = Strings.isBlank(code) ? null : areas.findByAreaCode(code.trim()).orElse(null);
                boolean isNew = a == null;
                if (isNew) a = new Area();
                else if (!scope.allows(TeamScope.Scope.TEAM_SELF, a.getEmpId())) throw new IllegalStateException("area " + code.trim() + " is not yours");
                String empId = null;
                if (!Strings.isBlank(row.get("emp_code"))) {
                    String ec = row.get("emp_code").trim();
                    empId = employees.findByEmpCode(ec).map(e -> e.getEmpId()).orElseThrow(() -> new IllegalStateException("unknown emp_code " + ec));
                }
                a.setEmpId(owner(empId, isNew ? null : a.getEmpId(), isNew, me));
                AreaDto f = new AreaDto();
                f.setAreaCode(code);
                f.setAreaName(row.get("area_name"));
                f.setCity(row.get("city"));
                f.setHqOid(resolveHq(row.get("hq")));
                f.setStateOid(resolveState(row.get("state")));
                f.setPincode(row.get("pincode"));
                f.setAreaType(row.get("area_type"));
                f.setDistanceKm(Numbers.toBigDecimal(row.get("distance_km")));
                apply(a, f);
                saveWithRequest(a, isNew, me);
                if (isNew) inserted++; else updated++;
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Row " + r + (Strings.isBlank(code) ? "" : " (" + code.trim() + ")") + ": " + e.getMessage());
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("inserted", inserted);
        res.put("updated", updated);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return res;
    }

    // ---- helpers ---------------------------------------------------------------

    /**
     * The area's employee: an admin's choice (blank = none); everyone else gets
     * himself on a new area and cannot move an existing one to someone else.
     */
    private String owner(String wanted, String current, boolean isNew, ApprovalEngine.Actor me) {
        String w = Strings.isBlank(wanted) ? null : wanted.trim();
        if (me.isAdmin()) {
            if (w != null && employees.findById(w).isEmpty()) throw new IllegalStateException("Employee not found: " + w);
            return w;
        }
        if (me.empId == null) throw new ForbiddenException("Your login is not linked to an employee");
        if (isNew) return me.empId;
        return current;
    }

    private void apply(Area a, AreaDto f) {
        String code = Strings.isBlank(f.getAreaCode()) ? null : f.getAreaCode().trim();
        String name = Strings.isBlank(f.getAreaName()) ? null : f.getAreaName().trim();
        if (code == null) throw new IllegalStateException("Area code is required");
        if (code.length() > 40) throw new IllegalStateException("Area code can be at most 40 characters");
        if (name == null) throw new IllegalStateException("Area name is required");
        if (name.length() > 150) throw new IllegalStateException("Area name can be at most 150 characters");
        areas.findByAreaCode(code).filter(o -> !o.getOid().equals(a.getOid()))
                .ifPresent(o -> { throw new IllegalStateException("Area code " + code + " already exists (" + o.getAreaName() + ")"); });
        a.setAreaCode(code);
        a.setAreaName(name);
        a.setCity(Strings.isBlank(f.getCity()) ? null : f.getCity().trim());
        a.setStateOid(f.getStateOid());
        a.setHqOid(f.getHqOid());
        a.setPincode(Strings.isBlank(f.getPincode()) ? null : f.getPincode().trim());
        a.setAreaType(Strings.isBlank(f.getAreaType()) ? null : f.getAreaType().trim());
        a.setDistanceKm(f.getDistanceKm());
    }

    /** Save; a new area (or one corrected after a rejection) asks for approval. */
    private void saveWithRequest(Area a, boolean isNew, ApprovalEngine.Actor me) {
        boolean request = isNew || ApprovalEngine.REJECTED.equals(a.getApprovalStatus());
        if (isNew) a.setCreatedBy(me.empId);
        a.setUpdatedBy(me.empId);
        List<ApprovalEngine.LogRow> log = request ? approval.requestAdd(a, APPROVAL, me) : List.of();
        areas.saveAndFlush(a);
        store(a.getOid(), log);
    }

    private void store(Long areaId, List<ApprovalEngine.LogRow> rows) {
        for (ApprovalEngine.LogRow r : rows) {
            AreaMasterApproval h = new AreaMasterApproval();
            h.setAreaId(areaId);
            h.setRequestType(r.requestType());
            h.setStep(r.step());
            h.setAction(r.action());
            h.setStatusAfter(r.statusAfter());
            h.setRemarks(r.remarks());
            h.setEmpId(r.by().empId);
            h.setRole(r.by().role);
            h.setEmpCode(r.by().empCode());
            h.setEmpName(r.by().empName());
            h.setDesignationName(r.by().designationName());
            h.setHierarchyLevel(r.by().hierarchyLevel());
            h.setActionAt(LocalDateTime.now());
            history.save(h);
        }
    }

    /** The area, if the user may see it (admin all, manager self + team, user own). */
    private Area reachable(Long oid, ApprovalEngine.Actor me) {
        Area a = areas.findById(oid).orElseThrow(() -> new IllegalStateException("Area not found: " + oid));
        scope.require(TeamScope.Scope.TEAM_SELF, a.getEmpId(), "This area is not yours");
        return a;
    }

    private AreaDto toDto(Area a, ApprovalEngine.Actor me) {
        AreaDto d = mapper.toDto(a);
        if (d.getEmpName() == null && a.getEmpId() != null) d.setEmpName(me.name(a.getEmpId()));
        d.setAddRequestedByName(me.name(a.getAddRequestedBy()));
        d.setManagerApprovedByName(me.name(a.getManagerApprovedBy()));
        d.setApprovedByName(me.name(a.getApprovedBy()));
        d.setRejectedByName(me.name(a.getRejectedBy()));
        d.setDeleteRequestedByName(me.name(a.getDeleteRequestedBy()));
        String req = ApprovalEngine.pendingRequest(a);
        d.setPendingRequest(req);
        d.setCanApprove(req != null && approval.canApprove(a, APPROVAL, me));
        return d;
    }

    private Long resolveHq(String v) {
        return Strings.isBlank(v) ? null : hqs.findFirstByHqCodeOrHqName(v.trim(), v.trim()).map(h -> h.getOid()).orElse(null);
    }
    private Long resolveState(String v) {
        return Strings.isBlank(v) ? null : states.findFirstByStateCodeOrStateName(v.trim(), v.trim()).map(s -> s.getOid()).orElse(null);
    }
}
