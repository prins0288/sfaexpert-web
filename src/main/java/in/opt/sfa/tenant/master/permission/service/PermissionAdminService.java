package in.opt.sfa.tenant.master.permission.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.permission.dto.PermissionAssignmentDto;
import in.opt.sfa.tenant.master.permission.dto.PermissionDefinitionDto;
import in.opt.sfa.tenant.master.permission.entity.PermissionAssignment;
import in.opt.sfa.tenant.master.permission.entity.PermissionDefinition;
import in.opt.sfa.tenant.master.permission.entity.PermissionTargetType;
import in.opt.sfa.tenant.master.permission.repository.PermissionAssignmentRepository;
import in.opt.sfa.tenant.master.permission.repository.PermissionDefinitionRepository;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.repository.DesignationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Admin CRUD for the permission catalog (permission_master) and its assignments (permission_assignment). */
@Service
public class PermissionAdminService {

    private final PermissionDefinitionRepository definitions;
    private final PermissionAssignmentRepository assignments;
    private final EmpDetailRepository empDetails;
    private final DesignationRepository designations;

    public PermissionAdminService(PermissionDefinitionRepository definitions, PermissionAssignmentRepository assignments,
                                  EmpDetailRepository empDetails, DesignationRepository designations) {
        this.definitions = definitions;
        this.assignments = assignments;
        this.empDetails = empDetails;
        this.designations = designations;
    }

    public record EmpOption(String empId, String empCode, String name) {}
    public record DesignationOption(String code, String name, Integer empLevel) {}
    /** Everything the admin screen can assign to — the same identity sources the JWT claims come from. */
    public record Targets(List<EmpOption> employees, List<DesignationOption> designations, List<Integer> empLevels) {}

    // ---- assignable targets (emp_id / designation_code / emp_level) ---------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Targets targets() {
        List<EmpDetail> emps = empDetails.findAllByOrderByEmpNameAsc().stream()
                .filter(e -> e.isActive() && e.getEmpId() != null).toList();
        List<DesignationOption> desigs = designations.findByStatusOrderByDesignationNameAsc("Y").stream()
                .filter(d -> !Strings.isBlank(d.getDesignationCode()))
                .map(d -> new DesignationOption(d.getDesignationCode(), d.getDesignationName(), d.getEmpLevel()))
                .toList();
        TreeSet<Integer> levels = new TreeSet<>();
        emps.stream().map(EmpDetail::getEmpLevel).filter(Objects::nonNull).forEach(levels::add);
        desigs.stream().map(DesignationOption::empLevel).filter(Objects::nonNull).forEach(levels::add);
        return new Targets(
                emps.stream().map(e -> new EmpOption(e.getEmpId(), e.getEmpCode(), e.getEmpName())).toList(),
                desigs, List.copyOf(levels));
    }

    // ---- permission_master (catalog) ---------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<PermissionDefinitionDto> listDefinitions() {
        return definitions.findAllByOrderByModuleAscPermissionCodeAsc().stream()
                .map(PermissionAdminService::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public PermissionDefinitionDto saveDefinition(PermissionDefinitionDto form) {
        PermissionDefinition target = form.getOid() != null
                ? definitions.findById(form.getOid()).orElseThrow(() -> new IllegalStateException("Permission not found: " + form.getOid()))
                : new PermissionDefinition();
        target.setPermissionCode(Strings.isBlank(form.getPermissionCode()) ? null : form.getPermissionCode().trim());
        target.setModule(form.getModule());
        target.setDescription(form.getDescription());
        target.setStatus(form.getStatus() == null ? Boolean.TRUE : form.getStatus());
        return toDto(definitions.save(target));
    }

    // ---- permission_assignment (who gets what) ------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<PermissionAssignmentDto> listAssignments(PermissionTargetType targetType, String targetValue) {
        List<PermissionAssignment> rows = (targetType != null && targetValue != null)
                ? assignments.findByTargetTypeAndTargetValueOrderByPermissionCodeAsc(targetType, targetValue)
                : assignments.findAllByOrderByOidAsc();
        return rows.stream().map(PermissionAdminService::toDto).collect(Collectors.toList());
    }

    /** Create or update the assignment for (targetType, targetValue, permissionCode) — the unique key. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public PermissionAssignmentDto saveAssignment(PermissionAssignmentDto form) {
        PermissionAssignment target = assignments
                .findByTargetTypeAndTargetValueAndPermissionCode(form.getTargetType(), form.getTargetValue(), form.getPermissionCode())
                .orElseGet(PermissionAssignment::new);
        target.setTargetType(form.getTargetType());
        target.setTargetValue(form.getTargetValue().trim());
        target.setPermissionCode(form.getPermissionCode().trim());
        target.setAllowed(form.getAllowed() == null ? Boolean.TRUE : form.getAllowed());
        return toDto(assignments.save(target));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteAssignment(Long oid) {
        assignments.deleteById(oid);
    }

    private static PermissionDefinitionDto toDto(PermissionDefinition e) {
        PermissionDefinitionDto d = new PermissionDefinitionDto();
        d.setOid(e.getOid());
        d.setPermissionCode(e.getPermissionCode());
        d.setModule(e.getModule());
        d.setDescription(e.getDescription());
        d.setStatus(e.getStatus());
        d.setCreatedAt(e.getCreatedAt());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        return d;
    }

    private static PermissionAssignmentDto toDto(PermissionAssignment e) {
        PermissionAssignmentDto d = new PermissionAssignmentDto();
        d.setOid(e.getOid());
        d.setTargetType(e.getTargetType());
        d.setTargetValue(e.getTargetValue());
        d.setPermissionCode(e.getPermissionCode());
        d.setAllowed(e.getAllowed());
        d.setCreatedAt(e.getCreatedAt());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        return d;
    }
}
