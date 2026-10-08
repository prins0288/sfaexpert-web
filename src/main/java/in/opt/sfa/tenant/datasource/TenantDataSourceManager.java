package in.opt.sfa.tenant.datasource;

import in.opt.sfa.common.entity.TenantConfig;
import in.opt.sfa.common.repository.TenantConfigRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The heart of the system.
 *
 * - A tenant datasource is created LAZILY, only the first time it is requested
 *   (i.e. on login / on the tenant's first request). Before that, the pool for
 *   that tenant does not exist -> no connection is opened at app startup.
 *
 * - Every access refreshes a "last used" timestamp. A scheduled job closes any
 *   pool that has been idle longer than the configured timeout (default 30 min).
 *   The next request for that tenant simply re-creates the pool.
 *
 * TenantConfigRepository is injected via ObjectProvider (lazy) so this bean can
 * be constructed early without forcing the common EMF to initialise first.
 */
@Component
@Slf4j
public class TenantDataSourceManager {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> openedAt = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    private final ObjectProvider<TenantConfigRepository> tenantConfigRepositoryProvider;
    private final long idleTimeoutMs;

    public TenantDataSourceManager(
            ObjectProvider<TenantConfigRepository> tenantConfigRepositoryProvider,
            @Value("${app.tenant.idle-timeout-minutes:30}") long idleTimeoutMinutes) {
        this.tenantConfigRepositoryProvider = tenantConfigRepositoryProvider;
        this.idleTimeoutMs = Duration.ofMinutes(idleTimeoutMinutes).toMillis();
    }

    /**
     * Returns the (creating if needed) datasource for a tenant and marks it used.
     */
    public DataSource getDataSource(String companyCode) {
        lastAccess.put(companyCode, System.currentTimeMillis());

        HikariDataSource existing = pools.get(companyCode);
        if (existing != null && !existing.isClosed()) {
            return existing;
        }

        synchronized (lock) {
            HikariDataSource again = pools.get(companyCode);
            if (again != null && !again.isClosed()) {
                return again;
            }
            HikariDataSource created = build(companyCode);
            pools.put(companyCode, created);
            lastAccess.put(companyCode, System.currentTimeMillis());
            LocalDateTime openTime = LocalDateTime.now();
            openedAt.put(companyCode, openTime);
            log.info("Pool '{}' OPENED for tenant '{}' at {} (active tenants: {})",
                    created.getPoolName(), companyCode, openTime.format(TS), pools.size());
            return created;
        }
    }

    private HikariDataSource build(String companyCode) {
        TenantConfig cfg = tenantConfigRepositoryProvider.getObject()
                .findByCompanyCode(companyCode)
                .orElseThrow(() -> new IllegalStateException(
                        "No tenant_config found for tenant '" + companyCode + "'"));

                //         log.info("Connection Details for tenant '{}': host={}, port={}, db={}, user={}, pool={}",
                // companyCode, cfg.getDbHost(), cfg.getDbPort(), cfg.getDbName(), cfg.getDbUsername(), cfg.getPoolName());

        HikariConfig hikari = new HikariConfig();

        // ---- JDBC URL built from the individual columns --------------------
        hikari.setJdbcUrl(buildJdbcUrl(cfg));
        hikari.setUsername(cfg.getDbUsername());
        hikari.setPassword(cfg.getDbPassword());
        if (hasText(cfg.getDriverClassName())) {
            hikari.setDriverClassName(cfg.getDriverClassName());
        }

        // ---- Per-tenant HikariCP settings (each from its own column) -------
        hikari.setPoolName(hasText(cfg.getPoolName()) ? cfg.getPoolName() : "tenant-" + companyCode);
        hikari.setMaximumPoolSize(cfg.getMaximumPoolSize() != null ? cfg.getMaximumPoolSize() : 5);
        // minimumIdle 0 (default) => Hikari drops connections when unused; our
        // scheduler then closes the whole pool once the tenant goes idle.
        hikari.setMinimumIdle(cfg.getMinimumIdle() != null ? cfg.getMinimumIdle() : 0);
        hikari.setConnectionTimeout(cfg.getConnectionTimeoutMs() != null
                ? cfg.getConnectionTimeoutMs() : Duration.ofSeconds(20).toMillis());
        hikari.setIdleTimeout(cfg.getIdleTimeoutMs() != null
                ? cfg.getIdleTimeoutMs() : Duration.ofMinutes(2).toMillis());
        if (cfg.getMaxLifetimeMs() != null) {
            hikari.setMaxLifetime(cfg.getMaxLifetimeMs());
        }
        if (cfg.getKeepaliveTimeMs() != null) {
            hikari.setKeepaliveTime(cfg.getKeepaliveTimeMs());
        }
        if (cfg.getValidationTimeoutMs() != null) {
            hikari.setValidationTimeout(cfg.getValidationTimeoutMs());
        }
        if (cfg.getLeakDetectionThresholdMs() != null) {
            hikari.setLeakDetectionThreshold(cfg.getLeakDetectionThresholdMs());
        }
        if (cfg.getAutoCommit() != null) {
            hikari.setAutoCommit(cfg.getAutoCommit());
        }
        if (cfg.getReadOnly() != null) {
            hikari.setReadOnly(cfg.getReadOnly());
        }
        if (hasText(cfg.getConnectionTestQuery())) {
            hikari.setConnectionTestQuery(cfg.getConnectionTestQuery());
        }

        return new HikariDataSource(hikari);
    }

    /** Assembles the JDBC URL from the host / port / db-name / params columns. */
    private String buildJdbcUrl(TenantConfig cfg) {
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(cfg.getDbHost())
                .append(':')
                .append(cfg.getDbPort() != null ? cfg.getDbPort() : 3306)
                .append('/')
                .append(cfg.getDbName());

        // Ensure UTF-8 on every tenant connection so multi-byte data (e.g. Hindi
        // menu titles) round-trips instead of coming back garbled — regardless of
        // what an individual tenant_config.db_params row happens to contain.
        String params = hasText(cfg.getDbParams()) ? cfg.getDbParams() : "";
        params = ensureParam(params, "characterEncoding", "UTF-8");
        params = ensureParam(params, "useUnicode", "true");
        url.append('?').append(params);
        return url.toString();
    }

    /** Append key=value to a JDBC query string unless the key is already present. */
    private static String ensureParam(String params, String key, String value) {
        if (params != null && params.toLowerCase().contains(key.toLowerCase() + "=")) {
            return params;
        }
        return (params == null || params.isBlank()) ? key + "=" + value : params + "&" + key + "=" + value;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** Runs every minute; closes pools idle beyond the timeout. */
    @Scheduled(fixedDelay = 60_000L)
    public void closeIdleDataSources() {
        long now = System.currentTimeMillis();
        for (String companyCode : new ArrayList<>(pools.keySet())) {
            Long last = lastAccess.get(companyCode);
            if (last == null || (now - last) > idleTimeoutMs) {
                synchronized (lock) {
                    HikariDataSource ds = pools.remove(companyCode);
                    lastAccess.remove(companyCode);
                    LocalDateTime opened = openedAt.remove(companyCode);
                    if (ds != null && !ds.isClosed()) {
                        String poolName = ds.getPoolName();
                        ds.close();
                        LocalDateTime closeTime = LocalDateTime.now();
                        long openMins = opened == null ? -1 : Duration.between(opened, closeTime).toMinutes();
                        log.info("Pool '{}' CLOSED for tenant '{}' at {} (opened at {}, was open ~{} min, idle > {} min)",
                                poolName, companyCode, closeTime.format(TS),
                                opened == null ? "?" : opened.format(TS), openMins, idleTimeoutMs / 60000);
                    }
                }
            }
        }
    }

    public boolean isOpen(String companyCode) {
        HikariDataSource ds = pools.get(companyCode);
        return ds != null && !ds.isClosed();
    }

    @PreDestroy
    public void closeAll() {
        pools.forEach((companyCode, ds) -> {
            if (!ds.isClosed()) {
                LocalDateTime opened = openedAt.get(companyCode);
                ds.close();
                log.info("Pool '{}' CLOSED for tenant '{}' at {} (opened at {}) — app shutdown",
                        ds.getPoolName(), companyCode, LocalDateTime.now().format(TS),
                        opened == null ? "?" : opened.format(TS));
            }
        });
        pools.clear();
        lastAccess.clear();
        openedAt.clear();
    }
}
