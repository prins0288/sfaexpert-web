package in.opt.sfa.tenant.hr.setting;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.hr.access.HrAccessService;
import in.opt.sfa.tenant.hr.common.AuditLogService;
import in.opt.sfa.tenant.hr.common.EffectiveDated;
import in.opt.sfa.tenant.hr.common.SettingTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/** Effective-dated settings. Later phases read them with {@link #decimal}, {@link #integer}, {@link #text}; nothing statutory is hard-coded. */
@Service
public class HrSettingService {

    private final JdbcTemplate jdbc;
    private final AuditLogService audit;
    private final HrAccessService access;

    public HrSettingService(JdbcTemplate jdbc, AuditLogService audit, HrAccessService access) { this.jdbc = jdbc; this.audit = audit; this.access = access; }

    record Row(long id, String value, LocalDate from, LocalDate to) { }

    private List<Row> rows(String key, String scopeType, int scopeId) {
        return jdbc.query("SELECT id, value_text, effective_from, effective_to FROM hr_setting WHERE company_id = 1 AND setting_key = ? AND scope_type = ? AND scope_id = ? ORDER BY effective_from",
                (rs, i) -> new Row(rs.getLong(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getDate(4) == null ? null : rs.getDate(4).toLocalDate()), key, scopeType, scopeId);
    }

    public Optional<String> text(String key, LocalDate date) { return EffectiveDated.pick(rows(key, "COMPANY", 0), date, Row::from, Row::to).map(Row::value); }
    public Optional<BigDecimal> decimal(String key, LocalDate date) { return text(key, date).map(BigDecimal::new); }
    public Optional<Integer> integer(String key, LocalDate date) { return text(key, date).map(Integer::valueOf); }
    public boolean bool(String key, LocalDate date, boolean dflt) { return text(key, date).map("true"::equals).orElse(dflt); }

    /** The catalogue with the value valid today and the next change already booked, if any. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> catalogue() {
        access.require("HR_SETTINGS_VIEW");
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> defs = jdbc.queryForList("SELECT setting_key AS `key`, grp AS `group`, label, help_text AS help, value_type AS type, unit, min_value AS min, max_value AS max, enum_values AS options FROM hr_setting_def ORDER BY sort_order");
        for (Map<String, Object> d : defs) {
            List<Row> rs = rows((String) d.get("key"), "COMPANY", 0);
            Optional<Row> cur = EffectiveDated.pick(rs, today, Row::from, Row::to);
            d.put("value", cur.map(Row::value).orElse(null));
            d.put("effectiveFrom", cur.map(r -> r.from().toString()).orElse(null));
            rs.stream().filter(r -> r.from().isAfter(today)).findFirst().ifPresent(n -> { d.put("nextValue", n.value()); d.put("nextFrom", n.from().toString()); });
        }
        return defs;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> history(String key) {
        access.require("HR_SETTINGS_VIEW");
        return jdbc.queryForList("SELECT s.id, s.value_text AS value, s.effective_from AS effectiveFrom, s.effective_to AS effectiveTo, s.source, s.reason, s.created_at AS createdAt, e.emp_name AS byName "
                + "FROM hr_setting s LEFT JOIN emp_detail e ON e.emp_id = s.created_by WHERE s.company_id = 1 AND s.setting_key = ? AND s.scope_type = 'COMPANY' ORDER BY s.effective_from DESC, s.id DESC", key);
    }

    /** New value from a date: the open row that was running ends the day before; rows starting later are replaced by this revision. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void save(String key, String rawValue, LocalDate from, String reason) {
        access.require("HR_SETTINGS");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please give a reason for the change");
        if (from == null) throw new IllegalArgumentException("Effective from date is required");
        Map<String, Object> def = jdbc.queryForList("SELECT value_type, min_value, max_value, enum_values FROM hr_setting_def WHERE setting_key = ?", key).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown setting " + key));
        String value = SettingTypes.normalise((String) def.get("value_type"), rawValue, (String) def.get("min_value"), (String) def.get("max_value"), (String) def.get("enum_values"));
        List<Row> rs = rows(key, "COMPANY", 0);
        for (Row r : rs) {
            if (r.from().isAfter(from) || r.from().equals(from)) {   // later (or same-day) revision is superseded
                audit.log("SETTING", key, null, "DELETE", "value@" + r.from(), r.value(), null, "Replaced by revision from " + from + ": " + reason);
                jdbc.update("DELETE FROM hr_setting WHERE id = ?", r.id());
            }
        }
        Optional<Row> running = EffectiveDated.pick(rs.stream().filter(r -> r.from().isBefore(from)).toList(), from.minusDays(1), Row::from, Row::to);
        LocalDate closes = EffectiveDated.closeDateFor(from);
        for (Row r : rs) {
            if (r.from().isBefore(from) && (r.to() == null || r.to().isAfter(closes))) {
                jdbc.update("UPDATE hr_setting SET effective_to = ? WHERE id = ?", Date.valueOf(closes), r.id());
            }
        }
        String old = running.map(Row::value).orElse(null);
        String me = UserContext.get() == null ? null : UserContext.get().empId();
        jdbc.update("INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason, created_by) VALUES (1,?,?,?,'USER',?,?)", key, value, Date.valueOf(from), reason, me);
        audit.log("SETTING", key, null, old == null ? "CREATE" : "UPDATE", "value@" + from, old, value, reason);
    }
}
