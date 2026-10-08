package in.opt.sfa.common.util;

import in.opt.sfa.common.service.CompanySettingStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates unique business codes (currently emp_code) for the ACTIVE tenant.
 *
 * The running number comes from the tenant's code_sequence table via the classic
 * MySQL atomic trick — one UPDATE takes a row lock and both increments and returns
 * the value through LAST_INSERT_ID — so two concurrent requests can never receive
 * the same number (no duplicate codes). The emp_code UNIQUE index is the final
 * safety net.
 *
 * Format is per-company (company_setting_master): prefix + number (+ optional
 * suffix), e.g. AWT201, AWT202. width 0 = no zero-padding.
 *
 * Uses the @Primary (tenant) JdbcTemplate, so it auto-routes to the active tenant DB.
 */
@Service
public class CodeGeneratorService {

    private final JdbcTemplate jdbc;              // tenant-routed (@Primary) — code_sequence lives per tenant
    private final CompanySettingStore settings;  // common db, scoped to the current tenant

    public CodeGeneratorService(JdbcTemplate jdbc, CompanySettingStore settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    /** Next unique employee code for the current tenant: prefix + running number (+ suffix). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public String nextEmpCode() {
        long n = nextSequence("emp_code");
        String prefix = setting("empcode.prefix", "EMP");
        String suffix = setting("empcode.suffix", "");
        int width = parseInt(setting("empcode.width", "0"), 0);
        return format(prefix, suffix, width, n);
    }

    // ---- emp_code format config (per tenant, company-facing) ----------------

    /** Current emp_code format for this tenant + a live preview of the next code. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public java.util.Map<String, Object> empCodeConfig() {
        String prefix = setting("empcode.prefix", "EMP");
        String suffix = setting("empcode.suffix", "");
        int width = parseInt(setting("empcode.width", "0"), 0);
        long next = peekNextSequence("emp_code");
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("prefix", prefix);
        m.put("width", width);
        m.put("suffix", suffix);
        m.put("nextNumber", next);                                 // raw running number (for live client preview)
        m.put("preview", format(prefix, suffix, width, next));     // fully-formatted next code
        return m;
    }

    /** Save the emp_code format (company-configurable; the running number is untouched). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public java.util.Map<String, Object> saveEmpCodeConfig(String prefix, Integer width, String suffix) {
        settings.put("empcode.prefix", prefix == null ? "" : prefix.trim());
        settings.put("empcode.suffix", suffix == null ? "" : suffix.trim());
        settings.put("empcode.width", String.valueOf(width == null || width < 0 ? 0 : width));
        return empCodeConfig();
    }

    private String format(String prefix, String suffix, int width, long n) {
        String num = (width > 0) ? String.format("%0" + width + "d", n) : Long.toString(n);
        return prefix + num + suffix;
    }

    /** The number the NEXT emp_code would use, WITHOUT consuming it (for previews). */
    private long peekNextSequence(String name) {
        Long cur = jdbc.query(
                "SELECT next_val FROM code_sequence WHERE seq_name = ?",
                rs -> rs.next() ? rs.getLong(1) : null, name);
        return (cur == null ? 0L : cur) + 1;
    }

    /** Atomic, race-free next value for a named sequence in the tenant DB. */
    private long nextSequence(String name) {
        jdbc.update("INSERT IGNORE INTO code_sequence(seq_name, next_val) VALUES (?, 0)", name);
        // row lock on this seq row + session-scoped LAST_INSERT_ID => no two callers get the same value
        jdbc.update("UPDATE code_sequence SET next_val = LAST_INSERT_ID(next_val + 1) WHERE seq_name = ?", name);
        Long val = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return val == null ? 0L : val;
    }

    private String setting(String key, String def) {
        return settings.value(key, def);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
