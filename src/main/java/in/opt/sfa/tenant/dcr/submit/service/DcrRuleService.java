package in.opt.sfa.tenant.dcr.submit.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.Authz;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

/**
 * DCR rules from app_validations (module DCR). emp_id NULL = master rule; an employee's own row
 * for the same config_code overrides it. Only enabled + active rows count; a missing rule means
 * "not configured" and the caller's default applies.
 */
@Service
public class DcrRuleService {

    private final JdbcTemplate jdbc;

    public DcrRuleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A company-wide (master row) boolean rule. */
    public boolean masterBool(String code, boolean dflt) {
        List<String> l = jdbc.query("SELECT config_value FROM app_validations WHERE module_name = 'DCR' AND config_code = ? AND emp_id IS NULL AND is_enabled = 1 AND is_active = 1 LIMIT 1", (rs, i) -> rs.getString(1), code);
        return l.isEmpty() || l.get(0) == null || l.get(0).isBlank() ? dflt : bool(Map.of("K", l.get(0).trim()), "K", dflt);
    }

    /** The company switch DCR_APPROVAL_REQUIRED (master row; default on). Off = no approval at all. */
    public boolean approvalOn() {
        List<String> l = jdbc.query("SELECT config_value FROM app_validations WHERE module_name = 'DCR' AND config_code = 'DCR_APPROVAL_REQUIRED' AND emp_id IS NULL AND is_enabled = 1 AND is_active = 1 LIMIT 1", (rs, i) -> rs.getString(1));
        return l.isEmpty() || l.get(0) == null || l.get(0).isBlank() || bool(Map.of("K", l.get(0).trim()), "K", true);
    }

    /** The effective rules of one employee: config_code -> value. */
    public Map<String, String> of(String empId) {
        Map<String, String> out = new LinkedHashMap<>();
        // master first, then the employee's own rows override
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT config_code, config_value FROM app_validations
                WHERE module_name = 'DCR' AND is_enabled = 1 AND is_active = 1 AND (emp_id IS NULL OR emp_id = ?)
                ORDER BY emp_id IS NOT NULL""", empId)) {
            out.put((String) r.get("config_code"), r.get("config_value") == null ? "" : r.get("config_value").toString().trim());
        }
        return out;
    }

    public static boolean bool(Map<String, String> rules, String code, boolean dflt) {
        String v = rules.get(code);
        return v == null || v.isEmpty() ? dflt : "TRUE".equalsIgnoreCase(v) || "1".equals(v) || "Y".equalsIgnoreCase(v);
    }

    public static int integer(Map<String, String> rules, String code, int dflt) {
        try { return rules.containsKey(code) ? Integer.parseInt(rules.get(code)) : dflt; } catch (NumberFormatException e) { return dflt; }
    }

    public static LocalTime time(Map<String, String> rules, String code) {
        try { String v = rules.get(code); return Strings.isBlank(v) ? null : LocalTime.parse(v); } catch (Exception e) { return null; }
    }

    /** "08:00-18:00" -> [08:00, 18:00], or null when not configured / malformed. */
    public static LocalTime[] window(Map<String, String> rules, String code) {
        try {
            String v = rules.get(code);
            if (Strings.isBlank(v) || !v.contains("-")) return null;
            String[] p = v.split("-");
            return new LocalTime[]{LocalTime.parse(p[0].trim()), LocalTime.parse(p[1].trim())};
        } catch (Exception e) { return null; }
    }

    /** Gives a new employee their own copy of the master DCR rules. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void copyMasterTo(String empId) {
        jdbc.update("""
                INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, is_enabled, display_order)
                SELECT ?, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, is_enabled, display_order
                FROM app_validations WHERE emp_id IS NULL AND module_name = 'DCR'""", empId);
    }

    // ---- admin ----------------------------------------------------------------------

    /**
     * The rules shown for a scope: the master rows, or - for an employee - EVERY master rule with the employee's own value when he has one,
     * else the master value marked {@code inherited} (saving it then creates his own row).
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(String empId) {
        Authz.requireRole("ADMIN");
        boolean master = Strings.isBlank(empId);
        String cols = "id, emp_id AS empId, config_code AS code, display_name AS name, screen_name AS screen, config_type AS type, data_type AS dataType, config_value AS value, default_value AS defaultValue, description, is_enabled AS enabled, display_order AS ord";
        List<Map<String, Object>> masters = jdbc.queryForList("SELECT " + cols + " FROM app_validations WHERE module_name = 'DCR' AND emp_id IS NULL ORDER BY display_order, id");
        if (master) { masters.forEach(r -> r.put("inherited", false)); return masters; }
        Map<String, Map<String, Object>> own = new HashMap<>();
        jdbc.queryForList("SELECT " + cols + " FROM app_validations WHERE module_name = 'DCR' AND emp_id = ?", empId).forEach(r -> own.put((String) r.get("code"), r));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : masters) {
            Map<String, Object> mine = own.get(m.get("code"));
            if (mine != null) { mine.put("inherited", false); out.add(mine); }
            else { Map<String, Object> x = new LinkedHashMap<>(m); x.put("id", null); x.put("empId", empId); x.put("inherited", true); out.add(x); }
        }
        return out;
    }

    /** Set one rule for one employee: updates his own row, or creates it (copied from the master rule) when he has none yet. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveFor(String empId, String code, String value, Boolean enabled) {
        Authz.requireRole("ADMIN");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM emp_detail WHERE emp_id = ?", Integer.class, empId) == 0) throw new IllegalStateException("Employee not found");
        List<Map<String, Object>> m = jdbc.queryForList("SELECT data_type, is_enabled FROM app_validations WHERE module_name = 'DCR' AND emp_id IS NULL AND config_code = ?", code);
        if (m.isEmpty()) throw new IllegalStateException("Rule not found");
        String v = check((String) m.get(0).get("data_type"), value);
        int en = enabled == null ? (flag(m.get(0).get("is_enabled")) ? 1 : 0) : (enabled ? 1 : 0);
        jdbc.update("""
                INSERT INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, is_enabled, display_order)
                SELECT ?, config_code, display_name, module_name, screen_name, platform, config_type, data_type, ?, default_value, description, ?, display_order
                FROM app_validations WHERE emp_id IS NULL AND module_name = 'DCR' AND config_code = ?
                ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), is_enabled = VALUES(is_enabled)""", empId, v, en, code);
    }

    private static boolean flag(Object o) { return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() == 1; }

    private static String check(String type, String value) {
        String v = value == null ? "" : value.trim();
        if ("BOOLEAN".equals(type) && !v.equalsIgnoreCase("TRUE") && !v.equalsIgnoreCase("FALSE")) throw new IllegalStateException("Value must be TRUE or FALSE");
        if ("INTEGER".equals(type) && !v.matches("\\d{1,6}")) throw new IllegalStateException("Value must be a whole number");
        if ("TIME".equals(type)) { try { LocalTime.parse(v); } catch (Exception e) { throw new IllegalStateException("Time must be HH:mm"); } }
        return "BOOLEAN".equals(type) ? v.toUpperCase() : v;
    }

    /** Who runs a rule with which value: code -> [{value, count}] over the employees' own rows. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> summary() {
        Authz.requireRole("ADMIN");
        return jdbc.queryForList("""
                SELECT config_code AS code, MAX(display_name) AS name, MAX(screen_name) AS screen,
                       COALESCE(config_value, '') AS value, is_enabled AS enabled, COUNT(*) AS employees
                FROM app_validations WHERE module_name = 'DCR' AND emp_id IS NOT NULL
                GROUP BY config_code, config_value, is_enabled ORDER BY MIN(display_order), config_code, employees DESC""");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void save(Long id, String value, Boolean enabled) {
        Authz.requireRole("ADMIN");
        List<Map<String, Object>> r = jdbc.queryForList("SELECT data_type FROM app_validations WHERE id = ? AND module_name = 'DCR'", id);
        if (r.isEmpty()) throw new IllegalStateException("Rule not found");
        String t = (String) r.get(0).get("data_type"), v = value == null ? "" : value.trim();
        if ("BOOLEAN".equals(t) && !v.equalsIgnoreCase("TRUE") && !v.equalsIgnoreCase("FALSE")) throw new IllegalStateException("Value must be TRUE or FALSE");
        if ("INTEGER".equals(t) && !v.matches("\\d{1,6}")) throw new IllegalStateException("Value must be a whole number");
        if ("TIME".equals(t)) { try { LocalTime.parse(v); } catch (Exception e) { throw new IllegalStateException("Time must be HH:mm"); } }
        if ("BOOLEAN".equals(t)) v = v.toUpperCase();
        jdbc.update("UPDATE app_validations SET config_value = ?, is_enabled = COALESCE(?, is_enabled) WHERE id = ?", v, enabled == null ? null : (enabled ? 1 : 0), id);
    }

    /** Apply one master rule's new value to every employee's copy (and the master row). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public int applyToAll(String code, String value) {
        Authz.requireRole("ADMIN");
        return jdbc.update("UPDATE app_validations SET config_value = ? WHERE module_name = 'DCR' AND config_code = ?", value, code);
    }
}
