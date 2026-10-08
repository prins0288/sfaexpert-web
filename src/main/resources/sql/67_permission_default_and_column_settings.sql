-- ===========================================================================
-- 67_permission_default_and_column_settings.sql — PER TENANT database
--
-- 1. permission_master.default_allowed: what a user gets when NO
--    permission_assignment row matches them. 1 (default for every existing
--    code) = allowed as before; 0 = denied until someone is granted it.
-- 2. COLUMN_SETTINGS (default DENIED): only employees / designations /
--    emp levels you Allow in Permission Master see the "Column Settings"
--    button and can save / reset a report's column layout. SUPER_ADMIN
--    always has it.
--
-- Requires 13_permission_master.sql. Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/67_permission_default_and_column_settings.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_master' AND COLUMN_NAME = 'default_allowed');
SET @sql := IF(@has = 0, 'ALTER TABLE permission_master ADD COLUMN default_allowed TINYINT(1) NOT NULL DEFAULT 1 AFTER status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by)
VALUES ('COLUMN_SETTINGS', 'Reports', 'Change / reset the column layout of reports and grids (whole company)', 1, 0, NOW(), 'seed')
ON DUPLICATE KEY UPDATE default_allowed = 0;

-- ==================== globex_db ====================
USE globex_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_master' AND COLUMN_NAME = 'default_allowed');
SET @sql := IF(@has = 0, 'ALTER TABLE permission_master ADD COLUMN default_allowed TINYINT(1) NOT NULL DEFAULT 1 AFTER status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by)
VALUES ('COLUMN_SETTINGS', 'Reports', 'Change / reset the column layout of reports and grids (whole company)', 1, 0, NOW(), 'seed')
ON DUPLICATE KEY UPDATE default_allowed = 0;

-- ==================== initech_db ====================
USE initech_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_master' AND COLUMN_NAME = 'default_allowed');
SET @sql := IF(@has = 0, 'ALTER TABLE permission_master ADD COLUMN default_allowed TINYINT(1) NOT NULL DEFAULT 1 AFTER status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by)
VALUES ('COLUMN_SETTINGS', 'Reports', 'Change / reset the column layout of reports and grids (whole company)', 1, 0, NOW(), 'seed')
ON DUPLICATE KEY UPDATE default_allowed = 0;

-- ==================== sfa_demo ====================
USE sfa_demo;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_master' AND COLUMN_NAME = 'default_allowed');
SET @sql := IF(@has = 0, 'ALTER TABLE permission_master ADD COLUMN default_allowed TINYINT(1) NOT NULL DEFAULT 1 AFTER status', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by)
VALUES ('COLUMN_SETTINGS', 'Reports', 'Change / reset the column layout of reports and grids (whole company)', 1, 0, NOW(), 'seed')
ON DUPLICATE KEY UPDATE default_allowed = 0;
