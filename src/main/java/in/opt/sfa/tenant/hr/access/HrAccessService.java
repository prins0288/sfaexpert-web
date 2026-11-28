package in.opt.sfa.tenant.hr.access;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.calc.RoleResolver;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** HR roles and the permission matrix, both in the database. Implicit roles come from the hierarchy level (see {@link RoleResolver}). */
@Service
public class HrAccessService {

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;

    public HrAccessService(JdbcTemplate jdbc, AuditLogService audit) { this.jdbc = jdbc; this.audit = audit; }

    private Map<String, Set<String>> permissionsByRole() {
        Map<String, Set<String>> m = new HashMap<>();
        jdbc.query("SELECT r.code AS role, p.permission_code AS perm FROM hr_role r JOIN hr_role_permission p ON p.role_id = r.id WHERE r.is_active = 1",
                rs -> { m.computeIfAbsent(rs.getString("role"), k -> new HashSet<>()).add(rs.getString("perm")); });
        return m;
    }

    private List<String> explicitRoles(String empId) {
        return jdbc.queryForList("SELECT r.code FROM hr_emp_role er JOIN hr_role r ON r.id = er.role_id WHERE er.emp_id = ? AND r.is_active = 1", String.class, empId);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Set<String> myRoles() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null) return Set.of();
        return RoleResolver.roles(u.role(), u.empId() == null ? List.of() : explicitRoles(u.empId()));
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public boolean has(String permission) { return RoleResolver.allowed(myRoles(), permissionsByRole(), permission); }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public void require(String permission) {
        if (!has(permission)) throw new ForbiddenException("You do not have the HR permission " + permission);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> me() {
        Set<String> roles = myRoles();
        Map<String, Set<String>> pr = permissionsByRole();
        List<String> perms = jdbc.queryForList("SELECT code FROM hr_permission_def ORDER BY sort_order", String.class).stream()
                .filter(p -> RoleResolver.allowed(roles, pr, p)).toList();
        return Map.of("roles", roles, "permissions", perms);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> matrix() {
        require("HR_ROLES");
        List<Map<String, Object>> roles = jdbc.queryForList("SELECT id, code, name, description, is_system AS isSystem, is_active AS active FROM hr_role ORDER BY id");
        List<Map<String, Object>> perms = jdbc.queryForList("SELECT code, name, module FROM hr_permission_def ORDER BY sort_order");
        Map<Long, List<String>> byRole = new HashMap<>();
        jdbc.query("SELECT role_id, permission_code FROM hr_role_permission", rs -> { byRole.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2)); });
        roles.forEach(r -> r.put("permissions", byRole.getOrDefault(((Number) r.get("id")).longValue(), List.of())));
        return Map.of("roles", roles, "permissions", perms);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void setRolePermissions(long roleId, List<String> codes, String reason) {
        require("HR_ROLES");
        String role = jdbc.queryForList("SELECT code FROM hr_role WHERE id = ?", String.class, roleId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Role not found"));
        if ("SUPER_ADMIN".equals(role)) throw new IllegalArgumentException("Super Admin always has every permission");
        Set<String> valid = new HashSet<>(jdbc.queryForList("SELECT code FROM hr_permission_def", String.class));
        Set<String> want = new TreeSet<>();
        for (String c : codes) { if (!valid.contains(c)) throw new IllegalArgumentException("Unknown permission " + c); want.add(c); }
        Set<String> have = new TreeSet<>(jdbc.queryForList("SELECT permission_code FROM hr_role_permission WHERE role_id = ?", String.class, roleId));
        for (String c : want) if (!have.contains(c)) {
            jdbc.update("INSERT INTO hr_role_permission (role_id, permission_code) VALUES (?,?)", roleId, c);
            audit.log("ROLE_PERMISSION", role, null, "CREATE", c, null, "granted", reason);
        }
        for (String c : have) if (!want.contains(c)) {
            jdbc.update("DELETE FROM hr_role_permission WHERE role_id = ? AND permission_code = ?", roleId, c);
            audit.log("ROLE_PERMISSION", role, null, "DELETE", c, "granted", null, reason);
        }
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> assignments() {
        require("HR_ROLES");
        return jdbc.queryForList("SELECT e.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS name, GROUP_CONCAT(r.code ORDER BY r.id) AS roles "
                + "FROM hr_emp_role er JOIN hr_role r ON r.id = er.role_id JOIN emp_detail e ON e.emp_id = er.emp_id GROUP BY e.emp_id, e.emp_code, e.emp_name ORDER BY e.emp_name");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void setEmployeeRoles(String empId, List<String> roleCodes, String reason) {
        require("HR_ROLES");
        if (jdbc.queryForList("SELECT 1 FROM emp_detail WHERE emp_id = ?", Integer.class, empId).isEmpty()) throw new IllegalArgumentException("Employee not found");
        Set<String> want = new TreeSet<>(roleCodes);
        want.remove("EMPLOYEE");   // everybody has it implicitly
        for (String c : want) if (jdbc.queryForList("SELECT 1 FROM hr_role WHERE code = ? AND is_active = 1", Integer.class, c).isEmpty()) throw new IllegalArgumentException("Unknown role " + c);
        if (want.contains("SUPER_ADMIN") && !myRoles().contains("SUPER_ADMIN")) throw new ForbiddenException("Only a Super Admin can hand out Super Admin");
        Set<String> have = new TreeSet<>(explicitRoles(empId));
        String me = UserContext.get() == null ? null : UserContext.get().empId();
        for (String c : want) if (!have.contains(c)) {
            jdbc.update("INSERT INTO hr_emp_role (emp_id, role_id, granted_by) SELECT ?, id, ? FROM hr_role WHERE code = ?", empId, me, c);
            audit.log("EMP_ROLE", c, empId, "CREATE", "role", null, c, reason);
        }
        for (String c : have) if (!want.contains(c)) {
            jdbc.update("DELETE er FROM hr_emp_role er JOIN hr_role r ON r.id = er.role_id WHERE er.emp_id = ? AND r.code = ?", empId, c);
            audit.log("EMP_ROLE", c, empId, "DELETE", "role", c, null, reason);
        }
    }
}
