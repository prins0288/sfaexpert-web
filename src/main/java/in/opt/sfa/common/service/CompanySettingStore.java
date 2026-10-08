package in.opt.sfa.common.service;

import in.opt.sfa.common.entity.CompanySettingMaster;
import in.opt.sfa.common.repository.CompanySettingMasterRepository;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The one place company settings are read and written. Data now lives in the
 * COMMON db (sfa_central, company_setting_master) with a company_code column, so
 * EVERY access is scoped to the current tenant ({@link TenantContext}) here —
 * consumers (SettingsController, ThemeService, LoaderController,
 * CodeGeneratorService) never deal with tenant filtering themselves and can
 * never leak one company's settings into another's.
 *
 * All methods run on the common transaction manager.
 */
@Service
public class CompanySettingStore {

    /** Reserved "tenant" id for APP-WIDE (global) settings — e.g. the default
     *  branding shown on the login page before any tenant is known. Real tenant
     *  ids are alphanumeric, so "*" can never collide with one. */
    public static final String GLOBAL = "*";

    private final CompanySettingMasterRepository repo;

    public CompanySettingStore(CompanySettingMasterRepository repo) {
        this.repo = repo;
    }

    private static String companyCode() {
        String t = TenantContext.getCompanyCode();
        if (t == null || t.isBlank()) {
            throw new IllegalStateException("No tenant bound — cannot read/write company settings");
        }
        return t;
    }

    /** All of the current tenant's settings as a flat key -> value map (insertion order). */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public Map<String, String> all() {
        Map<String, String> m = new LinkedHashMap<>();
        repo.findByCompanyCode(companyCode()).forEach(r -> m.put(r.getSettingKey(), r.getSettingValue()));
        return m;
    }

    /** Raw rows for the current tenant (when the caller needs the entities). */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public List<CompanySettingMaster> rows() {
        return repo.findByCompanyCode(companyCode());
    }

    /** One setting's value for the current tenant. */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public Optional<String> value(String key) {
        return repo.findByCompanyCodeAndSettingKey(companyCode(), key)
                .map(CompanySettingMaster::getSettingValue);
    }

    /** One setting's value, or {@code def} if unset/blank. */
    public String value(String key, String def) {
        return value(key).filter(v -> v != null && !v.isBlank()).orElse(def);
    }

    /** Upsert one setting for the current tenant. */
    @Transactional(transactionManager = "commonTransactionManager")
    public void put(String key, String value) {
        String t = companyCode();
        CompanySettingMaster row = repo.findByCompanyCodeAndSettingKey(t, key)
                .orElseGet(CompanySettingMaster::new);
        row.setCompanyCode(t);
        row.setSettingKey(key);
        row.setSettingValue(value);
        row.setUpdatedAt(LocalDateTime.now());
        row.setUpdatedBy(currentUser());
        repo.save(row);
    }

    /** Delete one setting for the current tenant (no-op if absent). */
    @Transactional(transactionManager = "commonTransactionManager")
    public void remove(String key) {
        repo.findByCompanyCodeAndSettingKey(companyCode(), key).ifPresent(repo::delete);
    }

    // ---- explicit-tenant access (e.g. GLOBAL app-wide settings, no tenant bound) ----

    /** One setting for a SPECIFIC tenant id (use {@link #GLOBAL} for app-wide). */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public Optional<String> valueFor(String companyCode, String key) {
        return repo.findByCompanyCodeAndSettingKey(companyCode, key)
                .map(CompanySettingMaster::getSettingValue);
    }

    /** One setting for a specific tenant, or {@code def} if unset/blank. */
    public String valueFor(String companyCode, String key, String def) {
        return valueFor(companyCode, key).filter(v -> v != null && !v.isBlank()).orElse(def);
    }

    /** Upsert one setting for a SPECIFIC tenant id (use {@link #GLOBAL} for app-wide). */
    @Transactional(transactionManager = "commonTransactionManager")
    public void putFor(String companyCode, String key, String value) {
        CompanySettingMaster row = repo.findByCompanyCodeAndSettingKey(companyCode, key)
                .orElseGet(CompanySettingMaster::new);
        row.setCompanyCode(companyCode);
        row.setSettingKey(key);
        row.setSettingValue(value);
        row.setUpdatedAt(LocalDateTime.now());
        row.setUpdatedBy(currentUser());
        repo.save(row);
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.username() == null) ? "system" : u.username();
    }
}
