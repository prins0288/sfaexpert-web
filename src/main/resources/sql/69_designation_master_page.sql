-- ===========================================================================
-- 69_designation_master_page.sql — PER TENANT database
--
-- Masters > Master Entry > Create Designation (master/designation-master.html):
--   1. designation_master.status becomes TINYINT(1) (1 = active, 0 = inactive)
--      where it is still the old CHAR(1) 'Y'/'N' — values are converted, not lost.
--   2. Permission codes DESIGNATION_SAVE / DESIGNATION_STATUS.
--   3. The placeholder "Create Designation" menu item is wired to the page.
--
-- Check afterwards: "inactive_designations" per DB. If a DB shows ALL its
-- designations inactive, its status was probably zeroed by an earlier
-- CHAR -> TINYINT change; reactivate them with
--     UPDATE designation_master SET status = 1;
--
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/69_designation_master_page.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
SET @type := (SELECT DATA_TYPE FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'status');
SET @sql := IF(@type IN ('char', 'varchar'),
    'UPDATE designation_master SET status = IF(UPPER(status) IN (''Y'', ''1'', ''A''), ''1'', ''0'')', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@type IN ('char', 'varchar'),
    'ALTER TABLE designation_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, COUNT(*) AS designations, SUM(status = 0) AS inactive_designations FROM designation_master;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('DESIGNATION_SAVE', 'Designation', 'Add / edit Designation', 1, NOW(), 'seed'),
    ('DESIGNATION_STATUS', 'Designation', 'Activate / deactivate Designation', 1, NOW(), 'seed');

UPDATE company_menu_self
SET page = 'designationmaster', href = 'master/designation-master.html'
WHERE label = 'Create Designation' AND (href IS NULL OR href = '' OR href = 'master/designation-master.html');

-- ==================== globex_db ====================
USE globex_db;
SET @type := (SELECT DATA_TYPE FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'status');
SET @sql := IF(@type IN ('char', 'varchar'),
    'UPDATE designation_master SET status = IF(UPPER(status) IN (''Y'', ''1'', ''A''), ''1'', ''0'')', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@type IN ('char', 'varchar'),
    'ALTER TABLE designation_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, COUNT(*) AS designations, SUM(status = 0) AS inactive_designations FROM designation_master;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('DESIGNATION_SAVE', 'Designation', 'Add / edit Designation', 1, NOW(), 'seed'),
    ('DESIGNATION_STATUS', 'Designation', 'Activate / deactivate Designation', 1, NOW(), 'seed');

UPDATE company_menu_self
SET page = 'designationmaster', href = 'master/designation-master.html'
WHERE label = 'Create Designation' AND (href IS NULL OR href = '' OR href = 'master/designation-master.html');

-- ==================== initech_db ====================
USE initech_db;
SET @type := (SELECT DATA_TYPE FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'status');
SET @sql := IF(@type IN ('char', 'varchar'),
    'UPDATE designation_master SET status = IF(UPPER(status) IN (''Y'', ''1'', ''A''), ''1'', ''0'')', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@type IN ('char', 'varchar'),
    'ALTER TABLE designation_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, COUNT(*) AS designations, SUM(status = 0) AS inactive_designations FROM designation_master;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('DESIGNATION_SAVE', 'Designation', 'Add / edit Designation', 1, NOW(), 'seed'),
    ('DESIGNATION_STATUS', 'Designation', 'Activate / deactivate Designation', 1, NOW(), 'seed');

UPDATE company_menu_self
SET page = 'designationmaster', href = 'master/designation-master.html'
WHERE label = 'Create Designation' AND (href IS NULL OR href = '' OR href = 'master/designation-master.html');

-- ==================== sfa_demo ====================
USE sfa_demo;
SET @type := (SELECT DATA_TYPE FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'designation_master' AND COLUMN_NAME = 'status');
SET @sql := IF(@type IN ('char', 'varchar'),
    'UPDATE designation_master SET status = IF(UPPER(status) IN (''Y'', ''1'', ''A''), ''1'', ''0'')', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@type IN ('char', 'varchar'),
    'ALTER TABLE designation_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, COUNT(*) AS designations, SUM(status = 0) AS inactive_designations FROM designation_master;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('DESIGNATION_SAVE', 'Designation', 'Add / edit Designation', 1, NOW(), 'seed'),
    ('DESIGNATION_STATUS', 'Designation', 'Activate / deactivate Designation', 1, NOW(), 'seed');

UPDATE company_menu_self
SET page = 'designationmaster', href = 'master/designation-master.html'
WHERE label = 'Create Designation' AND (href IS NULL OR href = '' OR href = 'master/designation-master.html');
