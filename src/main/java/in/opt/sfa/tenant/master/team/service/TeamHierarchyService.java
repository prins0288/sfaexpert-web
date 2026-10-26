package in.opt.sfa.tenant.master.team.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.approval.ApprovalEngine;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.master.empdetail.dto.EmpDetailSaveRequest;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpDetailService;
import in.opt.sfa.tenant.repository.DesignationRepository;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Team hierarchy: who reports to whom (emp_detail.manager_id). Add a member
 * under a manager, move someone to another manager, or remove them from a team.
 * Admin handles every employee; a manager only his own team (and may pick up an
 * employee who has no manager yet). Every change is saved through
 * EmpDetailService, so it is applied directly and lands in the promotion /
 * transfer history as MANAGER_CHANGE; loops are refused.
 */
@Service
public class TeamHierarchyService {

    private final EmpDetailRepository employees;
    private final DesignationRepository designations;
    private final ApprovalEngine approval;
    private final EmpDetailService employeeService;
    private final TeamScope scope;

    public TeamHierarchyService(EmpDetailRepository employees, DesignationRepository designations,
                                ApprovalEngine approval, EmpDetailService employeeService, TeamScope scope) {
        this.scope = scope;
        this.employees = employees;
        this.designations = designations;
        this.approval = approval;
        this.employeeService = employeeService;
    }

    /** Every employee with manager, designation, level, team size and whether the user may change him. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() {
        Authz.requireRole("ADMIN", "MANAGER");
        ApprovalEngine.Actor me = approval.reach();
        Map<Integer, Designation> desig = new HashMap<>();
        designations.findAll().forEach(d -> desig.put(d.getId(), d));
        EmpDetail meRow = me.empId == null ? null : employees.findById(me.empId).orElse(null);
        Integer myLevel = meRow == null ? null : level(meRow);
        List<EmpDetail> all = employees.findAllByOrderByEmpNameAsc();
        Map<String, Integer> direct = new HashMap<>();
        all.forEach(e -> { if (e.getManagerId() != null) direct.merge(e.getManagerId(), 1, Integer::sum); });
        List<Map<String, Object>> rows = new ArrayList<>();
        // manager: himself + his team + people with no manager yet (to pick them up); admin: everyone
        Set<String> visible = scope.ids(TeamScope.Scope.TEAM_SELF);
        for (EmpDetail e : all) {
            if (visible != null && !visible.contains(e.getEmpId()) && e.getManagerId() != null) continue;
            Designation d = e.getDesignationId() == null ? null : desig.get(e.getDesignationId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("empId", e.getEmpId());
            m.put("empCode", e.getEmpCode());
            m.put("empName", e.getEmpName());
            m.put("designation", d == null ? null : d.getDesignation());
            m.put("hierarchyLevel", d == null ? null : d.getHierarchyLevel());
            m.put("managerId", e.getManagerId());
            m.put("active", e.isActive());
            m.put("vacant", e.isVacant());
            m.put("directReports", direct.getOrDefault(e.getEmpId(), 0));
            m.put("canEdit", canTouch(me, e, myLevel));
            m.put("isMe", e.getEmpId().equals(me.empId));
            rows.add(m);
        }
        return rows;
    }

    /** Put {empId} under {managerId}; blank managerId = remove from the team (no manager). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> setManager(String empId, String managerId, String effectiveDate, String remarks) {
        Authz.requireRole("ADMIN", "MANAGER");
        ApprovalEngine.Actor me = approval.reach();
        EmpDetail target = employees.findById(empId).orElseThrow(() -> new IllegalStateException("Employee not found: " + empId));
        String newManager = Strings.isBlank(managerId) ? null : managerId.trim();
        EmpDetail meRow = me.empId == null ? null : employees.findById(me.empId).orElse(null);
        if (!canTouch(me, target, meRow == null ? null : level(meRow))) throw new ForbiddenException("You can change only your own team");
        if (Objects.equals(target.getManagerId(), newManager)) throw new IllegalStateException("No change — that is already the manager");
        if (newManager != null) {
            EmpDetail mgr = employees.findById(newManager).orElseThrow(() -> new IllegalStateException("Manager not found: " + newManager));
            if (!mgr.isActive()) throw new IllegalStateException(mgr.getEmpName() + " is inactive and cannot be a manager");
            if (!me.isAdmin() && !newManager.equals(me.empId) && !me.reaches(newManager)) {
                throw new ForbiddenException("You can add people only under yourself or your own team");
            }
            Integer ml = level(mgr), tl = level(target);
            if (ml != null && tl != null && ml <= tl) {
                throw new IllegalStateException(mgr.getEmpName() + " (level " + ml + ") must be senior to " + target.getEmpName() + " (level " + tl + ")");
            }
        }
        EmpDetailSaveRequest req = new EmpDetailSaveRequest();
        req.setEmpId(empId);
        req.setManagerId(newManager == null ? "" : newManager);   // blank clears
        req.setChangeReason("MANAGER_CHANGE");
        req.setEffectiveDate(effectiveDate);
        req.setChangeRemarks(Strings.isBlank(remarks) ? (newManager == null ? "Removed from team" : null) : remarks);
        return employeeService.save(req, true);
    }

    /** Admin: anyone. Manager: his own team's members, or someone who has no manager yet. User: nobody. */
    private boolean canTouch(ApprovalEngine.Actor me, EmpDetail e, Integer myLevel) {
        if (me.isAdmin()) return true;
        if (e.getEmpId().equals(me.empId)) return false;   // nobody re-parents himself
        if (e.getManagerId() != null) return me.reaches(e.getEmpId());
        // someone with no manager yet: only a person junior to the manager can be picked up
        Integer l = level(e);
        return me.empId != null && !"USER".equals(me.role) && (l == null || (myLevel != null && l < myLevel));
    }

    private Integer level(EmpDetail e) {
        return e.getDesignationId() == null ? null
                : designations.findById(e.getDesignationId()).map(Designation::getHierarchyLevel).orElse(null);
    }
}
