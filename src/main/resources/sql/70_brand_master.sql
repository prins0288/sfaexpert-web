-- ===========================================================================
-- 70_brand_master.sql — PER TENANT database
--
-- Masters > Catalog > Brand (master/brand-master.html).
--   1. brand_master (your DDL) + image columns. The image is stored on disk by
--      FileUploadService under <app.storage.base-dir>/<companyCode>/brandMaster/
--      and its thumbnail under .../brandMaster/thumbnail/; the table keeps the
--      relative paths and the MIME types as plain text.
--      If brand_master already exists (created from the DDL), only the missing
--      image columns are added.
--   2. Permission codes BRAND_SAVE / BRAND_STATUS.
--   3. Menu item "Brand" under Masters > Catalog (id = next free id).
--
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/70_brand_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
CREATE TABLE IF NOT EXISTS brand_master (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  brand_code      VARCHAR(30)  NOT NULL,
  brand_name      VARCHAR(150) NOT NULL,
  image_path      VARCHAR(255) NULL COMMENT 'relative path, e.g. brandMaster/ab12.png',
  image_type      VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/png',
  image_size      BIGINT       NULL COMMENT 'bytes stored',
  image_name      VARCHAR(255) NULL COMMENT 'original file name',
  thumbnail_path  VARCHAR(255) NULL COMMENT 'e.g. brandMaster/thumbnail/ab12_thumb.jpg',
  thumbnail_type  VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/jpeg',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_brand_code (brand_code),
  UNIQUE KEY uk_brand_name (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_path VARCHAR(255) NULL COMMENT ''relative path, e.g. brandMaster/ab12.png'' AFTER brand_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/png'' AFTER image_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_size');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_size BIGINT NULL COMMENT ''bytes stored'' AFTER image_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_name');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_name VARCHAR(255) NULL COMMENT ''original file name'' AFTER image_size', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_path VARCHAR(255) NULL COMMENT ''e.g. brandMaster/thumbnail/ab12_thumb.jpg'' AFTER image_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/jpeg'' AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('BRAND_SAVE', 'Brand', 'Add / edit Brand', 1, NOW(), 'seed'),
    ('BRAND_STATUS', 'Brand', 'Activate / deactivate Brand', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Brand', 'Brand Master', 'award',
       'brandmaster', 'master/brand-master.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'brandmaster');

-- ==================== globex_db ====================
USE globex_db;
CREATE TABLE IF NOT EXISTS brand_master (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  brand_code      VARCHAR(30)  NOT NULL,
  brand_name      VARCHAR(150) NOT NULL,
  image_path      VARCHAR(255) NULL COMMENT 'relative path, e.g. brandMaster/ab12.png',
  image_type      VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/png',
  image_size      BIGINT       NULL COMMENT 'bytes stored',
  image_name      VARCHAR(255) NULL COMMENT 'original file name',
  thumbnail_path  VARCHAR(255) NULL COMMENT 'e.g. brandMaster/thumbnail/ab12_thumb.jpg',
  thumbnail_type  VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/jpeg',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_brand_code (brand_code),
  UNIQUE KEY uk_brand_name (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_path VARCHAR(255) NULL COMMENT ''relative path, e.g. brandMaster/ab12.png'' AFTER brand_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/png'' AFTER image_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_size');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_size BIGINT NULL COMMENT ''bytes stored'' AFTER image_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_name');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_name VARCHAR(255) NULL COMMENT ''original file name'' AFTER image_size', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_path VARCHAR(255) NULL COMMENT ''e.g. brandMaster/thumbnail/ab12_thumb.jpg'' AFTER image_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/jpeg'' AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('BRAND_SAVE', 'Brand', 'Add / edit Brand', 1, NOW(), 'seed'),
    ('BRAND_STATUS', 'Brand', 'Activate / deactivate Brand', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Brand', 'Brand Master', 'award',
       'brandmaster', 'master/brand-master.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'brandmaster');

-- ==================== initech_db ====================
USE initech_db;
CREATE TABLE IF NOT EXISTS brand_master (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  brand_code      VARCHAR(30)  NOT NULL,
  brand_name      VARCHAR(150) NOT NULL,
  image_path      VARCHAR(255) NULL COMMENT 'relative path, e.g. brandMaster/ab12.png',
  image_type      VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/png',
  image_size      BIGINT       NULL COMMENT 'bytes stored',
  image_name      VARCHAR(255) NULL COMMENT 'original file name',
  thumbnail_path  VARCHAR(255) NULL COMMENT 'e.g. brandMaster/thumbnail/ab12_thumb.jpg',
  thumbnail_type  VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/jpeg',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_brand_code (brand_code),
  UNIQUE KEY uk_brand_name (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_path VARCHAR(255) NULL COMMENT ''relative path, e.g. brandMaster/ab12.png'' AFTER brand_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/png'' AFTER image_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_size');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_size BIGINT NULL COMMENT ''bytes stored'' AFTER image_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_name');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_name VARCHAR(255) NULL COMMENT ''original file name'' AFTER image_size', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_path VARCHAR(255) NULL COMMENT ''e.g. brandMaster/thumbnail/ab12_thumb.jpg'' AFTER image_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/jpeg'' AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('BRAND_SAVE', 'Brand', 'Add / edit Brand', 1, NOW(), 'seed'),
    ('BRAND_STATUS', 'Brand', 'Activate / deactivate Brand', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Brand', 'Brand Master', 'award',
       'brandmaster', 'master/brand-master.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'brandmaster');

-- ==================== sfa_demo ====================
USE sfa_demo;
CREATE TABLE IF NOT EXISTS brand_master (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  brand_code      VARCHAR(30)  NOT NULL,
  brand_name      VARCHAR(150) NOT NULL,
  image_path      VARCHAR(255) NULL COMMENT 'relative path, e.g. brandMaster/ab12.png',
  image_type      VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/png',
  image_size      BIGINT       NULL COMMENT 'bytes stored',
  image_name      VARCHAR(255) NULL COMMENT 'original file name',
  thumbnail_path  VARCHAR(255) NULL COMMENT 'e.g. brandMaster/thumbnail/ab12_thumb.jpg',
  thumbnail_type  VARCHAR(100) NULL COMMENT 'MIME type as text, e.g. image/jpeg',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_brand_code (brand_code),
  UNIQUE KEY uk_brand_name (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_path VARCHAR(255) NULL COMMENT ''relative path, e.g. brandMaster/ab12.png'' AFTER brand_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/png'' AFTER image_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_size');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_size BIGINT NULL COMMENT ''bytes stored'' AFTER image_type', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'image_name');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN image_name VARCHAR(255) NULL COMMENT ''original file name'' AFTER image_size', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_path VARCHAR(255) NULL COMMENT ''e.g. brandMaster/thumbnail/ab12_thumb.jpg'' AFTER image_name', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'brand_master' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE brand_master ADD COLUMN thumbnail_type VARCHAR(100) NULL COMMENT ''MIME type as text, e.g. image/jpeg'' AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('BRAND_SAVE', 'Brand', 'Add / edit Brand', 1, NOW(), 'seed'),
    ('BRAND_STATUS', 'Brand', 'Activate / deactivate Brand', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Brand', 'Brand Master', 'award',
       'brandmaster', 'master/brand-master.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'brandmaster');
