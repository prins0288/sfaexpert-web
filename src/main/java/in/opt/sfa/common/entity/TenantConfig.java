package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Lives in the COMMON database (sfa_central). Stores everything needed to build
 * a dedicated HikariCP datasource for a tenant:
 *
 *  - the JDBC URL split into parts (host / port / db name / params) so each
 *    piece is its own column, and
 *  - per-tenant HikariCP tuning (pool size, timeouts, lifetime, ...).
 *
 * All Hikari fields are nullable wrappers: when null, TenantDataSourceManager
 * falls back to a sensible default instead of overriding Hikari's own default.
 */
@Entity
@Table(name = "tenant_config")
@Getter
@Setter
public class TenantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    // ---- Connection parts (the JDBC URL, split into columns) ----------------
    @Column(name = "db_host", nullable = false)
    private String dbHost;

    @Column(name = "db_port", nullable = false)
    private Integer dbPort;

    @Column(name = "db_name", nullable = false)
    private String dbName;

    /** Query string placed after '?' in the URL, e.g. "useSSL=false&serverTimezone=UTC". */
    @Column(name = "db_params")
    private String dbParams;

    @Column(name = "db_username", nullable = false)
    private String dbUsername;

    @Column(name = "db_password", nullable = false)
    private String dbPassword;

    @Column(name = "driver_class_name")
    private String driverClassName;

    // ---- Per-tenant HikariCP configuration ----------------------------------
    @Column(name = "maximum_pool_size")
    private Integer maximumPoolSize;

    @Column(name = "minimum_idle")
    private Integer minimumIdle;

    @Column(name = "connection_timeout_ms")
    private Long connectionTimeoutMs;

    @Column(name = "idle_timeout_ms")
    private Long idleTimeoutMs;

    @Column(name = "max_lifetime_ms")
    private Long maxLifetimeMs;

    @Column(name = "keepalive_time_ms")
    private Long keepaliveTimeMs;

    @Column(name = "validation_timeout_ms")
    private Long validationTimeoutMs;

    @Column(name = "leak_detection_threshold_ms")
    private Long leakDetectionThresholdMs;

    @Column(name = "pool_name")
    private String poolName;

    @Column(name = "auto_commit")
    private Boolean autoCommit;

    @Column(name = "read_only")
    private Boolean readOnly;

    @Column(name = "connection_test_query")
    private String connectionTestQuery;
}
