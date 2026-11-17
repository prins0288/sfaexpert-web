package in.opt.sfa.tenant.hr.org;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Company, branches (with state), departments, grades, cost centres and employee types. One generic CRUD over a whitelist of tables / columns. */
@Service
public class HrMasterService {

    /** kind -> table, editable columns (name -> "S" string, "N" number, "B" flag). */
    private static final Map<String, Object[]> KINDS = new LinkedHashMap<>();
    static {
        KINDS.put("branches", new Object[]{"hr_branch", cols("code", "S", "name", "S", "state_id", "N", "city", "S", "address", "S", "timezone", "S", "pt_reg_no", "S", "lwf_reg_no", "S", "sort_order", "N", "is_active", "B")});
        KINDS.put("departments", new Object[]{"hr_department", cols("code", "S", "name", "S", "sort_order", "N", "is_active", "B")});
        KINDS.put("grades", new Object[]{"hr_grade", cols("code", "S", "name", "S", "sort_order", "N", "is_active", "B")});
        KINDS.put("cost-centers", new Object[]{"hr_cost_center", cols("code", "S", "name", "S", "sort_order", "N", "is_active", "B")});
        KINDS.put("employee-types", new Object[]{"hr_employee_type", cols("code", "S", "name", "S", "pf_applicable", "B", "esi_applicable", "B", "leave_applicable", "B", "compoff_applicable", "B", "probation_applicable", "B", "bonus_applicable", "B", "gratuity_applicable", "B", "sort_order", "N", "is_active", "B")});
    }
    private static final List<String> COMPANY_COLS = List.of("legal_name", "trade_name", "pan", "tan", "gstin", "cin", "pf_code", "esi_code", "address", "fy_start_month", "leave_year_mode", "payroll_start_day", "timezone");

    private static LinkedHashMap<String, String> cols(String... kv) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;

    public HrMasterService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access) { this.jdbc = jdbc; this.audit = audit; this.access = access; }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> columns(String kind) { return (LinkedHashMap<String, String>) KINDS.get(kind)[1]; }
    private String table(String kind) {
        if (!KINDS.containsKey(kind)) throw new IllegalArgumentException("Unknown master " + kind);
        return (String) KINDS.get(kind)[0];
    }
    private static String camel(String s) {
        StringBuilder b = new StringBuilder();
        boolean up = false;
        for (char c : s.toCharArray()) { if (c == '_') up = true; else { b.append(up ? Character.toUpperCase(c) : c); up = false; } }
        return b.toString();
    }
    private static Object conv(String type, Object v) {
        if (v == null || (v instanceof String s && s.isBlank())) return "B".equals(type) ? 0 : null;
        return switch (type) {
            case "N" -> Long.valueOf(v.toString());
            case "B" -> (v instanceof Boolean b ? b : "true".equalsIgnoreCase(v.toString()) || "1".equals(v.toString())) ? 1 : 0;
            default -> v.toString().trim();
        };
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(String kind) {
        String t = table(kind);
        access.require("HR_SETUP");
        StringBuilder sel = new StringBuilder("SELECT x.id");
        columns(kind).keySet().forEach(c -> sel.append(", x.").append(c).append(" AS ").append(camel(c)));
        String join = "";
        if ("branches".equals(kind)) { sel.append(", s.state_name AS stateName"); join = " LEFT JOIN state_master s ON s.oid = x.state_id"; }
        List<Map<String, Object>> rows = jdbc.queryForList(sel + " FROM " + t + " x" + join + " WHERE x.company_id = 1 ORDER BY x.sort_order, x.name");
        if (!"branches".equals(kind) && !"employee-types".equals(kind)) { /* no extras */ }
        return rows;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(String kind, Map<String, Object> body) {
        String t = table(kind);
        access.require("HR_SETUP");
        LinkedHashMap<String, String> cols = columns(kind);
        Long id = body.get("id") == null || body.get("id").toString().isBlank() ? null : Long.valueOf(body.get("id").toString());
        LinkedHashMap<String, Object> vals = new LinkedHashMap<>();
        for (Map.Entry<String, String> c : cols.entrySet()) {
            String key = camel(c.getKey());
            if (!body.containsKey(key) && id != null) continue;   // partial update
            Object v = conv(c.getValue(), body.get(key));
            if (v == null && ("code".equals(c.getKey()) || "name".equals(c.getKey()))) throw new IllegalArgumentException("Code and name are required");
            if (v == null && "sort_order".equals(c.getKey())) v = 0;
            if ("is_active".equals(c.getKey()) && !body.containsKey(key)) v = 1;
            vals.put(c.getKey(), v);
        }
        String me = UserContext.get() == null ? null : UserContext.get().empId();
        try {
            if (id == null) {
                vals.values().removeIf(Objects::isNull);   // let the column default apply
                String names = String.join(", ", vals.keySet());
                String qs = String.join(", ", Collections.nCopies(vals.size(), "?"));
                List<Object> args = new ArrayList<>(vals.values()); args.add(me);
                jdbc.update("INSERT INTO " + t + " (company_id, " + names + ", created_by) VALUES (1, " + qs + ", ?)", args.toArray());
                Long nid = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                audit.logDiff(kind.toUpperCase().replace('-', '_'), nid, null, "CREATE", Map.of(), vals, null);
                return Map.of("id", nid);
            }
            Map<String, Object> before = jdbc.queryForMap("SELECT " + String.join(", ", vals.keySet()) + " FROM " + t + " WHERE id = ? AND company_id = 1", id);
            String set = String.join(", ", vals.keySet().stream().map(k -> k + " = ?").toList());
            List<Object> args = new ArrayList<>(vals.values()); args.add(me); args.add(id);
            jdbc.update("UPDATE " + t + " SET " + set + ", updated_by = ?, updated_at = NOW() WHERE id = ? AND company_id = 1", args.toArray());
            Map<String, Object> after = new LinkedHashMap<>(before); after.putAll(vals);
            audit.logDiff(kind.toUpperCase().replace('-', '_'), id, null, "UPDATE", before, after, null);
            return Map.of("id", id);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new IllegalArgumentException("That code already exists");
        }
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int delete(String kind, List<Long> ids, String reason) {
        String t = table(kind);
        access.require("HR_SETUP");
        int n = 0;
        for (Long id : ids) {
            String name = jdbc.queryForList("SELECT name FROM " + t + " WHERE id = ? AND company_id = 1", String.class, id).stream().findFirst().orElse(null);
            if (name == null) continue;
            String col = switch (kind) { case "branches" -> "branch_id"; case "departments" -> "department_id"; case "grades" -> "grade_id"; case "cost-centers" -> "cost_center_id"; default -> "employee_type_id"; };
            Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM hr_emp_employment WHERE " + col + " = ?", Integer.class, id);
            if (used != null && used > 0) throw new IllegalArgumentException(name + " is used by " + used + " employee(s) - switch it off instead of deleting");
            jdbc.update("DELETE FROM " + t + " WHERE id = ? AND company_id = 1", id);
            audit.log(kind.toUpperCase().replace('-', '_'), id, null, "DELETE", "name", name, null, reason);
            n++;
        }
        return n;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> company() {
        access.require("HR_SETUP");
        return jdbc.queryForMap("SELECT id, legal_name AS legalName, trade_name AS tradeName, pan, tan, gstin, cin, pf_code AS pfCode, esi_code AS esiCode, address, fy_start_month AS fyStartMonth, leave_year_mode AS leaveYearMode, payroll_start_day AS payrollStartDay, timezone FROM hr_company WHERE id = 1");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveCompany(Map<String, Object> body, String reason) {
        access.require("HR_SETUP");
        Map<String, Object> before = jdbc.queryForMap("SELECT " + String.join(", ", COMPANY_COLS) + " FROM hr_company WHERE id = 1");
        LinkedHashMap<String, Object> vals = new LinkedHashMap<>();
        for (String c : COMPANY_COLS) {
            String k = camel(c);
            if (!body.containsKey(k)) continue;
            Object v = body.get(k);
            String s = v == null ? null : v.toString().trim();
            if (s != null && s.isEmpty()) s = null;
            if (c.equals("legal_name") && s == null) throw new IllegalArgumentException("Legal name is required");
            if (c.equals("fy_start_month")) { int m = Integer.parseInt(s == null ? "4" : s); if (m < 1 || m > 12) throw new IllegalArgumentException("Month must be 1-12"); vals.put(c, m); }
            else if (c.equals("payroll_start_day")) { int d = Integer.parseInt(s == null ? "1" : s); if (d < 1 || d > 28) throw new IllegalArgumentException("Payroll start day must be 1-28"); vals.put(c, d); }
            else if (c.equals("leave_year_mode")) { if (!List.of("FINANCIAL", "CALENDAR", "JOINING").contains(s)) throw new IllegalArgumentException("Leave year must be FINANCIAL, CALENDAR or JOINING"); vals.put(c, s); }
            else if (c.equals("pan") && s != null && !s.toUpperCase().matches("[A-Z]{5}[0-9]{4}[A-Z]")) throw new IllegalArgumentException("PAN looks wrong (AAAAA9999A)");
            else vals.put(c, c.equals("pan") && s != null ? s.toUpperCase() : s);
        }
        if (vals.isEmpty()) return;
        String me = UserContext.get() == null ? null : UserContext.get().empId();
        List<Object> args = new ArrayList<>(vals.values()); args.add(me);
        jdbc.update("UPDATE hr_company SET " + String.join(", ", vals.keySet().stream().map(k -> k + " = ?").toList()) + ", updated_by = ?, updated_at = NOW() WHERE id = 1", args.toArray());
        audit.logDiff("COMPANY", 1, null, "UPDATE", before, mergeKeepBefore(before, vals), reason);
    }

    private static Map<String, Object> mergeKeepBefore(Map<String, Object> before, Map<String, Object> vals) {
        Map<String, Object> m = new LinkedHashMap<>(before); m.putAll(vals); return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> states() { return jdbc.queryForList("SELECT oid AS id, state_name AS name FROM state_master ORDER BY state_name"); }
}
