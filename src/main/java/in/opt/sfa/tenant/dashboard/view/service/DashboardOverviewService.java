package in.opt.sfa.tenant.dashboard.view.service;

import in.opt.sfa.tenant.dcr.submit.service.DcrService;
import in.opt.sfa.tenant.dcr.view.service.DcrReportService;
import in.opt.sfa.tenant.team.TeamScope;
import in.opt.sfa.tenant.tp.service.TourPlanService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * The dashboard of whoever signed in: a user sees only himself, a manager his team with himself, an admin everyone - with names.
 * Month-to-date numbers come from the DCR report (so the dashboard and the report always agree).
 */
@Service
public class DashboardOverviewService {

    private final JdbcTemplate jdbc;
    private final DcrReportService report;
    private final DcrService dcr;
    private final TourPlanService tours;
    private final TeamScope scope;

    public DashboardOverviewService(JdbcTemplate jdbc, DcrReportService report, DcrService dcr, TourPlanService tours, TeamScope scope) {
        this.jdbc = jdbc;
        this.report = report;
        this.dcr = dcr;
        this.tours = tours;
        this.scope = scope;
    }

    private static double d(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }

    @SuppressWarnings("unchecked")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now(), first = today.withDayOfMonth(1);
        String me = TeamScope.me();
        String level = TeamScope.isAdmin() ? "ALL" : TeamScope.isManager() ? "TEAM" : "SELF";
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", level);
        Map<String, Object> sum = report.summary(new DcrReportService.Filter(first, today, null, null, null, null, null, null, true));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) sum.get("rows");
        // ---- one line per employee ----
        Map<String, Map<String, Object>> byEmp = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("empId");
            Map<String, Object> e = byEmp.computeIfAbsent(id, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("empName", r.get("empName")); m.put("empCode", r.get("empCode")); m.put("designation", r.get("designation")); m.put("hq", r.get("hq")); m.put("state", r.get("state"));
                m.put("dcrDays", 0); m.put("absentDays", 0); m.put("calls", 0L); m.put("pob", 0.0); m.put("expense", 0.0); m.put("today", "NONE"); m.put("todayText", ""); m.put("mine", id.equals(me));
                return m;
            });
            String kind = (String) r.get("kind");
            boolean isToday = today.toString().equals(r.get("date"));
            switch (kind) {
                case "DCR" -> {
                    e.put("dcrDays", (Integer) e.get("dcrDays") + 1); e.put("calls", (Long) e.get("calls") + ((Number) r.get("calls")).longValue());
                    e.put("pob", (Double) e.get("pob") + d(r.get("pob"))); e.put("expense", (Double) e.get("expense") + d(r.get("expense")));
                    if (isToday) { e.put("today", "DCR"); e.put("todayText", r.get("statusName") + " · " + r.get("sourceName") + " · " + r.get("calls") + " calls"); e.put("dcrId", r.get("dcrId")); }
                }
                case "ABSENT" -> e.put("absentDays", (Integer) e.get("absentDays") + 1);
                default -> { if (isToday) { e.put("today", kind); e.put("todayText", kind.equals("NOT_STARTED") ? "Not started yet" : String.valueOf(r.get("note"))); } }
            }
        }
        List<Map<String, Object>> emps = new ArrayList<>(byEmp.values());
        out.put("employees", emps);
        // ---- tiles ----
        Map<String, Object> t = new LinkedHashMap<>((Map<String, Object>) sum.get("totals"));
        List<String> ids = new ArrayList<>(byEmp.keySet());
        if (me != null && !ids.contains(me) && !"ALL".equals(level)) ids.add(me);
        if ("ALL".equals(level)) {
            t.put("areas", jdbc.queryForObject("SELECT COUNT(*) FROM area_master WHERE status = 'Y' AND approval_status = 'APPROVED'", Long.class));
            t.put("clients", jdbc.queryForObject("SELECT COUNT(*) FROM client_master WHERE is_active = 1 AND approval_status = 'APPROVED'", Long.class));
        } else if (ids.isEmpty()) { t.put("areas", 0); t.put("clients", 0); }
        else {
            String in = String.join(",", Collections.nCopies(ids.size(), "?"));
            t.put("areas", jdbc.queryForObject("SELECT COUNT(*) FROM area_master WHERE status = 'Y' AND approval_status = 'APPROVED' AND emp_id IN (" + in + ")", Long.class, ids.toArray()));
            t.put("clients", jdbc.queryForObject("SELECT COUNT(*) FROM client_master WHERE is_active = 1 AND approval_status = 'APPROVED' AND mapped_emp_code IN (" + in + ")", Long.class, ids.toArray()));
        }
        t.put("products", jdbc.queryForObject("SELECT COUNT(*) FROM item_master WHERE status = 1", Long.class));
        long started = emps.stream().filter(e -> "DCR".equals(e.get("today"))).count();
        t.put("startedToday", started); t.put("people", emps.size());
        out.put("totals", t);
        // ---- what waits for me ----
        Map<String, Object> wait = new LinkedHashMap<>();
        int dcrWait = 0, tpWait = 0;
        if (!"SELF".equals(level)) {
            for (Map<String, Object> r : dcr.list(today.minusDays(30), today, null)) if (Boolean.TRUE.equals(r.get("canApprove"))) dcrWait++;
            for (Map<String, Object> r : tours.list(today.getYear(), today.getMonthValue())) if (Boolean.TRUE.equals(r.get("canApprove"))) tpWait++;
        }
        wait.put("dcr", dcrWait); wait.put("tourPlans", tpWait);
        out.put("waiting", wait);
        Map<String, Object> my = tours.get(today.getYear(), today.getMonthValue(), null);
        Map<String, Object> mt = new LinkedHashMap<>();
        mt.put("status", my.get("status")); mt.put("canSubmit", my.get("canSubmit")); mt.put("canEdit", my.get("canEdit"));
        out.put("myTourPlan", mt);
        out.put("month", first.toString().substring(0, 7));
        return out;
    }
}
