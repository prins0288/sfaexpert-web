-- ===========================================================================
-- ADMIN SEED  (matches application.yml naming)
--   Common DB = sfa_central   (users + tenant_config)
--   Tenant DB = sfa_demo      (per-tenant business tables, e.g. product)
--
-- Run:  mysql -u myroot < src/main/resources/sql/03_admin_seed.sql
-- Test user:  admin / pass
-- ===========================================================================

-- ---- COMMON DB: sfa_central ------------------------------------------------
CREATE DATABASE IF NOT EXISTS sfa_central;
USE sfa_central;

-- NOTE: the authoritative tenant_config schema (URL split into columns + full
-- HikariCP tuning) lives in 05_hikari_tenant_config.sql. This CREATE mirrors it
-- so 03 stays runnable on its own.
CREATE TABLE IF NOT EXISTS tenant_config (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   VARCHAR(64)  NOT NULL UNIQUE,
    db_host                     VARCHAR(255) NOT NULL,
    db_port                     INT          NOT NULL DEFAULT 3306,
    db_name                     VARCHAR(128) NOT NULL,
    db_params                   VARCHAR(500),
    db_username                 VARCHAR(128) NOT NULL,
    db_password                 VARCHAR(256) NOT NULL,
    driver_class_name           VARCHAR(200),
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

CREATE TABLE IF NOT EXISTS app_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(128) NOT NULL UNIQUE,
    password   VARCHAR(256) NOT NULL,          -- BCrypt hash
    tenant_id  VARCHAR(64)  NOT NULL,
    enabled    TINYINT(1)   NOT NULL DEFAULT 1
);

-- Tenant "demo" points at the sfa_demo database (host/port/name in columns).
-- (db_username/password match the local MySQL used in application.yml.)
INSERT IGNORE INTO tenant_config
    (tenant_id, db_host, db_port, db_name, db_params, db_username, db_password, driver_class_name,
     maximum_pool_size, minimum_idle, connection_timeout_ms, idle_timeout_ms, max_lifetime_ms, pool_name)
VALUES
    ('demo', 'localhost', 3306, 'sfa_demo', 'useSSL=false&serverTimezone=UTC',
     'myroot', '', 'com.mysql.cj.jdbc.Driver',
     5, 1, 20000, 120000, 1800000, 'pool-demo');

-- admin / pass   (BCrypt strength 10; Spring accepts the $2y prefix)
INSERT IGNORE INTO app_user (username, password, tenant_id, enabled)
VALUES
    ('admin',
     '$2y$10$IAcdGFYlbtdqc6R6KCdACeQ16XiM8t1owOatGDBLpIGrgRtfdSzR.',
     'demo', 1);

-- ---- TENANT DB: sfa_demo ---------------------------------------------------
CREATE DATABASE IF NOT EXISTS sfa_demo;
USE sfa_demo;

CREATE TABLE IF NOT EXISTS product (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    price DECIMAL(12,2)
);

INSERT INTO product (name, price)
SELECT * FROM (SELECT 'Demo - Laptop' AS name, 55000.00 AS price
               UNION ALL SELECT 'Demo - Mouse', 700.00) t
WHERE NOT EXISTS (SELECT 1 FROM product);
