package in.opt.sfa.tenant.dcr.submit.service;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.dcr.submit.dto.DcrDto;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Daily Call Report (dcr_parent_summary + its child tables). One DCR per employee per day,
 * filled and submitted by the employee himself; a user sees only his own, a manager his and
 * his team's, an admin everyone's (TeamScope). The tabs he gets come from app_dcr_worktype.
 * A save REPLACES the day's children in one transaction, so a re-submit can never double an entry.
 */
@Service
public class DcrService {

    public static final int DRAFT = 0, SUBMITTED = 1, APPROVED = 2, REJECTED = 3;
    private static final String[] STATUS_NAMES = {"Draft", "Submitted", "Approved", "Rejected"};
    /** Company setting: how many days back a DCR may still be filled (default 7). */
    public static final String BACK_DAYS_KEY = "dcrBackDays";

    /** Which DCR check-in tab (CLIENT / RETAILER / PARTY) a client type belongs to: client_type.dcr_tab, else read from its code / name (works before migration 94 too). */
    private static final String TAB_GUESS = "CASE WHEN UPPER(ct.type_code) IN ('DR','CLIENT','DOCTOR','CL') OR ct.type_name IN ('Doctor','Client') THEN 'CLIENT' "
            + "WHEN UPPER(ct.type_code) IN ('RT','RETAILER','CHEMIST','RL','CH') OR ct.type_name LIKE 'Retailer%' OR ct.type_name LIKE 'Chemist%' THEN 'RETAILER' "
            + "WHEN UPPER(ct.type_code) IN ('PT','PARTY','STOCKIST','ST') OR ct.type_name LIKE 'Party%' OR ct.type_name LIKE 'Stockist%' THEN 'PARTY' END";
    private static final String TAB_COL = "COALESCE(ct.dcr_tab, " + TAB_GUESS + ")";

    private final JdbcTemplate jdbc;
    private final TeamScope scope;
    private final DcrWorktypeService worktypes;
    private final DcrRuleService rules;
    private final in.opt.sfa.tenant.tp.service.TourPlanLookup tourPlans;
    private final CompanySettingStore settings;

    public DcrService(JdbcTemplate jdbc, TeamScope scope, DcrWorktypeService worktypes, DcrRuleService rules, in.opt.sfa.tenant.tp.service.TourPlanLookup tourPlans, CompanySettingStore settings) {
        this.rules = rules;
        this.tourPlans = tourPlans;
        this.jdbc = jdbc;
        this.scope = scope;
        this.worktypes = worktypes;
        this.settings = settings;
    }

    // =========================================================================================
    // form options
    // =========================================================================================

    /** Everything the form needs for the signed-in user, in one call. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> options() {
        String me = me();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("me", me);
        Map<String, Object> who = jdbc.queryForList("""
                SELECT e.emp_name AS name, d.designation AS designation, h.hq_name AS hq FROM emp_detail e
                LEFT JOIN designation_master d ON d.id = e.designation LEFT JOIN hq_master h ON h.oid = e.hq_id WHERE e.emp_id = ?""", me).stream().findFirst().orElse(Map.of());
        m.put("meName", who.get("name"));
        m.put("meDesignation", who.get("designation"));
        m.put("meHq", who.get("hq"));
        m.put("workTypes", worktypes.dayTypes(me));
        m.put("rules", rules.of(me));
        m.put("isAdmin", TeamScope.isAdmin());
        m.put("backDays", backDays());
        m.put("tabs", worktypes.tabsFor(me));
        m.put("clients", myClients(me));
        m.put("areas", areasFor(me));
        String itemSql = """
                SELECT i.id, i.item_code AS code, i.item_name AS name, t.item_type_code AS type, %s AS panel,
                       COALESCE((SELECT r.pts FROM item_rate r WHERE r.item_id = i.id AND r.status = 1
                                 AND (r.effective_from IS NULL OR r.effective_from <= CURDATE()) AND (r.effective_to IS NULL OR r.effective_to >= CURDATE())
                                 ORDER BY r.effective_from DESC LIMIT 1), 0) AS rate
                FROM item_master i JOIN item_type_master t ON t.id = i.item_type_id
                WHERE i.status = 1 ORDER BY i.item_name""";
        try {
            m.put("items", jdbc.queryForList(itemSql.formatted("t.dcr_panel")));
        } catch (org.springframework.dao.DataAccessException e) {   // migration 91 not run yet: the page still opens, Sample / Input panels stay empty
            org.slf4j.LoggerFactory.getLogger(DcrService.class).warn("item_type_master.dcr_panel is missing - run migration 91_dcr_masters.sql");
            m.put("items", jdbc.queryForList(itemSql.formatted("NULL")));
        }
        m.put("expenseTypes", jdbc.queryForList("SELECT id, expense_name AS name, is_travel AS travel FROM expense_master WHERE is_active = 1 ORDER BY sort_order, expense_name"));
        m.put("treatments", jdbc.queryForList("SELECT id, treatment_type AS type, treatment_name AS name FROM treatment_master WHERE is_active = 1 ORDER BY treatment_type, sort_order, treatment_name"));
        m.put("visitTypes", jdbc.queryForList("SELECT type_name AS name FROM visit_type_master WHERE is_active = 1 ORDER BY type_name").stream().map(r -> r.get("name")).toList());
        m.put("activityTypes", jdbc.queryForList("SELECT type_name AS name FROM activity_type_master WHERE is_active = 1 ORDER BY type_name").stream().map(r -> r.get("name")).toList());
        m.put("travelModes", jdbc.queryForList("SELECT travel_type_name AS name FROM travel_type_master WHERE is_active = 1 ORDER BY travel_type_name").stream().map(r -> r.get("name")).toList());
        String ctSql = "SELECT ct.oid AS id, ct.type_name AS name, COALESCE(ct.singular_label, ct.type_name) AS label, %TAB% AS dcrTab FROM client_type ct WHERE ct.is_active = 1 ORDER BY ct.type_name";
        try {
            m.put("clientTypes", jdbc.queryForList(ctSql.replace("%TAB%", TAB_COL)));
        } catch (org.springframework.dao.DataAccessException e) {   // migration 94 not run yet
            m.put("clientTypes", jdbc.queryForList(ctSql.replace("%TAB%", TAB_GUESS)));
        }
        try {
            m.put("priorities", jdbc.queryForList("SELECT priority_name AS name FROM visit_priority_master WHERE is_active = 1 ORDER BY sort_order, priority_name").stream().map(r -> r.get("name")).toList());
        } catch (org.springframework.dao.DataAccessException e) {
            org.slf4j.LoggerFactory.getLogger(DcrService.class).warn("visit_priority_master is missing - run migration 91_dcr_masters.sql");
            m.put("priorities", List.of());
        }
        m.put("specialities", jdbc.queryForList("SELECT oid AS id, speciality_name AS name FROM speciality_master WHERE status = 1 ORDER BY speciality_name"));
        m.put("categories", jdbc.queryForList("SELECT oid AS id, category_name AS name FROM category_master WHERE status = 1 ORDER BY category_name"));
        m.put("joint", jointCandidates(me));
        return m;
    }

    /**
     * What the approved tour plan of a date lets the DCR offer. {enabled} follows the rule TP_VALIDATE_FILTER; when on and a plan exists:
     * the planned area ids and the client ids (per area: the clients picked for it, or all its clients when none was picked).
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> tpFilter(LocalDate date) {
        String me = me();
        Map<String, Object> out = new LinkedHashMap<>();
        boolean on = DcrRuleService.bool(rules.of(me), "TP_VALIDATE_FILTER", false);
        out.put("enabled", on);
        if (!on) return out;
        Map<String, Object> plan = tourPlans.planFor(me, date == null ? LocalDate.now() : date);
        out.put("planned", plan != null);
        if (plan != null) out.putAll(plan);
        return out;
    }

    /** The approved areas of an employee (his own; when he has none yet, every approved area). */
    public List<Map<String, Object>> areasFor(String empId) {
        List<Map<String, Object>> areas = jdbc.queryForList("""
                SELECT oid AS id, area_name AS name, area_type AS areaType FROM area_master
                WHERE status = 'Y' AND approval_status = 'APPROVED' AND emp_id = ? ORDER BY area_name""", empId);
        if (areas.isEmpty()) {
            areas = jdbc.queryForList("SELECT oid AS id, area_name AS name, area_type AS areaType FROM area_master WHERE status = 'Y' AND approval_status = 'APPROVED' ORDER BY area_name");
        }
        return areas;
    }

    /** My approved, active clients with their type / tab, category, speciality, area and visit history. */
    public List<Map<String, Object>> myClients(String me) {
        String clientSql = """
                SELECT c.id, c.client_code AS code, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS name,
                       c.client_type_id AS clientTypeId, ct.type_name AS clientTypeName, %TAB% AS dcrTab, c.city_id AS areaId, ar.area_name AS areaName,
                       c.speciality_id AS specialityId, c.category_id AS categoryId, sp.speciality_name AS speciality, cat.category_name AS category, c.mobile_number_1 AS mobile,
                       COALESCE(v.visits, 0) AS visits, v.last_visit AS lastVisit
                FROM client_master c
                LEFT JOIN (SELECT s.client_id, COUNT(*) AS visits, MAX(p.dcr_date) AS last_visit FROM dcr_client_summary s JOIN dcr_parent_summary p ON p.dcr_id = s.dcr_id
                           WHERE s.emp_id = ? GROUP BY s.client_id) v ON v.client_id = c.id
                LEFT JOIN client_type ct ON ct.oid = c.client_type_id
                LEFT JOIN area_master ar ON ar.oid = c.city_id
                LEFT JOIN speciality_master sp ON sp.oid = c.speciality_id
                LEFT JOIN category_master cat ON cat.oid = c.category_id
                WHERE c.is_active = 1 AND c.approval_status = 'APPROVED' AND (c.mapped_emp_code = ? OR (c.mapped_emp_code IS NULL AND c.created_by COLLATE utf8mb4_0900_ai_ci = ?))
                ORDER BY c.client_name""";
        try {
            return jdbc.queryForList(clientSql.replace("%TAB%", TAB_COL), me, me, me);
        } catch (org.springframework.dao.DataAccessException e) {   // migration 94 (client_type.dcr_tab) not run yet
            org.slf4j.LoggerFactory.getLogger(DcrService.class).warn("client_type.dcr_tab is missing - run migration 94_client_type_dcr_tab.sql");
            return jdbc.queryForList(clientSql.replace("%TAB%", TAB_GUESS), me, me, me);
        }
    }

    /** Who the user can work together with: his seniors and his team (never admin-level users). */
    public List<Map<String, Object>> jointCandidates(String me) {
        List<String> seniors = scope.seniors(me);
        Set<String> juniors = new LinkedHashSet<>(scope.team(me));
        Set<String> admins = scope.adminLevelIds();
        Map<String, String> relation = new LinkedHashMap<>();   // emp_id -> MANAGER / SENIOR / JUNIOR (never admin-level users, never me)
        for (int i = 0; i < seniors.size(); i++) relation.putIfAbsent(seniors.get(i), i == 0 ? "MANAGER" : "SENIOR");
        for (String j : juniors) relation.putIfAbsent(j, "JUNIOR");
        relation.keySet().removeAll(admins);
        relation.remove(me);
        if (relation.isEmpty()) return List.of();
        String in = String.join(",", Collections.nCopies(relation.size(), "?"));
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT e.emp_id AS id, e.emp_name AS empName, e.emp_code AS empCode, d.designation AS designation
                FROM emp_detail e LEFT JOIN designation_master d ON d.id = e.designation
                WHERE e.is_active = 1 AND e.emp_id IN (""" + in + ")", relation.keySet().toArray())) byId.put((String) r.get("id"), r);
        List<Map<String, Object>> out = new ArrayList<>();
        for (String rel : List.of("MANAGER", "SENIOR", "JUNIOR")) {
            List<Map<String, Object>> grp = new ArrayList<>();
            relation.forEach((id, r) -> { Map<String, Object> e = byId.get(id); if (rel.equals(r) && e != null) {
                Map<String, Object> m = new LinkedHashMap<>(e);
                m.put("name", e.get("empName") + " (" + e.get("empCode") + ")");   // name with the employee CODE, never the internal id
                m.put("relation", rel);
                grp.add(m); } });
            if (!"MANAGER".equals(rel) && !"SENIOR".equals(rel)) grp.sort(Comparator.comparing(x -> String.valueOf(x.get("empName"))));
            out.addAll(grp);
        }
        return out;
    }

    // =========================================================================================
    // read
    // =========================================================================================

    /** DCRs in a date range: own, or (manager) his team's, or (admin) everyone's; one employee with {@code empId}. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(LocalDate from, LocalDate to, String empId) {
        LocalDate f = from == null ? LocalDate.now().minusDays(30) : from, t = to == null ? LocalDate.now() : to;
        String who = Strings.isBlank(empId) ? null : empId.trim();
        if (who != null) scope.require(TeamScope.Scope.TEAM_SELF, who, "This employee is not in your team");
        Set<String> ids = scope.ids(TeamScope.Scope.TEAM_SELF);
        List<Object> args = new ArrayList<>(List.of(f, t));
        StringBuilder sql = new StringBuilder("""
                SELECT p.dcr_id AS dcrId, p.emp_id AS empId, e.emp_name AS empName, e.emp_code AS empCode, d.designation AS designation,
                       p.dcr_date AS dcrDate, p.work_type AS workType, p.status, p.listed_calls AS listedCalls, p.unlisted_calls AS unlistedCalls,
                       p.pob_total AS pobTotal, p.expense_total AS expenseTotal, p.manager_id AS managerId
                FROM dcr_parent_summary p JOIN emp_detail e ON e.emp_id = p.emp_id LEFT JOIN designation_master d ON d.id = p.designation_id
                WHERE p.dcr_date BETWEEN ? AND ?""");
        if (who != null) { sql.append(" AND p.emp_id = ?"); args.add(who); }
        else if (ids != null) {
            if (ids.isEmpty()) return List.of();
            sql.append(" AND p.emp_id IN (").append(String.join(",", Collections.nCopies(ids.size(), "?"))).append(")");
            args.addAll(ids);
        }
        sql.append(" ORDER BY p.dcr_date DESC, e.emp_name LIMIT 500");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        String me = me();
        for (Map<String, Object> r : rows) {
            int st = ((Number) r.get("status")).intValue();
            r.put("statusName", STATUS_NAMES[st]);
            r.put("mine", me.equals(r.get("empId")));
            r.put("canApprove", st == SUBMITTED && canApprove((String) r.get("empId")));
        }
        return rows;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public DcrDto get(Long dcrId) {
        Map<String, Object> p = parent(dcrId);
        scope.require(TeamScope.Scope.TEAM_SELF, (String) p.get("emp_id"), "This DCR is not in your team");
        return load(p);
    }

    /** My DCR of a date (null when none yet). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public DcrDto getMine(LocalDate date) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date = ?", me(), date);
        return rows.isEmpty() ? null : load(rows.get(0));
    }

    private Map<String, Object> parent(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM dcr_parent_summary WHERE dcr_id = ?", id);
        if (rows.isEmpty()) throw new IllegalStateException("DCR not found: " + id);
        return rows.get(0);
    }

    private DcrDto load(Map<String, Object> p) {
        long id = ((Number) p.get("dcr_id")).longValue();
        String owner = (String) p.get("emp_id");
        DcrDto d = new DcrDto();
        d.setDcrId(id);
        d.setEmpId(owner);
        d.setDcrDate(String.valueOf(p.get("dcr_date")));
        d.setWorkType((String) p.get("work_type"));
        d.setStartAreaId(lng(p.get("start_area_id")));
        d.setWorkingAreaId(lng(p.get("working_area_id")));
        d.setTimeIn(hhmm(p.get("time_in")));
        d.setTimeOut(hhmm(p.get("time_out")));
        d.setTravelMode((String) p.get("travel_mode"));
        d.setMeterStart((BigDecimal) p.get("meter_start"));
        d.setMeterEnd((BigDecimal) p.get("meter_end"));
        d.setRemarks((String) p.get("remarks"));
        d.setDayEndAreaId(lng(p.get("day_end_area_id")));
        d.setDayEndAt(p.get("day_end_at") == null ? null : p.get("day_end_at").toString().replace(' ', 'T').substring(0, 16));
        d.setDayEndRemarks((String) p.get("day_end_remarks"));
        d.setStayAreaId(lng(p.get("stay_area_id")));
        d.setStayHotel((String) p.get("stay_hotel"));
        int st = ((Number) p.get("status")).intValue();
        d.setStatus(st);
        d.setStatusName(STATUS_NAMES[st]);
        d.setListedCalls(((Number) p.get("listed_calls")).intValue());
        d.setUnlistedCalls(((Number) p.get("unlisted_calls")).intValue());
        d.setPobTotal((BigDecimal) p.get("pob_total"));
        d.setExpenseTotal((BigDecimal) p.get("expense_total"));
        d.setSubmittedAt(p.get("submitted_at") == null ? null : p.get("submitted_at").toString());
        d.setApprovalRemarks((String) p.get("approval_remarks"));
        d.setApprovedAt(p.get("approved_at") == null ? null : p.get("approved_at").toString());
        d.setApprovedByName(p.get("approved_by") == null ? null : name((String) p.get("approved_by")));
        d.setEmpName(name(owner));
        Map<String, Object> des = jdbc.queryForList("SELECT designation FROM designation_master WHERE id = ?", p.get("designation_id")).stream().findFirst().orElse(Map.of());
        d.setDesignation((String) des.get("designation"));
        d.setCanEdit(owner.equals(me()) && (st == DRAFT || st == REJECTED));   // after Final Submit nothing can change (a rejected DCR opens again)
        d.setCanApprove(st == SUBMITTED && canApprove(owner));

        Map<Long, List<DcrDto.Item>> items = new HashMap<>();   // key: kind*1e12 + ref
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT s.client_kind k, s.client_ref_id ref, s.item_id, s.item_type, s.qty, s.pob_qty, s.rate, s.is_detailed, s.remarks, i.item_name
                FROM dcr_client_sample s JOIN item_master i ON i.id = s.item_id WHERE s.dcr_id = ? ORDER BY s.id""", id)) {
            DcrDto.Item it = new DcrDto.Item();
            it.setItemId(lng(r.get("item_id"))); it.setItemType((String) r.get("item_type")); it.setQty(((Number) r.get("qty")).intValue());
            it.setPobQty(((Number) r.get("pob_qty")).intValue()); it.setRate((BigDecimal) r.get("rate"));
            it.setDetailed(flag(r.get("is_detailed")) == 1); it.setRemarks((String) r.get("remarks")); it.setItemName((String) r.get("item_name"));
            items.computeIfAbsent(key(r.get("k"), r.get("ref")), x -> new ArrayList<>()).add(it);
        }
        Map<Long, List<DcrDto.Leaflet>> leaflets = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT l.client_kind k, l.client_ref_id ref, l.item_id, l.qty, l.value, i.item_name
                FROM dcr_leaflets_summary l JOIN item_master i ON i.id = l.item_id WHERE l.dcr_id = ? ORDER BY l.id""", id)) {
            DcrDto.Leaflet lf = new DcrDto.Leaflet();
            lf.setItemId(lng(r.get("item_id"))); lf.setQty(((Number) r.get("qty")).intValue()); lf.setValue((BigDecimal) r.get("value")); lf.setItemName((String) r.get("item_name"));
            leaflets.computeIfAbsent(key(r.get("k"), r.get("ref")), x -> new ArrayList<>()).add(lf);
        }
        Map<Long, List<Integer>> treatments = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT client_kind k, client_ref_id ref, treatment_id FROM dcr_client_treatment WHERE dcr_id = ?", id)) {
            treatments.computeIfAbsent(key(r.get("k"), r.get("ref")), x -> new ArrayList<>()).add(((Number) r.get("treatment_id")).intValue());
        }
        Map<Long, List<String>> joint = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT client_kind k, client_ref_id ref, with_emp_id FROM dcr_sm_visits WHERE dcr_id = ?", id)) {
            joint.computeIfAbsent(key(r.get("k"), r.get("ref")), x -> new ArrayList<>()).add((String) r.get("with_emp_id"));
        }
        d.setJointWith(joint.getOrDefault(key(0, 0), new ArrayList<>()));

        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT s.*, c.client_name, c.client_code FROM dcr_client_summary s JOIN client_master c ON c.id = s.client_id WHERE s.dcr_id = ? ORDER BY s.id""", id)) {
            DcrDto.Visit v = new DcrDto.Visit();
            v.setClientId(lng(r.get("client_id"))); v.setClientName((String) r.get("client_name")); v.setClientCode((String) r.get("client_code"));
            fillCall(v, r, items.get(key(1, r.get("client_id"))), leaflets.get(key(1, r.get("client_id"))), treatments.get(key(1, r.get("client_id"))), joint.get(key(1, r.get("client_id"))));
            d.getVisits().add(v);
        }
        for (Map<String, Object> r : jdbc.queryForList("SELECT * FROM dcr_unlisted_client_summary WHERE dcr_id = ? ORDER BY id", id)) {
            DcrDto.Unlisted u = new DcrDto.Unlisted();
            long uid = ((Number) r.get("id")).longValue();
            u.setKey("u" + uid); u.setClientName((String) r.get("client_name")); u.setClientTypeId(lng(r.get("client_type_id")));
            u.setSpecialityId(lng(r.get("speciality_id"))); u.setCategoryId(lng(r.get("category_id"))); u.setQualification((String) r.get("qualification"));
            u.setMobile((String) r.get("mobile")); u.setContactPerson((String) r.get("contact_person")); u.setAddress((String) r.get("address"));
            u.setApprovalStatus(((Number) r.get("approval_status")).intValue());
            fillCall(u, r, items.get(key(2, uid)), leaflets.get(key(2, uid)), treatments.get(key(2, uid)), joint.get(key(2, uid)));
            d.getUnlisted().add(u);
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT x.*, m.expense_name FROM dcr_expense x JOIN expense_master m ON m.id = x.expense_id WHERE x.dcr_id = ? ORDER BY x.id""", id)) {
            DcrDto.Expense e = new DcrDto.Expense();
            e.setExpenseId(((Number) r.get("expense_id")).intValue()); e.setExpenseName((String) r.get("expense_name"));
            e.setAmount((BigDecimal) r.get("amount")); e.setDescription((String) r.get("description"));
            int kind = ((Number) r.get("client_kind")).intValue();
            e.setClientKind(kind);
            if (kind == 1) e.setClientId(lng(r.get("client_ref_id")));
            if (kind == 2) e.setUnlistedKey("u" + ((Number) r.get("client_ref_id")).longValue());
            e.setFromAreaId(lng(r.get("from_area_id"))); e.setToAreaId(lng(r.get("to_area_id")));
            e.setStartKm((BigDecimal) r.get("start_km")); e.setEndKm((BigDecimal) r.get("end_km"));
            e.setStartImagePath((String) r.get("start_image_path")); e.setEndImagePath((String) r.get("end_image_path"));
            e.setFileName((String) r.get("file_name")); e.setFileType((String) r.get("file_type")); e.setFilePath((String) r.get("file_path"));
            d.getExpenses().add(e);
        }
        return d;
    }

    private static void fillCall(DcrDto.Call c, Map<String, Object> r, List<DcrDto.Item> items, List<DcrDto.Leaflet> leaflets, List<Integer> treatments, List<String> joint) {
        c.setWorktypeCode((String) r.get("worktype_code")); c.setVisitType((String) r.get("visit_type")); c.setActivityType((String) r.get("activity_type"));
        c.setAreaId(lng(r.get("area_id"))); c.setInTime(hhmm(r.get("in_time"))); c.setOutTime(hhmm(r.get("out_time")));
        c.setCheckinAt(r.get("checkin_at") == null ? null : r.get("checkin_at").toString().replace(' ', 'T').substring(0, 16));
        c.setCheckinLat((BigDecimal) r.get("checkin_lat")); c.setCheckinLng((BigDecimal) r.get("checkin_lng"));
        c.setPobValue((BigDecimal) r.get("pob_value")); c.setRemarks((String) r.get("remarks"));
        c.setRefused(flag(r.get("is_refused")) == 1);
        c.setNextVisit(r.get("next_visit") == null ? null : r.get("next_visit").toString());
        c.setNextVisitPriority((String) r.get("next_visit_priority")); c.setNextVisitRemarks((String) r.get("next_visit_remarks"));
        c.setSignaturePath((String) r.get("signature_path"));
        if (items != null) c.setItems(items);
        if (leaflets != null) c.setLeaflets(leaflets);
        if (treatments != null) c.setTreatments(treatments);
        if (joint != null) c.setJointWith(joint);
    }

    // =========================================================================================
    // start the day
    // =========================================================================================

    /**
     * Start MY day: creates the DCR (a draft) with time in = now (server time). Until the day is started
     * there is nothing else to fill. Today only — a missed earlier day (within the back-days limit) is
     * started with the time in typed (timeIn).
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public DcrDto start(DcrDto f) {
        String me = me();
        LocalDate date = date(f.getDcrDate(), "DCR date");
        LocalDate today = LocalDate.now();
        if (date == null) date = today;
        if (date.isAfter(today)) throw new IllegalStateException("A day cannot be started in the future");
        if (date.isBefore(today.minusDays(backDays()))) throw new IllegalStateException("A DCR can be filled only up to " + backDays() + " days back");
        String workType = f.getWorkType() == null ? "" : f.getWorkType().toUpperCase();
        if (!worktypes.isDayType(me, workType)) throw new IllegalStateException("Please choose a valid work type");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date = ?", Integer.class, me, date) > 0) {
            throw new IllegalStateException("Your day is already started");
        }
        LocalTime timeIn = date.equals(today) ? LocalTime.now().withSecond(0).withNano(0) : time(f.getTimeIn(), "Time in");
        if (timeIn == null) throw new IllegalStateException("Enter the time your day started");
        boolean working = worktypes.isFieldWork(me, workType);
        Map<String, String> rl = rules.of(me);
        if (date.equals(today)) {
            LocalTime earliest = DcrRuleService.time(rl, "DAY_START_STOP_BEFORE_TIME"), latest = DcrRuleService.time(rl, "DAY_START_TIME");
            if (earliest != null && timeIn.isBefore(earliest)) throw new IllegalStateException("Your day can be started only after " + earliest);
            if (latest != null && timeIn.isAfter(latest)) throw new IllegalStateException("Day start time is over (allowed till " + latest + ")");
        }
        if (working && DcrRuleService.bool(rl, "DAY_START_TRAVEL_MODE_VISIBLITY", true) && DcrRuleService.bool(rl, "DAY_START_TRAVEL_MODE_MANDATORY", false) && Strings.isBlank(f.getTravelMode())) {
            throw new IllegalStateException("Select the travel mode to start your day");
        }
        if (working && f.getStartAreaId() == null) throw new IllegalStateException("Choose the area you start the day from");
        if (working && DcrRuleService.bool(rl, "TP_VALIDATE_FILTER", false)) {
            Map<String, Object> plan = tourPlans.planFor(me, date);
            if (plan == null) throw new IllegalStateException("There is no approved tour plan for " + date + " - your day cannot start");
            if (!((Collection<?>) plan.get("areaIds")).contains(f.getStartAreaId())) throw new IllegalStateException("Start the day from an area of your tour plan");
        }
        if (f.getStartAreaId() != null) checkAreaIds(List.of(f.getStartAreaId()));
        if (f.getMeterStart() != null && f.getMeterStart().signum() < 0) throw new IllegalStateException("Meter start cannot be negative");
        Map<String, Object> emp = jdbc.queryForList("SELECT designation, manager_id FROM emp_detail WHERE emp_id = ?", me).get(0);
        KeyHolder kh = new GeneratedKeyHolder();
        final LocalDate d = date; final LocalTime ti = timeIn;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dcr_parent_summary (emp_id, dcr_date, work_type, designation_id, manager_id, start_area_id, time_in, travel_mode, meter_start, remarks, status, submitted_through)
                    VALUES (?,?,?,?,?,?,?,?,?,?,0,2)""", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, me); ps.setObject(2, d); ps.setString(3, workType); ps.setObject(4, emp.get("designation")); ps.setObject(5, emp.get("manager_id"));
            ps.setObject(6, working ? f.getStartAreaId() : null); ps.setObject(7, ti); ps.setObject(8, blank(f.getTravelMode())); ps.setObject(9, DcrRuleService.bool(rl, "METER_READING_DAYSTART_VISIBLITY", true) ? f.getMeterStart() : null); ps.setObject(10, blank(f.getRemarks()));
            return ps;
        }, kh);
        long id = kh.getKey().longValue();
        if (working && f.getStartAreaId() != null) {
            try {   // area type (HQ / EX / OUT ...) of the start area, kept with the day (column from migration 95)
                jdbc.update("UPDATE dcr_parent_summary SET start_area_type = (SELECT area_type FROM area_master WHERE oid = ?) WHERE dcr_id = ?", f.getStartAreaId(), id);
            } catch (org.springframework.dao.DataAccessException e) { /* migration 95 not run yet */ }
        }
        if (working && f.getStartAreaId() != null) {
            jdbc.update("INSERT INTO dcr_area_summary (dcr_id, emp_id, dcr_date, area_id, area_role) VALUES (?,?,?,?, 'START')", id, me, date, f.getStartAreaId());
        }
        return load(parent(id));
    }

    // =========================================================================================
    // save / submit
    // =========================================================================================

    /** Create or replace MY DCR of the date; {@code submit} sends it for approval, else it stays a draft. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public DcrDto save(DcrDto f) {
        String me = me();
        LocalDate date = date(f.getDcrDate(), "DCR date");
        if (date == null) throw new IllegalStateException("DCR date is required");
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) throw new IllegalStateException("A DCR cannot be filled for a future date");
        if (date.isBefore(today.minusDays(backDays()))) throw new IllegalStateException("A DCR can be filled only up to " + backDays() + " days back");
        String workType = f.getWorkType() == null ? "" : f.getWorkType().toUpperCase();
        if (!worktypes.isDayType(me, workType)) throw new IllegalStateException("Please choose a valid work type");

        List<Map<String, Object>> existing = jdbc.queryForList("SELECT dcr_id, status, time_in, time_out FROM dcr_parent_summary WHERE emp_id = ? AND dcr_date = ?", me, date);
        if (existing.isEmpty()) throw new IllegalStateException("Start your day first");
        int curStatus = ((Number) existing.get(0).get("status")).intValue();
        if (curStatus == APPROVED) throw new IllegalStateException("This DCR is already approved and cannot be changed");
        if (curStatus == SUBMITTED) throw new IllegalStateException("This DCR is already submitted - nothing can be added, changed or deleted now");
        boolean working = worktypes.isFieldWork(me, workType);
        Map<String, Map<String, Object>> tabs = new HashMap<>();
        worktypes.tabsFor(me).forEach(t -> tabs.put((String) t.get("code"), t));

        // ---- checks that need no write ----
        Map<Long, Map<String, Object>> myClients = new HashMap<>();
        if (working) {
            String myClientSql = """
                    SELECT c.id, c.client_type_id, c.city_id, %TAB% AS dcr_tab FROM client_master c LEFT JOIN client_type ct ON ct.oid = c.client_type_id
                    WHERE c.is_active = 1 AND c.approval_status = 'APPROVED'
                      AND (c.mapped_emp_code = ? OR (c.mapped_emp_code IS NULL AND c.created_by COLLATE utf8mb4_0900_ai_ci = ?))""";
            List<Map<String, Object>> mine;
            try {
                mine = jdbc.queryForList(myClientSql.replace("%TAB%", TAB_COL), me, me);
            } catch (org.springframework.dao.DataAccessException e) {   // client_type.dcr_tab not there yet (migration 94)
                mine = jdbc.queryForList(myClientSql.replace("%TAB%", TAB_GUESS), me, me);
            }
            for (Map<String, Object> c : mine) myClients.put(((Number) c.get("id")).longValue(), c);
        }
        Set<Long> tpClients = null;
        if (working && !f.getVisits().isEmpty() && DcrRuleService.bool(rules.of(me), "TP_VALIDATE_FILTER", false)) {
            Map<String, Object> plan = tourPlans.planFor(me, date);
            if (plan == null) throw new IllegalStateException("There is no approved tour plan for " + date);
            tpClients = new HashSet<>(); for (Object c : (Collection<?>) plan.get("clientIds")) tpClients.add(((Number) c).longValue());
        }
        Set<String> seen = new HashSet<>();
        for (DcrDto.Visit v : f.getVisits()) {
            if (tpClients != null && v.getClientId() != null && !tpClients.contains(v.getClientId())) throw new IllegalStateException("That client is not in your tour plan for " + date);
            if (!working) throw new IllegalStateException("A " + workType.toLowerCase() + " day has no client calls");
            if (v.getClientId() == null || !myClients.containsKey(v.getClientId())) throw new IllegalStateException("Client " + v.getClientId() + " is not one of your approved, active clients");
            Map<String, Object> tab = tabs.get(v.getWorktypeCode() == null ? "CLIENT" : v.getWorktypeCode());
            if (tab == null || !"CHECKIN".equals(tab.get("form"))) throw new IllegalStateException("The tab '" + v.getWorktypeCode() + "' is not enabled for you");
            Object tType = tab.get("clientTypeId");
            Long cType = lng(myClients.get(v.getClientId()).get("client_type_id"));
            // Client / Retailer / Party tab = clients whose client type is mapped to that tab (client_type.dcr_tab), or the tab's own client type
            boolean fits = v.getWorktypeCode() != null && v.getWorktypeCode().equals(myClients.get(v.getClientId()).get("dcr_tab"))
                    || (tType != null && Objects.equals(((Number) tType).longValue(), cType));
            if (!fits) throw new IllegalStateException("That client does not belong in the " + tab.get("name") + " tab");
            if (!seen.add(v.getClientId() + "|" + nz(v.getVisitType()))) throw new IllegalStateException("The same client is added twice with the same visit type");
            checkCall(v, tab);
        }
        if (!f.getUnlisted().isEmpty() && !working) throw new IllegalStateException("A " + workType.toLowerCase() + " day has no client calls");
        Set<String> keys = new HashSet<>(), unlistedSeen = new HashSet<>();
        for (DcrDto.Unlisted u : f.getUnlisted()) {
            Map<String, Object> tab = tabs.get(u.getWorktypeCode() == null ? "UNLISTED" : u.getWorktypeCode());
            if (tab == null || !"UNLISTED".equals(tab.get("form"))) throw new IllegalStateException("The Unlisted tab is not enabled for you");
            if (Strings.isBlank(u.getClientName())) throw new IllegalStateException("Name of the unlisted client is required");
            // rule TRUE = mandatory, FALSE = optional
            if (DcrRuleService.bool(rules.of(me), "UNLISTED_VISIT_MOBILE_VISIBLITY", true) && DcrRuleService.bool(rules.of(me), "UNLISTED_VISIT_MOBILE", false) && Strings.isBlank(u.getMobile())) throw new IllegalStateException("Mobile of '" + u.getClientName() + "' is required");
            if (DcrRuleService.bool(rules.of(me), "UNLISTED_VISIT_SPECIALITY_VISIBLITY", true) && DcrRuleService.bool(rules.of(me), "UNLISTED_VISIT_SPECIALITY", false) && u.getSpecialityId() == null) throw new IllegalStateException("Speciality of '" + u.getClientName() + "' is required");
            if (DcrRuleService.bool(rules.of(me), "UNLISTED_VISIT_CATEGORY_VISIBLITY", true) && DcrRuleService.bool(rules.of(me), "UNLISTED_VISIT_CATEGORY", false) && u.getCategoryId() == null) throw new IllegalStateException("Category of '" + u.getClientName() + "' is required");
            if (!unlistedSeen.add(u.getClientName().trim().toLowerCase() + "|" + nz(u.getMobile()))) throw new IllegalStateException("The unlisted client '" + u.getClientName() + "' is added twice");
            if (u.getKey() != null && !keys.add(u.getKey())) throw new IllegalStateException("Duplicate unlisted key " + u.getKey());
            checkCall(u, tab);
        }
        if (!f.getExpenses().isEmpty() && !tabs.containsKey("EXPENSE")) throw new IllegalStateException("The Expense tab is not enabled for you");
        if (!f.getJointWith().isEmpty() && !tabs.containsKey("JOINT")) throw new IllegalStateException("The Joint Working tab is not enabled for you");
        if (hasDayEnd(f) && !tabs.containsKey("DAYEND")) throw new IllegalStateException("The Day End tab is not enabled for you");
        Set<String> allowedJoint = new HashSet<>();
        jointCandidates(me).forEach(j -> allowedJoint.add((String) j.get("id")));
        Set<String> wanted = new LinkedHashSet<>(f.getJointWith());
        f.getVisits().forEach(v -> wanted.addAll(v.getJointWith()));
        f.getUnlisted().forEach(u -> wanted.addAll(u.getJointWith()));
        for (String w : wanted) if (!allowedJoint.contains(w)) throw new IllegalStateException("You cannot record joint working with " + empLabel(w));
        Map<String, String> rl = rules.of(me);
        int maxUnlisted = DcrRuleService.integer(rl, "UNLISTED_CLIENT_MAXIMUM_VALIDATION", 0);
        if (maxUnlisted > 0 && f.getUnlisted().size() > maxUnlisted) throw new IllegalStateException("You can add at most " + maxUnlisted + " unlisted check-ins in a day");
        if (f.isSubmit() && DcrRuleService.bool(rl, "DCR_Remarks", false) && Strings.isBlank(f.getRemarks())) throw new IllegalStateException("Remarks are required to submit the day");
        if (f.isSubmit() && working && f.getVisits().isEmpty() && f.getUnlisted().isEmpty()) {
            throw new IllegalStateException("Add at least one client call before submitting (or save it as a draft)");
        }
        // every call is a check-in; one open at a time; all checked out before the day is submitted
        List<String> open = new ArrayList<>();
        for (DcrDto.Call c : concat(f.getVisits(), f.getUnlisted())) {
            if (Strings.isBlank(c.getInTime())) throw new IllegalStateException("A call is not checked in — check in the client first");
            if (Strings.isBlank(c.getOutTime()) && !c.isRefused()) open.add(callName(c));
            checkNotFuture(date, c);
            checkWindow(rl, c);
        }
        if (open.size() > 1) throw new IllegalStateException("Check out of " + open.get(0) + " before checking in another client");
        if (f.isSubmit() && !open.isEmpty() && DcrRuleService.bool(rl, "CHECKOUT", true)) throw new IllegalStateException("Check out of " + open.get(0) + " before submitting the day");
        // time in is fixed when the day starts; time out is stamped on submit (a past day: as typed)
        LocalTime tin = existing.get(0).get("time_in") == null ? null : LocalTime.parse(existing.get(0).get("time_in").toString());
        LocalTime tout = existing.get(0).get("time_out") == null ? null : LocalTime.parse(existing.get(0).get("time_out").toString());
        if (f.isSubmit() && tout == null) tout = date.equals(today) ? LocalTime.now().withSecond(0).withNano(0) : time(f.getTimeOut(), "Time out");
        if (tin != null && tout != null && tout.isBefore(tin) && date.equals(today)) tout = tin;
        if (f.isSubmit() && tin != null && tout != null && tout.isBefore(tin) && DcrRuleService.bool(rl, "DAYEND_BEFORE_DAYSTART_VALIDATION", false)) {
            throw new IllegalStateException("Day end time cannot be before the day start time (" + tin + ")");
        }
        if (f.getMeterStart() != null && f.getMeterEnd() != null && f.getMeterEnd().compareTo(f.getMeterStart()) < 0) throw new IllegalStateException("Meter end cannot be less than meter start");

        // ---- the parent row (snapshot of designation / immediate manager that day) ----
        Map<String, Object> emp = jdbc.queryForList("SELECT designation, manager_id FROM emp_detail WHERE emp_id = ?", me).get(0);
        int status = f.isSubmit() ? SUBMITTED : DRAFT;
        long dcrId;
        if (existing.isEmpty()) {
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("INSERT INTO dcr_parent_summary (emp_id, dcr_date, work_type) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, me); ps.setObject(2, date); ps.setString(3, workType);
                return ps;
            }, kh);
            dcrId = kh.getKey().longValue();
        } else {
            dcrId = ((Number) existing.get(0).get("dcr_id")).longValue();
            for (String t : List.of("dcr_client_sample", "dcr_leaflets_summary", "dcr_client_treatment", "dcr_sm_visits", "dcr_expense",
                    "dcr_area_summary", "dcr_client_summary", "dcr_unlisted_client_summary")) {
                jdbc.update("DELETE FROM " + t + " WHERE dcr_id = ?", dcrId);
            }
        }
        checkAreas(f);
        // day end time is never typed: it is captured when the day is submitted (a past day: its time out)
        LocalDateTime dayEndAt = !f.isSubmit() ? null : date.equals(today) || tout == null ? LocalDateTime.now().withSecond(0).withNano(0) : date.atTime(tout);
        jdbc.update("""
                UPDATE dcr_parent_summary SET work_type = ?, designation_id = ?, manager_id = ?, start_area_id = ?, working_area_id = ?,
                       time_in = ?, time_out = ?, travel_mode = ?, meter_start = ?, meter_end = ?, remarks = ?,
                       day_end_area_id = ?, day_end_at = ?, day_end_remarks = ?, stay_area_id = ?, stay_hotel = ?,
                       status = ?, submitted_at = ?, submitted_through = 2, approved_by = NULL, approved_at = NULL, approval_remarks = NULL
                WHERE dcr_id = ?""",
                workType, emp.get("designation"), emp.get("manager_id"), f.getStartAreaId(), f.getWorkingAreaId(), tin, tout, blank(f.getTravelMode()),
                f.getMeterStart(), f.getMeterEnd(), blank(f.getRemarks()), f.getDayEndAreaId(), dayEndAt,
                blank(f.getDayEndRemarks()), f.getStayAreaId(), blank(f.getStayHotel()), status,
                f.isSubmit() ? Timestamp.valueOf(LocalDateTime.now()) : null, dcrId);

        // ---- listed calls ----
        BigDecimal pobTotal = BigDecimal.ZERO;
        Set<String> areaRows = new LinkedHashSet<>();
        if (f.getStartAreaId() != null) areaRows.add(f.getStartAreaId() + "|START");
        if (f.getDayEndAreaId() != null) areaRows.add(f.getDayEndAreaId() + "|END");
        if (f.getStayAreaId() != null) areaRows.add(f.getStayAreaId() + "|STAY");
        for (DcrDto.Visit v : f.getVisits()) {
            Long area = v.getAreaId() != null ? v.getAreaId() : lng(myClients.get(v.getClientId()).get("city_id"));
            BigDecimal pob = pob(v);
            pobTotal = pobTotal.add(pob);
            jdbc.update("""
                    INSERT INTO dcr_client_summary (dcr_id, emp_id, client_id, worktype_code, visit_type, activity_type, area_id, in_time, out_time,
                           checkin_at, checkin_lat, checkin_lng, pob_value, remarks, is_refused, next_visit, next_visit_priority, next_visit_remarks, signature_path)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                    dcrId, me, v.getClientId(), code(v.getWorktypeCode(), "CLIENT"), nz(v.getVisitType()), blank(v.getActivityType()), area,
                    time(v.getInTime(), "In time"), time(v.getOutTime(), "Out time"), dateTime(v.getCheckinAt(), "Check-in time"), v.getCheckinLat(), v.getCheckinLng(),
                    pob, blank(v.getRemarks()), v.isRefused() ? 1 : 0, date(v.getNextVisit(), "Next visit"), blank(v.getNextVisitPriority()), blank(v.getNextVisitRemarks()), blank(v.getSignaturePath()));
            if (area != null) areaRows.add(area + "|VISITED");
            children(dcrId, 1, v.getClientId(), v, tabs.get(code(v.getWorktypeCode(), "CLIENT")));
        }
        // ---- unlisted calls ----
        Map<String, Long> unlistedId = new HashMap<>();
        for (DcrDto.Unlisted u : f.getUnlisted()) {
            BigDecimal pob = pob(u);
            pobTotal = pobTotal.add(pob);
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("""
                        INSERT INTO dcr_unlisted_client_summary (dcr_id, emp_id, worktype_code, client_name, client_type_id, speciality_id, category_id, qualification,
                               mobile, contact_person, address, area_id, visit_type, activity_type, in_time, out_time, checkin_at, checkin_lat, checkin_lng, pob_value,
                               remarks, is_refused, next_visit, next_visit_priority, next_visit_remarks, signature_path)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS);
                Object[] a = {dcrId, me, code(u.getWorktypeCode(), "UNLISTED"), u.getClientName().trim(), u.getClientTypeId(), u.getSpecialityId(), u.getCategoryId(),
                        blank(u.getQualification()), nz(u.getMobile()), blank(u.getContactPerson()), blank(u.getAddress()), u.getAreaId(), nz(u.getVisitType()),
                        blank(u.getActivityType()), time(u.getInTime(), "In time"), time(u.getOutTime(), "Out time"), dateTime(u.getCheckinAt(), "Check-in time"),
                        u.getCheckinLat(), u.getCheckinLng(), pob, blank(u.getRemarks()), u.isRefused() ? 1 : 0, date(u.getNextVisit(), "Next visit"),
                        blank(u.getNextVisitPriority()), blank(u.getNextVisitRemarks()), blank(u.getSignaturePath())};
                for (int i = 0; i < a.length; i++) ps.setObject(i + 1, a[i]);
                return ps;
            }, kh);
            long uid = kh.getKey().longValue();
            if (u.getKey() != null) unlistedId.put(u.getKey(), uid);
            if (u.getAreaId() != null) areaRows.add(u.getAreaId() + "|VISITED");
            children(dcrId, 2, uid, u, tabs.get(code(u.getWorktypeCode(), "UNLISTED")));
        }
        // ---- expenses (one table, client optional) ----
        BigDecimal expenseTotal = BigDecimal.ZERO;
        Set<Long> listedIds = new HashSet<>();
        f.getVisits().forEach(v -> listedIds.add(v.getClientId()));
        for (DcrDto.Expense x : f.getExpenses()) {
            if (x.getExpenseId() == null) throw new IllegalStateException("Choose the expense type");
            List<Map<String, Object>> type = jdbc.queryForList("SELECT expense_name, is_travel FROM expense_master WHERE id = ? AND is_active = 1", x.getExpenseId());
            if (type.isEmpty()) throw new IllegalStateException("Unknown expense type " + x.getExpenseId());
            if (x.getAmount() == null || x.getAmount().signum() <= 0) throw new IllegalStateException("Enter the amount of the " + type.get(0).get("expense_name") + " expense");
            boolean travel = flag(type.get(0).get("is_travel")) == 1;
            long ref = 0; int kind = x.getClientKind();
            if (kind == 1) { if (x.getClientId() == null || !listedIds.contains(x.getClientId())) throw new IllegalStateException("An expense is linked to a client who is not in today's calls"); ref = x.getClientId(); }
            else if (kind == 2) { Long u = unlistedId.get(x.getUnlistedKey()); if (u == null) throw new IllegalStateException("An expense is linked to an unlisted call that is not in this DCR"); ref = u; }
            else kind = 0;
            if (travel && x.getStartKm() != null && x.getEndKm() != null && x.getEndKm().compareTo(x.getStartKm()) < 0) throw new IllegalStateException("Travel: end km cannot be less than start km");
            expenseTotal = expenseTotal.add(x.getAmount());
            jdbc.update("""
                    INSERT INTO dcr_expense (dcr_id, expense_id, amount, description, client_kind, client_ref_id, from_area_id, to_area_id, start_km, end_km,
                           start_image_path, end_image_path, file_name, file_type, file_path)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                    dcrId, x.getExpenseId(), x.getAmount(), blank(x.getDescription()), kind, ref, travel ? x.getFromAreaId() : null, travel ? x.getToAreaId() : null,
                    travel ? x.getStartKm() : null, travel ? x.getEndKm() : null, travel ? blank(x.getStartImagePath()) : null, travel ? blank(x.getEndImagePath()) : null,
                    blank(x.getFileName()), blank(x.getFileType()), blank(x.getFilePath()));
        }
        // ---- joint working (day level) ----
        for (String w : new LinkedHashSet<>(f.getJointWith())) joint(dcrId, w, 0, 0);
        // ---- the day's unique areas ----
        for (String a : areaRows) {
            String[] p = a.split("\\|");
            jdbc.update("INSERT INTO dcr_area_summary (dcr_id, emp_id, dcr_date, area_id, area_role) VALUES (?,?,?,?,?)", dcrId, me, date, Long.valueOf(p[0]), p[1]);
        }
        jdbc.update("UPDATE dcr_parent_summary SET listed_calls = ?, unlisted_calls = ?, pob_total = ?, expense_total = ? WHERE dcr_id = ?",
                f.getVisits().size(), f.getUnlisted().size(), pobTotal, expenseTotal, dcrId);
        return load(parent(dcrId));
    }

    private void checkCall(DcrDto.Call c, Map<String, Object> tab) {
        List<?> subs = (List<?>) tab.get("subForms");
        if (!c.getItems().isEmpty() && !subs.contains("ITEMS")) throw new IllegalStateException("Items are not enabled in the " + tab.get("name") + " tab");
        if (!c.getLeaflets().isEmpty() && !subs.contains("LEAFLETS")) throw new IllegalStateException("Leaflets are not enabled in the " + tab.get("name") + " tab");
        if (!c.getTreatments().isEmpty() && !subs.contains("TREATMENT")) throw new IllegalStateException("Treatment is not enabled in the " + tab.get("name") + " tab");
        LocalTime in = time(c.getInTime(), "In time"), out = time(c.getOutTime(), "Out time");
        if (in != null && out != null && out.isBefore(in)) throw new IllegalStateException("Out time cannot be before in time");
        Set<String> itemSeen = new HashSet<>();
        for (DcrDto.Item i : c.getItems()) {
            if (i.getItemId() == null) throw new IllegalStateException("Choose the item");
            if (!itemSeen.add(i.getItemId() + "|" + i.getItemType())) throw new IllegalStateException("The same item is added twice");
        }
        Set<Long> leafSeen = new HashSet<>();
        for (DcrDto.Leaflet l : c.getLeaflets()) if (l.getItemId() == null || !leafSeen.add(l.getItemId())) throw new IllegalStateException("A leaflet is missing or added twice");
    }

    /** Items, leaflets, treatments and joint working of one call. */
    private void children(long dcrId, int kind, long ref, DcrDto.Call c, Map<String, Object> tab) {
        for (DcrDto.Item i : c.getItems()) {
            Map<String, Object> item = jdbc.queryForList("SELECT t.item_type_code type FROM item_master m JOIN item_type_master t ON t.id = m.item_type_id WHERE m.id = ?", i.getItemId())
                    .stream().findFirst().orElseThrow(() -> new IllegalStateException("Item not found: " + i.getItemId()));
            int qty = i.getQty() == null ? 0 : i.getQty(), pobQty = i.getPobQty() == null ? 0 : i.getPobQty();
            if (qty < 0 || pobQty < 0) throw new IllegalStateException("Quantities cannot be negative");
            BigDecimal rate = i.getRate() == null ? BigDecimal.ZERO : i.getRate();
            jdbc.update("INSERT INTO dcr_client_sample (dcr_id, client_kind, client_ref_id, item_id, item_type, qty, pob_qty, rate, amount, is_detailed, remarks) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    dcrId, kind, ref, i.getItemId(), (String) item.get("type"), qty, pobQty, rate, rate.multiply(BigDecimal.valueOf(pobQty)), i.isDetailed() ? 1 : 0, blank(i.getRemarks()));
        }
        for (DcrDto.Leaflet l : c.getLeaflets()) {
            jdbc.update("INSERT INTO dcr_leaflets_summary (dcr_id, client_kind, client_ref_id, item_id, qty, value) VALUES (?,?,?,?,?,?)",
                    dcrId, kind, ref, l.getItemId(), l.getQty() == null ? 0 : l.getQty(), l.getValue() == null ? BigDecimal.ZERO : l.getValue());
        }
        for (Integer t : new LinkedHashSet<>(c.getTreatments())) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM treatment_master WHERE id = ? AND is_active = 1", Integer.class, t) == 0) throw new IllegalStateException("Unknown treatment " + t);
            jdbc.update("INSERT INTO dcr_client_treatment (dcr_id, client_kind, client_ref_id, treatment_id) VALUES (?,?,?,?)", dcrId, kind, ref, t);
        }
        for (String w : new LinkedHashSet<>(c.getJointWith())) joint(dcrId, w, kind, ref);
    }

    private void joint(long dcrId, String withEmp, int kind, long ref) {
        Object des = jdbc.queryForList("SELECT designation FROM emp_detail WHERE emp_id = ?", withEmp).stream().findFirst().map(r -> r.get("designation")).orElse(null);
        jdbc.update("INSERT INTO dcr_sm_visits (dcr_id, with_emp_id, with_designation_id, client_kind, client_ref_id) VALUES (?,?,?,?,?)", dcrId, withEmp, des, kind, ref);
    }

    /** POB of a call: what was entered, else the value of its POB items. */
    private BigDecimal pob(DcrDto.Call c) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DcrDto.Item i : c.getItems()) {
            if (i.getPobQty() != null && i.getRate() != null) sum = sum.add(i.getRate().multiply(BigDecimal.valueOf(i.getPobQty())));
        }
        if (sum.signum() > 0) return sum;
        return c.getPobValue() == null ? BigDecimal.ZERO : c.getPobValue().max(BigDecimal.ZERO);
    }

    private void checkAreas(DcrDto f) {
        List<Long> ids = new ArrayList<>();
        for (Long a : new Long[]{f.getStartAreaId(), f.getWorkingAreaId(), f.getDayEndAreaId(), f.getStayAreaId()}) if (a != null) ids.add(a);
        f.getVisits().forEach(v -> { if (v.getAreaId() != null) ids.add(v.getAreaId()); });
        f.getUnlisted().forEach(u -> { if (u.getAreaId() != null) ids.add(u.getAreaId()); });
        f.getExpenses().forEach(x -> { if (x.getFromAreaId() != null) ids.add(x.getFromAreaId()); if (x.getToAreaId() != null) ids.add(x.getToAreaId()); });
        checkAreaIds(ids);
    }

    private void checkAreaIds(List<Long> ids) {
        for (Long a : new HashSet<>(ids)) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM area_master WHERE oid = ? AND status = 'Y' AND approval_status = 'APPROVED'", Integer.class, a) == 0) {
                throw new IllegalStateException("Area " + a + " is not an active, approved area");
            }
        }
    }

    private static List<DcrDto.Call> concat(List<DcrDto.Visit> v, List<DcrDto.Unlisted> u) {
        List<DcrDto.Call> all = new ArrayList<>(v);
        all.addAll(u);
        return all;
    }

    private String callName(DcrDto.Call c) {
        if (c instanceof DcrDto.Unlisted u) return u.getClientName();
        Long id = ((DcrDto.Visit) c).getClientId();
        return jdbc.queryForList("SELECT client_name FROM client_master WHERE id = ?", id).stream().findFirst().map(r -> (String) r.get("client_name")).orElse("the client");
    }

    /** Check-in / check-out times come from the phone's clock: never in the future (5 min grace) and never on another day. */
    private static void checkNotFuture(LocalDate date, DcrDto.Call c) {
        LocalDateTime now = LocalDateTime.now().plusMinutes(5);
        for (String t : new String[]{c.getInTime(), c.getOutTime()}) {
            LocalTime lt = time(t, "Call time");
            if (lt != null && date.equals(LocalDate.now()) && date.atTime(lt).isAfter(now)) throw new IllegalStateException("A call time is in the future — check your phone's clock");
        }
        LocalTime in = time(c.getInTime(), "In time"), out = time(c.getOutTime(), "Out time");
        if (in != null && out != null && out.isBefore(in)) throw new IllegalStateException("Check-out time cannot be before check-in time");
    }

    /** DCR_BETWEEN_TIME: a call's check-in / check-out must fall inside the allowed window. */
    private static void checkWindow(Map<String, String> rl, DcrDto.Call c) {
        LocalTime[] w = DcrRuleService.window(rl, "DCR_BETWEEN_TIME");
        if (w == null) return;
        for (String t : new String[]{c.getInTime(), c.getOutTime()}) {
            LocalTime lt = time(t, "Call time");
            if (lt != null && (lt.isBefore(w[0]) || lt.isAfter(w[1]))) throw new IllegalStateException("Client calls are allowed only between " + w[0] + " and " + w[1]);
        }
    }

    private static boolean hasDayEnd(DcrDto f) {
        return f.getDayEndAreaId() != null || !Strings.isBlank(f.getDayEndAt()) || !Strings.isBlank(f.getDayEndRemarks())
                || f.getStayAreaId() != null || !Strings.isBlank(f.getStayHotel()) || f.getMeterEnd() != null;
    }

    // =========================================================================================
    // approval
    // =========================================================================================

    @Transactional(transactionManager = "tenantTransactionManager")
    public DcrDto approve(Long dcrId, String remarks) {
        return decide(dcrId, APPROVED, remarks);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DcrDto reject(Long dcrId, String reason) {
        if (Strings.isBlank(reason)) throw new IllegalStateException("Please give the reason for rejecting");
        return decide(dcrId, REJECTED, reason);
    }

    private DcrDto decide(Long dcrId, int status, String remarks) {
        Map<String, Object> p = parent(dcrId);
        String owner = (String) p.get("emp_id");
        if (((Number) p.get("status")).intValue() != SUBMITTED) throw new IllegalStateException("Only a submitted DCR can be approved or rejected");
        if (owner.equals(me())) throw new IllegalStateException("You cannot approve your own DCR — your manager does");
        if (!canApprove(owner)) throw new ForbiddenException("Only " + name(approverOf(owner)) + ", the immediate manager of " + name(owner) + ", or an admin can do this");
        String r = Strings.isBlank(remarks) ? null : remarks.trim();
        if (r != null && r.length() > 150) throw new IllegalStateException("Remarks can be at most 150 characters");
        jdbc.update("UPDATE dcr_parent_summary SET status = ?, approved_by = ?, approved_at = NOW(), approval_remarks = ? WHERE dcr_id = ?", status, me(), r, dcrId);
        return load(parent(dcrId));
    }

    /** The immediate manager approves; if he is inactive / vacant the next one up the chain does. */
    private String approverOf(String owner) {
        for (String s : scope.seniors(owner)) {
            Map<String, Object> e = jdbc.queryForList("SELECT is_active, is_vacant FROM emp_detail WHERE emp_id = ?", s).stream().findFirst().orElse(null);
            if (e != null && flag(e.get("is_active")) == 1 && flag(e.get("is_vacant")) == 0) return s;
        }
        return null;
    }

    /**
     * Delete a whole DCR (the day and everything under it). The owner can delete their own draft or
     * rejected DCR; a submitted / approved one only an admin can delete.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void delete(Long dcrId) {
        Map<String, Object> p = parent(dcrId);
        String owner = (String) p.get("emp_id");
        int status = ((Number) p.get("status")).intValue();
        boolean admin = TeamScope.isAdmin();
        if (!admin) {
            if (!owner.equals(me())) throw new ForbiddenException("You can delete only your own DCR");
            if (status == SUBMITTED || status == 2) throw new IllegalStateException("A submitted or approved DCR cannot be deleted — ask your admin");
        }
        for (String t : List.of("dcr_client_sample", "dcr_leaflets_summary", "dcr_client_treatment", "dcr_sm_visits", "dcr_expense",
                "dcr_area_summary", "dcr_client_summary", "dcr_unlisted_client_summary")) {
            jdbc.update("DELETE FROM " + t + " WHERE dcr_id = ?", dcrId);
        }
        jdbc.update("DELETE FROM dcr_parent_summary WHERE dcr_id = ?", dcrId);
    }

    /**
     * Pre-Call Analysis of one of MY clients: client facts, how often / when I visited (month by month, last 6 months)
     * and the samples / inputs given. Only my own visits are listed.
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> clientAnalysis(Long clientId) {
        String me = me();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT c.id, c.client_code AS code, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS name, c.mobile_number_1 AS mobile,
                       cat.category_name AS category, sp.speciality_name AS speciality, dg.degree_name AS degree, c.visit_frequency AS frequency, c.star_rating AS starRating,
                       ar.area_name AS areaName, ct.type_name AS clientType
                FROM client_master c
                LEFT JOIN category_master cat ON cat.oid = c.category_id
                LEFT JOIN speciality_master sp ON sp.oid = c.speciality_id
                LEFT JOIN degree_master dg ON dg.id = c.degree_id
                LEFT JOIN area_master ar ON ar.oid = c.city_id
                LEFT JOIN client_type ct ON ct.oid = c.client_type_id
                WHERE c.id = ? AND c.is_active = 1 AND c.approval_status = 'APPROVED'
                  AND (c.mapped_emp_code = ? OR (c.mapped_emp_code IS NULL AND c.created_by COLLATE utf8mb4_0900_ai_ci = ?))""", clientId, me, me);
        if (rows.isEmpty()) throw new IllegalStateException("Client not found among your clients");
        Map<String, Object> out = new LinkedHashMap<>(rows.get(0));
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        List<Map<String, Object>> visits = jdbc.queryForList("""
                SELECT p.dcr_date AS d, s.visit_type AS visitType, s.in_time AS inTime, s.is_refused AS refused, p.dcr_id AS dcrId
                FROM dcr_client_summary s JOIN dcr_parent_summary p ON p.dcr_id = s.dcr_id
                WHERE s.emp_id = ? AND s.client_id = ? AND p.dcr_date >= ? ORDER BY p.dcr_date DESC, s.in_time DESC""", me, clientId, from);
        List<Map<String, Object>> given = jdbc.queryForList("""
                SELECT p.dcr_date AS d, t.dcr_panel AS panel, SUM(x.qty) AS qty
                FROM dcr_client_sample x JOIN dcr_parent_summary p ON p.dcr_id = x.dcr_id
                JOIN item_master i ON i.id = x.item_id JOIN item_type_master t ON t.id = i.item_type_id
                WHERE p.emp_id = ? AND x.client_kind = 1 AND x.client_ref_id = ? AND p.dcr_date >= ? AND t.dcr_panel IN ('SAMPLE', 'INPUT')
                GROUP BY p.dcr_date, t.dcr_panel""", me, clientId, from);
        String who = String.valueOf(jdbc.queryForList("SELECT emp_name FROM emp_detail WHERE emp_id = ?", me).stream().findFirst().map(r -> r.get("emp_name")).orElse(""));
        List<Map<String, Object>> months = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            LocalDate m0 = LocalDate.now().withDayOfMonth(1).minusMonths(i);
            Map<String, Object> mo = new LinkedHashMap<>();
            mo.put("month", m0.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + "-" + m0.getYear());
            List<Map<String, Object>> vs = new ArrayList<>();
            for (Map<String, Object> v : visits) {
                LocalDate d = LocalDate.parse(v.get("d").toString().substring(0, 10));
                if (d.getYear() == m0.getYear() && d.getMonth() == m0.getMonth()) {
                    Map<String, Object> e = new LinkedHashMap<>(); e.put("date", d.toString()); e.put("visitType", v.get("visitType")); e.put("by", who); e.put("refused", flag(v.get("refused")) == 1);
                    vs.add(e);
                }
            }
            long sample = 0, input = 0;
            for (Map<String, Object> g : given) {
                LocalDate d = LocalDate.parse(g.get("d").toString().substring(0, 10));
                if (d.getYear() == m0.getYear() && d.getMonth() == m0.getMonth()) {
                    long q = g.get("qty") == null ? 0 : ((Number) g.get("qty")).longValue();
                    if ("SAMPLE".equals(g.get("panel"))) sample += q; else input += q;
                }
            }
            mo.put("visits", vs); mo.put("totalVisits", vs.size()); mo.put("totalSample", sample); mo.put("totalInput", input);
            months.add(mo);
        }
        out.put("months", months);
        Object last = jdbc.queryForList("SELECT MAX(p.dcr_date) AS d FROM dcr_client_summary s JOIN dcr_parent_summary p ON p.dcr_id = s.dcr_id WHERE s.emp_id = ? AND s.client_id = ?", me, clientId).get(0).get("d");
        out.put("lastVisit", last == null ? null : last.toString().substring(0, 10));
        out.put("daysSinceLastVisit", last == null ? null : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(last.toString().substring(0, 10)), LocalDate.now()));
        return out;
    }

    private boolean canApprove(String owner) {
        String me = me();
        if (owner.equals(me)) return false;
        if (TeamScope.isAdmin()) return true;
        return TeamScope.isManager() && me.equals(approverOf(owner));
    }

    // =========================================================================================
    // helpers
    // =========================================================================================

    private int backDays() {
        try { return Math.max(0, Integer.parseInt(settings.value(BACK_DAYS_KEY, "7").trim())); } catch (NumberFormatException e) { return 7; }
    }

    private String me() {
        String me = TeamScope.me();
        if (me == null) throw new ForbiddenException("Your login is not linked to an employee");
        return me;
    }

    private String name(String empId) {
        if (empId == null) return null;
        return jdbc.queryForList("SELECT emp_name FROM emp_detail WHERE emp_id = ?", empId).stream().findFirst().map(r -> (String) r.get("emp_name")).orElse(empId);
    }

    /** TINYINT(1) columns come back as Boolean from MySQL, others as Number. */
    private static int flag(Object o) {
        return o instanceof Boolean b ? (b ? 1 : 0) : ((Number) o).intValue();
    }

    private static long key(Object kind, Object ref) {
        return ((Number) kind).longValue() * 1_000_000_000_000L + ((Number) ref).longValue();
    }

    private static Long lng(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static String hhmm(Object t) {
        return t == null ? null : t.toString().substring(0, 5);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String blank(String s) {
        return Strings.isBlank(s) ? null : s.trim();
    }

    private static String code(String s, String def) {
        return Strings.isBlank(s) ? def : s.trim().toUpperCase();
    }

    private static LocalDate date(String s, String what) {
        if (Strings.isBlank(s)) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { throw new IllegalStateException(what + " is not a valid date"); }
    }

    private static LocalTime time(String s, String what) {
        if (Strings.isBlank(s)) return null;
        try { return LocalTime.parse(s.trim()); } catch (Exception e) { throw new IllegalStateException(what + " is not a valid time (HH:mm)"); }
    }

    private static Timestamp dateTime(String s, String what) {
        if (Strings.isBlank(s)) return null;
        try { return Timestamp.valueOf(LocalDateTime.parse(s.trim().replace(' ', 'T'))); } catch (Exception e) { throw new IllegalStateException(what + " is not valid (yyyy-MM-ddTHH:mm)"); }
    }

    /** "Name (code)" of an employee for a message - never the internal employee id. */
    private String empLabel(String empId) {
        return jdbc.queryForList("SELECT CONCAT(emp_name, ' (', emp_code, ')') AS n FROM emp_detail WHERE emp_id = ?", empId).stream().findFirst().map(r -> String.valueOf(r.get("n"))).orElse("this employee");
    }
}
