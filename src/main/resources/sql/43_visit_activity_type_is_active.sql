-- ============================================================================
-- 43_visit_activity_type_is_active.sql   (TENANT db: run in acme_db,
-- globex_db, initech_db, sfa_demo)
--
-- Converts visit_type_master.status and activity_type_master.status from
-- CHAR(1) 'Y'/'N' to a TINYINT(1) is_active column, matching the convention
-- used by the newer masters (degree_master, item_type_master, bank_master...).
-- client_type ITSELF is intentionally left untouched here — it's a much older,
-- more widely-referenced table; converting it is scoped to a separate,
-- carefully-checked migration.
--
-- Idempotent: each table's block only runs if is_active is not already present.
-- ============================================================================

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'visit_type_master' AND column_name = 'is_active');
SET @sql := IF(@has_col = 0,
    'ALTER TABLE visit_type_master ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER client_type_id',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_col = 0,
    'UPDATE visit_type_master SET is_active = IF(status IN (''Y'',''y'',''1'',''true''), 1, 0)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_status := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'visit_type_master' AND column_name = 'status');
SET @sql := IF(@has_status > 0, 'ALTER TABLE visit_type_master DROP COLUMN status', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


SET @has_col2 := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'activity_type_master' AND column_name = 'is_active');
SET @sql2 := IF(@has_col2 = 0,
    'ALTER TABLE activity_type_master ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER client_type_id',
    'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @sql2 := IF(@has_col2 = 0,
    'UPDATE activity_type_master SET is_active = IF(status IN (''Y'',''y'',''1'',''true''), 1, 0)',
    'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @has_status2 := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'activity_type_master' AND column_name = 'status');
SET @sql2 := IF(@has_status2 > 0, 'ALTER TABLE activity_type_master DROP COLUMN status', 'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
