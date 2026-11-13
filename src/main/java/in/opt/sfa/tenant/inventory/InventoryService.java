package in.opt.sfa.tenant.inventory;

import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Stock &amp; Sales of a party / stockist, month wise: party_sales_parent (one row per party per month - a second submit UPDATES it, never a duplicate)
 * and party_sales_child (one row per item). What is typed, the rate shown and the file are set in Inventory Master.
 * Closing (when only the Sale is typed) = (Opening + Purchase) - (Sale + Free) + (Return from retailer - Return to consignee) + (Expiry from retailer - Expiry to consignee).
 * Only Closing typed: Sale = (Opening + Purchase) - Free + (Return retailer - Return consignee) + (Expiry retailer - Expiry consignee) - Closing. Both typed: no formula (setting SALE_CLOSING_CALC).
 * Stock value = Closing x the rate in use (the special rate when one is entered and RATE_SHOWN is SPECIAL, else the item rate / PTS).
 */
@Service
public class InventoryService {
    /** field code -> child column (the typed fields). */
    static final LinkedHashMap<String, String> COL = new LinkedHashMap<>();
    static {
        COL.put("OPENING", "opening_qty"); COL.put("PRIMARY", "purchase_qty"); COL.put("SECONDARY", "sale_qty"); COL.put("FREE", "free_qty");
        COL.put("RETURN_RETAILER", "return_retailer_qty"); COL.put("RETURN_CONSIGNEE", "return_consignee_qty");
        COL.put("EXPIRY_RETAILER", "expiry_retailer_qty"); COL.put("EXPIRY_CONSIGNEE", "expiry_consignee_qty"); COL.put("SPECIAL_RATE", "special_rate"); COL.put("CLOSING", "closing_qty");
    }

    private final JdbcTemplate jdbc;
    private final TeamScope scope;

    public InventoryService(JdbcTemplate jdbc, TeamScope scope) { this.jdbc = jdbc; this.scope = scope; }

    // ------------------------------------------------------------------ master (fields + settings)
    private static final Map<String, String> SETTING_DEFAULTS = Map.of("RATE_SHOWN", "SPECIAL", "FILE_UPLOAD", "N", "FILE_MANDATORY", "N", "SALE_CLOSING_CALC", "Y");

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> config() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fields", jdbc.queryForList("SELECT field_code AS code, label, is_enabled AS enabled, is_mandatory AS mandatory, is_auto AS auto, sort_order AS sortOrder FROM inventory_field_master ORDER BY sort_order, id"));
        out.put("settings", settings());
        return out;
    }

    private Map<String, String> settings() {
        Map<String, String> m = new LinkedHashMap<>(SETTING_DEFAULTS);
        for (Map<String, Object> r : jdbc.queryForList("SELECT setting_key, setting_value FROM inventory_setting")) m.put((String) r.get("setting_key"), String.valueOf(r.get("setting_value")));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public void saveConfig(Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        String me = TeamScope.me();
        if (b.get("fields") instanceof List<?> l) {
            for (Object o : l) {
                Map<String, Object> f = (Map<String, Object>) o;
                String code = String.valueOf(f.get("code"));
                String label = f.get("label") == null ? "" : f.get("label").toString().trim();
                if (label.isEmpty()) throw new IllegalArgumentException("Label of " + code + " is required");
                boolean auto = jdbc.queryForList("SELECT is_auto FROM inventory_field_master WHERE field_code = ?", Integer.class, code).stream().findFirst().orElse(0) == 1;
                jdbc.update("UPDATE inventory_field_master SET label = ?, is_enabled = ?, is_mandatory = ?, sort_order = ?, updated_by = ?, updated_at = NOW() WHERE field_code = ?",
                        label, flag(f.get("enabled")) ? 1 : 0, !auto && flag(f.get("mandatory")) ? 1 : 0, f.get("sortOrder") == null ? 100 : Integer.parseInt(f.get("sortOrder").toString()), me, code);
            }
        }
        if (b.get("settings") instanceof Map<?, ?> s) {
            for (Map.Entry<?, ?> e : s.entrySet()) {
                String k = String.valueOf(e.getKey()), v = String.valueOf(e.getValue());
                if (!SETTING_DEFAULTS.containsKey(k)) continue;
                if ("RATE_SHOWN".equals(k) && !List.of("SPECIAL", "ITEM").contains(v)) throw new IllegalArgumentException("Rate shown must be SPECIAL or ITEM");
                if (!"RATE_SHOWN".equals(k) && !List.of("Y", "N").contains(v)) throw new IllegalArgumentException(k + " must be Y or N");
                jdbc.update("INSERT INTO inventory_setting (setting_key, setting_value, updated_by, updated_at) VALUES (?,?,?,NOW()) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_by = VALUES(updated_by), updated_at = NOW()", k, v, me);
            }
        }
    }

    private static boolean flag(Object o) { return Boolean.TRUE.equals(o) || "1".equals(String.valueOf(o)) || "true".equalsIgnoreCase(String.valueOf(o)); }

    // ------------------------------------------------------------------ parties and list
    /** The parties / stockists (client type with the PARTY tab) of an employee. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> parties(String empId) {
        checkEmp(empId);
        return jdbc.queryForList("""
                SELECT c.id, c.client_code AS code, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS name
                FROM client_master c JOIN client_type ct ON ct.oid = c.client_type_id
                WHERE c.is_active = 1 AND c.approval_status = 'APPROVED' AND ct.dcr_tab = 'PARTY'
                  AND (c.mapped_emp_code = ? OR (c.mapped_emp_code IS NULL AND c.created_by COLLATE utf8mb4_0900_ai_ci = ?)) ORDER BY c.client_name""", empId, empId);
    }

    private void checkEmp(String empId) {
        if (empId == null || empId.isBlank()) throw new IllegalArgumentException("Choose the employee");
        if (!empId.equals(TeamScope.me())) scope.require(TeamScope.Scope.TEAM_SELF, empId, "This employee is not in your team");
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(Integer month, Integer year, Long stateId, String empId, Long partyId) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS empName, p.party_id AS partyId, c.client_code AS partyCode,
                       CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS party, st.state_name AS state, p.sale_month AS month, p.sale_year AS year,
                       (SELECT COUNT(*) FROM party_sales_child x WHERE x.parent_id = p.id) AS items, COALESCE((SELECT SUM(x.closing_qty) FROM party_sales_child x WHERE x.parent_id = p.id), 0) AS closing,
                       COALESCE((SELECT SUM(x.stock_value) FROM party_sales_child x WHERE x.parent_id = p.id), 0) AS stockValue,
                       p.file_name AS fileName, p.file_path AS filePath, p.updated_at AS updatedAt, p.created_at AS createdAt, p.status
                FROM party_sales_parent p JOIN emp_detail e ON e.emp_id = p.emp_id JOIN client_master c ON c.id = p.party_id LEFT JOIN state_master st ON st.oid = e.state_id WHERE 1 = 1""");
        List<Object> a = new ArrayList<>();
        if (month != null) { sql.append(" AND p.sale_month = ?"); a.add(month); }
        if (year != null) { sql.append(" AND p.sale_year = ?"); a.add(year); }
        if (stateId != null) { sql.append(" AND e.state_id = ?"); a.add(stateId); }
        if (empId != null && !empId.isBlank()) { sql.append(" AND p.emp_id = ?"); a.add(empId); }
        if (partyId != null) { sql.append(" AND p.party_id = ?"); a.add(partyId); }
        sql.append(" ORDER BY p.sale_year DESC, p.sale_month DESC, c.client_name");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), a.toArray());
        return scope.filter(TeamScope.Scope.TEAM_SELF, rows, r -> (String) r.get("empId"));
    }

    // ------------------------------------------------------------------ entry (open) and save
    private BigDecimal itemRate(long itemId) {
        List<BigDecimal> l = jdbc.query("SELECT r.pts FROM item_rate r WHERE r.item_id = ? AND r.status = 1 AND (r.effective_from IS NULL OR r.effective_from <= CURDATE()) AND (r.effective_to IS NULL OR r.effective_to >= CURDATE()) ORDER BY r.effective_from DESC LIMIT 1", (rs, i) -> rs.getBigDecimal(1), itemId);
        return l.isEmpty() || l.get(0) == null ? BigDecimal.ZERO : l.get(0);
    }

    /** The grid of a party and month: every product with its PTS and - when this party / month was submitted before - the values (the submit then UPDATES). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> entry(String empId, long partyId, int month, int year) {
        checkEmp(empId); checkPeriod(month, year);
        Map<String, Object> party = partyOf(empId, partyId);
        List<Map<String, Object>> parent = jdbc.queryForList("SELECT id, remarks, file_path AS filePath, file_name AS fileName, updated_at AS updatedAt, created_at AS createdAt FROM party_sales_parent WHERE party_id = ? AND sale_month = ? AND sale_year = ?", partyId, month, year);
        Map<Long, Map<String, Object>> saved = new HashMap<>();
        if (!parent.isEmpty()) for (Map<String, Object> r : jdbc.queryForList("SELECT * FROM party_sales_child WHERE parent_id = ?", parent.get(0).get("id"))) saved.put(((Number) r.get("item_id")).longValue(), r);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> it : jdbc.queryForList("""
                SELECT i.id, i.item_code AS code, i.item_name AS name, i.pack_size AS pack FROM item_master i JOIN item_type_master t ON t.id = i.item_type_id
                WHERE i.status = 1 AND t.item_type_code = 'PRODUCT' ORDER BY i.item_name""")) {
            long id = ((Number) it.get("id")).longValue();
            Map<String, Object> m = new LinkedHashMap<>(it);
            Map<String, Object> s = saved.get(id);
            m.put("rate", s != null ? s.get("item_rate") : itemRate(id));   // the rate stored with the first submit stays
            for (Map.Entry<String, String> c : COL.entrySet()) m.put(c.getKey(), s == null ? null : s.get(c.getValue()));
            String cf = s == null ? null : (String) s.get("calc_field");   // a calculated value is not "typed": it is shown as the calculation again, so a later change recalculates it
            if ("C".equals(cf)) m.put("CLOSING", null); if ("S".equals(cf)) m.put("SECONDARY", null);
            m.put("closing", s == null ? null : s.get("closing_qty")); m.put("sale", s == null ? null : s.get("sale_qty")); m.put("stockValue", s == null ? null : s.get("stock_value"));
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exists", !parent.isEmpty()); out.put("parent", parent.isEmpty() ? null : parent.get(0)); out.put("party", party); out.put("items", items);
        out.put("month", month); out.put("year", year); out.putAll(config());
        return out;
    }

    private Map<String, Object> partyOf(String empId, long partyId) {
        return parties(empId).stream().filter(p -> ((Number) p.get("id")).longValue() == partyId).findFirst().orElseThrow(() -> new IllegalArgumentException("This party / stockist is not of this employee"));
    }

    private static void checkPeriod(int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2100) throw new IllegalArgumentException("Choose a valid month and year");
        LocalDate now = LocalDate.now();
        if (year > now.getYear() || (year == now.getYear() && month > now.getMonthValue())) throw new IllegalArgumentException("A future month cannot be submitted");
    }

    private static BigDecimal num(Object o, String what) {
        if (o == null || o.toString().isBlank()) return null;
        try { BigDecimal v = new BigDecimal(o.toString().trim()); if (v.signum() < 0) throw new IllegalArgumentException(what + " cannot be negative"); return v.setScale(2, RoundingMode.HALF_UP); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(what + " must be a number"); }
    }

    /** Insert the party / month when it is new, update it when it exists (unique party + month + year). */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> b) {
        String empId = b.get("empId") == null ? null : b.get("empId").toString();
        checkEmp(empId);
        long partyId = Long.parseLong(String.valueOf(b.get("partyId")));
        int month = Integer.parseInt(String.valueOf(b.get("month"))), year = Integer.parseInt(String.valueOf(b.get("year")));
        checkPeriod(month, year); partyOf(empId, partyId);
        Map<String, Object> cfg = config();
        List<Map<String, Object>> fields = (List<Map<String, Object>>) cfg.get("fields");
        Map<String, String> st = (Map<String, String>) cfg.get("settings");
        Map<String, Boolean> enabled = new HashMap<>(), mandatory = new HashMap<>(); Map<String, String> label = new HashMap<>();
        for (Map<String, Object> f : fields) { String c = (String) f.get("code"); enabled.put(c, flag(f.get("enabled"))); mandatory.put(c, flag(f.get("mandatory"))); label.put(c, (String) f.get("label")); }

        List<Map<String, Object>> existing = jdbc.queryForList("SELECT id, file_path, file_name FROM party_sales_parent WHERE party_id = ? AND sale_month = ? AND sale_year = ?", partyId, month, year);
        String filePath = b.get("filePath") == null || b.get("filePath").toString().isBlank() ? null : b.get("filePath").toString(), fileName = b.get("fileName") == null ? null : b.get("fileName").toString();
        boolean fileOn = "Y".equals(st.get("FILE_UPLOAD"));
        if (!fileOn) { filePath = null; fileName = null; }
        else if (filePath == null && !existing.isEmpty() && existing.get(0).get("file_path") != null && !Boolean.TRUE.equals(b.get("removeFile"))) { filePath = (String) existing.get(0).get("file_path"); fileName = (String) existing.get(0).get("file_name"); }
        if (fileOn && "Y".equals(st.get("FILE_MANDATORY")) && filePath == null) throw new IllegalArgumentException("Upload the file (statement) - it is required");

        // the rows: only items with something typed; the mandatory fields must be filled in those rows; closing and value are calculated here, never taken from the client
        List<Object[]> rows = new ArrayList<>(); Map<Long, String> calcs = new HashMap<>();
        for (Object o : (List<Object>) b.getOrDefault("rows", List.of())) {
            Map<String, Object> r = (Map<String, Object>) o;
            long itemId = Long.parseLong(String.valueOf(r.get("itemId")));
            Map<String, BigDecimal> v = new HashMap<>(); boolean any = false;
            for (String code : COL.keySet()) {
                BigDecimal x = enabled.getOrDefault(code, false) ? num(r.get(code), label.get(code)) : null;
                v.put(code, x); if (x != null) any = true;
            }
            if (!any) continue;
            String name = jdbc.queryForList("SELECT item_name FROM item_master WHERE id = ?", String.class, itemId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
            for (String code : COL.keySet()) if (enabled.getOrDefault(code, false) && mandatory.getOrDefault(code, false) && v.get(code) == null && !List.of("SPECIAL_RATE", "SECONDARY", "CLOSING").contains(code)) throw new IllegalArgumentException(label.get(code) + " is required for " + name);
            if ("SPECIAL_RATE".equals("SPECIAL_RATE") && enabled.getOrDefault("SPECIAL_RATE", false) && mandatory.getOrDefault("SPECIAL_RATE", false) && v.get("SPECIAL_RATE") == null) throw new IllegalArgumentException(label.get("SPECIAL_RATE") + " is required for " + name);
            BigDecimal base = nz(v.get("OPENING")).add(nz(v.get("PRIMARY"))).subtract(nz(v.get("FREE")))
                    .add(nz(v.get("RETURN_RETAILER")).subtract(nz(v.get("RETURN_CONSIGNEE")))).add(nz(v.get("EXPIRY_RETAILER")).subtract(nz(v.get("EXPIRY_CONSIGNEE"))));   // closing = base - sale
            BigDecimal sale = v.get("SECONDARY"), closing = v.get("CLOSING"); String calc = null;
            if ("Y".equals(st.get("SALE_CLOSING_CALC")) && !(sale != null && closing != null)) {   // both typed: no formula
                if (sale != null) { closing = base.subtract(sale); calc = "C"; }
                else if (closing != null) { sale = base.subtract(closing); calc = "S"; if (sale.signum() < 0) throw new IllegalArgumentException("Closing of " + name + " is more than opening + purchase + returns allow (sale would be " + sale.stripTrailingZeros().toPlainString() + ")"); }
                else { closing = base; calc = "C"; }
            }
            if (closing == null) closing = BigDecimal.ZERO;
            if (closing.signum() < 0) throw new IllegalArgumentException("Closing of " + name + " is negative (" + closing.stripTrailingZeros().toPlainString() + ") - check the quantities");
            v.put("SECONDARY", sale); v.put("CLOSING", closing); v.put("_calc", null);
            calcs.put(itemId, calc);
            rows.add(new Object[]{itemId, v, closing});
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("Enter the stock of at least one item");

        String me = TeamScope.me();
        long parentId;
        if (existing.isEmpty()) {
            jdbc.update("INSERT INTO party_sales_parent (emp_id, party_id, sale_month, sale_year, status, remarks, file_path, file_name, created_by) VALUES (?,?,?,?, 'SUBMITTED', ?,?,?,?) ON DUPLICATE KEY UPDATE updated_by = VALUES(created_by), updated_at = NOW()",
                    empId, partyId, month, year, rem(b), filePath, fileName, me);
            parentId = jdbc.queryForObject("SELECT id FROM party_sales_parent WHERE party_id = ? AND sale_month = ? AND sale_year = ?", Long.class, partyId, month, year);
        } else {
            parentId = ((Number) existing.get(0).get("id")).longValue();
            jdbc.update("UPDATE party_sales_parent SET emp_id = ?, remarks = ?, file_path = ?, file_name = ?, updated_by = ?, updated_at = NOW() WHERE id = ?", empId, rem(b), filePath, fileName, me, parentId);
        }
        Map<Long, BigDecimal> oldRate = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT item_id, item_rate FROM party_sales_child WHERE parent_id = ?", parentId)) oldRate.put(((Number) r.get("item_id")).longValue(), (BigDecimal) r.get("item_rate"));
        jdbc.update("DELETE FROM party_sales_child WHERE parent_id = ?", parentId);
        boolean useSpecial = "SPECIAL".equals(st.get("RATE_SHOWN"));
        for (Object[] row : rows) {
            long itemId = (Long) row[0]; @SuppressWarnings("unchecked") Map<String, BigDecimal> v = (Map<String, BigDecimal>) row[1]; BigDecimal closing = (BigDecimal) row[2];
            BigDecimal rate = oldRate.containsKey(itemId) ? oldRate.get(itemId) : itemRate(itemId);          // the item master rate - always stored
            BigDecimal special = v.get("SPECIAL_RATE") != null && v.get("SPECIAL_RATE").signum() > 0 ? v.get("SPECIAL_RATE") : null;   // stored too, only when entered
            BigDecimal use = useSpecial && special != null ? special : rate;
            jdbc.update("INSERT INTO party_sales_child (parent_id, item_id, opening_qty, purchase_qty, sale_qty, free_qty, return_retailer_qty, return_consignee_qty, expiry_retailer_qty, expiry_consignee_qty, closing_qty, calc_field, item_rate, special_rate, stock_value) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    parentId, itemId, v.get("OPENING"), v.get("PRIMARY"), v.get("SECONDARY"), v.get("FREE"), v.get("RETURN_RETAILER"), v.get("RETURN_CONSIGNEE"), v.get("EXPIRY_RETAILER"), v.get("EXPIRY_CONSIGNEE"),
                    closing, calcs.get(itemId), rate, special, closing.multiply(use).setScale(2, RoundingMode.HALF_UP));
        }
        return Map.of("id", parentId, "updated", !existing.isEmpty(), "items", rows.size());
    }

    private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
    private static String rem(Map<String, Object> b) { Object r = b.get("remarks"); return r == null || r.toString().isBlank() ? null : (r.toString().length() > 500 ? r.toString().substring(0, 500) : r.toString().trim()); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int delete(long id) {
        Authz.requireRole("ADMIN");
        return jdbc.update("DELETE FROM party_sales_parent WHERE id = ?", id);
    }
}
