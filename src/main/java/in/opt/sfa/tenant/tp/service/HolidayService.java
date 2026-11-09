package in.opt.sfa.tenant.tp.service;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.Authz;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Holidays of the company. A holiday with no assignment is for everyone; otherwise it applies to the
 * assigned STATEs, HQs and employees. Weekly off days (setting weeklyOffDays, e.g. "SUN" or "SAT,SUN")
 * are shown on the calendar as well; with nothing set only the holidays of the master show.
 */
@Service
public class HolidayService {

    public static final String WEEKLY_OFF_KEY = "weeklyOffDays";
    private static final List<String> DAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    private static final List<String> TARGETS = List.of("STATE", "HQ", "EMP");

    private final JdbcTemplate jdbc;
    private final CompanySettingStore settings;

    public HolidayService(JdbcTemplate jdbc, CompanySettingStore settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    // ---- what an employee sees on the calendar ------------------------------------------

    public Set<DayOfWeek> weeklyOff() {
        Set<DayOfWeek> out = EnumSet.noneOf(DayOfWeek.class);
        String v = settings.value(WEEKLY_OFF_KEY, "");
        for (String d : v == null ? new String[0] : v.split(",")) {
            int i = DAYS.indexOf(d.trim().toUpperCase());
            if (i >= 0) out.add(DayOfWeek.of(i + 1));
        }
        return out;
    }

    /** date -> [{name, remarks, kind WEEKLY_OFF / HOLIDAY}] for one employee over a date range. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, List<Map<String, Object>>> forEmployee(String empId, LocalDate from, LocalDate to) {
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        Set<DayOfWeek> off = weeklyOff();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (off.contains(d.getDayOfWeek())) {
                out.computeIfAbsent(d.toString(), k -> new ArrayList<>())
                        .add(Map.of("name", "Weekly off", "remarks", d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), "kind", "WEEKLY_OFF"));
            }
        }
        for (Map<String, Object> h : jdbc.queryForList("""
                SELECT h.holiday_date AS d, h.holiday_name AS name, h.remarks FROM holiday_master h
                JOIN emp_detail e ON e.emp_id = ?
                WHERE h.is_active = 1 AND h.holiday_date BETWEEN ? AND ?
                  AND (NOT EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id)
                       OR EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id AND (
                            (a.target_type = 'EMP' AND a.target_value = e.emp_id COLLATE utf8mb4_0900_ai_ci)
                         OR (a.target_type = 'HQ' AND a.target_value = CAST(e.hq_id AS CHAR))
                         OR (a.target_type = 'STATE' AND a.target_value = CAST(e.state_id AS CHAR)))))
                ORDER BY h.holiday_date, h.holiday_name""", empId, from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", h.get("name")); m.put("remarks", h.get("remarks")); m.put("kind", "HOLIDAY");
            out.computeIfAbsent(h.get("d").toString().substring(0, 10), k -> new ArrayList<>()).add(m);
        }
        return out;
    }

    // ---- master (admin) ---------------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> config() {
        Authz.requireRole("ADMIN");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("weeklyOff", weeklyOff().stream().map(d -> DAYS.get(d.getValue() - 1)).sorted(Comparator.comparingInt(DAYS::indexOf)).toList());
        m.put("states", jdbc.queryForList("SELECT oid AS id, state_name AS name FROM state_master ORDER BY state_name"));
        m.put("hqs", jdbc.queryForList("SELECT oid AS id, hq_name AS name FROM hq_master ORDER BY hq_name"));
        m.put("employees", jdbc.queryForList("SELECT emp_id AS id, CONCAT(emp_name, ' (', emp_code, ')') AS name FROM emp_detail WHERE is_active = 1 ORDER BY emp_name"));
        return m;
    }

    /** Holidays with a short "who it is for" text. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(Integer year) {
        Authz.requireRole("ADMIN");
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT h.id, h.holiday_date AS date, h.holiday_name AS name, h.remarks, h.is_active AS active,
                       (SELECT COUNT(*) FROM holiday_assignment a WHERE a.holiday_id = h.id) AS assigned
                FROM holiday_master h WHERE (? IS NULL OR YEAR(h.holiday_date) = ?) ORDER BY h.holiday_date, h.holiday_name""", year, year);
        return rows;
    }

    /** Create / update one holiday; {@code assign} is {states: [], hqs: [], employees: []} (all empty = everyone). */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> f) {
        Authz.requireRole("ADMIN");
        String name = str(f.get("name"));
        if (name == null) throw new IllegalStateException("Holiday name is required");
        if (name.length() > 100) throw new IllegalStateException("Holiday name can be at most 100 characters");
        LocalDate date;
        try { date = LocalDate.parse(String.valueOf(f.get("date"))); } catch (Exception e) { throw new IllegalStateException("Choose the holiday date"); }
        String remarks = str(f.get("remarks"));
        if (remarks != null && remarks.length() > 250) throw new IllegalStateException("Remarks can be at most 250 characters");
        int active = Boolean.FALSE.equals(f.get("active")) || "false".equals(String.valueOf(f.get("active"))) ? 0 : 1;
        String me = in.opt.sfa.tenant.team.TeamScope.me();
        Long id = f.get("id") == null || str(f.get("id")) == null ? null : Long.valueOf(str(f.get("id")));
        if (id == null) {
            Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM holiday_master WHERE holiday_date = ? AND holiday_name = ?", Integer.class, date, name);
            if (dup != null && dup > 0) throw new IllegalStateException("'" + name + "' is already a holiday on " + date);
            jdbc.update("INSERT INTO holiday_master (holiday_date, holiday_name, remarks, is_active, created_by) VALUES (?,?,?,?,?)", date, name, remarks, active, me);
            id = jdbc.queryForObject("SELECT id FROM holiday_master WHERE holiday_date = ? AND holiday_name = ?", Long.class, date, name);
        } else {
            Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM holiday_master WHERE holiday_date = ? AND holiday_name = ? AND id <> ?", Integer.class, date, name, id);
            if (dup != null && dup > 0) throw new IllegalStateException("'" + name + "' is already a holiday on " + date);
            if (jdbc.update("UPDATE holiday_master SET holiday_date = ?, holiday_name = ?, remarks = ?, is_active = ?, updated_by = ?, updated_at = NOW() WHERE id = ?", date, name, remarks, active, me, id) == 0)
                throw new IllegalStateException("Holiday not found");
        }
        if (f.get("assign") instanceof Map<?, ?> a) {
            Map<String, Object> as = (Map<String, Object>) a;
            jdbc.update("DELETE FROM holiday_assignment WHERE holiday_id = ?", id);
            insertTargets(id, "STATE", (Collection<?>) as.get("states"));
            insertTargets(id, "HQ", (Collection<?>) as.get("hqs"));
            insertTargets(id, "EMP", (Collection<?>) as.get("employees"));
        }
        return Map.of("id", id);
    }

    /** Add more targets to many holidays at once (keeps what is there). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public int assign(List<Long> holidayIds, Map<String, Object> as) {
        Authz.requireRole("ADMIN");
        if (holidayIds == null || holidayIds.isEmpty()) throw new IllegalStateException("Choose the holidays");
        int n = 0;
        for (Long id : holidayIds) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM holiday_master WHERE id = ?", Integer.class, id) == 0) continue;
            n += insertTargets(id, "STATE", (Collection<?>) as.get("states")) + insertTargets(id, "HQ", (Collection<?>) as.get("hqs")) + insertTargets(id, "EMP", (Collection<?>) as.get("employees"));
        }
        return n;
    }

    private int insertTargets(Long holidayId, String type, Collection<?> values) {
        if (values == null) return 0;
        if (!TARGETS.contains(type)) throw new IllegalStateException("Unknown target " + type);
        int n = 0;
        for (Object v : new LinkedHashSet<>(values)) {
            String val = str(v);
            if (val == null) continue;
            n += jdbc.update("INSERT IGNORE INTO holiday_assignment (holiday_id, target_type, target_value) VALUES (?,?,?)", holidayId, type, val);
        }
        return n;
    }

    /**
     * The assignment REPORT: one row per holiday -> state / HQ / employee, filterable, so a wrong assignment can be seen and
     * removed (many at once). A holiday with no assignment is listed once as "Everyone".
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> assignments(LocalDate from, LocalDate to, String type, String target) {
        Authz.requireRole("ADMIN");
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT a.id AS assignmentId, h.id AS holidayId, h.holiday_date AS date, h.holiday_name AS name, h.remarks, h.is_active AS active,
                       a.target_type AS targetType, a.target_value AS targetValue,
                       CASE a.target_type
                         WHEN 'STATE' THEN (SELECT s.state_name FROM state_master s WHERE s.oid = CAST(a.target_value AS UNSIGNED))
                         WHEN 'HQ' THEN (SELECT q.hq_name FROM hq_master q WHERE q.oid = CAST(a.target_value AS UNSIGNED))
                         WHEN 'EMP' THEN (SELECT CONCAT(e.emp_name, ' (', e.emp_code, ')') FROM emp_detail e WHERE e.emp_id = a.target_value COLLATE utf8mb4_0900_ai_ci)
                         ELSE 'Everyone' END AS targetName
                FROM holiday_master h LEFT JOIN holiday_assignment a ON a.holiday_id = h.id
                WHERE (? IS NULL OR h.holiday_date >= ?) AND (? IS NULL OR h.holiday_date <= ?)
                  AND (? IS NULL OR a.target_type = ?) AND (? IS NULL OR a.target_value = ?)
                ORDER BY h.holiday_date, h.holiday_name, a.target_type, targetName""", from, from, to, to, blankNull(type), blankNull(type), blankNull(target), blankNull(target));
        for (Map<String, Object> r : rows) if (r.get("targetType") == null) { r.put("targetType", "ALL"); r.put("targetName", "Everyone"); }
        return rows;
    }

    /** Remove wrong assignments (many at once). A holiday left with none becomes "for everyone" - so the response says how many. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> deleteAssignments(List<Long> ids) {
        Authz.requireRole("ADMIN");
        if (ids == null || ids.isEmpty()) throw new IllegalStateException("Choose the assignments to remove");
        int n = 0;
        for (Long id : ids) n += jdbc.update("DELETE FROM holiday_assignment WHERE id = ?", id);
        int everyone = jdbc.queryForObject("SELECT COUNT(*) FROM holiday_master h WHERE NOT EXISTS (SELECT 1 FROM holiday_assignment a WHERE a.holiday_id = h.id)", Integer.class);
        return Map.of("removed", n, "holidaysForEveryone", everyone);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int delete(List<Long> ids) {
        Authz.requireRole("ADMIN");
        if (ids == null || ids.isEmpty()) throw new IllegalStateException("Choose the holidays to delete");
        int n = 0;
        for (Long id : ids) n += jdbc.update("DELETE FROM holiday_master WHERE id = ?", id);   // assignments go with them (ON DELETE CASCADE)
        return n;
    }

    @Transactional(transactionManager = "commonTransactionManager")
    public List<String> saveWeeklyOff(List<String> days) {
        Authz.requireRole("ADMIN");
        List<String> ok = new ArrayList<>();
        for (String d : days == null ? List.<String>of() : days) {
            String u = d == null ? "" : d.trim().toUpperCase();
            if (!DAYS.contains(u)) throw new IllegalStateException("Unknown day " + d);
            if (!ok.contains(u)) ok.add(u);
        }
        ok.sort(Comparator.comparingInt(DAYS::indexOf));
        settings.put(WEEKLY_OFF_KEY, String.join(",", ok));
        return ok;
    }

    private static String str(Object o) { return o == null || o.toString().isBlank() ? null : o.toString().trim(); }
    private static String blankNull(String s) { return Strings.isBlank(s) ? null : s.trim(); }
}
