-- ===========================================================================
-- MIGRATION: rebuild tenant_config with
--   (a) the JDBC URL split into columns: host / port / db_name / params
--   (b) full per-tenant HikariCP configuration columns
-- and re-seed all tenants with DIFFERENT Hikari settings each.
--
-- The COMMON pool's HikariCP config is NOT here — it lives in application.yml
-- (app.datasource.common). This table is per-TENANT only.
--
-- Run:  mysql -u myroot < src/main/resources/sql/05_hikari_tenant_config.sql
-- ===========================================================================
USE sfa_central;

DROP TABLE IF EXISTS tenant_config;

CREATE TABLE tenant_config (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   VARCHAR(64)  NOT NULL UNIQUE,

    -- ---- connection parts (URL split into columns) ----
    db_host                     VARCHAR(255) NOT NULL,
    db_port                     INT          NOT NULL DEFAULT 3306,
    db_name                     VARCHAR(128) NOT NULL,
    db_params                   VARCHAR(500),                 -- text after '?', nullable
    db_username                 VARCHAR(128) NOT NULL,
    db_password                 VARCHAR(256) NOT NULL,
    driver_class_name           VARCHAR(200),

    -- ---- per-tenant HikariCP tuning (NULL => manager default) ----
    maximum_pool_size           INT,
    minimum_idle                INT,
    connection_timeout_ms       BIGINT,
    idle_timeout_ms             BIGINT,
    max_lifetime_ms             BIGINT,
    keepalive_time_ms           BIGINT,
    validation_timeout_ms       BIGINT,
    leak_detection_threshold_ms BIGINT,
    pool_name                   VARCHAR(128),
    auto_commit                 TINYINT(1),
    read_only                   TINYINT(1),
    connection_test_query       VARCHAR(255)
);

-- ---------------------------------------------------------------------------
-- Seed: 4 tenants, each with its OWN host/port/db + its OWN Hikari settings.
-- ---------------------------------------------------------------------------
INSERT INTO tenant_config
  (tenant_id, db_host, db_port, db_name, db_params, db_username, db_password, driver_class_name,
   maximum_pool_size, minimum_idle, connection_timeout_ms, idle_timeout_ms, max_lifetime_ms,
   keepalive_time_ms, validation_timeout_ms, leak_detection_threshold_ms,
   pool_name, auto_commit, read_only, connection_test_query)
VALUES
  -- demo (admin) — modest pool, short idle
  ('demo', 'localhost', 3306, 'sfa_demo', 'useSSL=false&serverTimezone=UTC',
   'myroot', '', 'com.mysql.cj.jdbc.Driver',
   5, 1, 20000, 120000, 1800000,
   NULL, NULL, NULL,
   'pool-demo', 1, 0, NULL),

  -- acme — big pool, leak detection on
  ('acme', 'localhost', 3306, 'acme_db', 'useSSL=false&serverTimezone=UTC',
   'myroot', '', 'com.mysql.cj.jdbc.Driver',
   10, 2, 30000, 300000, 1800000,
   30000, NULL, 15000,
   'pool-acme', 1, 0, NULL),

  -- globex — small pool, aggressive recycle
  ('globex', 'localhost', 3306, 'globex_db', 'useSSL=false&serverTimezone=UTC',
   'myroot', '', 'com.mysql.cj.jdbc.Driver',
   3, 0, 15000, 60000, 600000,
   NULL, 5000, NULL,
   'pool-globex', 1, 0, NULL),

  -- initech — medium pool, explicit validation query
  ('initech', 'localhost', 3306, 'initech_db', 'useSSL=false&serverTimezone=UTC',
   'myroot', '', 'com.mysql.cj.jdbc.Driver',
   8, 2, 25000, 240000, 900000,
   NULL, NULL, NULL,
   'pool-initech', 1, 0, 'SELECT 1');
