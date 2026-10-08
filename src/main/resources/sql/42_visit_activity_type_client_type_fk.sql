-- ============================================================================
-- 42_visit_activity_type_client_type_fk.sql   (TENANT db: run in acme_db,
-- globex_db, initech_db, sfa_demo)
--
-- Links visit_type_master and activity_type_master to client_type: every Visit
-- Type / Activity Type row belongs to one Client Type (Doctor/Chemist/
-- Stockist...), so both master pages can show + filter by Client Type (the
-- existing per-column Filter button picks this up automatically once the
-- column exists — no extra filter code needed).
--
-- Idempotent: each ALTER only runs if the column is not already present.
-- ============================================================================

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'visit_type_master' AND column_name = 'client_type_id');
SET @sql := IF(@has_col = 0,
    'ALTER TABLE visit_type_master ADD COLUMN client_type_id BIGINT NULL AFTER plural_label, ADD KEY idx_visit_type_client_type (client_type_id), ADD CONSTRAINT fk_visit_type_client_type FOREIGN KEY (client_type_id) REFERENCES client_type(oid) ON DELETE SET NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col2 := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'activity_type_master' AND column_name = 'client_type_id');
SET @sql2 := IF(@has_col2 = 0,
    'ALTER TABLE activity_type_master ADD COLUMN client_type_id BIGINT NULL AFTER plural_label, ADD KEY idx_activity_type_client_type (client_type_id), ADD CONSTRAINT fk_activity_type_client_type FOREIGN KEY (client_type_id) REFERENCES client_type(oid) ON DELETE SET NULL',
    'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
