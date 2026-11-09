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

    /** Rules of one employee (own rows) or the master rows when {@code empId} is blank. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(String empId) {
        Authz.requireRole("ADMIN");
        boolean master = Strings.isBlank(empId);
        return jdbc.queryForList("""
                SELECT id, emp_id AS empId, config_code AS code, display_name AS name, screen_name AS screen, config_type AS type, data_type AS dataType,
                       config_value AS value, default_value AS defaultValue, description, is_enabled AS enabled
                FROM app_validations WHERE module_name = 'DCR' AND %s ORDER BY display_order, id""".formatted(master ? "emp_id IS NULL" : "emp_id = ?"),
                master ? new Object[0] : new Object[]{empId});
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
