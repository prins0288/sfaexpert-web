package in.opt.sfa.tenant.dcr.submit.service;

import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Senior Client Access: which clients of his team a senior / manager may check in, and whether the location check is ON for that
 * manager + client (manager_client_geo, unique per manager + client). Admin only.
 */
@Service
public class SeniorClientService {
    private final JdbcTemplate jdbc;
    private final TeamScope scope;

    public SeniorClientService(JdbcTemplate jdbc, TeamScope scope) { this.jdbc = jdbc; this.scope = scope; }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(String empId) {
        Authz.requireRole("ADMIN");
        String sql = """
                SELECT m.id, mg.emp_code AS managerCode, mg.emp_name AS manager, c.client_code AS clientCode, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS client,
                       ow.emp_name AS owner, m.geo_enabled AS geoEnabled, m.created_by AS createdBy, m.created_at AS createdAt
                FROM manager_client_geo m JOIN emp_detail mg ON mg.emp_id = m.emp_id JOIN client_master c ON c.id = m.client_id LEFT JOIN emp_detail ow ON ow.emp_id = c.mapped_emp_code""";
        return empId == null || empId.isBlank() ? jdbc.queryForList(sql + " ORDER BY mg.emp_name, c.client_name") : jdbc.queryForList(sql + " WHERE m.emp_id = ? ORDER BY c.client_name", empId);
    }

    /** The clients of the employees under this manager (any depth), with whether he already has them. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> candidates(String empId) {
        Authz.requireRole("ADMIN");
        Set<String> team = scope.team(empId);
        if (team.isEmpty()) return List.of();
        String in = String.join(",", Collections.nCopies(team.size(), "?"));
        List<Object> a = new ArrayList<>(); a.add(empId); a.addAll(team);
        return jdbc.queryForList("""
                SELECT c.id, c.client_code AS code, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS name, ow.emp_name AS owner,
                       EXISTS (SELECT 1 FROM manager_client_geo m WHERE m.emp_id = ? AND m.client_id = c.id) AS assigned
                FROM client_master c LEFT JOIN emp_detail ow ON ow.emp_id = c.mapped_emp_code
                WHERE c.is_active = 1 AND c.approval_status = 'APPROVED' AND c.mapped_emp_code IN (""" + in + ") ORDER BY c.client_name", a.toArray());
    }

    /** Give the manager these clients; a manager + client that already exists is skipped (unique key), never duplicated. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> add(String empId, List<Long> clientIds, boolean geoEnabled) {
        Authz.requireRole("ADMIN");
        if (empId == null || empId.isBlank()) throw new IllegalArgumentException("Choose the manager");
        if (clientIds == null || clientIds.isEmpty()) throw new IllegalArgumentException("Choose at least one client");
        Set<String> team = scope.team(empId);
        int created = 0, skipped = 0;
        for (Long cid : new LinkedHashSet<>(clientIds)) {
            List<String> owner = jdbc.query("SELECT mapped_emp_code FROM client_master WHERE id = ?", (rs, i) -> rs.getString(1), cid);
            if (owner.isEmpty() || owner.get(0) == null || !team.contains(owner.get(0))) throw new IllegalArgumentException("Client " + cid + " does not belong to the team of this manager");
            int n = jdbc.update("INSERT IGNORE INTO manager_client_geo (emp_id, client_id, geo_enabled, created_by) VALUES (?,?,?,?)", empId, cid, geoEnabled ? 1 : 0, TeamScope.me());
            if (n == 0) skipped++; else created++;
        }
        return Map.of("created", created, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void setGeo(long id, boolean enabled) {
        Authz.requireRole("ADMIN");
        jdbc.update("UPDATE manager_client_geo SET geo_enabled = ? WHERE id = ?", enabled ? 1 : 0, id);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int remove(List<Long> ids) {
        Authz.requireRole("ADMIN");
        int n = 0; for (Long id : ids) n += jdbc.update("DELETE FROM manager_client_geo WHERE id = ?", id);
        return n;
    }
}
