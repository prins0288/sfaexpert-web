package in.opt.sfa.tenant.master.permission.service;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.master.permission.entity.PermissionAssignment;
import in.opt.sfa.tenant.master.permission.entity.PermissionDefinition;
import in.opt.sfa.tenant.master.permission.entity.PermissionTargetType;
import in.opt.sfa.tenant.master.permission.repository.PermissionAssignmentRepository;
import in.opt.sfa.tenant.master.permission.repository.PermissionDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves whether the current user has a given permission.
 *
 * Precedence (most specific wins): a row targeting the user's exact emp_id,
 * then a row targeting their designation, then a row targeting their
 * emp_level. If NONE of those rows exist, the permission is allowed by
 * default — a company that never configures permission_assignment keeps full
 * access everywhere, which is the required fallback.
 */
@Service
public class PermissionService {

    private final PermissionAssignmentRepository assignments;
    private final PermissionDefinitionRepository definitions;

    public PermissionService(PermissionAssignmentRepository assignments, PermissionDefinitionRepository definitions) {
        this.assignments = assignments;
        this.definitions = definitions;
    }

    /** True unless the current user is explicitly denied this permission. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public boolean isAllowed(String permissionCode) {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null) return false; // no verified identity on the thread -> deny
        if ("SUPER_ADMIN".equals(u.role())) return true;
        return resolve(u.empId(), u.designationCode(), u.empLevel(), permissionCode);
    }

    public boolean resolve(String empId, String designationCode, Integer empLevel, String permissionCode) {
        if (empId != null) {
            var row = assignments.findByTargetTypeAndTargetValueAndPermissionCode(
                    PermissionTargetType.EMP_ID, empId, permissionCode);
            if (row.isPresent()) return row.get().getAllowed();
        }
        if (designationCode != null) {
            var row = assignments.findByTargetTypeAndTargetValueAndPermissionCode(
                    PermissionTargetType.DESIGNATION, designationCode, permissionCode);
            if (row.isPresent()) return row.get().getAllowed();
        }
        if (empLevel != null) {
            var row = assignments.findByTargetTypeAndTargetValueAndPermissionCode(
                    PermissionTargetType.EMP_LEVEL, String.valueOf(empLevel), permissionCode);
            if (row.isPresent()) return row.get().getAllowed();
        }
        return true; // no assignment configured anywhere -> full permission by default
    }

    /**
     * Every defined permission code mapped to whether the CURRENT user has it —
     * what the front-end calls once after login to decide which buttons/menu
     * items to hide.
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Boolean> myPermissions() {
        UserContext.CurrentUser u = UserContext.get();
        List<PermissionDefinition> defs = definitions.findByStatusTrueOrderByModuleAscPermissionCodeAsc();
        Map<String, Boolean> result = new LinkedHashMap<>();
        boolean superAdmin = u != null && "SUPER_ADMIN".equals(u.role());
        for (PermissionDefinition d : defs) {
            boolean allowed = superAdmin || u == null
                    ? superAdmin
                    : resolve(u.empId(), u.designationCode(), u.empLevel(), d.getPermissionCode());
            result.put(d.getPermissionCode(), allowed);
        }
        return result;
    }
}
