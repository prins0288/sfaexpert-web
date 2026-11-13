package in.opt.sfa.tenant.master.area.service;

import in.opt.sfa.tenant.dcr.submit.service.DcrRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/** The starting point of an employee: an area named like his HQ with is_home = 1 (one per employee), created with the employee when AUTO_HOME_AREA is on. */
@Service
public class HomeAreaService {
    private final JdbcTemplate jdbc;
    private final DcrRuleService rules;

    public HomeAreaService(JdbcTemplate jdbc, DcrRuleService rules) { this.jdbc = jdbc; this.rules = rules; }

    /** Called when an employee is created. Returns the oid of the new area, or null when nothing was created (setting off, no HQ, already has a home, migration 119 not run). */
    public Long createFor(String empId, Long hqId, Long stateId, String by) {
        try {
            if (hqId == null || !rules.masterBool("AUTO_HOME_AREA", true)) return null;
            if (jdbc.queryForObject("SELECT COUNT(*) FROM area_master WHERE emp_id = ? AND is_home = 1", Integer.class, empId) > 0) return null;
            List<String> hq = jdbc.query("SELECT hq_name FROM hq_master WHERE oid = ?", (rs, i) -> rs.getString(1), hqId);
            if (hq.isEmpty() || hq.get(0) == null || hq.get(0).isBlank()) return null;
            String code = ("HOME-" + empId);
            code = code.length() > 40 ? code.substring(0, 40) : code;
            if (jdbc.queryForObject("SELECT COUNT(*) FROM area_master WHERE area_code = ?", Integer.class, code) > 0) return null;
            jdbc.update("INSERT INTO area_master (area_code, area_name, state_id, hq_id, emp_id, area_type, status, approval_status, created_by, is_home) VALUES (?, ?, ?, ?, ?, 'HQ', 1, 'APPROVED', ?, 1)",
                    code, hq.get(0).trim(), stateId, hqId, empId, by);
            return jdbc.queryForObject("SELECT id FROM area_master WHERE area_code = ?", Long.class, code);
        } catch (org.springframework.dao.DataAccessException e) {
            org.slf4j.LoggerFactory.getLogger(HomeAreaService.class).warn("home area not created (run migration 119_area_home.sql): {}", e.getMessage());
            return null;
        }
    }

    /** Make this area the only home of its employee. */
    public void makeHome(Long oid, String empId) {
        jdbc.update("UPDATE area_master SET is_home = 0 WHERE emp_id = ? AND id <> ?", empId, oid == null ? -1L : oid);
    }
}
