-- ===========================================================================
-- COMMON DATABASE  (holds users + tenant connection details)
-- ===========================================================================
CREATE DATABASE IF NOT EXISTS common_db;
USE common_db;

CREATE TABLE IF NOT EXISTS tenant_config (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL UNIQUE,
    db_url            VARCHAR(500) NOT NULL,
    db_username       VARCHAR(128) NOT NULL,
    db_password       VARCHAR(256) NOT NULL,
    driver_class_name VARCHAR(200),
    max_pool_size     INT
);

CREATE TABLE IF NOT EXISTS app_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(128) NOT NULL UNIQUE,
    password   VARCHAR(256) NOT NULL,          -- BCrypt hash
    tenant_id  VARCHAR(64)  NOT NULL,
    enabled    TINYINT(1)   NOT NULL DEFAULT 1
);

-- ---------------------------------------------------------------------------
-- Seed: two tenants, each pointing at its own database
-- ---------------------------------------------------------------------------
INSERT INTO tenant_config (tenant_id, db_url, db_username, db_password, driver_class_name, max_pool_size)
VALUES
  ('tenant_a', 'jdbc:mysql://localhost:3306/tenant_a_db?useSSL=false&serverTimezone=UTC', 'root', 'root', 'com.mysql.cj.jdbc.Driver', 5),
  ('tenant_b', 'jdbc:mysql://localhost:3306/tenant_b_db?useSSL=false&serverTimezone=UTC', 'root', 'root', 'com.mysql.cj.jdbc.Driver', 5);

-- Password for both users below is:  password123
-- (BCrypt hash generated with strength 10)
INSERT INTO app_user (username, password, tenant_id, enabled)
VALUES
  ('alice', '$2b$10$bjMxAXZ/6izsNcMAUnzIeewJlMsgMc8b/QgHXpZwZLNzKFJYsbv5C', 'tenant_a', 1),
  ('bob',   '$2b$10$bjMxAXZ/6izsNcMAUnzIeewJlMsgMc8b/QgHXpZwZLNzKFJYsbv5C', 'tenant_b', 1);
