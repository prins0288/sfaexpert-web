-- ===========================================================================
-- 71_brand_image_default.sql — PER TENANT database
--
-- Brand image rules:
--   * an uploaded image keeps the user's own file name (brandMaster/Dolo 650.jpg);
--   * a brand without an uploaded image gets its own copy of the default
--     "no image" picture named after the brand (brandMaster/<Brand Name>.jpg),
--     marked image_is_default = 1. Older brands with no image get their copy
--     automatically the next time the Brand page is opened.
--
-- Safe to re-run (column added only when missing).
-- Run:  mysql -u myroot < src/main/resources/sql/71_brand_image_default.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_is_default');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = automatic no-image picture'' AFTER thumbnail_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== globex_db ====================
USE globex_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_is_default');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = automatic no-image picture'' AFTER thumbnail_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== initech_db ====================
USE initech_db;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_is_default');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = automatic no-image picture'' AFTER thumbnail_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ==================== sfa_demo ====================
USE sfa_demo;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_is_default');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = automatic no-image picture'' AFTER thumbnail_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
