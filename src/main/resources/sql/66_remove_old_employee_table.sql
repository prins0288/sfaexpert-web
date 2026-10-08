-- ===========================================================================
-- 66_remove_old_employee_table.sql — PER TENANT database
--
-- Removes the old `employee` table (acme_db: `employee_legacy_backup`) from
-- the app. Employees now live ONLY in emp_detail + the emp_* tables.
--
--   1. dcr.employee_oid (-> employee.oid)  becomes  dcr.emp_id (-> emp_detail.emp_id),
--      back-filled from the old table's oid -> emp_id.
--   2. The "Employee Master" menu item (master/employee.html) is removed,
--      with its favourites / visibility rows.
--   3. The EMPLOYEE_SAVE / EMPLOYEE_STATUS permission codes are removed
--      (their controller is gone).
--   4. The old table is DROPPED.
--
-- SAFE TO RE-RUN. Steps 1's column drop and 4 run ONLY when every dcr row
-- got an emp_id that exists in emp_detail — otherwise nothing is dropped and
-- the "unmapped_dcr_rows" result shows how many rows need fixing first:
--     SELECT * FROM dcr WHERE emp_id IS NULL
--        OR emp_id NOT IN (SELECT emp_id FROM emp_detail);
--
-- TAKE A BACKUP FIRST (the DROP cannot be undone):
--     mysqldump -u myroot --databases acme_db globex_db initech_db sfa_demo > before_66.sql
-- Run:
--     mysql -u myroot < src/main/resources/sql/66_remove_old_employee_table.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;

-- 1a. dcr.emp_id
SET @has_emp := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'emp_id');
SET @sql := IF(@has_emp = 0, 'ALTER TABLE dcr ADD COLUMN emp_id VARCHAR(40) NULL AFTER oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- same charset/collation as emp_detail.emp_id, or the join fails with
-- "Illegal mix of collations" (dcr and emp_detail were created with different defaults)
SET @emp_cs := (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_coll := (SELECT COLLATION_NAME FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_type := CONCAT('VARCHAR(40) CHARACTER SET ', @emp_cs, ' COLLATE ', @emp_coll);
SET @sql := CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1b. back-fill from whichever old table still exists
SET @has_old_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'employee_oid');
SET @src := (SELECT TABLE_NAME FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('employee', 'employee_legacy_backup')
             ORDER BY TABLE_NAME = 'employee' DESC LIMIT 1);
SET @sql := IF(@has_old_col = 1 AND @src IS NOT NULL,
    CONCAT('UPDATE dcr d JOIN `', @src, '` e ON e.oid = d.employee_oid SET d.emp_id = e.emp_id WHERE d.emp_id IS NULL'),
    'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1c. only when every row is mapped to a real emp_detail employee
SET @unmapped := (SELECT COUNT(*) FROM dcr d LEFT JOIN emp_detail e ON e.emp_id = d.emp_id WHERE e.emp_id IS NULL);
SELECT DATABASE() AS db, @unmapped AS unmapped_dcr_rows;

SET @has_old_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_employee');
SET @sql := IF(@unmapped = 0 AND @has_old_idx > 0, 'ALTER TABLE dcr DROP INDEX idx_dcr_employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0 AND @has_old_col = 1, 'ALTER TABLE dcr DROP COLUMN employee_oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0, CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NOT NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_new_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_emp');
SET @sql := IF(@has_new_idx = 0, 'ALTER TABLE dcr ADD INDEX idx_dcr_emp (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. "Employee Master" menu item
DELETE FROM user_menu_favorite WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM menu_visibility    WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM company_menu_self  WHERE href = 'master/employee.html';

-- 3. permission codes of the removed Employee controller
DELETE FROM permission_assignment WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');
DELETE FROM permission_master     WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');

-- 4. the old table itself
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee_legacy_backup', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== globex_db ====================
USE globex_db;

-- 1a. dcr.emp_id
SET @has_emp := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'emp_id');
SET @sql := IF(@has_emp = 0, 'ALTER TABLE dcr ADD COLUMN emp_id VARCHAR(40) NULL AFTER oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- same charset/collation as emp_detail.emp_id, or the join fails with
-- "Illegal mix of collations" (dcr and emp_detail were created with different defaults)
SET @emp_cs := (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_coll := (SELECT COLLATION_NAME FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_type := CONCAT('VARCHAR(40) CHARACTER SET ', @emp_cs, ' COLLATE ', @emp_coll);
SET @sql := CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1b. back-fill from whichever old table still exists
SET @has_old_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'employee_oid');
SET @src := (SELECT TABLE_NAME FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('employee', 'employee_legacy_backup')
             ORDER BY TABLE_NAME = 'employee' DESC LIMIT 1);
SET @sql := IF(@has_old_col = 1 AND @src IS NOT NULL,
    CONCAT('UPDATE dcr d JOIN `', @src, '` e ON e.oid = d.employee_oid SET d.emp_id = e.emp_id WHERE d.emp_id IS NULL'),
    'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1c. only when every row is mapped to a real emp_detail employee
SET @unmapped := (SELECT COUNT(*) FROM dcr d LEFT JOIN emp_detail e ON e.emp_id = d.emp_id WHERE e.emp_id IS NULL);
SELECT DATABASE() AS db, @unmapped AS unmapped_dcr_rows;

SET @has_old_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_employee');
SET @sql := IF(@unmapped = 0 AND @has_old_idx > 0, 'ALTER TABLE dcr DROP INDEX idx_dcr_employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0 AND @has_old_col = 1, 'ALTER TABLE dcr DROP COLUMN employee_oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0, CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NOT NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_new_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_emp');
SET @sql := IF(@has_new_idx = 0, 'ALTER TABLE dcr ADD INDEX idx_dcr_emp (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. "Employee Master" menu item
DELETE FROM user_menu_favorite WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM menu_visibility    WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM company_menu_self  WHERE href = 'master/employee.html';

-- 3. permission codes of the removed Employee controller
DELETE FROM permission_assignment WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');
DELETE FROM permission_master     WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');

-- 4. the old table itself
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee_legacy_backup', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== initech_db ====================
USE initech_db;

-- 1a. dcr.emp_id
SET @has_emp := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'emp_id');
SET @sql := IF(@has_emp = 0, 'ALTER TABLE dcr ADD COLUMN emp_id VARCHAR(40) NULL AFTER oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- same charset/collation as emp_detail.emp_id, or the join fails with
-- "Illegal mix of collations" (dcr and emp_detail were created with different defaults)
SET @emp_cs := (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_coll := (SELECT COLLATION_NAME FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_type := CONCAT('VARCHAR(40) CHARACTER SET ', @emp_cs, ' COLLATE ', @emp_coll);
SET @sql := CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1b. back-fill from whichever old table still exists
SET @has_old_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'employee_oid');
SET @src := (SELECT TABLE_NAME FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('employee', 'employee_legacy_backup')
             ORDER BY TABLE_NAME = 'employee' DESC LIMIT 1);
SET @sql := IF(@has_old_col = 1 AND @src IS NOT NULL,
    CONCAT('UPDATE dcr d JOIN `', @src, '` e ON e.oid = d.employee_oid SET d.emp_id = e.emp_id WHERE d.emp_id IS NULL'),
    'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1c. only when every row is mapped to a real emp_detail employee
SET @unmapped := (SELECT COUNT(*) FROM dcr d LEFT JOIN emp_detail e ON e.emp_id = d.emp_id WHERE e.emp_id IS NULL);
SELECT DATABASE() AS db, @unmapped AS unmapped_dcr_rows;

SET @has_old_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_employee');
SET @sql := IF(@unmapped = 0 AND @has_old_idx > 0, 'ALTER TABLE dcr DROP INDEX idx_dcr_employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0 AND @has_old_col = 1, 'ALTER TABLE dcr DROP COLUMN employee_oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0, CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NOT NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_new_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_emp');
SET @sql := IF(@has_new_idx = 0, 'ALTER TABLE dcr ADD INDEX idx_dcr_emp (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. "Employee Master" menu item
DELETE FROM user_menu_favorite WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM menu_visibility    WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM company_menu_self  WHERE href = 'master/employee.html';

-- 3. permission codes of the removed Employee controller
DELETE FROM permission_assignment WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');
DELETE FROM permission_master     WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');

-- 4. the old table itself
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee_legacy_backup', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== sfa_demo ====================
USE sfa_demo;

-- 1a. dcr.emp_id
SET @has_emp := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'emp_id');
SET @sql := IF(@has_emp = 0, 'ALTER TABLE dcr ADD COLUMN emp_id VARCHAR(40) NULL AFTER oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- same charset/collation as emp_detail.emp_id, or the join fails with
-- "Illegal mix of collations" (dcr and emp_detail were created with different defaults)
SET @emp_cs := (SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_coll := (SELECT COLLATION_NAME FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id');
SET @emp_type := CONCAT('VARCHAR(40) CHARACTER SET ', @emp_cs, ' COLLATE ', @emp_coll);
SET @sql := CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NULL');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1b. back-fill from whichever old table still exists
SET @has_old_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND COLUMN_NAME = 'employee_oid');
SET @src := (SELECT TABLE_NAME FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('employee', 'employee_legacy_backup')
             ORDER BY TABLE_NAME = 'employee' DESC LIMIT 1);
SET @sql := IF(@has_old_col = 1 AND @src IS NOT NULL,
    CONCAT('UPDATE dcr d JOIN `', @src, '` e ON e.oid = d.employee_oid SET d.emp_id = e.emp_id WHERE d.emp_id IS NULL'),
    'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1c. only when every row is mapped to a real emp_detail employee
SET @unmapped := (SELECT COUNT(*) FROM dcr d LEFT JOIN emp_detail e ON e.emp_id = d.emp_id WHERE e.emp_id IS NULL);
SELECT DATABASE() AS db, @unmapped AS unmapped_dcr_rows;

SET @has_old_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_employee');
SET @sql := IF(@unmapped = 0 AND @has_old_idx > 0, 'ALTER TABLE dcr DROP INDEX idx_dcr_employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0 AND @has_old_col = 1, 'ALTER TABLE dcr DROP COLUMN employee_oid', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF(@unmapped = 0, CONCAT('ALTER TABLE dcr MODIFY emp_id ', @emp_type, ' NOT NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_new_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dcr' AND INDEX_NAME = 'idx_dcr_emp');
SET @sql := IF(@has_new_idx = 0, 'ALTER TABLE dcr ADD INDEX idx_dcr_emp (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. "Employee Master" menu item
DELETE FROM user_menu_favorite WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM menu_visibility    WHERE menu_item_id IN (SELECT id FROM company_menu_self WHERE href = 'master/employee.html');
DELETE FROM company_menu_self  WHERE href = 'master/employee.html';

-- 3. permission codes of the removed Employee controller
DELETE FROM permission_assignment WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');
DELETE FROM permission_master     WHERE permission_code IN ('EMPLOYEE_SAVE', 'EMPLOYEE_STATUS');

-- 4. the old table itself
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@unmapped = 0, 'DROP TABLE IF EXISTS employee_legacy_backup', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
