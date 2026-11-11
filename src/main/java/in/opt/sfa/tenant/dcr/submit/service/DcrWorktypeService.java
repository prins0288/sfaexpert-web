package in.opt.sfa.tenant.dcr.submit.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * app_dcr_worktype: what the DCR offers a user — the day types of Start Day (row_type wd / attn)
 * and the tabs of the form (av = client check-in, tab = unlisted / joint / expense / day end).
 * <p>
 * emp_id NULL = MASTER rows. A user with rows of their own gets ONLY those; a user without
 * gets the master rows. New employees get a copy of the master rows (copyMasterTo).
 */
@Service
public class DcrWorktypeService {

    public static final List<String> ROW_TYPES = List.of("wd", "attn", "av", "tab");
    /** The three client check-in kinds are fixed; only their label (Doctor / Chemist / Stockist ...) is free. */
    public static final List<String> CHECKIN_CODES = List.of("CLIENT", "RETAILER", "PARTY");
    public static final List<String> FORMS = List.of("CHECKIN", "UNLISTED", "EXPENSE", "JOINT", "DAYEND");
    public static final List<String> SUB_FORMS = List.of("ITEMS", "LEAFLETS", "TREATMENT");

    private final JdbcTemplate jdbc;

    public DcrWorktypeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static boolean on(Object o) {
        return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() == 1;
    }

    /** The enabled rows {@code empId} gets (own rows if any, else master), in order. */
    private List<Map<String, Object>> rowsFor(String empId) {
        Integer own = jdbc.queryForObject("SELECT COUNT(*) FROM app_dcr_worktype WHERE emp_id = ?", Integer.class, empId);
        List<Map<String, Object>> rows = own != null && own > 0
                ? jdbc.queryForList("SELECT * FROM app_dcr_worktype WHERE emp_id = ? AND is_enabled = 1 ORDER BY sort_order, id", empId)
                : jdbc.queryForList("SELECT * FROM app_dcr_worktype WHERE emp_id IS NULL AND is_enabled = 1 ORDER BY sort_order, id");
        return rows;
    }

    /** Day types for Start Day: [{code, name, fieldWork}]. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> dayTypes(String empId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rowsFor(empId)) {
            String t = (String) r.get("row_type");
            if (!"wd".equals(t) && !"attn".equals(t)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", r.get("worktype_code"));
            m.put("name", r.get("display_name"));
            m.put("fieldWork", "WORK".equals(r.get("form_open")));
            out.add(m);
        }
        return out;
    }

    /** The tabs {@code empId} gets, in order, with their settings (client type, sub forms …). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> tabsFor(String empId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rowsFor(empId)) {
            String t = (String) r.get("row_type");
            if (!"av".equals(t) && !"tab".equals(t)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", r.get("worktype_code"));
            m.put("name", r.get("display_name"));
            m.put("form", r.get("form_open"));
            m.put("icon", r.get("icon"));
            m.put("clientTypeId", r.get("client_type_id"));
            m.put("subForms", Strings.isBlank((String) r.get("sub_forms")) ? List.of() : Arrays.asList(((String) r.get("sub_forms")).split(",")));
            out.add(m);
        }
        return out;
    }

    /** True when the day type is a field day (client calls, start area ...) - the row's form_open = WORK. */
    public boolean isFieldWork(String empId, String code) {
        return dayTypes(empId).stream().anyMatch(d -> d.get("code").equals(code) && Boolean.TRUE.equals(d.get("fieldWork")));
    }

    /** True when {@code code} is a day type the user may start a day with. */
    public boolean isDayType(String empId, String code) {
        return dayTypes(empId).stream().anyMatch(d -> d.get("code").equals(code));
    }

    /** Gives a new employee their own copy of the master rows (no-op if they already have rows). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void copyMasterTo(String empId) {
        jdbc.update("""
                INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order, icon, is_enabled, needs_approval, created_by)
                SELECT ?, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order, icon, is_enabled, needs_approval, ?
                FROM app_dcr_worktype WHERE emp_id IS NULL""", empId, TeamScope.me());
    }

    /** Does a submitted DCR of this work type wait for approval (app_dcr_worktype.needs_approval of the master row: it is a company decision)? Default yes. */
    public boolean needsApproval(String empId, String code) {
        try {
            List<Integer> l = jdbc.query("SELECT needs_approval FROM app_dcr_worktype WHERE worktype_code = ? AND emp_id IS NULL LIMIT 1", (rs, i) -> rs.getInt(1), code);
            return l.isEmpty() || l.get(0) != 0;
        } catch (org.springframework.dao.DataAccessException e) { return true; }   // migration 116 not run
    }

    // ---- admin: maintain the rows ---------------------------------------------------

    /** Rows of one employee, or the master rows when {@code empId} is blank. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> list(String empId) {
        Authz.requireRole("ADMIN");
        boolean master = Strings.isBlank(empId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT w.id, w.emp_id AS empId, w.worktype_code AS worktypeCode, w.display_name AS displayName, w.row_type AS rowType,
                       w.form_open AS formOpen, w.client_type_id AS clientTypeId, ct.type_name AS clientTypeName, w.sub_forms AS subForms,
                       w.sort_order AS sortOrder, w.icon, w.is_enabled AS enabled, w.needs_approval AS needsApproval
                FROM app_dcr_worktype w LEFT JOIN client_type ct ON ct.oid = w.client_type_id
                WHERE %s ORDER BY w.sort_order, w.id""".formatted(master ? "w.emp_id IS NULL" : "w.emp_id = ?"), master ? new Object[0] : new Object[]{empId});
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", rows);
        out.put("master", master);
        // an employee with no rows of their own is currently getting the master rows
        out.put("usesMaster", !master && rows.isEmpty());
        return out;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void save(Map<String, Object> f) {
        Authz.requireRole("ADMIN");
        String emp = str(f.get("empId"));
        if (emp != null) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM emp_detail WHERE emp_id = ?", Integer.class, emp) == 0) throw new IllegalStateException("Employee not found");
            // first edit for this employee: start from the master rows so nothing else disappears
            Integer own = jdbc.queryForObject("SELECT COUNT(*) FROM app_dcr_worktype WHERE emp_id = ?", Integer.class, emp);
            if (own == null || own == 0) copyMasterTo(emp);
        }
        String code = str(f.get("worktypeCode"));
        if (code == null || !code.toUpperCase().matches("[A-Z][A-Z0-9_]{1,29}")) throw new IllegalStateException("Code: 2-30 letters / digits / _ (e.g. RETAILER)");
        code = code.toUpperCase();
        String name = str(f.get("displayName"));
        if (name == null) throw new IllegalStateException("Name is required");
        if (name.length() > 60) throw new IllegalStateException("Name can be at most 60 characters");
        String rowType = str(f.get("rowType"));
        if (rowType == null || !ROW_TYPES.contains(rowType)) throw new IllegalStateException("Type must be one of " + ROW_TYPES);
        boolean day = "wd".equals(rowType) || "attn".equals(rowType);
        if ("av".equals(rowType) && !CHECKIN_CODES.contains(code)) throw new IllegalStateException("A client check-in tab is CLIENT, RETAILER or PARTY (only its label changes)");
        String form = day ? (Boolean.TRUE.equals(f.get("fieldWork")) || "true".equals(String.valueOf(f.get("fieldWork"))) ? "WORK" : "ATTN") : str(f.get("formOpen"));
        if (form == null || (!day && !FORMS.contains(form))) throw new IllegalStateException("Form must be one of " + FORMS);
        if ("av".equals(rowType) && !"CHECKIN".equals(form)) throw new IllegalStateException("A check-in tab opens the Check-in form");
        Long ctype = day || str(f.get("clientTypeId")) == null ? null : Long.valueOf(str(f.get("clientTypeId")));
        if (ctype != null && !"CHECKIN".equals(form)) throw new IllegalStateException("A client type applies only to a Check-in tab");
        StringBuilder subs = new StringBuilder();
        if (!day && f.get("subForms") instanceof Collection<?> c) {
            for (Object o : c) { String s = String.valueOf(o); if (!SUB_FORMS.contains(s)) throw new IllegalStateException("Unknown sub form " + s); if (subs.length() > 0) subs.append(','); subs.append(s); }
        }
        String icon = str(f.get("icon"));
        if (icon != null && !icon.matches("[a-z0-9-]{1,40}")) throw new IllegalStateException("Icon is a bootstrap-icons name, e.g. person-badge");
        int sort = f.get("sortOrder") == null ? 100 : Integer.parseInt(String.valueOf(f.get("sortOrder")));
        int enabled = Boolean.FALSE.equals(f.get("enabled")) || "0".equals(String.valueOf(f.get("enabled"))) ? 0 : 1;
        int needsAppr = Boolean.FALSE.equals(f.get("needsApproval")) || "0".equals(String.valueOf(f.get("needsApproval"))) ? 0 : 1;
        String me = TeamScope.me();
        jdbc.update("""
                INSERT INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order, icon, is_enabled, needs_approval, created_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), row_type = VALUES(row_type), form_open = VALUES(form_open),
                  client_type_id = VALUES(client_type_id), sub_forms = VALUES(sub_forms), sort_order = VALUES(sort_order), icon = VALUES(icon),
                  is_enabled = VALUES(is_enabled), needs_approval = VALUES(needs_approval), updated_by = VALUES(created_by), updated_at = NOW()""",
                emp, code, name, rowType, form, ctype, subs.toString(), sort, icon, enabled, needsAppr, me);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void delete(Long id) {
        Authz.requireRole("ADMIN");
        int n = jdbc.update("DELETE FROM app_dcr_worktype WHERE id = ? AND emp_id IS NOT NULL", id);
        if (n == 0) throw new IllegalStateException("Master rows cannot be deleted — switch them off instead");
    }

    private static String str(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().trim();
    }
}
