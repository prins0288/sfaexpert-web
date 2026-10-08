-- ============================================================================
-- 45_visit_activity_type_drop_labels.sql   (TENANT db: run in acme_db,
-- globex_db, initech_db, sfa_demo)
--
-- Visit Type and Activity Type never needed the singular/plural label pair
-- (that's a Client Type concept, not theirs) — dropping both columns.
--
-- Idempotent: each DROP only runs if the column is still present.
-- ============================================================================

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'visit_type_master' AND column_name = 'singular_label');
SET @sql := IF(@has_col > 0, 'ALTER TABLE visit_type_master DROP COLUMN singular_label', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'visit_type_master' AND column_name = 'plural_label');
SET @sql := IF(@has_col > 0, 'ALTER TABLE visit_type_master DROP COLUMN plural_label', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


SET @has_col2 := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'activity_type_master' AND column_name = 'singular_label');
SET @sql2 := IF(@has_col2 > 0, 'ALTER TABLE activity_type_master DROP COLUMN singular_label', 'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @has_col2 := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'activity_type_master' AND column_name = 'plural_label');
SET @sql2 := IF(@has_col2 > 0, 'ALTER TABLE activity_type_master DROP COLUMN plural_label', 'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
