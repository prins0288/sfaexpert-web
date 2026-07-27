-- ===========================================================================
-- THREE TENANTS DEMO  — 3 separate databases, each with different data.
--   Common DB = sfa_central  (users + tenant_config)
--   Tenant DBs: acme_db, globex_db, initech_db
--
-- Run:  mysql -u myroot < src/main/resources/sql/04_three_tenants_seed.sql
--
-- Logins (all password = pass):
--   acme    / pass  -> acme_db     (tech gadgets)
--   globex  / pass  -> globex_db   (heavy machinery)
--   initech / pass  -> initech_db  (office supplies)
-- ===========================================================================

-- ---- COMMON: register the 3 tenants + 3 users ------------------------------
USE sfa_central;

-- Host/port/db-name in separate columns + per-tenant Hikari settings.
-- (Canonical schema + tuning is in 05_hikari_tenant_config.sql.)
INSERT INTO tenant_config
  (tenant_id, db_host, db_port, db_name, db_params, db_username, db_password, driver_class_name,
   maximum_pool_size, minimum_idle, connection_timeout_ms, idle_timeout_ms, max_lifetime_ms,
   leak_detection_threshold_ms, pool_name, connection_test_query)
VALUES
  ('acme',    'localhost', 3306, 'acme_db',    'useSSL=false&serverTimezone=UTC', 'myroot', '', 'com.mysql.cj.jdbc.Driver',
   10, 2, 30000, 300000, 1800000, 15000, 'pool-acme',    NULL),
  ('globex',  'localhost', 3306, 'globex_db',  'useSSL=false&serverTimezone=UTC', 'myroot', '', 'com.mysql.cj.jdbc.Driver',
   3,  0, 15000, 60000,  600000,  NULL,  'pool-globex',  NULL),
  ('initech', 'localhost', 3306, 'initech_db', 'useSSL=false&serverTimezone=UTC', 'myroot', '', 'com.mysql.cj.jdbc.Driver',
   8,  2, 25000, 240000, 900000,  NULL,  'pool-initech', 'SELECT 1')
ON DUPLICATE KEY UPDATE
  db_host = VALUES(db_host), db_port = VALUES(db_port), db_name = VALUES(db_name),
  db_params = VALUES(db_params), db_username = VALUES(db_username), db_password = VALUES(db_password),
  maximum_pool_size = VALUES(maximum_pool_size), minimum_idle = VALUES(minimum_idle),
  connection_timeout_ms = VALUES(connection_timeout_ms), idle_timeout_ms = VALUES(idle_timeout_ms),
  max_lifetime_ms = VALUES(max_lifetime_ms), leak_detection_threshold_ms = VALUES(leak_detection_threshold_ms),
  pool_name = VALUES(pool_name), connection_test_query = VALUES(connection_test_query);

-- all three passwords are "pass" (BCrypt strength 10)
INSERT INTO app_user (username, password, tenant_id, enabled)
VALUES
  ('acme',    '$2y$10$IAcdGFYlbtdqc6R6KCdACeQ16XiM8t1owOatGDBLpIGrgRtfdSzR.', 'acme',    1),
  ('globex',  '$2y$10$IAcdGFYlbtdqc6R6KCdACeQ16XiM8t1owOatGDBLpIGrgRtfdSzR.', 'globex',  1),
  ('initech', '$2y$10$IAcdGFYlbtdqc6R6KCdACeQ16XiM8t1owOatGDBLpIGrgRtfdSzR.', 'initech', 1)
ON DUPLICATE KEY UPDATE tenant_id = VALUES(tenant_id), password = VALUES(password), enabled = 1;

-- ---- TENANT 1: acme_db (tech gadgets) --------------------------------------
CREATE DATABASE IF NOT EXISTS acme_db;
USE acme_db;
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2)
);
DELETE FROM product;
INSERT INTO product (name, price) VALUES
  ('ACME Anvil',     1200.00),
  ('ACME Rocket',    9500.00),
  ('ACME Dynamite',   300.00),
  ('ACME Jetpack',  15000.00);

-- ---- TENANT 2: globex_db (heavy machinery) ---------------------------------
CREATE DATABASE IF NOT EXISTS globex_db;
USE globex_db;
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2)
);
DELETE FROM product;
INSERT INTO product (name, price) VALUES
  ('Globex Reactor',  250000.00),
  ('Globex Turbine',   88000.00),
  ('Globex Generator', 42000.00);

-- ---- TENANT 3: initech_db (office supplies) --------------------------------
CREATE DATABASE IF NOT EXISTS initech_db;
USE initech_db;
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2)
);
DELETE FROM product;
INSERT INTO product (name, price) VALUES
  ('TPS Report Cover',   5.00),
  ('Red Stapler',       45.00),
  ('Laser Printer',   1200.00),
  ('Flair (37 pieces)', 90.00),
  ('Coffee Mug',        12.00);
