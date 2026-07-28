-- ===========================================================================
-- Migrate category_master.status: CHAR(1) 'Y'/'N'  ->  TINYINT(1) 1/0.
-- Safe to run repeatedly (no-op once already TINYINT). Per tenant DB.
--
-- Run:  mysql -u myroot < src/main/resources/sql/11_category_status_tinyint.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
-- convert category_master.status from CHAR(1) 'Y'/'N' to TINYINT(1) 1/0.
-- Idempotent: if the column is already TINYINT the block is a no-op.
SET @needs := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'category_master'
      AND column_name = 'status' AND data_type IN ('char','varchar'));
SET @u := IF(@needs > 0,
    "UPDATE category_master SET status = IF(status IN ('Y','y','1','true'), '1', '0')", 'DO 0');
PREPARE s FROM @u; EXECUTE s; DEALLOCATE PREPARE s;
SET @m := IF(@needs > 0,
    'ALTER TABLE category_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @m; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== globex_db ====================
USE globex_db;
-- convert category_master.status from CHAR(1) 'Y'/'N' to TINYINT(1) 1/0.
-- Idempotent: if the column is already TINYINT the block is a no-op.
SET @needs := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'category_master'
      AND column_name = 'status' AND data_type IN ('char','varchar'));
SET @u := IF(@needs > 0,
    "UPDATE category_master SET status = IF(status IN ('Y','y','1','true'), '1', '0')", 'DO 0');
PREPARE s FROM @u; EXECUTE s; DEALLOCATE PREPARE s;
SET @m := IF(@needs > 0,
    'ALTER TABLE category_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @m; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== initech_db ====================
USE initech_db;
-- convert category_master.status from CHAR(1) 'Y'/'N' to TINYINT(1) 1/0.
-- Idempotent: if the column is already TINYINT the block is a no-op.
SET @needs := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'category_master'
      AND column_name = 'status' AND data_type IN ('char','varchar'));
SET @u := IF(@needs > 0,
    "UPDATE category_master SET status = IF(status IN ('Y','y','1','true'), '1', '0')", 'DO 0');
PREPARE s FROM @u; EXECUTE s; DEALLOCATE PREPARE s;
SET @m := IF(@needs > 0,
    'ALTER TABLE category_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @m; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== sfa_demo ====================
USE sfa_demo;
-- convert category_master.status from CHAR(1) 'Y'/'N' to TINYINT(1) 1/0.
-- Idempotent: if the column is already TINYINT the block is a no-op.
SET @needs := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'category_master'
      AND column_name = 'status' AND data_type IN ('char','varchar'));
SET @u := IF(@needs > 0,
    "UPDATE category_master SET status = IF(status IN ('Y','y','1','true'), '1', '0')", 'DO 0');
PREPARE s FROM @u; EXECUTE s; DEALLOCATE PREPARE s;
SET @m := IF(@needs > 0,
    'ALTER TABLE category_master MODIFY status TINYINT(1) NOT NULL DEFAULT 1', 'DO 0');
PREPARE s FROM @m; EXECUTE s; DEALLOCATE PREPARE s;

