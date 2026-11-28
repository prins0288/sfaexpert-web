package in.opt.sfa.tenant.dcr.view.service;

import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** Everything one employee did on each day of a period: calls by client type (listed / unlisted), refused, POB, samples / gifts, joint working, areas, expense. Feeds the personal calendar. */
@Service
public class CalendarDetailService {
    private final JdbcTemplate jdbc;
    private final TeamScope scope;

    public CalendarDetailService(JdbcTemplate jdbc, TeamScope scope) { this.jdbc = jdbc; this.scope = scope; }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> month(String empId, LocalDate from, LocalDate to) {
        if (!empId.equals(TeamScope.me())) scope.require(TeamScope.Scope.TEAM_SELF, empId, "This employee is not in your team");
        Map<String, Map<String, Object>> days = new LinkedHashMap<>();
        Map<String, Long> dcrDate = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT dcr_id, DATE_FORMAT(dcr_date, '%Y-%m-%d') AS d, work_type, status, pob_total, expense_total FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date BETWEEN ? AND ?", empId, from, to)) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("dcrId", r.get("dcr_id")); day.put("pob", r.get("pob_total")); day.put("expense", r.get("expense_total"));
            day.put("listed", new LinkedHashMap<String, Object>()); day.put("unlisted", new LinkedHashMap<String, Object>()); day.put("items", new LinkedHashMap<String, Object>());
            day.put("areas", new ArrayList<String>()); day.put("joint", new ArrayList<String>()); day.put("refused", 0);
            days.put((String) r.get("d"), day); dcrDate.put((String) r.get("d"), ((Number) r.get("dcr_id")).longValue());
        }
        // calls by client type: listed
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT DATE_FORMAT(p.dcr_date, '%Y-%m-%d') AS d, COALESCE(ct.plural_label, ct.type_name, 'Client') AS t, COUNT(*) AS n, SUM(s.is_refused) AS refused, SUM(s.pob_value) AS pob
                FROM dcr_client_summary s JOIN dcr_parent_summary p ON p.dcr_id = s.dcr_id LEFT JOIN client_master c ON c.id = s.client_id LEFT JOIN client_type ct ON ct.oid = c.client_type_id
                WHERE p.emp_id = ? AND p.dcr_date BETWEEN ? AND ? GROUP BY d, t""", empId, from, to)) put(days, r, "listed");
        // unlisted
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT DATE_FORMAT(p.dcr_date, '%Y-%m-%d') AS d, COALESCE(ct.type_name, 'Client') AS t, COUNT(*) AS n, SUM(u.is_refused) AS refused, SUM(u.pob_value) AS pob
                FROM dcr_unlisted_client_summary u JOIN dcr_parent_summary p ON p.dcr_id = u.dcr_id LEFT JOIN client_type ct ON ct.oid = u.client_type_id
                WHERE p.emp_id = ? AND p.dcr_date BETWEEN ? AND ? GROUP BY d, t""", empId, from, to)) put(days, r, "unlisted");
        // samples / gifts / products given, and what was ordered
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT DATE_FORMAT(p.dcr_date, '%Y-%m-%d') AS d, x.item_type AS t, SUM(x.qty) AS qty, SUM(x.pob_qty) AS pobQty, SUM(x.amount) AS amount
                FROM dcr_client_sample x JOIN dcr_parent_summary p ON p.dcr_id = x.dcr_id WHERE p.emp_id = ? AND p.dcr_date BETWEEN ? AND ? GROUP BY d, t""", empId, from, to)) {
            Map<String, Object> day = days.get((String) r.get("d")); if (day == null) continue;
            Map<String, Object> m = new LinkedHashMap<>(); m.put("qty", r.get("qty")); m.put("orderQty", r.get("pobQty")); m.put("amount", r.get("amount"));
            ((Map<String, Object>) day.get("items")).put(String.valueOf(r.get("t")), m);
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT DATE_FORMAT(a.dcr_date, '%Y-%m-%d') AS d, am.area_name AS n FROM dcr_area_summary a JOIN area_master am ON am.id = a.area_id
                WHERE a.emp_id = ? AND a.dcr_date BETWEEN ? AND ? AND a.area_role = 'VISITED' GROUP BY d, n ORDER BY n""", empId, from, to)) {
            Map<String, Object> day = days.get((String) r.get("d")); if (day != null) ((List<String>) day.get("areas")).add((String) r.get("n"));
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT DATE_FORMAT(p.dcr_date, '%Y-%m-%d') AS d, e.emp_name AS n FROM dcr_sm_visits v JOIN dcr_parent_summary p ON p.dcr_id = v.dcr_id JOIN emp_detail e ON e.emp_id = v.with_emp_id
                WHERE p.emp_id = ? AND p.dcr_date BETWEEN ? AND ? GROUP BY d, n ORDER BY n""", empId, from, to)) {
            Map<String, Object> day = days.get((String) r.get("d")); if (day != null) ((List<String>) day.get("joint")).add((String) r.get("n"));
        }
        Map<String, Object> out = new LinkedHashMap<>(); out.put("days", days);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Map<String, Object>> days, Map<String, Object> r, String key) {
        Map<String, Object> day = days.get((String) r.get("d")); if (day == null) return;
        ((Map<String, Object>) day.get(key)).put(String.valueOf(r.get("t")), r.get("n"));
        day.put("refused", ((Number) day.get("refused")).intValue() + (r.get("refused") == null ? 0 : ((Number) r.get("refused")).intValue()));
    }
}
