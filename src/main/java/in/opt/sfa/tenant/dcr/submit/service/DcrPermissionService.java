package in.opt.sfa.tenant.dcr.submit.service;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * DCR date locking and the two per-employee permissions.
 * <ul>
 *   <li>lock period (emp_app_setting.lock_period_days): 0 = only today can be filled, 1 = today and yesterday, 3 = the last 3 days too;
 *       a day whose last fillable day (date + lock_period) has passed and that has no DCR is "Locked".</li>
 *   <li>BACKDATE permission: DCR dates dcr_from..dcr_to may be filled, but only while today is inside allow_from..allow_to.</li>
 *   <li>EDIT permission: an already submitted DCR of dcr_from..dcr_to may be changed, but only while today is inside allow_from..allow_to.</li>
 * </ul>
 * A permission whose allow_to has passed is no longer listed and no longer counts.
 */
@Service
public class DcrPermissionService {
    public static final String BACKDATE = "BACKDATE", EDIT = "EDIT";
    private final JdbcTemplate jdbc;
    private final CompanySettingStore settings;

    public DcrPermissionService(JdbcTemplate jdbc, CompanySettingStore settings) { this.jdbc = jdbc; this.settings = settings; }

    private int fallbackDays() {
        try { return Math.max(0, Integer.parseInt(settings.value(DcrService.BACK_DAYS_KEY, "7").trim())); } catch (NumberFormatException e) { return 7; }
    }

    /** Days back this employee may fill (emp_app_setting.lock_period_days; the company back-days setting when he has no row). */
    public int lockDays(String empId) {
        try {
            List<Integer> l = jdbc.query("SELECT lock_period_days FROM emp_app_setting WHERE emp_id = ?", (rs, i) -> rs.getInt(1), empId);
            if (!l.isEmpty()) return l.get(0);
        } catch (org.springframework.dao.DataAccessException e) { /* column not there */ }
        return fallbackDays();
    }

    public boolean has(String empId, String type, LocalDate date) {
        try {
            return jdbc.queryForObject("SELECT COUNT(*) FROM dcr_date_permission WHERE emp_id = ? AND perm_type = ? AND ? BETWEEN dcr_from AND dcr_to AND CURDATE() BETWEEN allow_from AND allow_to", Integer.class, empId, type, date) > 0;
        } catch (org.springframework.dao.DataAccessException e) { return false; }   // migration 115 not run
    }

    /** Throws when the employee may not fill the DCR of this date. */
    public void checkFill(String empId, LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) throw new IllegalStateException("A DCR cannot be filled for a future date");
        int lock = lockDays(empId);
        if (!date.isBefore(today.minusDays(lock)) || has(empId, BACKDATE, date)) return;
        throw new IllegalStateException(lock == 0 ? "DCR is locked: only today's DCR can be filled. Ask your admin for a back-date permission."
                : "DCR is locked: it can be filled only up to " + lock + " day" + (lock == 1 ? "" : "s") + " back. Ask your admin for a back-date permission.");
    }

    public boolean canEdit(String empId, LocalDate date) { return has(empId, EDIT, date); }

    /** Locked = past its last fillable day, and no back-date permission is open for it. Used by the calendar. */
    public Set<String> lockedHelper(Collection<String> empIds, List<String> dates) {
        Set<String> out = new HashSet<>();   // "empId|date"
        LocalDate today = LocalDate.now();
        for (String e : empIds) {
            int lock = lockDays(e);
            for (String d : dates) {
                LocalDate dt = LocalDate.parse(d);
                if (dt.isBefore(today.minusDays(lock)) && !has(e, BACKDATE, dt)) out.add(e + "|" + d);
            }
        }
        return out;
    }

    // ---- admin ----------------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(String type, String empId, boolean withExpired) {
        Authz.requireRole("ADMIN");
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.perm_type AS type, e.emp_code AS empCode, e.emp_name AS name, d.designation, h.hq_name AS hq, p.dcr_from AS dcrFrom, p.dcr_to AS dcrTo,
                       p.allow_from AS allowFrom, p.allow_to AS allowTo, p.reason, p.created_by AS createdBy, p.created_at AS createdAt,
                       CASE WHEN p.allow_to < CURDATE() THEN 'EXPIRED' WHEN p.allow_from > CURDATE() THEN 'UPCOMING' ELSE 'ACTIVE' END AS state
                FROM dcr_date_permission p JOIN emp_detail e ON e.emp_id = p.emp_id LEFT JOIN designation_master d ON d.id = e.designation LEFT JOIN hq_master h ON h.oid = e.hq_id WHERE 1 = 1""");
        List<Object> a = new ArrayList<>();
        if (type != null && !type.isBlank()) { sql.append(" AND p.perm_type = ?"); a.add(type); }
        if (empId != null && !empId.isBlank()) { sql.append(" AND p.emp_id = ?"); a.add(empId); }
        if (!withExpired) sql.append(" AND p.allow_to >= CURDATE()");
        sql.append(" ORDER BY p.allow_to, e.emp_name, p.id");
        return jdbc.queryForList(sql.toString(), a.toArray());
    }

    /** Give the permission to several employees at once. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public int grant(String type, List<String> empIds, LocalDate dcrFrom, LocalDate dcrTo, LocalDate allowFrom, LocalDate allowTo, String reason) {
        Authz.requireRole("ADMIN");
        if (!BACKDATE.equals(type) && !EDIT.equals(type)) throw new IllegalArgumentException("Choose Back-date submit or DCR edit");
        if (empIds == null || empIds.isEmpty()) throw new IllegalArgumentException("Choose at least one employee");
        if (dcrFrom == null || dcrTo == null || allowFrom == null || allowTo == null) throw new IllegalArgumentException("DCR dates and the permission window are required");
        if (dcrTo.isBefore(dcrFrom)) throw new IllegalArgumentException("DCR 'to' date is before the 'from' date");
        if (allowTo.isBefore(allowFrom)) throw new IllegalArgumentException("Permission 'to' date is before the 'from' date");
        LocalDate today = LocalDate.now();
        if (allowTo.isBefore(today)) throw new IllegalArgumentException("The permission window has already ended");
        if (dcrTo.isAfter(today)) throw new IllegalArgumentException("A DCR date cannot be in the future");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Reason is required");
        String by = TeamScope.me(); int n = 0;
        for (String e : empIds) {
            n += jdbc.update("INSERT INTO dcr_date_permission (emp_id, perm_type, dcr_from, dcr_to, allow_from, allow_to, reason, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", e, type, dcrFrom, dcrTo, allowFrom, allowTo, reason.trim(), by);
        }
        return n;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public int remove(List<Long> ids) {
        Authz.requireRole("ADMIN");
        int n = 0; for (Long id : ids) n += jdbc.update("DELETE FROM dcr_date_permission WHERE id = ?", id);
        return n;
    }
}
