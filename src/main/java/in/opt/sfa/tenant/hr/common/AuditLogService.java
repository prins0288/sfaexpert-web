package in.opt.sfa.tenant.hr.common;

import in.opt.sfa.security.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** One append-only audit table for everything HR: who, when, entity, field, old, new, reason. Callers run inside their own transaction. */
@Service
public class AuditLogService {

    private final JdbcTemplate jdbc;

    public AuditLogService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void log(String entity, Object entityId, String subjectEmpId, String action, String field, String oldValue, String newValue, String reason) {
        UserContext.CurrentUser u = UserContext.get();
        String by = u == null ? null : u.empId();
        String name = null;
        if (by != null) {
            try { name = jdbc.queryForList("SELECT emp_name FROM emp_detail WHERE emp_id = ?", String.class, by).stream().findFirst().orElse(null); }
            catch (Exception ignored) { }
        }
        jdbc.update("INSERT INTO hr_audit_log (by_emp_id, by_name, entity, entity_id, subject_emp_id, action, field_name, old_value, new_value, reason) VALUES (?,?,?,?,?,?,?,?,?,?)",
                by, name, entity, entityId == null ? null : entityId.toString(), subjectEmpId, action, field, oldValue, newValue, reason);
    }

    /** Writes one row per changed field (CREATE writes every field of {@code after}, DELETE every field of {@code before}). */
    public void logDiff(String entity, Object entityId, String subjectEmpId, String action, Map<String, ?> before, Map<String, ?> after, String reason) {
        for (AuditDiff.Change c : AuditDiff.diff(before, after)) log(entity, entityId, subjectEmpId, action, c.field(), c.oldValue(), c.newValue(), reason);
    }

    public List<Map<String, Object>> search(String entity, String subjectEmpId, String byEmpId, String from, String to, int limit) {
        StringBuilder sql = new StringBuilder("SELECT a.id, a.at_time AS atTime, a.by_name AS byName, ec.emp_code AS byCode, a.entity, a.entity_id AS entityId, a.action, a.field_name AS field, a.old_value AS oldValue, a.new_value AS newValue, a.reason, s.emp_name AS subjectName, s.emp_code AS subjectCode "
                + "FROM hr_audit_log a LEFT JOIN emp_detail s ON s.emp_id = a.subject_emp_id LEFT JOIN emp_detail ec ON ec.emp_id = a.by_emp_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (entity != null && !entity.isBlank()) { sql.append(" AND a.entity = ?"); args.add(entity); }
        if (subjectEmpId != null && !subjectEmpId.isBlank()) { sql.append(" AND a.subject_emp_id = ?"); args.add(subjectEmpId); }
        if (byEmpId != null && !byEmpId.isBlank()) { sql.append(" AND a.by_emp_id = ?"); args.add(byEmpId); }
        if (from != null && !from.isBlank()) { sql.append(" AND a.at_time >= ?"); args.add(from + " 00:00:00"); }
        if (to != null && !to.isBlank()) { sql.append(" AND a.at_time <= ?"); args.add(to + " 23:59:59"); }
        sql.append(" ORDER BY a.id DESC LIMIT ").append(Math.max(1, Math.min(limit, 500)));
        return jdbc.queryForList(sql.toString(), args.toArray());
    }
}
