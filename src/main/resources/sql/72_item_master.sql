-- ===========================================================================
-- 72_item_master.sql — PER TENANT database
--
-- Masters > Catalog > Item (master/item-master.html). Replaces the 2012
-- item_details design:
--   item_master       one row per product (item_code unique, same in every state)
--   item_rate         many rates per item: state-wise (NULL = All India) + date-wise
--   item_composition  many salts per item, typed by the user (no master)
-- Also: Item types Product / Sample / Gift / Input in item_type_master (only
-- when missing), permission codes ITEM_SAVE / ITEM_STATUS / ITEM_UPLOAD and the
-- menu item "Item" under Masters > Catalog.
--
-- Needs: item_type_master (33), brand_master (70), division_master, state_master.
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/72_item_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;

INSERT INTO item_type_master (item_type_code, item_type_name, short_name, description, display_order, is_active, created_by)
SELECT t.code, t.name, t.short, t.descr, t.ord, 1, 'seed'
FROM (SELECT 'PRODUCT' code, 'Product' name, 'PRD' short, 'Saleable product (PTS / PTR / MRP needed in rates)' descr, 1 ord
      UNION ALL SELECT 'SAMPLE', 'Sample', 'SMP', 'Free sample', 2
      UNION ALL SELECT 'GIFT', 'Gift', 'GFT', 'Gift item', 3
      UNION ALL SELECT 'INPUT', 'Input', 'INP', 'Promotional input', 4) t
WHERE NOT EXISTS (SELECT 1 FROM item_type_master m WHERE m.item_type_code = t.code OR m.item_type_name = t.name);

CREATE TABLE IF NOT EXISTS item_master (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_code          VARCHAR(50)  NOT NULL                COMMENT 'business code, same in every state (old item_code)',
  item_name          VARCHAR(200) NOT NULL,
  short_name         VARCHAR(100) NULL,
  item_type_id       BIGINT UNSIGNED NOT NULL             COMMENT '-> item_type_master.id (Product / Sample / Gift / Input ...)',
  division_id        BIGINT       NULL                    COMMENT '-> division_master.oid',
  brand_id           BIGINT UNSIGNED NULL                 COMMENT '-> brand_master.id',
  item_class         VARCHAR(100) NULL                    COMMENT 'old item_class / class',
  manufacturer       VARCHAR(150) NULL                    COMMENT 'old Company_Name',
  pack_size          VARCHAR(50)  NULL                    COMMENT 'e.g. 10x10',
  sample_pack_size   VARCHAR(50)  NULL,
  uom                VARCHAR(20)  NULL                    COMMENT 'strip / bottle / tube ...',
  hsn_code           VARCHAR(8)   NULL,
  gst_rate           DECIMAL(5,2) NULL                    COMMENT 'e.g. 12.00',
  composition_text   VARCHAR(1000) NULL                   COMMENT 'auto-built from item_composition, for lists / search',
  image_path         VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/Dolo 650.jpg (user file name kept)',
  image_type         VARCHAR(100) NULL                    COMMENT 'MIME type as text',
  image_size         BIGINT       NULL,
  image_name         VARCHAR(255) NULL                    COMMENT 'original file name',
  thumbnail_path     VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/thumbnail/Dolo 650_thumb.jpg',
  thumbnail_type     VARCHAR(100) NULL,
  item_code_main     VARCHAR(100) NULL                    COMMENT 'kept from 2012 table',
  legacy_set_id      VARCHAR(100) NULL                    COMMENT 'old set_id',
  legacy_new_set_id  VARCHAR(100) NULL                    COMMENT 'old new_set_id',
  status             TINYINT(1)   NOT NULL DEFAULT 1,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by         VARCHAR(128) NULL,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by         VARCHAR(128) NULL,
  version            INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_code (item_code),
  KEY idx_item_name (item_name),
  KEY idx_item_type_status (item_type_id, status),
  KEY idx_item_division (division_id),
  KEY idx_item_brand (brand_id),
  CONSTRAINT fk_item_type     FOREIGN KEY (item_type_id) REFERENCES item_type_master (id),
  CONSTRAINT fk_item_division FOREIGN KEY (division_id)  REFERENCES division_master (oid),
  CONSTRAINT fk_item_brand    FOREIGN KEY (brand_id)     REFERENCES brand_master (id),
  CONSTRAINT chk_item_gst CHECK (gst_rate IS NULL OR (gst_rate >= 0 AND gst_rate <= 100)),
  CONSTRAINT chk_item_hsn CHECK (hsn_code IS NULL OR REGEXP_LIKE(hsn_code, '^[0-9]{4,8}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_rate (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id         BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  state_id        BIGINT       NULL                     COMMENT '-> state_master.oid (old state_main); NULL = all states',
  state_key       BIGINT       GENERATED ALWAYS AS (IFNULL(state_id, 0)) STORED COMMENT 'lets the unique key cover All India',
  effective_from  DATE         NOT NULL,
  effective_to    DATE         NULL                     COMMENT 'NULL = current rate',
  pts             DECIMAL(12,2) NULL                    COMMENT 'price to stockist',
  ptr             DECIMAL(12,2) NULL                    COMMENT 'price to retailer',
  mrp             DECIMAL(12,2) NULL,
  purchase_rate   DECIMAL(12,2) NULL,
  sample_rate     DECIMAL(12,2) NULL,
  discount_pct    DECIMAL(5,2)  NULL,
  tax_included    TINYINT(1)   NOT NULL DEFAULT 0       COMMENT 'old include_sttx',
  legacy_item_id  VARCHAR(255) NULL                     COMMENT 'old item_newid (one product x state row)',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rate_item_state_from (item_id, state_key, effective_from),
  UNIQUE KEY uk_rate_legacy (legacy_item_id),
  KEY idx_rate_lookup (item_id, state_key, effective_from, effective_to),
  KEY idx_rate_state (state_id),
  CONSTRAINT fk_rate_item  FOREIGN KEY (item_id)  REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT fk_rate_state FOREIGN KEY (state_id) REFERENCES state_master (oid),
  CONSTRAINT chk_rate_dates    CHECK (effective_to IS NULL OR effective_to >= effective_from),
  CONSTRAINT chk_rate_positive CHECK (COALESCE(pts, 0) >= 0 AND COALESCE(ptr, 0) >= 0 AND COALESCE(mrp, 0) >= 0
                                      AND COALESCE(purchase_rate, 0) >= 0 AND COALESCE(sample_rate, 0) >= 0),
  CONSTRAINT chk_rate_order    CHECK ((mrp IS NULL OR ptr IS NULL OR mrp >= ptr) AND (ptr IS NULL OR pts IS NULL OR ptr >= pts)),
  CONSTRAINT chk_rate_discount CHECK (discount_pct IS NULL OR (discount_pct >= 0 AND discount_pct <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_composition (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id        BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  molecule_name  VARCHAR(150) NOT NULL                 COMMENT 'typed by the user, e.g. Paracetamol',
  molecule_key   VARCHAR(150) GENERATED ALWAYS AS (LOWER(TRIM(molecule_name))) STORED,
  strength       DECIMAL(12,3) NULL                    COMMENT 'e.g. 500',
  strength_unit  VARCHAR(20)  NULL                     COMMENT 'mg / mcg / g / ml / IU / % w/w',
  per_qty        VARCHAR(30)  NULL                     COMMENT 'per tablet / per 5 ml',
  sort_order     INT          NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comp_item_molecule (item_id, molecule_key),
  KEY idx_comp_molecule (molecule_key),
  CONSTRAINT fk_comp_item FOREIGN KEY (item_id) REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT chk_comp_strength CHECK (strength IS NULL OR strength > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ITEM_SAVE', 'Item', 'Add / edit Item (with rates and composition)', 1, NOW(), 'seed'),
    ('ITEM_STATUS', 'Item', 'Activate / deactivate Item', 1, NOW(), 'seed'),
    ('ITEM_UPLOAD', 'Item', 'Bulk upload Items / Rates / Composition', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Item', 'Item Master', 'capsule',
       'itemmaster', 'master/item-master.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'itemmaster');

-- ==================== globex_db ====================
USE globex_db;

INSERT INTO item_type_master (item_type_code, item_type_name, short_name, description, display_order, is_active, created_by)
SELECT t.code, t.name, t.short, t.descr, t.ord, 1, 'seed'
FROM (SELECT 'PRODUCT' code, 'Product' name, 'PRD' short, 'Saleable product (PTS / PTR / MRP needed in rates)' descr, 1 ord
      UNION ALL SELECT 'SAMPLE', 'Sample', 'SMP', 'Free sample', 2
      UNION ALL SELECT 'GIFT', 'Gift', 'GFT', 'Gift item', 3
      UNION ALL SELECT 'INPUT', 'Input', 'INP', 'Promotional input', 4) t
WHERE NOT EXISTS (SELECT 1 FROM item_type_master m WHERE m.item_type_code = t.code OR m.item_type_name = t.name);

CREATE TABLE IF NOT EXISTS item_master (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_code          VARCHAR(50)  NOT NULL                COMMENT 'business code, same in every state (old item_code)',
  item_name          VARCHAR(200) NOT NULL,
  short_name         VARCHAR(100) NULL,
  item_type_id       BIGINT UNSIGNED NOT NULL             COMMENT '-> item_type_master.id (Product / Sample / Gift / Input ...)',
  division_id        BIGINT       NULL                    COMMENT '-> division_master.oid',
  brand_id           BIGINT UNSIGNED NULL                 COMMENT '-> brand_master.id',
  item_class         VARCHAR(100) NULL                    COMMENT 'old item_class / class',
  manufacturer       VARCHAR(150) NULL                    COMMENT 'old Company_Name',
  pack_size          VARCHAR(50)  NULL                    COMMENT 'e.g. 10x10',
  sample_pack_size   VARCHAR(50)  NULL,
  uom                VARCHAR(20)  NULL                    COMMENT 'strip / bottle / tube ...',
  hsn_code           VARCHAR(8)   NULL,
  gst_rate           DECIMAL(5,2) NULL                    COMMENT 'e.g. 12.00',
  composition_text   VARCHAR(1000) NULL                   COMMENT 'auto-built from item_composition, for lists / search',
  image_path         VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/Dolo 650.jpg (user file name kept)',
  image_type         VARCHAR(100) NULL                    COMMENT 'MIME type as text',
  image_size         BIGINT       NULL,
  image_name         VARCHAR(255) NULL                    COMMENT 'original file name',
  thumbnail_path     VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/thumbnail/Dolo 650_thumb.jpg',
  thumbnail_type     VARCHAR(100) NULL,
  item_code_main     VARCHAR(100) NULL                    COMMENT 'kept from 2012 table',
  legacy_set_id      VARCHAR(100) NULL                    COMMENT 'old set_id',
  legacy_new_set_id  VARCHAR(100) NULL                    COMMENT 'old new_set_id',
  status             TINYINT(1)   NOT NULL DEFAULT 1,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by         VARCHAR(128) NULL,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by         VARCHAR(128) NULL,
  version            INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_code (item_code),
  KEY idx_item_name (item_name),
  KEY idx_item_type_status (item_type_id, status),
  KEY idx_item_division (division_id),
  KEY idx_item_brand (brand_id),
  CONSTRAINT fk_item_type     FOREIGN KEY (item_type_id) REFERENCES item_type_master (id),
  CONSTRAINT fk_item_division FOREIGN KEY (division_id)  REFERENCES division_master (oid),
  CONSTRAINT fk_item_brand    FOREIGN KEY (brand_id)     REFERENCES brand_master (id),
  CONSTRAINT chk_item_gst CHECK (gst_rate IS NULL OR (gst_rate >= 0 AND gst_rate <= 100)),
  CONSTRAINT chk_item_hsn CHECK (hsn_code IS NULL OR REGEXP_LIKE(hsn_code, '^[0-9]{4,8}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_rate (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id         BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  state_id        BIGINT       NULL                     COMMENT '-> state_master.oid (old state_main); NULL = all states',
  state_key       BIGINT       GENERATED ALWAYS AS (IFNULL(state_id, 0)) STORED COMMENT 'lets the unique key cover All India',
  effective_from  DATE         NOT NULL,
  effective_to    DATE         NULL                     COMMENT 'NULL = current rate',
  pts             DECIMAL(12,2) NULL                    COMMENT 'price to stockist',
  ptr             DECIMAL(12,2) NULL                    COMMENT 'price to retailer',
  mrp             DECIMAL(12,2) NULL,
  purchase_rate   DECIMAL(12,2) NULL,
  sample_rate     DECIMAL(12,2) NULL,
  discount_pct    DECIMAL(5,2)  NULL,
  tax_included    TINYINT(1)   NOT NULL DEFAULT 0       COMMENT 'old include_sttx',
  legacy_item_id  VARCHAR(255) NULL                     COMMENT 'old item_newid (one product x state row)',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rate_item_state_from (item_id, state_key, effective_from),
  UNIQUE KEY uk_rate_legacy (legacy_item_id),
  KEY idx_rate_lookup (item_id, state_key, effective_from, effective_to),
  KEY idx_rate_state (state_id),
  CONSTRAINT fk_rate_item  FOREIGN KEY (item_id)  REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT fk_rate_state FOREIGN KEY (state_id) REFERENCES state_master (oid),
  CONSTRAINT chk_rate_dates    CHECK (effective_to IS NULL OR effective_to >= effective_from),
  CONSTRAINT chk_rate_positive CHECK (COALESCE(pts, 0) >= 0 AND COALESCE(ptr, 0) >= 0 AND COALESCE(mrp, 0) >= 0
                                      AND COALESCE(purchase_rate, 0) >= 0 AND COALESCE(sample_rate, 0) >= 0),
  CONSTRAINT chk_rate_order    CHECK ((mrp IS NULL OR ptr IS NULL OR mrp >= ptr) AND (ptr IS NULL OR pts IS NULL OR ptr >= pts)),
  CONSTRAINT chk_rate_discount CHECK (discount_pct IS NULL OR (discount_pct >= 0 AND discount_pct <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_composition (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id        BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  molecule_name  VARCHAR(150) NOT NULL                 COMMENT 'typed by the user, e.g. Paracetamol',
  molecule_key   VARCHAR(150) GENERATED ALWAYS AS (LOWER(TRIM(molecule_name))) STORED,
  strength       DECIMAL(12,3) NULL                    COMMENT 'e.g. 500',
  strength_unit  VARCHAR(20)  NULL                     COMMENT 'mg / mcg / g / ml / IU / % w/w',
  per_qty        VARCHAR(30)  NULL                     COMMENT 'per tablet / per 5 ml',
  sort_order     INT          NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comp_item_molecule (item_id, molecule_key),
  KEY idx_comp_molecule (molecule_key),
  CONSTRAINT fk_comp_item FOREIGN KEY (item_id) REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT chk_comp_strength CHECK (strength IS NULL OR strength > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ITEM_SAVE', 'Item', 'Add / edit Item (with rates and composition)', 1, NOW(), 'seed'),
    ('ITEM_STATUS', 'Item', 'Activate / deactivate Item', 1, NOW(), 'seed'),
    ('ITEM_UPLOAD', 'Item', 'Bulk upload Items / Rates / Composition', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Item', 'Item Master', 'capsule',
       'itemmaster', 'master/item-master.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'itemmaster');

-- ==================== initech_db ====================
USE initech_db;

INSERT INTO item_type_master (item_type_code, item_type_name, short_name, description, display_order, is_active, created_by)
SELECT t.code, t.name, t.short, t.descr, t.ord, 1, 'seed'
FROM (SELECT 'PRODUCT' code, 'Product' name, 'PRD' short, 'Saleable product (PTS / PTR / MRP needed in rates)' descr, 1 ord
      UNION ALL SELECT 'SAMPLE', 'Sample', 'SMP', 'Free sample', 2
      UNION ALL SELECT 'GIFT', 'Gift', 'GFT', 'Gift item', 3
      UNION ALL SELECT 'INPUT', 'Input', 'INP', 'Promotional input', 4) t
WHERE NOT EXISTS (SELECT 1 FROM item_type_master m WHERE m.item_type_code = t.code OR m.item_type_name = t.name);

CREATE TABLE IF NOT EXISTS item_master (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_code          VARCHAR(50)  NOT NULL                COMMENT 'business code, same in every state (old item_code)',
  item_name          VARCHAR(200) NOT NULL,
  short_name         VARCHAR(100) NULL,
  item_type_id       BIGINT UNSIGNED NOT NULL             COMMENT '-> item_type_master.id (Product / Sample / Gift / Input ...)',
  division_id        BIGINT       NULL                    COMMENT '-> division_master.oid',
  brand_id           BIGINT UNSIGNED NULL                 COMMENT '-> brand_master.id',
  item_class         VARCHAR(100) NULL                    COMMENT 'old item_class / class',
  manufacturer       VARCHAR(150) NULL                    COMMENT 'old Company_Name',
  pack_size          VARCHAR(50)  NULL                    COMMENT 'e.g. 10x10',
  sample_pack_size   VARCHAR(50)  NULL,
  uom                VARCHAR(20)  NULL                    COMMENT 'strip / bottle / tube ...',
  hsn_code           VARCHAR(8)   NULL,
  gst_rate           DECIMAL(5,2) NULL                    COMMENT 'e.g. 12.00',
  composition_text   VARCHAR(1000) NULL                   COMMENT 'auto-built from item_composition, for lists / search',
  image_path         VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/Dolo 650.jpg (user file name kept)',
  image_type         VARCHAR(100) NULL                    COMMENT 'MIME type as text',
  image_size         BIGINT       NULL,
  image_name         VARCHAR(255) NULL                    COMMENT 'original file name',
  thumbnail_path     VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/thumbnail/Dolo 650_thumb.jpg',
  thumbnail_type     VARCHAR(100) NULL,
  item_code_main     VARCHAR(100) NULL                    COMMENT 'kept from 2012 table',
  legacy_set_id      VARCHAR(100) NULL                    COMMENT 'old set_id',
  legacy_new_set_id  VARCHAR(100) NULL                    COMMENT 'old new_set_id',
  status             TINYINT(1)   NOT NULL DEFAULT 1,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by         VARCHAR(128) NULL,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by         VARCHAR(128) NULL,
  version            INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_code (item_code),
  KEY idx_item_name (item_name),
  KEY idx_item_type_status (item_type_id, status),
  KEY idx_item_division (division_id),
  KEY idx_item_brand (brand_id),
  CONSTRAINT fk_item_type     FOREIGN KEY (item_type_id) REFERENCES item_type_master (id),
  CONSTRAINT fk_item_division FOREIGN KEY (division_id)  REFERENCES division_master (oid),
  CONSTRAINT fk_item_brand    FOREIGN KEY (brand_id)     REFERENCES brand_master (id),
  CONSTRAINT chk_item_gst CHECK (gst_rate IS NULL OR (gst_rate >= 0 AND gst_rate <= 100)),
  CONSTRAINT chk_item_hsn CHECK (hsn_code IS NULL OR REGEXP_LIKE(hsn_code, '^[0-9]{4,8}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_rate (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id         BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  state_id        BIGINT       NULL                     COMMENT '-> state_master.oid (old state_main); NULL = all states',
  state_key       BIGINT       GENERATED ALWAYS AS (IFNULL(state_id, 0)) STORED COMMENT 'lets the unique key cover All India',
  effective_from  DATE         NOT NULL,
  effective_to    DATE         NULL                     COMMENT 'NULL = current rate',
  pts             DECIMAL(12,2) NULL                    COMMENT 'price to stockist',
  ptr             DECIMAL(12,2) NULL                    COMMENT 'price to retailer',
  mrp             DECIMAL(12,2) NULL,
  purchase_rate   DECIMAL(12,2) NULL,
  sample_rate     DECIMAL(12,2) NULL,
  discount_pct    DECIMAL(5,2)  NULL,
  tax_included    TINYINT(1)   NOT NULL DEFAULT 0       COMMENT 'old include_sttx',
  legacy_item_id  VARCHAR(255) NULL                     COMMENT 'old item_newid (one product x state row)',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rate_item_state_from (item_id, state_key, effective_from),
  UNIQUE KEY uk_rate_legacy (legacy_item_id),
  KEY idx_rate_lookup (item_id, state_key, effective_from, effective_to),
  KEY idx_rate_state (state_id),
  CONSTRAINT fk_rate_item  FOREIGN KEY (item_id)  REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT fk_rate_state FOREIGN KEY (state_id) REFERENCES state_master (oid),
  CONSTRAINT chk_rate_dates    CHECK (effective_to IS NULL OR effective_to >= effective_from),
  CONSTRAINT chk_rate_positive CHECK (COALESCE(pts, 0) >= 0 AND COALESCE(ptr, 0) >= 0 AND COALESCE(mrp, 0) >= 0
                                      AND COALESCE(purchase_rate, 0) >= 0 AND COALESCE(sample_rate, 0) >= 0),
  CONSTRAINT chk_rate_order    CHECK ((mrp IS NULL OR ptr IS NULL OR mrp >= ptr) AND (ptr IS NULL OR pts IS NULL OR ptr >= pts)),
  CONSTRAINT chk_rate_discount CHECK (discount_pct IS NULL OR (discount_pct >= 0 AND discount_pct <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_composition (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id        BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  molecule_name  VARCHAR(150) NOT NULL                 COMMENT 'typed by the user, e.g. Paracetamol',
  molecule_key   VARCHAR(150) GENERATED ALWAYS AS (LOWER(TRIM(molecule_name))) STORED,
  strength       DECIMAL(12,3) NULL                    COMMENT 'e.g. 500',
  strength_unit  VARCHAR(20)  NULL                     COMMENT 'mg / mcg / g / ml / IU / % w/w',
  per_qty        VARCHAR(30)  NULL                     COMMENT 'per tablet / per 5 ml',
  sort_order     INT          NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comp_item_molecule (item_id, molecule_key),
  KEY idx_comp_molecule (molecule_key),
  CONSTRAINT fk_comp_item FOREIGN KEY (item_id) REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT chk_comp_strength CHECK (strength IS NULL OR strength > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ITEM_SAVE', 'Item', 'Add / edit Item (with rates and composition)', 1, NOW(), 'seed'),
    ('ITEM_STATUS', 'Item', 'Activate / deactivate Item', 1, NOW(), 'seed'),
    ('ITEM_UPLOAD', 'Item', 'Bulk upload Items / Rates / Composition', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Item', 'Item Master', 'capsule',
       'itemmaster', 'master/item-master.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'itemmaster');

-- ==================== sfa_demo ====================
USE sfa_demo;

INSERT INTO item_type_master (item_type_code, item_type_name, short_name, description, display_order, is_active, created_by)
SELECT t.code, t.name, t.short, t.descr, t.ord, 1, 'seed'
FROM (SELECT 'PRODUCT' code, 'Product' name, 'PRD' short, 'Saleable product (PTS / PTR / MRP needed in rates)' descr, 1 ord
      UNION ALL SELECT 'SAMPLE', 'Sample', 'SMP', 'Free sample', 2
      UNION ALL SELECT 'GIFT', 'Gift', 'GFT', 'Gift item', 3
      UNION ALL SELECT 'INPUT', 'Input', 'INP', 'Promotional input', 4) t
WHERE NOT EXISTS (SELECT 1 FROM item_type_master m WHERE m.item_type_code = t.code OR m.item_type_name = t.name);

CREATE TABLE IF NOT EXISTS item_master (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_code          VARCHAR(50)  NOT NULL                COMMENT 'business code, same in every state (old item_code)',
  item_name          VARCHAR(200) NOT NULL,
  short_name         VARCHAR(100) NULL,
  item_type_id       BIGINT UNSIGNED NOT NULL             COMMENT '-> item_type_master.id (Product / Sample / Gift / Input ...)',
  division_id        BIGINT       NULL                    COMMENT '-> division_master.oid',
  brand_id           BIGINT UNSIGNED NULL                 COMMENT '-> brand_master.id',
  item_class         VARCHAR(100) NULL                    COMMENT 'old item_class / class',
  manufacturer       VARCHAR(150) NULL                    COMMENT 'old Company_Name',
  pack_size          VARCHAR(50)  NULL                    COMMENT 'e.g. 10x10',
  sample_pack_size   VARCHAR(50)  NULL,
  uom                VARCHAR(20)  NULL                    COMMENT 'strip / bottle / tube ...',
  hsn_code           VARCHAR(8)   NULL,
  gst_rate           DECIMAL(5,2) NULL                    COMMENT 'e.g. 12.00',
  composition_text   VARCHAR(1000) NULL                   COMMENT 'auto-built from item_composition, for lists / search',
  image_path         VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/Dolo 650.jpg (user file name kept)',
  image_type         VARCHAR(100) NULL                    COMMENT 'MIME type as text',
  image_size         BIGINT       NULL,
  image_name         VARCHAR(255) NULL                    COMMENT 'original file name',
  thumbnail_path     VARCHAR(255) NULL                    COMMENT 'e.g. itemMaster/thumbnail/Dolo 650_thumb.jpg',
  thumbnail_type     VARCHAR(100) NULL,
  item_code_main     VARCHAR(100) NULL                    COMMENT 'kept from 2012 table',
  legacy_set_id      VARCHAR(100) NULL                    COMMENT 'old set_id',
  legacy_new_set_id  VARCHAR(100) NULL                    COMMENT 'old new_set_id',
  status             TINYINT(1)   NOT NULL DEFAULT 1,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by         VARCHAR(128) NULL,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by         VARCHAR(128) NULL,
  version            INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_code (item_code),
  KEY idx_item_name (item_name),
  KEY idx_item_type_status (item_type_id, status),
  KEY idx_item_division (division_id),
  KEY idx_item_brand (brand_id),
  CONSTRAINT fk_item_type     FOREIGN KEY (item_type_id) REFERENCES item_type_master (id),
  CONSTRAINT fk_item_division FOREIGN KEY (division_id)  REFERENCES division_master (oid),
  CONSTRAINT fk_item_brand    FOREIGN KEY (brand_id)     REFERENCES brand_master (id),
  CONSTRAINT chk_item_gst CHECK (gst_rate IS NULL OR (gst_rate >= 0 AND gst_rate <= 100)),
  CONSTRAINT chk_item_hsn CHECK (hsn_code IS NULL OR REGEXP_LIKE(hsn_code, '^[0-9]{4,8}$'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_rate (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id         BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  state_id        BIGINT       NULL                     COMMENT '-> state_master.oid (old state_main); NULL = all states',
  state_key       BIGINT       GENERATED ALWAYS AS (IFNULL(state_id, 0)) STORED COMMENT 'lets the unique key cover All India',
  effective_from  DATE         NOT NULL,
  effective_to    DATE         NULL                     COMMENT 'NULL = current rate',
  pts             DECIMAL(12,2) NULL                    COMMENT 'price to stockist',
  ptr             DECIMAL(12,2) NULL                    COMMENT 'price to retailer',
  mrp             DECIMAL(12,2) NULL,
  purchase_rate   DECIMAL(12,2) NULL,
  sample_rate     DECIMAL(12,2) NULL,
  discount_pct    DECIMAL(5,2)  NULL,
  tax_included    TINYINT(1)   NOT NULL DEFAULT 0       COMMENT 'old include_sttx',
  legacy_item_id  VARCHAR(255) NULL                     COMMENT 'old item_newid (one product x state row)',
  status          TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(128) NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(128) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rate_item_state_from (item_id, state_key, effective_from),
  UNIQUE KEY uk_rate_legacy (legacy_item_id),
  KEY idx_rate_lookup (item_id, state_key, effective_from, effective_to),
  KEY idx_rate_state (state_id),
  CONSTRAINT fk_rate_item  FOREIGN KEY (item_id)  REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT fk_rate_state FOREIGN KEY (state_id) REFERENCES state_master (oid),
  CONSTRAINT chk_rate_dates    CHECK (effective_to IS NULL OR effective_to >= effective_from),
  CONSTRAINT chk_rate_positive CHECK (COALESCE(pts, 0) >= 0 AND COALESCE(ptr, 0) >= 0 AND COALESCE(mrp, 0) >= 0
                                      AND COALESCE(purchase_rate, 0) >= 0 AND COALESCE(sample_rate, 0) >= 0),
  CONSTRAINT chk_rate_order    CHECK ((mrp IS NULL OR ptr IS NULL OR mrp >= ptr) AND (ptr IS NULL OR pts IS NULL OR ptr >= pts)),
  CONSTRAINT chk_rate_discount CHECK (discount_pct IS NULL OR (discount_pct >= 0 AND discount_pct <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS item_composition (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id        BIGINT UNSIGNED NOT NULL              COMMENT '-> item_master.id',
  molecule_name  VARCHAR(150) NOT NULL                 COMMENT 'typed by the user, e.g. Paracetamol',
  molecule_key   VARCHAR(150) GENERATED ALWAYS AS (LOWER(TRIM(molecule_name))) STORED,
  strength       DECIMAL(12,3) NULL                    COMMENT 'e.g. 500',
  strength_unit  VARCHAR(20)  NULL                     COMMENT 'mg / mcg / g / ml / IU / % w/w',
  per_qty        VARCHAR(30)  NULL                     COMMENT 'per tablet / per 5 ml',
  sort_order     INT          NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comp_item_molecule (item_id, molecule_key),
  KEY idx_comp_molecule (molecule_key),
  CONSTRAINT fk_comp_item FOREIGN KEY (item_id) REFERENCES item_master (id) ON DELETE CASCADE,
  CONSTRAINT chk_comp_strength CHECK (strength IS NULL OR strength > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ITEM_SAVE', 'Item', 'Add / edit Item (with rates and composition)', 1, NOW(), 'seed'),
    ('ITEM_STATUS', 'Item', 'Activate / deactivate Item', 1, NOW(), 'seed'),
    ('ITEM_UPLOAD', 'Item', 'Bulk upload Items / Rates / Composition', 1, NOW(), 'seed');

INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, 12, cat.emp_id, 'WEB', 'Item', 'Item Master', 'capsule',
       'itemmaster', 'master/item-master.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = 12 LIMIT 1) cat
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'itemmaster');
