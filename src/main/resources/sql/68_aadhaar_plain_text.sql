-- ===========================================================================
-- 68_aadhaar_plain_text.sql — PER TENANT database
--
-- Aadhaar is stored as PLAIN TEXT in emp_statutory.aadhaar_no (12 digits,
-- unique), like every other value in the app — no encryption.
--
-- The old encrypted columns (aadhaar_enc / aadhaar_hash / aadhaar_last4 and
-- uk_stat_aadhaar) are dropped ONLY when none of them holds data: the app has
-- no key to decrypt them, so dropping filled ones would lose those numbers.
-- If "encrypted_aadhaar_rows" is > 0 for a DB, the old columns are kept —
-- re-enter those Aadhaar numbers in the form, set the old columns to NULL,
-- then run this file again.
--
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/68_aadhaar_plain_text.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_statutory ADD COLUMN aadhaar_no CHAR(12) NULL AFTER esi_no, ADD UNIQUE KEY uk_stat_aadhaar_no (aadhaar_no)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_enc := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_enc');
SET @sql := IF(@has_enc = 1,
    'SELECT COUNT(*) INTO @filled FROM emp_statutory WHERE aadhaar_enc IS NOT NULL OR aadhaar_hash IS NOT NULL OR aadhaar_last4 IS NOT NULL',
    'SET @filled := 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, @filled AS encrypted_aadhaar_rows;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND INDEX_NAME = 'uk_stat_aadhaar');
SET @sql := IF(@filled = 0 AND @has_idx > 0, 'ALTER TABLE emp_statutory DROP INDEX uk_stat_aadhaar', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@filled = 0 AND @has_enc = 1,
    'ALTER TABLE emp_statutory DROP COLUMN aadhaar_enc, DROP COLUMN aadhaar_hash, DROP COLUMN aadhaar_last4', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== globex_db ====================
USE globex_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_statutory ADD COLUMN aadhaar_no CHAR(12) NULL AFTER esi_no, ADD UNIQUE KEY uk_stat_aadhaar_no (aadhaar_no)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_enc := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_enc');
SET @sql := IF(@has_enc = 1,
    'SELECT COUNT(*) INTO @filled FROM emp_statutory WHERE aadhaar_enc IS NOT NULL OR aadhaar_hash IS NOT NULL OR aadhaar_last4 IS NOT NULL',
    'SET @filled := 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, @filled AS encrypted_aadhaar_rows;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND INDEX_NAME = 'uk_stat_aadhaar');
SET @sql := IF(@filled = 0 AND @has_idx > 0, 'ALTER TABLE emp_statutory DROP INDEX uk_stat_aadhaar', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@filled = 0 AND @has_enc = 1,
    'ALTER TABLE emp_statutory DROP COLUMN aadhaar_enc, DROP COLUMN aadhaar_hash, DROP COLUMN aadhaar_last4', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== initech_db ====================
USE initech_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_statutory ADD COLUMN aadhaar_no CHAR(12) NULL AFTER esi_no, ADD UNIQUE KEY uk_stat_aadhaar_no (aadhaar_no)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_enc := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_enc');
SET @sql := IF(@has_enc = 1,
    'SELECT COUNT(*) INTO @filled FROM emp_statutory WHERE aadhaar_enc IS NOT NULL OR aadhaar_hash IS NOT NULL OR aadhaar_last4 IS NOT NULL',
    'SET @filled := 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, @filled AS encrypted_aadhaar_rows;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND INDEX_NAME = 'uk_stat_aadhaar');
SET @sql := IF(@filled = 0 AND @has_idx > 0, 'ALTER TABLE emp_statutory DROP INDEX uk_stat_aadhaar', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@filled = 0 AND @has_enc = 1,
    'ALTER TABLE emp_statutory DROP COLUMN aadhaar_enc, DROP COLUMN aadhaar_hash, DROP COLUMN aadhaar_last4', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== sfa_demo ====================
USE sfa_demo;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_statutory ADD COLUMN aadhaar_no CHAR(12) NULL AFTER esi_no, ADD UNIQUE KEY uk_stat_aadhaar_no (aadhaar_no)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_enc := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND COLUMN_NAME = 'aadhaar_enc');
SET @sql := IF(@has_enc = 1,
    'SELECT COUNT(*) INTO @filled FROM emp_statutory WHERE aadhaar_enc IS NOT NULL OR aadhaar_hash IS NOT NULL OR aadhaar_last4 IS NOT NULL',
    'SET @filled := 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SELECT DATABASE() AS db, @filled AS encrypted_aadhaar_rows;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_statutory' AND INDEX_NAME = 'uk_stat_aadhaar');
SET @sql := IF(@filled = 0 AND @has_idx > 0, 'ALTER TABLE emp_statutory DROP INDEX uk_stat_aadhaar', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql := IF(@filled = 0 AND @has_enc = 1,
    'ALTER TABLE emp_statutory DROP COLUMN aadhaar_enc, DROP COLUMN aadhaar_hash, DROP COLUMN aadhaar_last4', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
