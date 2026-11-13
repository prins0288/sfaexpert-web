package in.opt.sfa.tenant.inventory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * and party_sales_child (one row per item). The company decides the fields in Inventory Master (inventory_field_master): which are shown, mandatory,
 * added or subtracted in the closing (sign), and it can add its own fields (kept in extra_json).
 * Closing = sum of (sign x value) of the shown fields (the Sale field is subtracted). Only Sale typed: Closing is calculated; only Closing typed: Sale is calculated;
 * both typed: no formula (setting SALE_CLOSING_CALC). Stock value = Closing x the rate in use (RATE_SHOWN) - the item rate is taken from the item_rate column chosen in RATE_BASIS (PTS / PTR / MRP ...).
 */
@Service
public class InventoryService {
    /** built-in field code -> child column. */
    static final Map<String, String> COL = Map.of("OPENING", "opening_qty", "PRIMARY", "purchase_qty", "SECONDARY", "sale_qty", "FREE", "free_qty",
            "RETURN_RETAILER", "return_retailer_qty", "RETURN_CONSIGNEE", "return_consignee_qty", "EXPIRY_RETAILER", "expiry_retailer_qty", "EXPIRY_CONSIGNEE", "expiry_consignee_qty", "CLOSING", "closing_qty", "SPECIAL_RATE", "special_rate");
    /** rate basis -> item_rate column (a fixed list: the value goes into SQL). */
    static final Map<String, String> BASIS = Map.of("PTS", "pts", "PTR", "ptr", "MRP", "mrp", "PURCHASE_RATE", "purchase_rate");

    public record Field(String code, String label, String shortCode, boolean enabled, boolean mandatory, int sign, String role, boolean builtin, boolean core, int sort) { }

    private final JdbcTemplate jdbc;
    private final TeamScope scope;
    private final ObjectMapper json = new ObjectMapper();

    public InventoryService(JdbcTemplate jdbc, TeamScope scope) { this.jdbc = jdbc; this.scope = scope; }

    // ------------------------------------------------------------------ master (fields + settings)
    private static final Map<String, String> SETTING_DEFAULTS = Map.of("RATE_SHOWN", "SPECIAL", "FILE_UPLOAD", "N", "FILE_MANDATORY", "N", "SALE_CLOSING_CALC", "Y", "RATE_BASIS", "PTS");

    public List<Field> fields() {
        List<Field> l = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT field_code, label, short_code, is_enabled, is_mandatory, sign_op, field_role, is_builtin, is_core, sort_order FROM inventory_field_master ORDER BY sort_order, id"))
            l.add(new Field((String) r.get("field_code"), (String) r.get("label"), r.get("short_code") == null ? (String) r.get("field_code") : (String) r.get("short_code"), flag(r.get("is_enabled")), flag(r.get("is_mandatory")), ((Number) r.get("sign_op")).intValue(), (String) r.get("field_role"), flag(r.get("is_builtin")), flag(r.get("is_core")), ((Number) r.get("sort_order")).intValue()));
        return l;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> config() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fields", fields().stream().map(f -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("code", f.code()); m.put("label", f.label()); m.put("short", f.shortCode()); m.put("enabled", f.enabled()); m.put("mandatory", f.mandatory()); m.put("sign", f.sign()); m.put("role", f.role()); m.put("builtin", f.builtin()); m.put("core", f.core()); m.put("sortOrder", f.sort()); return m; }).toList());
        out.put("settings", settings());
        return out;
    }

    Map<String, String> settings() {
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
                String code = String.valueOf(f.get("code")).trim().toUpperCase();
                String label = f.get("label") == null ? "" : f.get("label").toString().trim();
                if (label.isEmpty()) throw new IllegalArgumentException("Label of " + code + " is required");
                String shortCode = f.get("short") == null || f.get("short").toString().isBlank() ? null : f.get("short").toString().trim().toUpperCase();
                if (shortCode != null && shortCode.length() > 10) throw new IllegalArgumentException("Short code can be at most 10 characters");
                List<Map<String, Object>> cur = jdbc.queryForList("SELECT field_role, is_builtin, is_core FROM inventory_field_master WHERE field_code = ?", code);
                int sign = f.get("sign") == null ? 1 : Integer.parseInt(f.get("sign").toString());
                if (sign < -1 || sign > 1) throw new IllegalArgumentException("Sign must be -1, 0 or 1");
                if (cur.isEmpty()) {   // a field the company adds
                    if (!code.matches("[A-Z][A-Z0-9_]{1,23}")) throw new IllegalArgumentException("Code: 2-24 capital letters / digits / _ (e.g. DAMAGE)");
                    jdbc.update("INSERT INTO inventory_field_master (field_code, label, short_code, is_enabled, is_mandatory, sign_op, field_role, is_builtin, sort_order, updated_by, updated_at) VALUES (?,?,?,?,?,?, 'NORMAL', ?, ?, ?, NOW())",
                            code, label, shortCode == null ? code.substring(0, Math.min(6, code.length())) : shortCode, flag(f.get("enabled")) ? 1 : 0, flag(f.get("mandatory")) ? 1 : 0, sign, COL.containsKey(code) && !"CLOSING".equals(code) && !"SPECIAL_RATE".equals(code) ? 1 : 0, f.get("sortOrder") == null ? 200 : Integer.parseInt(f.get("sortOrder").toString()), me);
                    continue;
                }
                String role = (String) cur.get(0).get("field_role");
                boolean core = flag(cur.get(0).get("is_core")), fixedRole = !"NORMAL".equals(role);   // sale / closing / rate / value: the role decides the sign and they can never be mandatory; a core field is always shown
                jdbc.update("UPDATE inventory_field_master SET label = ?, short_code = COALESCE(?, short_code), is_enabled = ?, is_mandatory = ?, sign_op = ?, sort_order = ?, updated_by = ?, updated_at = NOW() WHERE field_code = ?",
                        label, shortCode, core || flag(f.get("enabled")) ? 1 : 0, !fixedRole && flag(f.get("mandatory")) ? 1 : 0, fixedRole ? ("SALE".equals(role) ? -1 : 0) : (core ? ("FREE".equals(code) ? -1 : 1) : sign), f.get("sortOrder") == null ? 100 : Integer.parseInt(f.get("sortOrder").toString()), me, code);
            }
        }
        if (b.get("settings") instanceof Map<?, ?> s) {
            for (Map.Entry<?, ?> e : s.entrySet()) {
                String k = String.valueOf(e.getKey()), v = String.valueOf(e.getValue());
                if (!SETTING_DEFAULTS.containsKey(k)) continue;
                if ("RATE_SHOWN".equals(k) && !List.of("SPECIAL", "ITEM").contains(v)) throw new IllegalArgumentException("Rate shown must be SPECIAL or ITEM");
                if ("RATE_BASIS".equals(k) && !BASIS.containsKey(v)) throw new IllegalArgumentException("Rate basis must be one of " + BASIS.keySet());
                if (!List.of("RATE_SHOWN", "RATE_BASIS").contains(k) && !List.of("Y", "N").contains(v)) throw new IllegalArgumentException(k + " must be Y or N");
                jdbc.update("INSERT INTO inventory_setting (setting_key, setting_value, updated_by, updated_at) VALUES (?,?,?,NOW()) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_by = VALUES(updated_by), updated_at = NOW()", k, v, me);
            }
        }
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteField(String code) {
        Authz.requireRole("ADMIN");
        if (jdbc.update("DELETE FROM inventory_field_master WHERE field_code = ? AND is_core = 0", code) == 0) throw new IllegalArgumentException("Opening, Purchase, Sale, Free Goods, Closing and Stock Value always stay (you can rename them); any other field can be deleted");
    }

    static boolean flag(Object o) { return Boolean.TRUE.equals(o) || "1".equals(String.valueOf(o)) || "true".equalsIgnoreCase(String.valueOf(o)); }

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
                       CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS party, st.state_name AS state, p.sale_month AS month, p.sale_year AS year, p.rate_basis AS rateBasis,
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
        return scope.filter(TeamScope.Scope.TEAM_SELF, jdbc.queryForList(sql.toString(), a.toArray()), r -> (String) r.get("empId"));
    }

    // ------------------------------------------------------------------ rates
    /** The item rate on the chosen basis (item_rate.pts / ptr / mrp ...): the rate of the employee's state when there is one, else the all-state rate. */
    BigDecimal itemRate(long itemId, String basis, Long stateId) {
        String col = BASIS.getOrDefault(basis, "pts");
        List<BigDecimal> l = jdbc.query("SELECT r." + col + " FROM item_rate r WHERE r.item_id = ? AND r.status = 1 AND (r.effective_from IS NULL OR r.effective_from <= CURDATE()) AND (r.effective_to IS NULL OR r.effective_to >= CURDATE()) AND r." + col + " IS NOT NULL AND (r.state_key = 0 OR r.state_id = ?) ORDER BY (r.state_id = ?) DESC, r.effective_from DESC LIMIT 1",
                (rs, i) -> rs.getBigDecimal(1), itemId, stateId, stateId);
        return l.isEmpty() || l.get(0) == null ? BigDecimal.ZERO : l.get(0);
    }

    private Long stateOf(String empId) { List<Long> l = jdbc.query("SELECT state_id FROM emp_detail WHERE emp_id = ?", (rs, i) -> rs.getObject(1) == null ? null : rs.getLong(1), empId); return l.isEmpty() ? null : l.get(0); }

    // ------------------------------------------------------------------ entry (open) and save
    /** The grid of a party and month: every product with its rate and - when this party / month was submitted before - the values (the submit then UPDATES). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> entry(String empId, long partyId, int month, int year) {
        checkEmp(empId); checkPeriod(month, year);
        Map<String, Object> party = partyOf(empId, partyId);
        List<Map<String, Object>> parent = jdbc.queryForList("SELECT id, remarks, rate_basis AS rateBasis, file_path AS filePath, file_name AS fileName, updated_at AS updatedAt, created_at AS createdAt FROM party_sales_parent WHERE party_id = ? AND sale_month = ? AND sale_year = ?", partyId, month, year);
        Map<Long, Map<String, Object>> saved = new HashMap<>();
        if (!parent.isEmpty()) for (Map<String, Object> r : jdbc.queryForList("SELECT * FROM party_sales_child WHERE parent_id = ?", parent.get(0).get("id"))) saved.put(((Number) r.get("item_id")).longValue(), r);
        Map<String, String> st = settings(); Long state = stateOf(empId);
        List<Field> fields = fields();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> it : jdbc.queryForList("""
                SELECT i.id, i.item_code AS code, i.item_name AS name, i.pack_size AS pack FROM item_master i JOIN item_type_master t ON t.id = i.item_type_id
                WHERE i.status = 1 AND t.item_type_code = 'PRODUCT' ORDER BY i.item_name""")) {
            long id = ((Number) it.get("id")).longValue();
            Map<String, Object> m = new LinkedHashMap<>(it);
            Map<String, Object> s = saved.get(id);
            m.put("rate", s != null ? s.get("item_rate") : itemRate(id, st.get("RATE_BASIS"), state));   // the rate stored with the first submit stays
            Map<String, Object> v = new LinkedHashMap<>();
            Map<String, Object> extra = s == null || s.get("extra_json") == null ? Map.of() : parse((String) s.get("extra_json"));
            String cf = s == null ? null : (String) s.get("calc_field");   // a calculated value is not "typed": it shows as the calculation again, so a later change recalculates it
            for (Field f : fields) {
                Object val = s == null ? null : (COL.containsKey(f.code()) ? s.get(COL.get(f.code())) : extra.get(f.code()));
                if (("C".equals(cf) && "CLOSING".equals(f.code())) || ("S".equals(cf) && "SECONDARY".equals(f.code()))) val = null;
                v.put(f.code(), val);
            }
            m.put("v", v); m.put("closing", s == null ? null : s.get("closing_qty")); m.put("stockValue", s == null ? null : s.get("stock_value"));
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exists", !parent.isEmpty()); out.put("parent", parent.isEmpty() ? null : parent.get(0)); out.put("party", party); out.put("items", items);
        out.put("month", month); out.put("year", year); out.putAll(config());
        return out;
    }

    private Map<String, Object> parse(String j) { try { return json.readValue(j, new TypeReference<Map<String, Object>>() { }); } catch (Exception e) { return Map.of(); } }

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
    private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }

    /** Insert the party / month when it is new, update it when it exists (unique party + month + year). */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> b) throws Exception {
        String empId = b.get("empId") == null ? null : b.get("empId").toString();
        checkEmp(empId);
        long partyId = Long.parseLong(String.valueOf(b.get("partyId")));
        int month = Integer.parseInt(String.valueOf(b.get("month"))), year = Integer.parseInt(String.valueOf(b.get("year")));
        checkPeriod(month, year); partyOf(empId, partyId);
        Map<String, String> st = settings();
        List<Field> fields = fields();
        Field sale = fields.stream().filter(f -> "SALE".equals(f.role()) && f.enabled()).findFirst().orElse(null), closingF = fields.stream().filter(f -> "CLOSING".equals(f.role()) && f.enabled()).findFirst().orElse(null);
        List<Field> typed = fields.stream().filter(f -> f.enabled() && !"VALUE".equals(f.role())).toList();
        boolean calcOn = "Y".equals(st.get("SALE_CLOSING_CALC"));

        List<Map<String, Object>> existing = jdbc.queryForList("SELECT id, file_path, file_name, rate_basis FROM party_sales_parent WHERE party_id = ? AND sale_month = ? AND sale_year = ?", partyId, month, year);
        String filePath = b.get("filePath") == null || b.get("filePath").toString().isBlank() ? null : b.get("filePath").toString(), fileName = b.get("fileName") == null ? null : b.get("fileName").toString();
        boolean fileOn = "Y".equals(st.get("FILE_UPLOAD"));
        if (!fileOn) { filePath = null; fileName = null; }
        else if (filePath == null && !existing.isEmpty() && existing.get(0).get("file_path") != null && !Boolean.TRUE.equals(b.get("removeFile"))) { filePath = (String) existing.get(0).get("file_path"); fileName = (String) existing.get(0).get("file_name"); }
        if (fileOn && "Y".equals(st.get("FILE_MANDATORY")) && filePath == null) throw new IllegalArgumentException("Upload the file (statement) - it is required");

        // the rows: only items with something typed; the mandatory fields must be filled in those rows; closing and value are calculated here, never taken from the client
        List<Object[]> rows = new ArrayList<>();
        for (Object o : (List<Object>) b.getOrDefault("rows", List.of())) {
            Map<String, Object> r = (Map<String, Object>) o;
            long itemId = Long.parseLong(String.valueOf(r.get("itemId")));
            Map<String, BigDecimal> v = new LinkedHashMap<>(); boolean any = false;
            for (Field f : typed) { BigDecimal x = num(r.get(f.code()), f.label()); v.put(f.code(), x); if (x != null) any = true; }
            if (!any) continue;
            String name = jdbc.queryForList("SELECT item_name FROM item_master WHERE id = ?", String.class, itemId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
            for (Field f : typed) if (f.mandatory() && "NORMAL".equals(f.role()) && v.get(f.code()) == null) throw new IllegalArgumentException(f.label() + " is required for " + name);
            if (sale != null && sale.mandatory() && closingF == null && v.get(sale.code()) == null) throw new IllegalArgumentException(sale.label() + " is required for " + name);
            BigDecimal base = BigDecimal.ZERO;   // closing = base - sale
            for (Field f : typed) if ("NORMAL".equals(f.role()) && f.sign() != 0) base = base.add(nz(v.get(f.code())).multiply(BigDecimal.valueOf(f.sign())));
            BigDecimal s = sale == null ? null : v.get(sale.code()), c = closingF == null ? null : v.get(closingF.code()); String calc = null;
            if (calcOn && !(s != null && c != null)) {   // both typed: no formula
                if (s != null) { c = base.subtract(s); calc = "C"; }
                else if (c != null && sale != null) { s = base.subtract(c); calc = "S"; if (s.signum() < 0) throw new IllegalArgumentException("Closing of " + name + " is more than the stock allows (" + sale.label() + " would be " + s.stripTrailingZeros().toPlainString() + ")"); }
                else if (c == null) { c = base; calc = "C"; }
            }
            if (c == null) c = BigDecimal.ZERO;
            if (c.signum() < 0) throw new IllegalArgumentException("Closing of " + name + " is negative (" + c.stripTrailingZeros().toPlainString() + ") - check the quantities");
            if (sale != null) v.put(sale.code(), s);
            rows.add(new Object[]{itemId, v, c, calc});
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("Enter the stock of at least one item");

        String me = TeamScope.me(), basis = st.get("RATE_BASIS");
        long parentId;
        if (existing.isEmpty()) {
            jdbc.update("INSERT INTO party_sales_parent (emp_id, party_id, sale_month, sale_year, rate_basis, status, remarks, file_path, file_name, created_by) VALUES (?,?,?,?,?, 'SUBMITTED', ?,?,?,?) ON DUPLICATE KEY UPDATE updated_by = VALUES(created_by), updated_at = NOW()",
                    empId, partyId, month, year, basis, rem(b), filePath, fileName, me);
            parentId = jdbc.queryForObject("SELECT id FROM party_sales_parent WHERE party_id = ? AND sale_month = ? AND sale_year = ?", Long.class, partyId, month, year);
        } else {
            parentId = ((Number) existing.get(0).get("id")).longValue();
            jdbc.update("UPDATE party_sales_parent SET emp_id = ?, remarks = ?, file_path = ?, file_name = ?, updated_by = ?, updated_at = NOW() WHERE id = ?", empId, rem(b), filePath, fileName, me, parentId);
            basis = (String) existing.get(0).get("rate_basis");   // an updated record keeps the rate basis it was submitted on
        }
        Map<Long, BigDecimal> oldRate = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT item_id, item_rate FROM party_sales_child WHERE parent_id = ?", parentId)) oldRate.put(((Number) r.get("item_id")).longValue(), (BigDecimal) r.get("item_rate"));
        jdbc.update("DELETE FROM party_sales_child WHERE parent_id = ?", parentId);
        boolean useSpecial = "SPECIAL".equals(st.get("RATE_SHOWN")); Long state = stateOf(empId);
        Field rateF = fields.stream().filter(f -> "RATE".equals(f.role()) && f.enabled()).findFirst().orElse(null);
        for (Object[] row : rows) {
            long itemId = (Long) row[0]; Map<String, BigDecimal> v = (Map<String, BigDecimal>) row[1]; BigDecimal closing = (BigDecimal) row[2];
            BigDecimal rate = oldRate.containsKey(itemId) ? oldRate.get(itemId) : itemRate(itemId, basis, state);   // the item rate on the chosen basis - always stored
            BigDecimal special = rateF != null && v.get(rateF.code()) != null && v.get(rateF.code()).signum() > 0 ? v.get(rateF.code()) : null;   // stored too, only when entered
            BigDecimal use = useSpecial && special != null ? special : rate;
            Map<String, Object> cols = new LinkedHashMap<>(), extra = new LinkedHashMap<>();
            for (Field f : typed) { BigDecimal x = v.get(f.code()); if (f.builtin() && COL.containsKey(f.code()) && !"CLOSING".equals(f.code()) && !"SPECIAL_RATE".equals(f.code())) cols.put(COL.get(f.code()), x); else if (!f.builtin() && x != null) extra.put(f.code(), x); }
            StringBuilder ins = new StringBuilder("INSERT INTO party_sales_child (parent_id, item_id, closing_qty, calc_field, item_rate, special_rate, stock_value, extra_json"); List<Object> args = new ArrayList<>(List.of(parentId, itemId, closing, row[3] == null ? "" : row[3], rate, special == null ? "" : special, closing.multiply(use).setScale(2, RoundingMode.HALF_UP), extra.isEmpty() ? "" : json.writeValueAsString(extra)));
            for (String c : cols.keySet()) ins.append(", ").append(c);
            ins.append(") VALUES (?,?,?,NULLIF(?, ''),?,NULLIF(?, ''),?,NULLIF(?, '')").append(", ?".repeat(cols.size())).append(")");
            args.addAll(cols.values());
            jdbc.update(ins.toString(), args.toArray());
        }
        return Map.of("id", parentId, "updated", !existing.isEmpty(), "items", rows.size());
    }

    private static String rem(Map<String, Object> b) { Object r = b.get("remarks"); return r == null || r.toString().isBlank() ? null : (r.toString().length() > 500 ? r.toString().substring(0, 500) : r.toString().trim()); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int delete(long id) {
        Authz.requireRole("ADMIN");
        return jdbc.update("DELETE FROM party_sales_parent WHERE id = ?", id);
    }
}
