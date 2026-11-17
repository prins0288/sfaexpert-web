package in.opt.sfa.tenant.expense;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.dcr.geo.GeoCalc;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Monthly expense statement of an employee, built from his DCR days. The user submits it, his manager approves it, then the admin approves it; the manager and the admin
 * can change any amount (every change is logged with who, when and why, and the changed row is coloured by the last level that changed it).
 * Route of a working day: starting point (area_master.is_home) -> the areas visited in the order of the visits -> back to the starting point, or - when he stays the night in
 * an area - ending at that area. The next working day then starts from the stay area; after a holiday / weekly off it starts from the stay area only when the setting
 * START_FROM_STAY_AFTER_OFF is Y, else from the starting point.
 */
@Service
public class ExpenseService {
    private final JdbcTemplate jdbc;
    private final TeamScope scope;
    private final ObjectMapper json = new ObjectMapper();
    private final in.opt.sfa.tenant.master.areatype.AreaTypeService areaTypes;

    public ExpenseService(JdbcTemplate jdbc, TeamScope scope, in.opt.sfa.tenant.master.areatype.AreaTypeService areaTypes) { this.jdbc = jdbc; this.scope = scope; this.areaTypes = areaTypes; }

    // ------------------------------------------------------------------ settings and rates (admin)
    public Map<String, String> settings() {
        Map<String, String> m = new LinkedHashMap<>(Map.of("START_FROM_STAY_AFTER_OFF", "N", "TYPE_RULE", "MAX", "STAY_FORCES_TYPE", "Y"));
        for (Map<String, Object> r : jdbc.queryForList("SELECT setting_key, setting_value FROM expense_setting")) m.put((String) r.get("setting_key"), String.valueOf(r.get("setting_value")));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> setup() {
        Authz.requireRole("ADMIN");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("settings", settings());
        out.put("areaTypes", areaTypes.types().stream().map(t -> Map.of("code", t.code(), "name", t.name())).toList());
        out.put("da", jdbc.queryForList("SELECT r.id, r.designation_id AS designationId, d.designation AS designation, r.area_type AS areaType, r.amount FROM expense_da_rate r LEFT JOIN designation_master d ON d.id = r.designation_id ORDER BY d.designation, r.area_type"));
        out.put("fare", jdbc.queryForList("SELECT r.id, r.designation_id AS designationId, d.designation AS designation, r.travel_mode AS travelMode, r.rate_per_km AS ratePerKm FROM expense_fare_rate r LEFT JOIN designation_master d ON d.id = r.designation_id ORDER BY d.designation, r.travel_mode"));
        out.put("designations", jdbc.queryForList("SELECT id, designation AS name FROM designation_master ORDER BY hierarchy_level, designation"));
        out.put("travelModes", jdbc.queryForList("SELECT travel_type_name AS name FROM travel_type_master WHERE is_active = 1 ORDER BY travel_type_name").stream().map(r -> r.get("name")).toList());
        return out;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveSettings(Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        for (String k : List.of("START_FROM_STAY_AFTER_OFF", "TYPE_RULE", "STAY_FORCES_TYPE")) {
            if (!b.containsKey(k)) continue;
            String v = String.valueOf(b.get(k));
            if ("TYPE_RULE".equals(k) ? !List.of("MAX", "LAST").contains(v) : !List.of("Y", "N").contains(v)) throw new IllegalArgumentException(k + " has a wrong value");
            jdbc.update("INSERT INTO expense_setting (setting_key, setting_value, updated_by, updated_at) VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_by = VALUES(updated_by), updated_at = NOW()", k, v, TeamScope.me());
        }
    }

    private static Integer intOrNull(Object o) { return o == null || o.toString().isBlank() ? null : Integer.valueOf(o.toString()); }
    private static BigDecimal money(Object o, String what) {
        try { BigDecimal v = new BigDecimal(String.valueOf(o).trim()); if (v.signum() < 0) throw new IllegalArgumentException(what + " cannot be negative"); return v.setScale(2, RoundingMode.HALF_UP); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(what + " must be a number"); }
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveDa(Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        String type = String.valueOf(b.get("areaType")).trim().toUpperCase();
        if (areaTypes.types().stream().noneMatch(t -> t.code().equals(type))) throw new IllegalArgumentException("Choose an area type of the Area Type Master");
        jdbc.update("INSERT INTO expense_da_rate (designation_id, area_type, amount) VALUES (?,?,?) ON DUPLICATE KEY UPDATE amount = VALUES(amount)", intOrNull(b.get("designationId")), type, money(b.get("amount"), "Amount"));
    }
    @Transactional(transactionManager = "tenantTransactionManager")
    public void saveFare(Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        String mode = String.valueOf(b.get("travelMode")).trim();
        if (mode.isEmpty() || "null".equals(mode)) throw new IllegalArgumentException("Choose the travel mode");
        jdbc.update("INSERT INTO expense_fare_rate (designation_id, travel_mode, rate_per_km) VALUES (?,?,?) ON DUPLICATE KEY UPDATE rate_per_km = VALUES(rate_per_km)", intOrNull(b.get("designationId")), mode, money(b.get("ratePerKm"), "Rate"));
    }
    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteRate(String kind, long id) { Authz.requireRole("ADMIN"); jdbc.update("DELETE FROM " + ("da".equals(kind) ? "expense_da_rate" : "expense_fare_rate") + " WHERE id = ?", id); }

    // ------------------------------------------------------------------ DA / fare of one employee by area type (set in Create Employee)
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> empRates(String empId) {
        Authz.requireRole("ADMIN", "MANAGER");
        return jdbc.queryForList("SELECT type_code AS typeCode, da_amount AS da, fare_per_km AS fare FROM expense_emp_rate WHERE emp_id = ?", empId);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public void saveEmpRates(String empId, List<Map<String, Object>> rates) {
        Authz.requireRole("ADMIN", "MANAGER");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM emp_detail WHERE emp_id = ?", Integer.class, empId) == 0) throw new IllegalArgumentException("Employee not found");
        Set<String> codes = new HashSet<>(); areaTypes.types().forEach(t -> codes.add(t.code()));
        for (Map<String, Object> r : rates) {
            String code = String.valueOf(r.get("typeCode"));
            if (!codes.contains(code)) throw new IllegalArgumentException("Unknown area type " + code);
            boolean noDa = r.get("da") == null || r.get("da").toString().isBlank(), noFare = r.get("fare") == null || r.get("fare").toString().isBlank();
            if (noDa && noFare) { jdbc.update("DELETE FROM expense_emp_rate WHERE emp_id = ? AND type_code = ?", empId, code); continue; }
            jdbc.update("INSERT INTO expense_emp_rate (emp_id, type_code, da_amount, fare_per_km) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE da_amount = VALUES(da_amount), fare_per_km = VALUES(fare_per_km)", empId, code, noDa ? null : money(r.get("da"), "DA"), noFare ? null : money(r.get("fare"), "Fare"));
        }
    }

    // ------------------------------------------------------------------ generation from the DCR
    private record Area(long id, String name, String type, Double lat, Double lng, Double distKm) { }

    private Map<Long, Area> areas() {
        Map<Long, Area> m = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT id, area_name, area_type, latitude, longitude, distance_km FROM area_master"))
            m.put(((Number) r.get("id")).longValue(), new Area(((Number) r.get("id")).longValue(), (String) r.get("area_name"), (String) r.get("area_type"), r.get("latitude") == null ? null : ((Number) r.get("latitude")).doubleValue(), r.get("longitude") == null ? null : ((Number) r.get("longitude")).doubleValue(), r.get("distance_km") == null ? null : ((Number) r.get("distance_km")).doubleValue()));
        return m;
    }

    /** Km of a leg: from the positions of the two areas; else from their distance from the HQ (area_master.distance_km); else 0 (the user / manager can type it). */
    private static BigDecimal legKm(Area a, Area b, Area home) {
        if (a == null || b == null || a.id() == b.id()) return BigDecimal.ZERO;
        double km;
        if (a.lat() != null && a.lng() != null && b.lat() != null && b.lng() != null) km = GeoCalc.distanceM(a.lat(), a.lng(), b.lat(), b.lng()) / 1000.0;
        else {
            double da = a.distKm() != null ? a.distKm() : (home != null && a.id() == home.id() ? 0 : -1), db = b.distKm() != null ? b.distKm() : (home != null && b.id() == home.id() ? 0 : -1);
            km = da < 0 || db < 0 ? 0 : (home != null && (a.id() == home.id() || b.id() == home.id()) ? Math.max(da, db) : Math.abs(da - db));
        }
        return BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP);
    }

    /** The master type of an area: its stored area type, else the range of its distance from the HQ, else the lowest type. */
    private String typeCode(Area a, List<in.opt.sfa.tenant.master.areatype.AreaTypeService.Type> types) {
        if (a == null) return types.isEmpty() ? "HQ" : types.get(0).code();
        String c = areaTypes.codeOf(a.type());
        if (c == null) c = areaTypes.codeForKm(a.distKm());
        return c != null ? c : (types.isEmpty() ? "HQ" : types.get(0).code());
    }
    private static int rankOf(String code, List<in.opt.sfa.tenant.master.areatype.AreaTypeService.Type> types) { for (int i = 0; i < types.size(); i++) if (types.get(i).code().equals(code)) return i; return 0; }

    private BigDecimal rate(String sql, Object desig, Object key) {
        List<BigDecimal> l = jdbc.query(sql, (rs, i) -> rs.getBigDecimal(1), desig, key);
        return l.isEmpty() ? null : l.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> calculate(String empId, int month, int year) throws Exception {
        YearMonth ym = YearMonth.of(year, month); LocalDate from = ym.atDay(1), to = ym.atEndOfMonth();
        if (to.isAfter(LocalDate.now())) to = LocalDate.now();
        Map<Long, Area> areas = areas();
        List<Long> homeL = jdbc.query("SELECT id FROM area_master WHERE emp_id = ? AND is_home = 1 LIMIT 1", (rs, i) -> rs.getLong(1), empId);
        Area home = homeL.isEmpty() ? null : areas.get(homeL.get(0));
        Map<String, String> sett = settings(); boolean fromStayAfterOff = "Y".equals(sett.get("START_FROM_STAY_AFTER_OFF"));
        List<in.opt.sfa.tenant.master.areatype.AreaTypeService.Type> types = areaTypes.types();
        Map<String, BigDecimal[]> emp = new HashMap<>();   // type code -> {da, fare per km} given to this employee
        for (Map<String, Object> r : jdbc.queryForList("SELECT type_code, da_amount, fare_per_km FROM expense_emp_rate WHERE emp_id = ?", empId)) emp.put((String) r.get("type_code"), new BigDecimal[]{(BigDecimal) r.get("da_amount"), (BigDecimal) r.get("fare_per_km")});
        Integer desig = jdbc.query("SELECT designation FROM emp_detail WHERE emp_id = ?", (rs, i) -> rs.getObject(1) == null ? null : rs.getInt(1), empId).stream().findFirst().orElse(null);
        Set<String> fieldWork = new HashSet<>(jdbc.queryForList("SELECT worktype_code FROM app_dcr_worktype WHERE emp_id IS NULL AND form_open = 'WORK'", String.class));
        Map<String, Map<String, Object>> dcr = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT dcr_id, dcr_date, work_type, status, stay_area_id, travel_mode, meter_start, meter_end FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date BETWEEN ? AND ? ORDER BY dcr_date", empId, from, to)) dcr.put(String.valueOf(r.get("dcr_date")).substring(0, 10), r);
        Map<Long, List<Long>> visits = new HashMap<>(); Map<Long, Integer> visitCount = new HashMap<>();
        Map<Long, Map<Long, BigDecimal>> heads = new HashMap<>(); Map<Long, Integer> bills = new HashMap<>(); Map<Long, String> withMap = new HashMap<>(); Map<Long, BigDecimal> travelExp = new HashMap<>();
        if (!dcr.isEmpty()) {
            String in = String.join(",", Collections.nCopies(dcr.size(), "?")); Object[] ids = dcr.values().stream().map(r -> r.get("dcr_id")).toArray();
            Object[] twice = new Object[ids.length * 2]; System.arraycopy(ids, 0, twice, 0, ids.length); System.arraycopy(ids, 0, twice, ids.length, ids.length);
            for (Map<String, Object> r : jdbc.queryForList("SELECT dcr_id, area_id, in_time FROM (SELECT dcr_id, area_id, in_time FROM dcr_client_summary WHERE dcr_id IN (" + in + ") UNION ALL SELECT dcr_id, area_id, in_time FROM dcr_unlisted_client_summary WHERE dcr_id IN (" + in + ")) x ORDER BY dcr_id, in_time", twice)) {
                long id = ((Number) r.get("dcr_id")).longValue(); visitCount.merge(id, 1, Integer::sum);
                if (r.get("area_id") != null) visits.computeIfAbsent(id, k -> new ArrayList<>()).add(((Number) r.get("area_id")).longValue());
            }
            for (Map<String, Object> r : jdbc.queryForList("SELECT x.dcr_id, x.expense_id, em.is_travel, SUM(x.amount) AS amt, SUM(CASE WHEN x.file_path IS NULL THEN 0 ELSE 1 END) AS bills FROM dcr_expense x JOIN expense_master em ON em.id = x.expense_id WHERE x.dcr_id IN (" + in + ") GROUP BY x.dcr_id, x.expense_id, em.is_travel", ids)) {
                long id = ((Number) r.get("dcr_id")).longValue(); BigDecimal amt = (BigDecimal) r.get("amt");
                if (Boolean.TRUE.equals(r.get("is_travel")) || (r.get("is_travel") instanceof Number tn && tn.intValue() == 1)) travelExp.merge(id, amt, BigDecimal::add); else heads.computeIfAbsent(id, k -> new LinkedHashMap<>()).merge(((Number) r.get("expense_id")).longValue(), amt, BigDecimal::add);
                bills.merge(id, ((Number) r.get("bills")).intValue(), Integer::sum);
            }
            for (Map<String, Object> r : jdbc.queryForList("SELECT v.dcr_id, GROUP_CONCAT(DISTINCT e.emp_name ORDER BY e.emp_name SEPARATOR ', ') AS w FROM dcr_sm_visits v JOIN emp_detail e ON e.emp_id = v.with_emp_id WHERE v.dcr_id IN (" + in + ") GROUP BY v.dcr_id", ids)) withMap.put(((Number) r.get("dcr_id")).longValue(), (String) r.get("w"));
        }
        Map<String, String> tp = new HashMap<>();
        try {
            for (Map<String, Object> r : jdbc.queryForList("SELECT a.tour_date AS d, GROUP_CONCAT(DISTINCT ar.area_name ORDER BY ar.area_name SEPARATOR ', ') AS n FROM tour_emp_area a JOIN tour_emp_parent p ON p.tour_main = a.tour_main JOIN area_master ar ON ar.id = a.area_id WHERE p.emp_id = ? AND a.tour_date BETWEEN ? AND ? GROUP BY a.tour_date", empId, from, to)) tp.put(String.valueOf(r.get("d")).substring(0, 10), (String) r.get("n"));
        } catch (org.springframework.dao.DataAccessException e) { /* no tour plan tables */ }

        List<Map<String, Object>> days = new ArrayList<>();
        Area prevStay = null; boolean offSince = false;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String ds = d.toString(); Map<String, Object> p = dcr.get(ds);
            Map<String, Object> row = new LinkedHashMap<>(); row.put("date", ds);
            String wt = p == null ? null : (String) p.get("work_type"); int status = p == null ? -1 : ((Number) p.get("status")).intValue();
            boolean working = p != null && status >= 1 && wt != null && fieldWork.contains(wt);
            row.put("dcrId", p == null ? null : p.get("dcr_id")); row.put("dayType", wt == null ? "-" : wt); row.put("tpArea", tp.get(ds));
            if (!working) {
                if (p == null || wt == null || List.of("HOLIDAY", "WEEKLY_OFF", "LEAVE").contains(wt) || status < 1) offSince = true;
                row.put("route", p == null ? null : ("HOLIDAY".equals(wt) ? "Holiday" : "WEEKLY_OFF".equals(wt) ? "Weekly off" : "LEAVE".equals(wt) ? "Leave" : null)); row.put("visits", 0);
                row.put("km", BigDecimal.ZERO); row.put("fare", BigDecimal.ZERO); row.put("da", BigDecimal.ZERO); row.put("travelExp", BigDecimal.ZERO); row.put("heads", new LinkedHashMap<Long, BigDecimal>()); row.put("bills", 0); row.put("workWith", null); row.put("stayArea", null);
                days.add(row); continue;
            }
            long dcrId = ((Number) p.get("dcr_id")).longValue();
            Area start = home;
            if (prevStay != null && (!offSince || fromStayAfterOff)) start = prevStay;   // the night stay carries to the next working day; after an off day only when the setting says so
            offSince = false;
            Area stay = p.get("stay_area_id") == null ? null : areas.get(((Number) p.get("stay_area_id")).longValue());
            List<Area> seq = new ArrayList<>(); Area pos = start;
            for (Long id : visits.getOrDefault(dcrId, List.of())) { Area a = areas.get(id); if (a != null && (pos == null || a.id() != pos.id())) { seq.add(a); pos = a; } }
            Area end = stay != null ? stay : home;
            List<String> legs = new ArrayList<>(); BigDecimal km = BigDecimal.ZERO; Area cur = start; int topRank = 0; String lastType = null;
            for (Area a : seq) { legs.add(nm(cur, home) + "-" + a.name()); km = km.add(legKm(cur, a, home)); cur = a; String tc = typeCode(a, types); topRank = Math.max(topRank, rankOf(tc, types)); lastType = tc; }
            if (end != null && (cur == null || cur.id() != end.id())) { legs.add(nm(cur, home) + "-" + end.name()); km = km.add(legKm(cur, end, home)); }
            BigDecimal meterKm = p.get("meter_start") != null && p.get("meter_end") != null ? ((BigDecimal) p.get("meter_end")).subtract((BigDecimal) p.get("meter_start")) : null;
            if (meterKm != null && meterKm.signum() > 0) km = meterKm.setScale(1, RoundingMode.HALF_UP);   // the meter of the day wins over the calculated km
            // the type of the day: the highest type of the areas visited (TYPE_RULE MAX) or the type of the last area visited (LAST); a night stay makes it the stay type (STAY_FORCES_TYPE)
            String type = "LAST".equals(sett.get("TYPE_RULE")) && lastType != null ? lastType : (types.isEmpty() ? "HQ" : types.get(topRank).code());
            if (stay != null && "Y".equals(sett.get("STAY_FORCES_TYPE"))) type = types.stream().filter(t -> t.stay()).map(t -> t.code()).findFirst().orElse(type);
            BigDecimal[] er = emp.get(type);
            String daSql = "SELECT amount FROM expense_da_rate WHERE desig_key = COALESCE(?, 0) AND area_type = ?";
            BigDecimal da = er != null && er[0] != null ? er[0] : Optional.ofNullable(rate(daSql, desig, type)).orElse(Optional.ofNullable(rate(daSql, null, type)).orElse(BigDecimal.ZERO));   // the employee's DA, else the designation's, else the all-designation rate
            String mode = p.get("travel_mode") == null ? null : p.get("travel_mode").toString();
            String fareSql = "SELECT rate_per_km FROM expense_fare_rate WHERE desig_key = COALESCE(?, 0) AND travel_mode = ?";
            BigDecimal per = er != null && er[1] != null ? er[1] : (mode == null ? null : Optional.ofNullable(rate(fareSql, desig, mode)).orElse(rate(fareSql, null, mode)));
            BigDecimal fare = per == null ? BigDecimal.ZERO : km.multiply(per).setScale(2, RoundingMode.HALF_UP);
            row.put("route", String.join(",", legs)); row.put("visits", visitCount.getOrDefault(dcrId, 0)); row.put("stayArea", stay == null ? null : stay.name());
            row.put("km", km); row.put("fare", fare); row.put("da", da); row.put("travelExp", fare.add(travelExp.getOrDefault(dcrId, BigDecimal.ZERO)));
            row.put("heads", heads.getOrDefault(dcrId, new LinkedHashMap<>())); row.put("bills", bills.getOrDefault(dcrId, 0)); row.put("workWith", withMap.get(dcrId));
            days.add(row);
            prevStay = stay;   // null = he went back to the starting point
        }
        return days;
    }
    private static String nm(Area a, Area home) { return a == null ? (home == null ? "Start" : home.name()) : a.name(); }

    private BigDecimal total(BigDecimal da, BigDecimal travel, Map<?, BigDecimal> heads) { BigDecimal t = da.add(travel); for (BigDecimal h : heads.values()) t = t.add(h); return t.setScale(2, RoundingMode.HALF_UP); }

    @SuppressWarnings("unchecked")
    private String headsJson(Object heads) throws Exception { Map<String, Object> m = new LinkedHashMap<>(); ((Map<Object, BigDecimal>) heads).forEach((k, v) -> m.put(String.valueOf(k), v)); return json.writeValueAsString(m); }

    private Map<Long, BigDecimal> parseHeads(String j) {
        Map<Long, BigDecimal> m = new LinkedHashMap<>();
        if (j == null || j.isBlank()) return m;
        try { Map<String, Object> raw = json.readValue(j, new TypeReference<Map<String, Object>>() { }); raw.forEach((k, v) -> m.put(Long.valueOf(k), new BigDecimal(v.toString()))); } catch (Exception e) { /* ignore */ }
        return m;
    }

    /** Create the statement of the month, or - while it is still a draft / rejected - refresh it from the DCR (rows that someone changed keep their amounts). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> generate(String empId, int month, int year, boolean refresh) throws Exception {
        if (!empId.equals(TeamScope.me()) && !TeamScope.isAdmin()) throw new ForbiddenException("A user or manager builds his own statement; only an admin can build it for any employee");
        if (month < 1 || month > 12 || year < 2000 || year > 2100) throw new IllegalArgumentException("Choose a valid month and year");
        if (year > LocalDate.now().getYear() || (year == LocalDate.now().getYear() && month > LocalDate.now().getMonthValue())) throw new IllegalArgumentException("A future month has no expense yet");
        List<Map<String, Object>> st = jdbc.queryForList("SELECT id, status FROM expense_statement WHERE emp_id = ? AND exp_month = ? AND exp_year = ?", empId, month, year);
        long sid;
        if (st.isEmpty()) {
            jdbc.update("INSERT INTO expense_statement (emp_id, exp_month, exp_year) VALUES (?,?,?) ON DUPLICATE KEY UPDATE updated_at = NOW()", empId, month, year);
            sid = jdbc.queryForObject("SELECT id FROM expense_statement WHERE emp_id = ? AND exp_month = ? AND exp_year = ?", Long.class, empId, month, year);
        } else {
            sid = ((Number) st.get(0).get("id")).longValue();
            if (!refresh) return Map.of("id", sid, "created", false);
            String status = (String) st.get(0).get("status");
            if (!List.of("DRAFT", "REJECTED").contains(status)) throw new IllegalStateException("The statement is already " + status.toLowerCase().replace('_', ' ') + " - it cannot be refreshed");
        }
        Map<String, Map<String, Object>> have = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT id, exp_date, edited_level FROM expense_statement_day WHERE statement_id = ?", sid)) have.put(String.valueOf(r.get("exp_date")).substring(0, 10), r);
        for (Map<String, Object> d : calculate(empId, month, year)) {
            String ds = (String) d.get("date"); BigDecimal km = (BigDecimal) d.get("km"), fare = (BigDecimal) d.get("fare"), da = (BigDecimal) d.get("da"), tr = (BigDecimal) d.get("travelExp");
            @SuppressWarnings("unchecked") Map<Long, BigDecimal> heads = (Map<Long, BigDecimal>) d.get("heads");
            Map<String, Object> orig = new LinkedHashMap<>(); orig.put("km", km); orig.put("fare", fare); orig.put("da", da); orig.put("travelExp", tr); orig.put("heads", json.readValue(headsJson(heads), Map.class));
            Map<String, Object> ex = have.get(ds);
            if (ex != null && ex.get("edited_level") != null) {   // changed by someone: only the information columns and the calculated values are refreshed
                jdbc.update("UPDATE expense_statement_day SET dcr_id = ?, day_type = ?, tp_area = ?, route = ?, visits = ?, stay_area = ?, work_with = ?, orig_json = ? WHERE id = ?", d.get("dcrId"), d.get("dayType"), d.get("tpArea"), d.get("route"), d.get("visits"), d.get("stayArea"), d.get("workWith"), json.writeValueAsString(orig), ex.get("id"));
            } else {
                jdbc.update("INSERT INTO expense_statement_day (statement_id, exp_date, dcr_id, day_type, tp_area, route, visits, stay_area, km, fare, da, travel_exp, heads_json, bills, work_with, total, orig_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE dcr_id = VALUES(dcr_id), day_type = VALUES(day_type), tp_area = VALUES(tp_area), route = VALUES(route), visits = VALUES(visits), stay_area = VALUES(stay_area), km = VALUES(km), fare = VALUES(fare), da = VALUES(da), travel_exp = VALUES(travel_exp), heads_json = VALUES(heads_json), bills = VALUES(bills), work_with = VALUES(work_with), total = VALUES(total), orig_json = VALUES(orig_json)",
                        sid, ds, d.get("dcrId"), d.get("dayType"), d.get("tpArea"), d.get("route"), d.get("visits"), d.get("stayArea"), km, fare, da, tr, headsJson(heads), d.get("bills"), d.get("workWith"), total(da, tr, heads), json.writeValueAsString(orig));
            }
        }
        recount(sid);
        return Map.of("id", sid, "created", st.isEmpty());
    }

    private void recount(long sid) { jdbc.update("UPDATE expense_statement SET total_amount = COALESCE((SELECT SUM(total) FROM expense_statement_day WHERE statement_id = ?), 0), updated_at = NOW() WHERE id = ?", sid, sid); }

    // ------------------------------------------------------------------ who may do what
    private String ownerOf(long sid) { return jdbc.queryForList("SELECT emp_id FROM expense_statement WHERE id = ?", String.class, sid).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Statement not found")); }
    private boolean isMgrOf(String owner) { String me = TeamScope.me(); return !owner.equals(me) && me.equals(scope.immediateManager(owner)); }
    private void canView(String owner) { if (!owner.equals(TeamScope.me())) scope.require(TeamScope.Scope.TEAM_SELF, owner, "This employee is not in your team"); }

    /** The level the user acts at on a statement in this status (U user / M manager / A admin) and what he may do. */
    private Map<String, Object> perms(String owner, String status) {
        String me = TeamScope.me(); boolean admin = TeamScope.isAdmin() && !owner.equals(me), mgr = isMgrOf(owner), self = owner.equals(me);
        Map<String, Object> p = new LinkedHashMap<>();
        boolean draft = List.of("DRAFT", "REJECTED").contains(status);
        boolean editU = self && draft, editM = mgr && !admin && "SUBMITTED".equals(status), editA = admin && (draft || List.of("SUBMITTED", "MGR_APPROVED").contains(status));   // an admin can also work on the draft of any employee (build, change, submit for him)
        boolean forOwner = (self || admin) && draft;
        p.put("level", editU ? "U" : editA ? "A" : editM ? "M" : null);
        p.put("canEdit", editU || editM || editA); p.put("canSubmit", forOwner);
        p.put("canApprove", (mgr && !admin && "SUBMITTED".equals(status)) || (admin && List.of("SUBMITTED", "MGR_APPROVED").contains(status)));
        p.put("canReject", (mgr && !admin && "SUBMITTED".equals(status)) || (admin && List.of("SUBMITTED", "MGR_APPROVED").contains(status)));
        p.put("canRefresh", forOwner); p.put("approveAs", admin ? "A" : "M");
        return p;
    }

    // ------------------------------------------------------------------ read
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> get(long sid) throws Exception {
        Map<String, Object> s = jdbc.queryForList("""
                SELECT s.id, s.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS empName, s.exp_month AS month, s.exp_year AS year, s.status, s.total_amount AS total, s.submitted_at AS submittedAt,
                       mg.emp_name AS mgrBy, s.mgr_at AS mgrAt, ad.emp_name AS adminBy, s.admin_at AS adminAt, rj.emp_name AS rejectedBy, s.rejected_at AS rejectedAt, s.reject_reason AS rejectReason
                FROM expense_statement s JOIN emp_detail e ON e.emp_id = s.emp_id LEFT JOIN emp_detail mg ON mg.emp_id = s.mgr_by LEFT JOIN emp_detail ad ON ad.emp_id = s.admin_by LEFT JOIN emp_detail rj ON rj.emp_id = s.rejected_by WHERE s.id = ?""", sid).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Statement not found"));
        String owner = (String) s.get("empId"); canView(owner);
        List<Map<String, Object>> days = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT d.id, d.exp_date AS date, d.day_type AS dayType, d.tp_area AS tpArea, d.route, d.visits, d.stay_area AS stayArea, d.km, d.fare, d.da, d.travel_exp AS travelExp, d.heads_json, d.bills, d.remark, d.work_with AS workWith,
                       d.total, d.edited_level AS editedLevel, eb.emp_name AS editedBy, d.edited_at AS editedAt
                FROM expense_statement_day d LEFT JOIN emp_detail eb ON eb.emp_id = d.edited_by WHERE d.statement_id = ? ORDER BY d.exp_date""", sid)) {
            Map<String, Object> m = new LinkedHashMap<>(r); m.put("heads", parseHeads((String) r.remove("heads_json"))); m.put("date", String.valueOf(r.get("date")).substring(0, 10)); days.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("statement", s); out.put("days", days); out.put("perms", perms(owner, (String) s.get("status")));
        out.put("headCols", jdbc.queryForList("SELECT id, expense_name AS name FROM expense_master WHERE is_travel = 0 AND is_active = 1 ORDER BY sort_order, id"));
        out.put("legend", List.of(Map.of("level", "U", "name", "Changed by the employee"), Map.of("level", "M", "name", "Changed by the manager"), Map.of("level", "A", "name", "Changed by the admin")));
        return out;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> find(String empId, int month, int year) throws Exception {
        List<Long> l = jdbc.query("SELECT id FROM expense_statement WHERE emp_id = ? AND exp_month = ? AND exp_year = ?", (rs, i) -> rs.getLong(1), empId, month, year);
        if (l.isEmpty()) { canView(empId); return Map.of("exists", false, "canBuild", empId.equals(TeamScope.me()) || TeamScope.isAdmin()); }
        Map<String, Object> o = new LinkedHashMap<>(get(l.get(0))); o.put("exists", true); return o;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(Integer month, Integer year, String status, String empId) {
        StringBuilder sql = new StringBuilder("SELECT s.id, s.emp_id AS empId, e.emp_code AS empCode, e.emp_name AS empName, s.exp_month AS month, s.exp_year AS year, s.status, s.total_amount AS total, s.submitted_at AS submittedAt, mg.emp_name AS mgrBy, ad.emp_name AS adminBy FROM expense_statement s JOIN emp_detail e ON e.emp_id = s.emp_id LEFT JOIN emp_detail mg ON mg.emp_id = s.mgr_by LEFT JOIN emp_detail ad ON ad.emp_id = s.admin_by WHERE 1 = 1");
        List<Object> a = new ArrayList<>();
        if (month != null) { sql.append(" AND s.exp_month = ?"); a.add(month); }
        if (year != null) { sql.append(" AND s.exp_year = ?"); a.add(year); }
        if (status != null && !status.isBlank()) { sql.append(" AND s.status = ?"); a.add(status); }
        if (empId != null && !empId.isBlank()) { sql.append(" AND s.emp_id = ?"); a.add(empId); }
        sql.append(" ORDER BY s.exp_year DESC, s.exp_month DESC, e.emp_name");
        List<Map<String, Object>> rows = new ArrayList<>(scope.filter(TeamScope.Scope.TEAM_SELF, jdbc.queryForList(sql.toString(), a.toArray()), r -> (String) r.get("empId")));
        for (Map<String, Object> r : rows) { Map<String, Object> p = perms((String) r.get("empId"), (String) r.get("status")); r.put("canApprove", p.get("canApprove")); }
        return rows;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> history(long sid, Long dayId) {
        canView(ownerOf(sid));
        return jdbc.queryForList("SELECT l.id, l.exp_date AS date, l.field_name AS field, l.old_value AS oldValue, l.new_value AS newValue, e.emp_name AS changedBy, l.level, l.reason, l.changed_at AS changedAt FROM expense_change_log l LEFT JOIN emp_detail e ON e.emp_id = l.changed_by WHERE l.statement_id = ?" + (dayId == null ? "" : " AND l.day_id = " + dayId.longValue()) + " ORDER BY l.id DESC", sid);
    }

    // ------------------------------------------------------------------ change a day
    private static String fmt(Object o) { if (o == null) return null; if (o instanceof BigDecimal b) return b.stripTrailingZeros().toPlainString(); return o.toString(); }

    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> changeDay(long dayId, Map<String, Object> b) throws Exception {
        Map<String, Object> d = jdbc.queryForList("SELECT d.*, s.emp_id, s.status FROM expense_statement_day d JOIN expense_statement s ON s.id = d.statement_id WHERE d.id = ?", dayId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Row not found"));
        String owner = (String) d.get("emp_id"); long sid = ((Number) d.get("statement_id")).longValue();
        Map<String, Object> pm = perms(owner, (String) d.get("status"));
        if (!Boolean.TRUE.equals(pm.get("canEdit"))) throw new ForbiddenException("You cannot change this statement now");
        String level = (String) pm.get("level"), reason = b.get("reason") == null || b.get("reason").toString().isBlank() ? null : b.get("reason").toString().trim();
        if (!"U".equals(level) && reason == null) throw new IllegalArgumentException("Give the reason of the change");
        String me = TeamScope.me(); LocalDate dt = ((Date) d.get("exp_date")).toLocalDate();
        Map<String, Object> upd = new LinkedHashMap<>(); int changed = 0;
        String[][] num = {{"km", "km"}, {"fare", "fare"}, {"da", "da"}, {"travelExp", "travel_exp"}};
        for (String[] f : num) {
            if (!b.containsKey(f[0])) continue;
            BigDecimal nv = money(b.get(f[0]), f[0]), ov = (BigDecimal) d.get(f[1]);
            if (nv.compareTo(ov) != 0) { log(sid, dayId, dt, f[0], fmt(ov), fmt(nv), me, level, reason); upd.put(f[1], nv); changed++; }
        }
        if (b.containsKey("bills")) { int nv = Integer.parseInt(String.valueOf(b.get("bills"))), ov = ((Number) d.get("bills")).intValue(); if (nv != ov) { log(sid, dayId, dt, "bills", String.valueOf(ov), String.valueOf(nv), me, level, reason); upd.put("bills", nv); changed++; } }
        if (b.containsKey("remark")) { String nv = b.get("remark") == null || b.get("remark").toString().isBlank() ? null : b.get("remark").toString().trim(), ov = (String) d.get("remark"); if (!Objects.equals(nv, ov)) { log(sid, dayId, dt, "remark", ov, nv, me, level, reason); upd.put("remark", nv); changed++; } }
        BigDecimal da = (BigDecimal) upd.getOrDefault("da", d.get("da")), tr = (BigDecimal) upd.getOrDefault("travel_exp", d.get("travel_exp"));
        Map<Long, BigDecimal> heads = parseHeads((String) d.get("heads_json"));
        if (b.get("heads") instanceof Map<?, ?> hm) {
            Map<Long, String> names = new HashMap<>(); jdbc.queryForList("SELECT id, expense_name FROM expense_master").forEach(r -> names.put(((Number) r.get("id")).longValue(), (String) r.get("expense_name")));
            for (Map.Entry<?, ?> e : hm.entrySet()) {
                long id = Long.parseLong(e.getKey().toString()); BigDecimal nv = e.getValue() == null || e.getValue().toString().isBlank() ? BigDecimal.ZERO : money(e.getValue(), names.getOrDefault(id, "Amount")), ov = heads.getOrDefault(id, BigDecimal.ZERO);
                if (nv.compareTo(ov) != 0) { log(sid, dayId, dt, "head:" + names.getOrDefault(id, String.valueOf(id)), fmt(ov), fmt(nv), me, level, reason); heads.put(id, nv); changed++; }
            }
            upd.put("heads_json", headsJson(heads));
        }
        if (changed == 0) return Map.of("changed", 0);
        upd.put("total", total(da, tr, heads)); upd.put("edited_level", level); upd.put("edited_by", me);
        StringBuilder sql = new StringBuilder("UPDATE expense_statement_day SET edited_at = NOW()"); List<Object> a = new ArrayList<>();
        for (Map.Entry<String, Object> e : upd.entrySet()) { sql.append(", ").append(e.getKey()).append(" = ?"); a.add(e.getValue()); }
        sql.append(" WHERE id = ?"); a.add(dayId);
        jdbc.update(sql.toString(), a.toArray()); recount(sid);
        return Map.of("changed", changed, "total", upd.get("total"));
    }

    private void log(long sid, long dayId, LocalDate dt, String field, String oldV, String newV, String by, String level, String reason) {
        jdbc.update("INSERT INTO expense_change_log (statement_id, day_id, exp_date, field_name, old_value, new_value, changed_by, level, reason) VALUES (?,?,?,?,?,?,?,?,?)", sid, dayId, dt, field, oldV, newV, by, level, reason);
    }

    // ------------------------------------------------------------------ workflow
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> submit(long sid) {
        String owner = ownerOf(sid); String status = jdbc.queryForObject("SELECT status FROM expense_statement WHERE id = ?", String.class, sid);
        if (!owner.equals(TeamScope.me()) && !TeamScope.isAdmin()) throw new ForbiddenException("Only the employee (or an admin for him) submits the statement");
        if (!List.of("DRAFT", "REJECTED").contains(status)) throw new IllegalStateException("The statement is already " + status.toLowerCase().replace('_', ' '));
        if (jdbc.queryForObject("SELECT COALESCE(SUM(total), 0) FROM expense_statement_day WHERE statement_id = ?", BigDecimal.class, sid).signum() <= 0) throw new IllegalStateException("There is no expense to submit");
        jdbc.update("UPDATE expense_statement SET status = 'SUBMITTED', submitted_at = NOW(), reject_reason = NULL, updated_at = NOW() WHERE id = ?", sid);
        return Map.of("status", "SUBMITTED");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> approve(long sid) {
        String owner = ownerOf(sid); String status = jdbc.queryForObject("SELECT status FROM expense_statement WHERE id = ?", String.class, sid); Map<String, Object> p = perms(owner, status);
        if (!Boolean.TRUE.equals(p.get("canApprove"))) throw new ForbiddenException("You cannot approve this statement now");
        String me = TeamScope.me();
        if ("A".equals(p.get("approveAs"))) { jdbc.update("UPDATE expense_statement SET status = 'APPROVED', admin_by = ?, admin_at = NOW(), updated_at = NOW() WHERE id = ?", me, sid); return Map.of("status", "APPROVED"); }
        jdbc.update("UPDATE expense_statement SET status = 'MGR_APPROVED', mgr_by = ?, mgr_at = NOW(), updated_at = NOW() WHERE id = ?", me, sid); return Map.of("status", "MGR_APPROVED");
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> reject(long sid, String reason) {
        String owner = ownerOf(sid); String status = jdbc.queryForObject("SELECT status FROM expense_statement WHERE id = ?", String.class, sid);
        if (!Boolean.TRUE.equals(perms(owner, status).get("canReject"))) throw new ForbiddenException("You cannot reject this statement now");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Give the reason of the rejection");
        jdbc.update("UPDATE expense_statement SET status = 'REJECTED', rejected_by = ?, rejected_at = NOW(), reject_reason = ?, mgr_by = NULL, mgr_at = NULL, updated_at = NOW() WHERE id = ?", TeamScope.me(), reason.trim(), sid);
        return Map.of("status", "REJECTED");
    }
}
