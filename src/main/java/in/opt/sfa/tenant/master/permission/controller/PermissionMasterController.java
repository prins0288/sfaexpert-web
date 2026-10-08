package in.opt.sfa.tenant.master.permission.controller;

import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.master.permission.dto.PermissionAssignmentDto;
import in.opt.sfa.tenant.master.permission.dto.PermissionDefinitionDto;
import in.opt.sfa.tenant.master.permission.entity.PermissionTargetType;
import in.opt.sfa.tenant.master.permission.service.PermissionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin screens: the catalog of permission codes (permission_master) and who
 * they're assigned to (permission_assignment) — by emp_id, designation, or
 * emp_level. ADMIN / SUPER_ADMIN only.
 */
@RestController
@RequestMapping("/api/master/permission-master")
@Tag(name = "Permission Master", description = "Permission catalog + emp_id/designation/emp_level assignments")
public class PermissionMasterController {

    private final PermissionAdminService service;

    public PermissionMasterController(PermissionAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all permission codes in the catalog")
    public List<PermissionDefinitionDto> listDefinitions() {
        Authz.requireRole("ADMIN");
        return service.listDefinitions();
    }

    @PostMapping
    @Operation(summary = "Create or update a permission code", description = "Send oid to update, omit it to create.")
    public PermissionDefinitionDto saveDefinition(@RequestBody PermissionDefinitionDto form) {
        Authz.requireRole("ADMIN");
        return service.saveDefinition(form);
    }

    @GetMapping("/targets")
    @Operation(summary = "Assignable targets: active employees (emp_id), designations (designation_code) and emp levels")
    public PermissionAdminService.Targets targets() {
        Authz.requireRole("ADMIN");
        return service.targets();
    }

    @GetMapping("/assignments")
    @Operation(summary = "List assignments, optionally filtered to one target (targetType + targetValue)")
    public List<PermissionAssignmentDto> listAssignments(
            @RequestParam(required = false) PermissionTargetType targetType,
            @RequestParam(required = false) String targetValue) {
        Authz.requireRole("ADMIN");
        return service.listAssignments(targetType, targetValue);
    }

    @PostMapping("/assignments")
    @Operation(summary = "Grant or deny a permission to an emp_id, a designation, or an emp_level",
            description = "Upserts on (targetType, targetValue, permissionCode). allowed=false denies even if the " +
                    "designation/emp_level above it is allowed — emp_id rows take precedence over designation, which " +
                    "takes precedence over emp_level.")
    public PermissionAssignmentDto saveAssignment(@RequestBody PermissionAssignmentDto form) {
        Authz.requireRole("ADMIN");
        return service.saveAssignment(form);
    }

    @DeleteMapping("/assignments/{oid}")
    @Operation(summary = "Remove an assignment (reverts that target to the default — no row = full permission)")
    public void deleteAssignment(@PathVariable Long oid) {
        Authz.requireRole("ADMIN");
        service.deleteAssignment(oid);
    }
}
