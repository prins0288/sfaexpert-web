package in.opt.sfa.tenant.dcr.submit.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.Authz;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * app_dcr_worktype: which tabs of the DCR form a user gets. One row = one tab for a target
 * (ALL / one employee / a designation / a hierarchy level); per worktype_code the most
 * specific row wins — EMP_ID > DESIGNATION > HIERARCHY_LEVEL > ALL — and its is_enabled
 * decides, so a more specific row with is_enabled = 0 switches a tab OFF for that target.
 */
@Service
public class DcrWorktypeService {

    public static final List<String> TARGETS = List.of("ALL", "EMP_ID", "DESIGNATION", "HIERARCHY_LEVEL");
    public static final List<String> FORMS = List.of("CHECKIN", "UNLISTED", "EXPENSE", "JOINT", "DAYEND");
    public static final List<String> SUB_FORMS = List.of("ITEMS", "LEAFLETS", "TREATMENT");

    private final JdbcTemplate jdbc;

    public DcrWorktypeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static int rank(String targetType) {
        return switch (targetType) { case "EMP_ID" -> 4; case "DESIGNATION" -> 3; case "HIERARCHY_LEVEL" -> 2; default -> 1; };
    }

    /** The tabs {@code empId} gets, in order, with their settings (client type, sub forms …). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> tabsFor(String empId) {
        Map<String, Object> who = jdbc.query("""
                SELECT e.designation AS des, d.hierarchy_level AS lvl FROM emp_detail e
                LEFT JOIN designation_master d ON d.id = e.designation WHERE e.emp_id = ?""",
                rs -> { if (!rs.next()) return new HashMap<String, Object>(); Map<String, Object> m = new HashMap<>();
                        m.put("des", rs.getObject("des")); m.put("lvl", rs.getObject("lvl")); return m; }, empId);
        String des = who.get("des") == null ? "-" : String.valueOf(who.get("des"));
        String lvl = who.get("lvl") == null ? "-" : String.valueOf(who.get("lvl"));
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, target_type, worktype_code, display_name, form_open, client_type_id, sub_forms, sort_order, is_enabled
                FROM app_dcr_worktype
                WHERE target_type = 'ALL'
                   OR (target_type = 'EMP_ID' AND target_value = ?)
                   OR (target_type = 'DESIGNATION' AND target_value = ?)
                   OR (target_type = 'HIERARCHY_LEVEL' AND target_value = ?)""", empId, des, lvl);
        Map<String, Map<String, Object>> best = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String code = (String) r.get("worktype_code");
            Map<String, Object> cur = best.get(code);
            if (cur == null || rank((String) r.get("target_type")) > rank((String) cur.get("target_type"))) best.put(code, r);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : best.values()) {
            if (!(r.get("is_enabled") instanceof Boolean b ? b : ((Number) r.get("is_enabled")).intValue() == 1)) continue;
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("code", r.get("worktype_code"));
            t.put("name", r.get("display_name"));
            t.put("form", r.get("form_open"));
            t.put("clientTypeId", r.get("client_type_id"));
            t.put("subForms", Strings.isBlank((String) r.get("sub_forms")) ? List.of() : Arrays.asList(((String) r.get("sub_forms")).split(",")));
            t.put("sort", r.get("sort_order"));
            out.add(t);
        }
        out.sort(Comparator.comparingInt(t -> ((Number) t.get("sort")).intValue()));
        return out;
    }

    // ---- admin: maintain the rows ---------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() {
        Authz.requireRole("ADMIN");
        return jdbc.queryForList("""
                SELECT w.id, w.target_type AS targetType, w.target_value AS targetValue, w.worktype_code AS worktypeCode,
                       w.display_name AS displayName, w.form_open AS formOpen, w.client_type_id AS clientTypeId,
                       ct.type_name AS clientTypeName, w.sub_forms AS subForms, w.sort_order AS sortOrder, w.is_enabled AS enabled,
                       CASE w.target_type WHEN 'ALL' THEN 'Everyone'
                         WHEN 'EMP_ID' THEN (SELECT CONCAT(e.emp_name, ' (', e.emp_code, ')') FROM emp_detail e WHERE e.emp_id = w.target_value)
                         WHEN 'DESIGNATION' THEN (SELECT d.designation FROM designation_master d WHERE d.id = CAST(w.target_value AS UNSIGNED))
                         ELSE CONCAT('Level ', w.target_value) END AS targetLabel
                FROM app_dcr_worktype w LEFT JOIN client_type ct ON ct.oid = w.client_type_id
                ORDER BY FIELD(w.target_type, 'ALL', 'HIERARCHY_LEVEL', 'DESIGNATION', 'EMP_ID'), w.target_value, w.sort_order""");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void save(Map<String, Object> f) {
        Authz.requireRole("ADMIN");
        String type = str(f.get("targetType"));
        if (type == null || !TARGETS.contains(type)) throw new IllegalStateException("Target must be one of " + TARGETS);
        String value = "ALL".equals(type) ? "" : str(f.get("targetValue"));
        if (!"ALL".equals(type) && value == null) throw new IllegalStateException("Choose the " + type.toLowerCase().replace('_', ' '));
        if ("EMP_ID".equals(type) && jdbc.queryForObject("SELECT COUNT(*) FROM emp_detail WHERE emp_id = ?", Integer.class, value) == 0) throw new IllegalStateException("Employee not found");
        if ("DESIGNATION".equals(type) && jdbc.queryForObject("SELECT COUNT(*) FROM designation_master WHERE id = ?", Integer.class, value) == 0) throw new IllegalStateException("Designation not found");
        if ("HIERARCHY_LEVEL".equals(type) && !value.matches("\\d{1,4}")) throw new IllegalStateException("Hierarchy level must be a number");
        String code = str(f.get("worktypeCode"));
        if (code == null || !code.toUpperCase().matches("[A-Z][A-Z0-9_]{1,29}")) throw new IllegalStateException("Tab code: 2-30 letters / digits / _ (e.g. RETAILER)");
        code = code.toUpperCase();
        String name = str(f.get("displayName"));
        if (name == null) throw new IllegalStateException("Tab name is required");
        if (name.length() > 60) throw new IllegalStateException("Tab name can be at most 60 characters");
        String form = str(f.get("formOpen"));
        if (form == null || !FORMS.contains(form)) throw new IllegalStateException("Form must be one of " + FORMS);
        Long ctype = f.get("clientTypeId") == null || str(f.get("clientTypeId")) == null ? null : Long.valueOf(str(f.get("clientTypeId")));
        if (ctype != null && !"CHECKIN".equals(form)) throw new IllegalStateException("A client type applies only to a Check-in tab");
        StringBuilder subs = new StringBuilder();
        if (f.get("subForms") instanceof Collection<?> c) {
            for (Object o : c) { String s = String.valueOf(o); if (!SUB_FORMS.contains(s)) throw new IllegalStateException("Unknown sub form " + s); if (subs.length() > 0) subs.append(','); subs.append(s); }
        }
        int sort = f.get("sortOrder") == null ? 100 : Integer.parseInt(String.valueOf(f.get("sortOrder")));
        int enabled = Boolean.FALSE.equals(f.get("enabled")) || "0".equals(String.valueOf(f.get("enabled"))) ? 0 : 1;
        jdbc.update("""
                INSERT INTO app_dcr_worktype (target_type, target_value, worktype_code, display_name, form_open, client_type_id, sub_forms, sort_order, is_enabled)
                VALUES (?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), form_open = VALUES(form_open), client_type_id = VALUES(client_type_id),
                                        sub_forms = VALUES(sub_forms), sort_order = VALUES(sort_order), is_enabled = VALUES(is_enabled)""",
                type, value, code, name, form, ctype, subs.toString(), sort, enabled);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void delete(Long id) {
        Authz.requireRole("ADMIN");
        int n = jdbc.update("DELETE FROM app_dcr_worktype WHERE id = ? AND target_type <> 'ALL'", id);
        if (n == 0) throw new IllegalStateException("Only a row for one employee / designation / level can be deleted — switch an 'Everyone' tab off instead");
    }

    private static String str(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().trim();
    }
}
