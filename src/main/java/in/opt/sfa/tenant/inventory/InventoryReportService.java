package in.opt.sfa.tenant.inventory;

import in.opt.sfa.tenant.team.TeamScope;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Company stock report: items x (State / HQ / Designation / Employee / Party) x the stock fields (Opening, Purchase, Sale, Free, Returns, Expiry, Closing) in qty and / or value.
 * Several months: the flows are summed, the Opening is the opening of the first month and the Closing the closing of the last month (per party and item).
 * Value = quantity x the price type chosen (APPLIED = the Inventory Master setting, ITEM = item rate, SPECIAL = special rate when entered else item rate).
 */
@Service
public class InventoryReportService {
    public record Q(Integer year, List<Integer> months, Long stateId, Long hqId, Integer designationId, String empId, Long partyId, String groupBy, List<String> types, String priceType) { }

    private final JdbcTemplate jdbc;
    private final TeamScope scope;
    private final InventoryService inv;
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();

    public InventoryReportService(JdbcTemplate jdbc, TeamScope scope, InventoryService inv) { this.jdbc = jdbc; this.scope = scope; this.inv = inv; }

    /** Parties of the employees the user may see (optionally of one employee / state). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> partyOptions(String empId, Long stateId) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS name, c.mapped_emp_code AS empId, e.state_id AS stateId
                FROM client_master c JOIN client_type ct ON ct.oid = c.client_type_id JOIN emp_detail e ON e.emp_id = c.mapped_emp_code
                WHERE c.is_active = 1 AND c.approval_status = 'APPROVED' AND ct.dcr_tab = 'PARTY'""");
        List<Object> a = new ArrayList<>();
        if (empId != null && !empId.isBlank()) { sql.append(" AND c.mapped_emp_code = ?"); a.add(empId); }
        if (stateId != null) { sql.append(" AND e.state_id = ?"); a.add(stateId); }
        sql.append(" ORDER BY c.client_name");
        return scope.filter(TeamScope.Scope.TEAM_SELF, jdbc.queryForList(sql.toString(), a.toArray()), r -> (String) r.get("empId"));
    }

    private static BigDecimal d(Object o) { return o == null ? BigDecimal.ZERO : (o instanceof BigDecimal b ? b : new BigDecimal(o.toString())); }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> pivot(Q q) {
        String applied = inv.settings().get("RATE_SHOWN");
        String price = q.priceType() == null || q.priceType().isBlank() || "APPLIED".equals(q.priceType()) ? ("ITEM".equals(applied) ? "ITEM" : "SPECIAL") : q.priceType();
        String gb = q.groupBy() == null ? "PARTY" : q.groupBy();
        // the fields of the report are the fields the company set up (shown, not the rate / value ones)
        List<InventoryService.Field> flds = inv.fields().stream().filter(f -> f.enabled() && !"VALUE".equals(f.role()) && !"RATE".equals(f.role())).toList();

        StringBuilder sql = new StringBuilder("SELECT p.id AS pid, p.party_id, p.emp_id, p.sale_month AS mon, c.client_name AS party, e.emp_name AS emp, e.emp_code AS ecode, e.state_id, st.state_name AS state, e.hq_id, hq.hq_name AS hq, e.designation AS des_id, ds.designation AS des, ch.*");
        sql.append("""

                FROM party_sales_child ch JOIN party_sales_parent p ON p.id = ch.parent_id JOIN client_master c ON c.id = p.party_id JOIN emp_detail e ON e.emp_id = p.emp_id
                LEFT JOIN state_master st ON st.oid = e.state_id LEFT JOIN hq_master hq ON hq.oid = e.hq_id LEFT JOIN designation_master ds ON ds.id = e.designation WHERE 1 = 1""");
        List<Object> a = new ArrayList<>();
        if (q.year() != null) { sql.append(" AND p.sale_year = ?"); a.add(q.year()); }
        if (q.months() != null && !q.months().isEmpty()) { sql.append(" AND p.sale_month IN (").append(String.join(",", Collections.nCopies(q.months().size(), "?"))).append(")"); a.addAll(q.months()); }
        if (q.stateId() != null) { sql.append(" AND e.state_id = ?"); a.add(q.stateId()); }
        if (q.hqId() != null) { sql.append(" AND e.hq_id = ?"); a.add(q.hqId()); }
        if (q.designationId() != null) { sql.append(" AND e.designation = ?"); a.add(q.designationId()); }
        if (q.empId() != null && !q.empId().isBlank()) { sql.append(" AND p.emp_id = ?"); a.add(q.empId()); }
        if (q.partyId() != null) { sql.append(" AND p.party_id = ?"); a.add(q.partyId()); }
        List<Map<String, Object>> raw = new ArrayList<>(scope.filter(TeamScope.Scope.TEAM_SELF, jdbc.queryForList(sql.toString(), a.toArray()), r -> (String) r.get("emp_id")));

        // per party + item: flows summed, opening of the first month, closing of the last month
        Map<String, Map<String, Object>> pi = new LinkedHashMap<>();
        raw.sort(Comparator.comparingInt(r -> ((Number) r.get("mon")).intValue()));
        for (Map<String, Object> r : raw) {
            String key = r.get("party_id") + "|" + r.get("item_id");
            Map<String, Object> acc = pi.get(key);
            BigDecimal rate = "ITEM".equals(price) || r.get("special_rate") == null ? d(r.get("item_rate")) : d(r.get("special_rate"));
            if (acc == null) { acc = new HashMap<>(r); acc.put("_q", new HashMap<String, BigDecimal>()); acc.put("_v", new HashMap<String, BigDecimal>()); pi.put(key, acc); }
            @SuppressWarnings("unchecked") Map<String, BigDecimal> qm = (Map<String, BigDecimal>) acc.get("_q"); @SuppressWarnings("unchecked") Map<String, BigDecimal> vm = (Map<String, BigDecimal>) acc.get("_v");
            Map<String, Object> extra = Map.of();
            if (r.get("extra_json") != null) { try { extra = json.readValue((String) r.get("extra_json"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }); } catch (Exception e) { /* ignore */ } }
            for (InventoryService.Field f : flds) {
                BigDecimal x = d(InventoryService.COL.containsKey(f.code()) ? r.get(InventoryService.COL.get(f.code())) : extra.get(f.code()));
                switch (f.code()) {
                    case "OPENING" -> { if (!qm.containsKey("OPENING")) { qm.put("OPENING", x); vm.put("OPENING", x.multiply(rate)); } }   // first month only
                    case "CLOSING" -> { qm.put("CLOSING", x); vm.put("CLOSING", x.multiply(rate)); }                                         // the last month wins
                    default -> { qm.merge(f.code(), x, BigDecimal::add); vm.merge(f.code(), x.multiply(rate), BigDecimal::add); }
                }
            }
        }
        // group
        Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
        Map<Long, Map<String, Object>> items = new LinkedHashMap<>();
        Map<String, Map<Long, Map<String, BigDecimal[]>>> cell = new HashMap<>();
        for (Map<String, Object> r : pi.values()) {
            String key, label, sub = null;
            switch (gb) {
                case "STATE" -> { key = String.valueOf(r.get("state_id")); label = (String) r.get("state"); }
                case "HQ" -> { key = String.valueOf(r.get("hq_id")); label = (String) r.get("hq"); sub = (String) r.get("state"); }
                case "DESIGNATION" -> { key = String.valueOf(r.get("des_id")); label = (String) r.get("des"); }
                case "EMPLOYEE" -> { key = (String) r.get("emp_id"); label = r.get("emp") + " (" + r.get("ecode") + ")"; sub = join(r.get("state"), r.get("hq")); }
                default -> { key = String.valueOf(r.get("party_id")); label = (String) r.get("party"); sub = join(r.get("state"), r.get("hq"), r.get("emp")); }
            }
            if (label == null || label.isBlank()) label = "-";
            groups.computeIfAbsent(key, k -> { Map<String, Object> g = new LinkedHashMap<>(); g.put("key", k); return g; }).putIfAbsent("label", label);
            if (sub != null) groups.get(key).putIfAbsent("sub", sub);
            long itemId = ((Number) r.get("item_id")).longValue();
            items.putIfAbsent(itemId, null);
            @SuppressWarnings("unchecked") Map<String, BigDecimal> qm = (Map<String, BigDecimal>) r.get("_q"); @SuppressWarnings("unchecked") Map<String, BigDecimal> vm = (Map<String, BigDecimal>) r.get("_v");
            Map<String, BigDecimal[]> cm = cell.computeIfAbsent(key, k -> new HashMap<>()).computeIfAbsent(itemId, k -> new HashMap<>());
            for (InventoryService.Field f : flds) { BigDecimal[] t = cm.computeIfAbsent(f.code(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}); t[0] = t[0].add(qm.getOrDefault(f.code(), BigDecimal.ZERO)); t[1] = t[1].add(vm.getOrDefault(f.code(), BigDecimal.ZERO)); }
        }
        List<Map<String, Object>> glist = new ArrayList<>(groups.values()); glist.sort(Comparator.comparing(g -> String.valueOf(g.get("label"))));
        List<InventoryService.Field> fields = flds.stream().filter(f -> q.types() == null || q.types().isEmpty() || q.types().contains(f.shortCode())).toList();

        List<Map<String, Object>> irows = new ArrayList<>();
        if (!items.isEmpty()) {
            String in = String.join(",", Collections.nCopies(items.size(), "?"));
            Map<Long, Map<String, Object>> meta = new HashMap<>();
            for (Map<String, Object> m : jdbc.queryForList("SELECT id, item_code AS code, item_name AS name, pack_size AS pack FROM item_master WHERE id IN (" + in + ")", items.keySet().toArray())) meta.put(((Number) m.get("id")).longValue(), m);
            List<Long> ids = new ArrayList<>(items.keySet()); ids.sort(Comparator.comparing(i -> String.valueOf(meta.getOrDefault(i, Map.of()).get("name"))));
            String basis = inv.settings().get("RATE_BASIS");
            for (Long id : ids) {
                Map<String, Object> m = new LinkedHashMap<>(meta.getOrDefault(id, Map.of())); m.put("id", id);
                m.put("rate", inv.itemRate(id, basis, null));
                List<Object> cells = new ArrayList<>();
                for (Map<String, Object> g : glist) for (InventoryService.Field f : fields) { BigDecimal[] t = Optional.ofNullable(cell.get(g.get("key"))).map(x -> x.get(id)).map(x -> x.get(f.code())).orElse(null); cells.add(t == null ? new double[]{0, 0} : new double[]{t[0].doubleValue(), t[1].setScale(2, RoundingMode.HALF_UP).doubleValue()}); }
                m.put("cells", cells); irows.add(m);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("groups", glist); out.put("fields", flds.stream().map(f -> Map.of("code", f.shortCode(), "label", f.label(), "fieldCode", f.code())).toList()); out.put("items", irows); out.put("price", price); out.put("groupBy", gb); out.put("rateBasis", inv.settings().get("RATE_BASIS"));
        return out;
    }

    private static String join(Object... p) { StringBuilder s = new StringBuilder(); for (Object o : p) if (o != null && !o.toString().isBlank()) { if (s.length() > 0) s.append(" · "); s.append(o); } return s.length() == 0 ? null : s.toString(); }

    /** The same pivot as an Excel file; view = QTY | VAL | BOTH. */
    @SuppressWarnings("unchecked")
    public byte[] excel(Q q, String view) throws Exception {
        Map<String, Object> p = pivot(q);
        List<Map<String, Object>> groups = (List<Map<String, Object>>) p.get("groups"), fields = (List<Map<String, Object>>) p.get("fields"), items = (List<Map<String, Object>>) p.get("items");
        boolean bq = !"VAL".equals(view), bv = !"QTY".equals(view); int per = (bq ? 1 : 0) + (bv ? 1 : 0);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Stock");
            CellStyle hs = wb.createCellStyle(); Font hf = wb.createFont(); hf.setBold(true); hf.setColor(IndexedColors.WHITE.getIndex()); hs.setFont(hf); hs.setFillForegroundColor(IndexedColors.TEAL.getIndex()); hs.setFillPattern(FillPatternType.SOLID_FOREGROUND); hs.setAlignment(HorizontalAlignment.CENTER);
            Row r0 = sh.createRow(0), r1 = sh.createRow(1), r2 = sh.createRow(2);
            String[] fixed = {"#", "Item", "Pack", "Rate"};
            for (int i = 0; i < 4; i++) { Cell c = r0.createCell(i); c.setCellValue(fixed[i]); c.setCellStyle(hs); r1.createCell(i).setCellStyle(hs); r2.createCell(i).setCellStyle(hs); sh.addMergedRegion(new CellRangeAddress(0, 2, i, i)); }
            int col = 4;
            for (Map<String, Object> g : groups) {
                int gw = fields.size() * per; Cell c = r0.createCell(col); c.setCellValue(String.valueOf(g.get("label")) + (g.get("sub") != null ? " (" + g.get("sub") + ")" : "")); c.setCellStyle(hs);
                for (int k = 1; k < gw; k++) r0.createCell(col + k).setCellStyle(hs);
                if (gw > 1) sh.addMergedRegion(new CellRangeAddress(0, 0, col, col + gw - 1));
                for (Map<String, Object> f : fields) {
                    Cell fc = r1.createCell(col); fc.setCellValue(String.valueOf(f.get("label"))); fc.setCellStyle(hs); if (per > 1) { r1.createCell(col + 1).setCellStyle(hs); sh.addMergedRegion(new CellRangeAddress(1, 1, col, col + 1)); }
                    if (bq) { Cell x = r2.createCell(col++); x.setCellValue("Qty"); x.setCellStyle(hs); }
                    if (bv) { Cell x = r2.createCell(col++); x.setCellValue("Val"); x.setCellStyle(hs); }
                }
            }
            sh.createFreezePane(4, 3);
            int rn = 3, no = 1;
            for (Map<String, Object> it : items) {
                Row r = sh.createRow(rn++); r.createCell(0).setCellValue(no++); r.createCell(1).setCellValue(String.valueOf(it.get("name"))); r.createCell(2).setCellValue(it.get("pack") == null ? "" : it.get("pack").toString()); r.createCell(3).setCellValue(d(it.get("rate")).doubleValue());
                int cc = 4; for (Object o : (List<Object>) it.get("cells")) { double[] v = (double[]) o; if (bq) r.createCell(cc++).setCellValue(v[0]); if (bv) r.createCell(cc++).setCellValue(v[1]); }
            }
            sh.setColumnWidth(1, 9000);
            wb.write(os); return os.toByteArray();
        }
    }
}
