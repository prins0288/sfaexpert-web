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
 * then a row targeting their designation (id), then a row targeting their
 * designation's hierarchy level. If NONE of those rows exist, the code's own default_allowed
 * decides — true for almost every code, so a company that never configures
 * permission_assignment keeps full access; false for opt-in features such as
 * COLUMN_SETTINGS, which nobody gets until they are granted it.
 */
@Service
public class PermissionService {

    private final PermissionAssignmentRepository assignments;
    private final PermissionDefinitionRepository definitions;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public PermissionService(PermissionAssignmentRepository assignments, PermissionDefinitionRepository definitions,
                             org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.assignments = assignments;
        this.definitions = definitions;
    }

    /** True unless the current user is explicitly denied this permission. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public boolean isAllowed(String permissionCode) {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null) return false; // no verified identity on the thread -> deny
        if ("SUPER_ADMIN".equals(u.role())) return true;
        return resolve(u.empId(), u.designationId(), u.hierarchyLevel(), permissionCode);
    }

    /** Effective permission: the most specific assignment, else the code's own default. */
    public boolean resolve(String empId, Integer designationId, Integer hierarchyLevel, String permissionCode) {
        Boolean explicit = explicitRule(empId, designationId, hierarchyLevel, permissionCode);
        if (explicit != null) return explicit;
        return definitions.findByPermissionCode(permissionCode)
                .map(d -> !Boolean.FALSE.equals(d.getDefaultAllowed()))
                .orElse(true);   // code not in the catalog at all -> allowed, as before
    }

    /** The matching assignment's allowed flag (EMP_ID > DESIGNATION > HIERARCHY_LEVEL), or null if none. */
    private Boolean explicitRule(String empId, Integer designationId, Integer hierarchyLevel, String permissionCode) {
        if (empId != null) {
            var row = assignments.findByTargetTypeAndTargetValueAndPermissionCode(
                    PermissionTargetType.EMP_ID, empId, permissionCode);
            if (row.isPresent()) return row.get().getAllowed();
        }
        if (designationId != null) {
            var row = assignments.findByTargetTypeAndTargetValueAndPermissionCode(
                    PermissionTargetType.DESIGNATION, String.valueOf(designationId), permissionCode);
            if (row.isPresent()) return row.get().getAllowed();
        }
        if (hierarchyLevel != null) {
            var row = assignments.findByTargetTypeAndTargetValueAndPermissionCode(
                    PermissionTargetType.HIERARCHY_LEVEL, String.valueOf(hierarchyLevel), permissionCode);
            if (row.isPresent()) return row.get().getAllowed();
        }
        return null;
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
            boolean allowed;
            if (superAdmin || u == null) {
                allowed = superAdmin;
            } else {
                Boolean explicit = explicitRule(u.empId(), u.designationId(), u.hierarchyLevel(), d.getPermissionCode());
                allowed = explicit != null ? explicit : !Boolean.FALSE.equals(d.getDefaultAllowed());
            }
            result.put(d.getPermissionCode(), allowed);
        }
        return result;
    }

    /**
     * Called when an employee is created: writes the user's OWN permission rows (target EMP_ID) for every
     * active permission - what their designation / hierarchy level gives, else the code's default - so the
     * permission page shows them and a missing / odd default can never lock a new user out (e.g. DCR_SAVE).
     * Existing rows are left alone.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void grantDefaults(String empId, Integer designationId) {
        Integer level = designationId == null ? null : jdbc.query("SELECT hierarchy_level FROM designation_master WHERE id = ?",
                rs -> rs.next() ? (Integer) rs.getObject(1) : null, designationId);
        jdbc.update("""
                INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
                SELECT 'EMP_ID', ?, m.permission_code,
                       COALESCE((SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'DESIGNATION' AND a.target_value = ? AND a.permission_code = m.permission_code),
                                (SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'HIERARCHY_LEVEL' AND a.target_value = ? AND a.permission_code = m.permission_code),
                                m.default_allowed),
                       NOW(), 'auto-on-create'
                FROM permission_master m WHERE m.status = 1""",
                empId, designationId == null ? "-" : String.valueOf(designationId), level == null ? "-" : String.valueOf(level));
    }
}
