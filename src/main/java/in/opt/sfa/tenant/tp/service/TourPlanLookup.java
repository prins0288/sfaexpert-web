package in.opt.sfa.tenant.tp.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** Read-only view of the APPROVED tour plan for the DCR (rule TP_VALIDATE_FILTER). */
@Service
public class TourPlanLookup {

    private final JdbcTemplate jdbc;

    public TourPlanLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Map<String, Object> parentRow(String empId, int year, int month) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT * FROM tour_emp_parent WHERE emp_id = ? AND tour_year = ? AND tour_month = ?", empId, year, month);
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * The approved plan of {@code empId} for {@code date}: planned areas and the clients the day may visit — per area, the clients picked
     * for it, or ALL the area's clients when none was picked. Null when there is no approved plan for that date.
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> planFor(String empId, LocalDate date) {
        Map<String, Object> p = parentRow(empId, date.getYear(), date.getMonthValue());
        if (p == null || !"APPROVED".equals(p.get("approval_status"))) return null;
        long tm = ((Number) p.get("tour_main")).longValue();
        List<Map<String, Object>> day = jdbc.queryForList("SELECT worktype_code AS wt FROM tour_child WHERE tour_main = ? AND tour_date = ?", tm, date);
        if (day.isEmpty()) return null;
        List<Long> areas = new ArrayList<>();
        jdbc.queryForList("SELECT area_id FROM tour_emp_area WHERE tour_main = ? AND tour_date = ?", tm, date).forEach(r -> areas.add(((Number) r.get("area_id")).longValue()));
        Map<Long, List<Long>> picked = new HashMap<>();
        jdbc.queryForList("SELECT area_id, client_id FROM tour_emp_clients WHERE tour_main = ? AND tour_date = ?", tm, date)
                .forEach(r -> picked.computeIfAbsent(((Number) r.get("area_id")).longValue(), k -> new ArrayList<>()).add(((Number) r.get("client_id")).longValue()));
        Set<Long> allowed = new LinkedHashSet<>();
        for (Long a : areas) {
            if (picked.containsKey(a)) allowed.addAll(picked.get(a));
            else jdbc.queryForList("SELECT id FROM client_master WHERE city_id = ?", a).forEach(r -> allowed.add(((Number) r.get("id")).longValue()));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("worktypeCode", day.get(0).get("wt")); m.put("areaIds", areas); m.put("clientIds", allowed);
        return m;
    }
}
