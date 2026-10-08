-- ============================================================================
-- 44_client_type_is_active.sql   (TENANT db: run in acme_db, globex_db,
-- initech_db, sfa_demo)
--
-- Converts client_type.status from CHAR(1) 'Y'/'N' to a TINYINT(1) is_active
-- column, matching the convention used by the newer masters and by the
-- already-converted visit_type_master / activity_type_master.
--
-- Checked before this migration: no other table/query reads client_type.status
-- (Client's @Formula columns and DcrRepository's join only read type_name /
-- singular_label; ClientService.resolveClientType matches by code/name, not
-- status). Only in-app consumers were ClientType itself and LookupController's
-- "clientTypes" lookup list, both updated in code alongside this migration.
--
-- Idempotent: only runs if is_active is not already present.
-- ============================================================================

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'client_type' AND column_name = 'is_active');
SET @sql := IF(@has_col = 0,
    'ALTER TABLE client_type ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER plural_label',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_col = 0,
    'UPDATE client_type SET is_active = IF(status IN (''Y'',''y'',''1'',''true''), 1, 0)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_status := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'client_type' AND column_name = 'status');
SET @sql := IF(@has_status > 0, 'ALTER TABLE client_type DROP COLUMN status', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
