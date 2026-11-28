package in.opt.sfa.tenant.marquee;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The scrolling message in the page header. The text is HTML (the admin writes it; the page filters it again before showing it) with
 * placeholders - {name} {empCode} {designation} {hq} {state} {manager} {date} - filled from the signed-in employee. Substituted values are
 * HTML-escaped. {empId} is accepted but shows the employee CODE: the internal id is never shown. A message with no target is for everyone;
 * otherwise for the chosen states, HQs, designations and employees.
 */
@Service
public class MarqueeService {

    private static final List<String> TARGETS = List.of("STATE", "HQ", "DESIGNATION", "EMP");
    public static final List<String> PLACEHOLDERS = List.of("{name}", "{empCode}", "{designation}", "{hq}", "{state}", "{manager}", "{date}");

    private final JdbcTemplate jdbc;

    public MarqueeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- what the signed-in employee sees ----------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> mine() {
        String me = TeamScope.me();
        Map<String, Object> e = me == null ? Map.of() : jdbc.queryForList("""
                SELECT e.emp_name AS name, e.emp_code AS code, e.state_id AS stateId, e.hq_id AS hqId, e.designation AS desId, d.designation AS designation, h.hq_name AS hq, s.state_name AS state, mg.emp_name AS manager
                FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation LEFT JOIN hq_master h ON h.oid = e.hq_id LEFT JOIN state_master s ON s.oid = e.state_id LEFT JOIN emp_detail mg ON mg.emp_id = e.manager_id
                WHERE e.emp_id = ?""", me).stream().findFirst().orElse(Map.of());
        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList("""
                    SELECT m.id, m.message_html AS html, m.speed FROM marquee_message m
                    WHERE m.is_active = 1 AND (m.starts_at IS NULL OR m.starts_at <= NOW()) AND (m.ends_at IS NULL OR m.ends_at >= NOW())
                      AND (NOT EXISTS (SELECT 1 FROM marquee_target t WHERE t.marquee_id = m.id)
                           OR EXISTS (SELECT 1 FROM marquee_target t WHERE t.marquee_id = m.id AND (
                                (t.target_type = 'STATE' AND t.target_value = ?) OR (t.target_type = 'HQ' AND t.target_value = ?)
                             OR (t.target_type = 'DESIGNATION' AND t.target_value = ?) OR (t.target_type = 'EMP' AND t.target_value = ?))))
                    ORDER BY m.sort_order, m.id""", s(e.get("stateId")), s(e.get("hqId")), s(e.get("desId")), me == null ? "" : me);
        } catch (org.springframework.dao.DataAccessException ex) {
            return List.of();   // migration 100 not run yet - the page simply has no marquee
        }
        Map<String, String> vals = new LinkedHashMap<>();
        vals.put("{name}", s(e.get("name"))); vals.put("{empCode}", s(e.get("code"))); vals.put("{empId}", s(e.get("code")));   // {empId} shows the CODE, never the id
        vals.put("{designation}", s(e.get("designation"))); vals.put("{hq}", s(e.get("hq"))); vals.put("{state}", s(e.get("state"))); vals.put("{manager}", s(e.get("manager")));
        vals.put("{date}", LocalDate.now().toString());
        for (Map<String, Object> r : rows) {
            String html = (String) r.get("html");
            for (Map.Entry<String, String> v : vals.entrySet()) html = html.replace(v.getKey(), HtmlUtils.htmlEscape(v.getValue()));
            r.put("html", html);
        }
        return rows;
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }

    // ---- admin ----------------------------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> config() {
        Authz.requireRole("ADMIN");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("placeholders", PLACEHOLDERS);
        m.put("states", jdbc.queryForList("SELECT oid AS id, state_name AS name FROM state_master ORDER BY state_name"));
        m.put("hqs", jdbc.queryForList("SELECT oid AS id, hq_name AS name FROM hq_master ORDER BY hq_name"));
        m.put("designations", jdbc.queryForList("SELECT id, designation AS name FROM designation_master ORDER BY hierarchy_level, designation"));
        m.put("employees", jdbc.queryForList("SELECT emp_id AS id, CONCAT(emp_name, ' (', emp_code, ')') AS name FROM emp_detail WHERE is_active = 1 ORDER BY emp_name"));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() {
        Authz.requireRole("ADMIN");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, message_html AS html, speed, starts_at AS startsAt, ends_at AS endsAt, sort_order AS sortOrder, is_active AS active FROM marquee_message ORDER BY sort_order, id");
        for (Map<String, Object> r : rows) {
            r.put("targets", jdbc.queryForList("""
                    SELECT t.id, t.target_type AS targetType, t.target_value AS targetValue,
                           CASE t.target_type WHEN 'STATE' THEN (SELECT state_name FROM state_master WHERE oid = CAST(t.target_value AS UNSIGNED))
                             WHEN 'HQ' THEN (SELECT hq_name FROM hq_master WHERE oid = CAST(t.target_value AS UNSIGNED))
                             WHEN 'DESIGNATION' THEN (SELECT designation FROM designation_master WHERE id = CAST(t.target_value AS UNSIGNED))
                             ELSE (SELECT CONCAT(emp_name, ' (', emp_code, ')') FROM emp_detail WHERE emp_id = t.target_value COLLATE utf8mb4_0900_ai_ci) END AS targetName
                    FROM marquee_target t WHERE t.marquee_id = ? ORDER BY t.target_type, targetName""", r.get("id")));
        }
        return rows;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> f) {
        Authz.requireRole("ADMIN");
        String html = f.get("html") == null ? null : f.get("html").toString().trim();
        if (Strings.isBlank(html)) throw new IllegalStateException("Write the message");
        if (html.length() > 2000) throw new IllegalStateException("The message can be at most 2000 characters");
        int speed = f.get("speed") == null || String.valueOf(f.get("speed")).isBlank() ? 60 : Integer.parseInt(String.valueOf(f.get("speed")));
        if (speed < 10 || speed > 300) throw new IllegalStateException("Speed must be between 10 and 300 pixels per second");
        LocalDateTime from = dt(f.get("startsAt")), to = dt(f.get("endsAt"));
        if (from != null && to != null && to.isBefore(from)) throw new IllegalStateException("The end is before the start");
        int sort = f.get("sortOrder") == null || String.valueOf(f.get("sortOrder")).isBlank() ? 0 : Integer.parseInt(String.valueOf(f.get("sortOrder")));
        int active = Boolean.FALSE.equals(f.get("active")) || "false".equals(String.valueOf(f.get("active"))) ? 0 : 1;
        String me = TeamScope.me();
        Long id = f.get("id") == null || Strings.isBlank(String.valueOf(f.get("id"))) ? null : Long.valueOf(String.valueOf(f.get("id")));
        if (id == null) {
            jdbc.update("INSERT INTO marquee_message (message_html, speed, starts_at, ends_at, sort_order, is_active, created_by) VALUES (?,?,?,?,?,?,?)", html, speed, from, to, sort, active, me);
            id = jdbc.queryForObject("SELECT MAX(id) FROM marquee_message", Long.class);
        } else if (jdbc.update("UPDATE marquee_message SET message_html = ?, speed = ?, starts_at = ?, ends_at = ?, sort_order = ?, is_active = ?, updated_by = ?, updated_at = NOW() WHERE id = ?", html, speed, from, to, sort, active, me, id) == 0) {
            throw new IllegalStateException("Message not found");
        }
        if (f.get("assign") instanceof Map<?, ?> a) {
            Map<String, Object> as = (Map<String, Object>) a;
            jdbc.update("DELETE FROM marquee_target WHERE marquee_id = ?", id);
            for (Object[] t : new Object[][]{{"STATE", as.get("states")}, {"HQ", as.get("hqs")}, {"DESIGNATION", as.get("designations")}, {"EMP", as.get("employees")}}) {
                if (!(t[1] instanceof Collection<?> c)) continue;
                if (!TARGETS.contains(t[0])) continue;
                for (Object v : new LinkedHashSet<>(c)) { String val = v == null ? null : v.toString().trim(); if (!Strings.isBlank(val)) jdbc.update("INSERT IGNORE INTO marquee_target (marquee_id, target_type, target_value) VALUES (?,?,?)", id, t[0], val); }
            }
        }
        return Map.of("id", id);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int delete(List<Long> ids) {
        Authz.requireRole("ADMIN");
        if (ids == null || ids.isEmpty()) throw new IllegalStateException("Choose the messages");
        int n = 0;
        for (Long id : ids) n += jdbc.update("DELETE FROM marquee_message WHERE id = ?", id);
        return n;
    }

    private static LocalDateTime dt(Object o) {
        if (o == null || o.toString().isBlank()) return null;
        try { return LocalDateTime.parse(o.toString().trim().replace(' ', 'T').substring(0, Math.min(16, o.toString().trim().length()))); } catch (Exception e) { throw new IllegalStateException("Invalid date / time"); }
    }
}
