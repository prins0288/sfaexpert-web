-- ===========================================================================
-- 73_item_details_import.sql — PER TENANT database, run AFTER 72_item_master.sql
--
-- Copies the 2012 table item_details into the new design (only if a database
-- has item_details; otherwise it just says "nothing to import"):
--   * same item_code in many rows (one per state)  -> ONE item_master row
--     (values from the most recently modified row)
--   * every old row (item_newid)                   -> one item_rate row
--     state_main -> state_id (blank / 0 = All India), item_newid -> legacy_item_id
--   * TYPE -> item_type_master by name / code; a new type is created when missing,
--     blank = Product
--   * item_code_main, set_id, new_set_id kept; other1..4 dropped
--   * rates start on @item_rates_from (old rows have no date) — change below if needed
--
-- Every row that could not be copied as-is is listed in item_import_log
-- (SELECT * FROM item_import_log;) and printed at the end.
-- Safe to re-run: items / rates already copied (same item_code / item_newid) are skipped.
-- Run:  mysql -u myroot < src/main/resources/sql/73_item_details_import.sql
-- ===========================================================================

SET @item_rates_from = CURDATE();   -- e.g. '2026-04-01'

-- ==================== acme_db ====================
USE acme_db;
DROP PROCEDURE IF EXISTS sfa_import_item_details;
DELIMITER $$
CREATE PROCEDURE sfa_import_item_details(IN p_from DATE)
proc: BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item_details') = 0 THEN
    SELECT CONCAT(DATABASE(), ': item_details not found - nothing to import') AS result;
    LEAVE proc;
  END IF;

  CREATE TABLE IF NOT EXISTS item_import_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    legacy_item_id  VARCHAR(255) NULL,
    item_code       VARCHAR(255) NULL,
    problem         VARCHAR(500) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  DELETE FROM item_import_log;   -- keeps the latest run only

  -- 1) cleaned copy of the old rows
  DROP TABLE IF EXISTS item_import_work;
  CREATE TABLE item_import_work ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AS
  SELECT TRIM(d.item_newid)                                   AS legacy_id,
         UPPER(TRIM(d.item_code))                             AS code,
         TRIM(d.item_name)                                    AS name,
         NULLIF(TRIM(d.item_short_name), '')                  AS short_name,
         TRIM(d.`TYPE`)                                       AS type_text,
         CAST(NULL AS UNSIGNED)                               AS type_id,
         NULLIF(TRIM(d.division_id), '')                      AS division_text,
         NULLIF(TRIM(d.brand_id), '')                         AS brand_text,
         COALESCE(NULLIF(TRIM(d.item_class), ''), NULLIF(TRIM(d.`class`), '')) AS item_class,
         NULLIF(TRIM(d.Company_Name), '')                     AS manufacturer,
         NULLIF(TRIM(d.item_pack_size), '')                   AS pack_size,
         NULLIF(TRIM(d.sample_pack_size), '')                 AS sample_pack_size,
         NULLIF(TRIM(d.item_code_main), '')                   AS item_code_main,
         NULLIF(TRIM(d.set_id), '')                           AS set_id,
         NULLIF(TRIM(d.new_set_id), '')                       AS new_set_id,
         IF(LOWER(TRIM(d.item_status)) IN ('active', 'y', 'yes', '1'), 1, 0) AS status,
         NULLIF(NULLIF(TRIM(d.state_main), ''), '0')          AS state_text,
         CAST(NULLIF(d.pts, 0) AS DECIMAL(12,2))              AS pts,
         CAST(NULLIF(d.PTR, 0) AS DECIMAL(12,2))              AS ptr,
         CAST(NULLIF(d.MRP, 0) AS DECIMAL(12,2))              AS mrp,
         CAST(NULLIF(d.purchase_rate, 0) AS DECIMAL(12,2))    AS purchase_rate,
         IF(REGEXP_LIKE(TRIM(d.sample_rate), '^[0-9]+(\\.[0-9]+)?$') AND TRIM(d.sample_rate) + 0 > 0,
            CAST(TRIM(d.sample_rate) AS DECIMAL(12,2)), NULL)  AS sample_rate,
         CAST(NULLIF(d.discount, 0) AS DECIMAL(5,2))          AS discount_pct,
         IF(LOWER(TRIM(d.include_sttx)) IN ('y', 'yes', '1', 'true', 'include', 'included'), 1, 0) AS tax_included,
         ROW_NUMBER() OVER (PARTITION BY UPPER(TRIM(d.item_code))
                            ORDER BY d.last_modified_date_time DESC, d.item_newid DESC) AS rn
  FROM item_details d;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_code is blank - row not imported' FROM item_import_work WHERE code = '';
  DELETE FROM item_import_work WHERE code = '';

  -- 2) item types: match by name or code, create the missing ones, blank = Product
  INSERT IGNORE INTO item_type_master (item_type_code, item_type_name, display_order, is_active, created_by)
  SELECT LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30), LEFT(MIN(w.type_text), 100), 50, 1, 'import'
  FROM item_import_work w
  WHERE w.type_text <> ''
    AND NOT EXISTS (SELECT 1 FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text)
  GROUP BY LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30);

  UPDATE item_import_work w
  SET w.type_id = COALESCE(
        (SELECT t.id FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text
         ORDER BY t.item_type_name = w.type_text DESC LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30) LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = 'PRODUCT' LIMIT 1));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'TYPE is blank - taken as Product' FROM item_import_work WHERE type_text = '' AND rn = 1;

  -- 3) one item per item_code, values from the latest row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('rows of this item_code differ (name / pack size / type) - kept the latest row: ', LEFT(l.name, 150))
  FROM item_import_work w JOIN item_import_work l ON l.code = w.code AND l.rn = 1
  WHERE w.rn > 1 AND (w.name <> l.name OR NOT (w.pack_size <=> l.pack_size) OR NOT (w.type_id <=> l.type_id));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('division ', w.division_text, ' not found in division_master - left blank')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.division_text IS NOT NULL AND w.division_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('brand ', w.brand_text, ' not found in brand_master - left blank (add the brand, then set it on the item)')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.brand_text IS NOT NULL AND w.brand_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text);

  INSERT INTO item_master (item_code, item_name, short_name, item_type_id, division_id, brand_id, item_class, manufacturer,
                           pack_size, sample_pack_size, item_code_main, legacy_set_id, legacy_new_set_id, status, created_by, updated_by, version)
  SELECT LEFT(w.code, 50), LEFT(IF(w.name = '', w.code, w.name), 200), LEFT(w.short_name, 100), w.type_id,
         (SELECT m.oid FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text LIMIT 1),
         (SELECT b.id FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text LIMIT 1),
         LEFT(w.item_class, 100), LEFT(w.manufacturer, 150), LEFT(w.pack_size, 50), LEFT(w.sample_pack_size, 50),
         LEFT(w.item_code_main, 100), LEFT(w.set_id, 100), LEFT(w.new_set_id, 100), w.status, 'import', 'import', 0
  FROM item_import_work w
  WHERE w.rn = 1 AND NOT EXISTS (SELECT 1 FROM item_master i WHERE i.item_code = LEFT(w.code, 50));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_name is blank - item_code used as the name' FROM item_import_work WHERE rn = 1 AND name = '';

  -- 4) rates: one per old row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('state_main ', w.state_text, ' not found in state_master - rate not imported')
  FROM item_import_work w
  WHERE w.state_text IS NOT NULL AND NOT EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('price order wrong (needs MRP >= PTR >= PTS): PTS ', IFNULL(w.pts, '-'), ', PTR ', IFNULL(w.ptr, '-'),
                                     ', MRP ', IFNULL(w.mrp, '-'), ' - rate not imported')
  FROM item_import_work w
  WHERE (w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
     OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0;

  INSERT IGNORE INTO item_rate (item_id, state_id, effective_from, pts, ptr, mrp, purchase_rate, sample_rate, discount_pct,
                                tax_included, legacy_item_id, status, created_by, updated_by)
  SELECT i.id, CAST(w.state_text AS UNSIGNED), p_from, w.pts, w.ptr, w.mrp, w.purchase_rate, w.sample_rate,
         IF(w.discount_pct BETWEEN 0 AND 100, w.discount_pct, NULL), w.tax_included, w.legacy_id, 1, 'import', 'import'
  FROM item_import_work w
  JOIN item_master i ON i.item_code = LEFT(w.code, 50)
  WHERE (w.state_text IS NULL OR EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text))
    AND NOT ((w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
             OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0)
    AND NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
  ORDER BY w.rn;   -- the latest row wins when two rows have the same item_code + state

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'Product rate without PTS / PTR / MRP - imported, fill the prices on the Item page'
  FROM item_import_work w
  JOIN item_rate r ON r.legacy_item_id = w.legacy_id
  JOIN item_master i ON i.id = r.item_id
  JOIN item_type_master t ON t.id = i.item_type_id AND t.item_type_code = 'PRODUCT'
  WHERE r.pts IS NULL OR r.ptr IS NULL OR r.mrp IS NULL;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'another row has the same item_code + state - only the latest row''s rate was imported'
  FROM item_import_work w
  WHERE NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
    AND NOT EXISTS (SELECT 1 FROM item_import_log g WHERE g.legacy_item_id = w.legacy_id AND g.problem LIKE '%rate not imported');

  SELECT DATABASE() AS db,
         (SELECT COUNT(*) FROM item_details)                                         AS old_rows,
         (SELECT COUNT(DISTINCT code) FROM item_import_work)                          AS item_codes,
         (SELECT COUNT(*) FROM item_master)                                          AS items_now,
         (SELECT COUNT(*) FROM item_rate WHERE legacy_item_id IS NOT NULL)            AS imported_rates,
         (SELECT COUNT(*) FROM item_import_log)                                      AS problems;
  SELECT legacy_item_id, item_code, problem FROM item_import_log ORDER BY item_code, legacy_item_id;
  DROP TABLE item_import_work;
END$$
DELIMITER ;
CALL sfa_import_item_details(@item_rates_from);
DROP PROCEDURE sfa_import_item_details;

-- ==================== globex_db ====================
USE globex_db;
DROP PROCEDURE IF EXISTS sfa_import_item_details;
DELIMITER $$
CREATE PROCEDURE sfa_import_item_details(IN p_from DATE)
proc: BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item_details') = 0 THEN
    SELECT CONCAT(DATABASE(), ': item_details not found - nothing to import') AS result;
    LEAVE proc;
  END IF;

  CREATE TABLE IF NOT EXISTS item_import_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    legacy_item_id  VARCHAR(255) NULL,
    item_code       VARCHAR(255) NULL,
    problem         VARCHAR(500) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  DELETE FROM item_import_log;   -- keeps the latest run only

  -- 1) cleaned copy of the old rows
  DROP TABLE IF EXISTS item_import_work;
  CREATE TABLE item_import_work ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AS
  SELECT TRIM(d.item_newid)                                   AS legacy_id,
         UPPER(TRIM(d.item_code))                             AS code,
         TRIM(d.item_name)                                    AS name,
         NULLIF(TRIM(d.item_short_name), '')                  AS short_name,
         TRIM(d.`TYPE`)                                       AS type_text,
         CAST(NULL AS UNSIGNED)                               AS type_id,
         NULLIF(TRIM(d.division_id), '')                      AS division_text,
         NULLIF(TRIM(d.brand_id), '')                         AS brand_text,
         COALESCE(NULLIF(TRIM(d.item_class), ''), NULLIF(TRIM(d.`class`), '')) AS item_class,
         NULLIF(TRIM(d.Company_Name), '')                     AS manufacturer,
         NULLIF(TRIM(d.item_pack_size), '')                   AS pack_size,
         NULLIF(TRIM(d.sample_pack_size), '')                 AS sample_pack_size,
         NULLIF(TRIM(d.item_code_main), '')                   AS item_code_main,
         NULLIF(TRIM(d.set_id), '')                           AS set_id,
         NULLIF(TRIM(d.new_set_id), '')                       AS new_set_id,
         IF(LOWER(TRIM(d.item_status)) IN ('active', 'y', 'yes', '1'), 1, 0) AS status,
         NULLIF(NULLIF(TRIM(d.state_main), ''), '0')          AS state_text,
         CAST(NULLIF(d.pts, 0) AS DECIMAL(12,2))              AS pts,
         CAST(NULLIF(d.PTR, 0) AS DECIMAL(12,2))              AS ptr,
         CAST(NULLIF(d.MRP, 0) AS DECIMAL(12,2))              AS mrp,
         CAST(NULLIF(d.purchase_rate, 0) AS DECIMAL(12,2))    AS purchase_rate,
         IF(REGEXP_LIKE(TRIM(d.sample_rate), '^[0-9]+(\\.[0-9]+)?$') AND TRIM(d.sample_rate) + 0 > 0,
            CAST(TRIM(d.sample_rate) AS DECIMAL(12,2)), NULL)  AS sample_rate,
         CAST(NULLIF(d.discount, 0) AS DECIMAL(5,2))          AS discount_pct,
         IF(LOWER(TRIM(d.include_sttx)) IN ('y', 'yes', '1', 'true', 'include', 'included'), 1, 0) AS tax_included,
         ROW_NUMBER() OVER (PARTITION BY UPPER(TRIM(d.item_code))
                            ORDER BY d.last_modified_date_time DESC, d.item_newid DESC) AS rn
  FROM item_details d;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_code is blank - row not imported' FROM item_import_work WHERE code = '';
  DELETE FROM item_import_work WHERE code = '';

  -- 2) item types: match by name or code, create the missing ones, blank = Product
  INSERT IGNORE INTO item_type_master (item_type_code, item_type_name, display_order, is_active, created_by)
  SELECT LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30), LEFT(MIN(w.type_text), 100), 50, 1, 'import'
  FROM item_import_work w
  WHERE w.type_text <> ''
    AND NOT EXISTS (SELECT 1 FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text)
  GROUP BY LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30);

  UPDATE item_import_work w
  SET w.type_id = COALESCE(
        (SELECT t.id FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text
         ORDER BY t.item_type_name = w.type_text DESC LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30) LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = 'PRODUCT' LIMIT 1));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'TYPE is blank - taken as Product' FROM item_import_work WHERE type_text = '' AND rn = 1;

  -- 3) one item per item_code, values from the latest row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('rows of this item_code differ (name / pack size / type) - kept the latest row: ', LEFT(l.name, 150))
  FROM item_import_work w JOIN item_import_work l ON l.code = w.code AND l.rn = 1
  WHERE w.rn > 1 AND (w.name <> l.name OR NOT (w.pack_size <=> l.pack_size) OR NOT (w.type_id <=> l.type_id));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('division ', w.division_text, ' not found in division_master - left blank')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.division_text IS NOT NULL AND w.division_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('brand ', w.brand_text, ' not found in brand_master - left blank (add the brand, then set it on the item)')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.brand_text IS NOT NULL AND w.brand_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text);

  INSERT INTO item_master (item_code, item_name, short_name, item_type_id, division_id, brand_id, item_class, manufacturer,
                           pack_size, sample_pack_size, item_code_main, legacy_set_id, legacy_new_set_id, status, created_by, updated_by, version)
  SELECT LEFT(w.code, 50), LEFT(IF(w.name = '', w.code, w.name), 200), LEFT(w.short_name, 100), w.type_id,
         (SELECT m.oid FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text LIMIT 1),
         (SELECT b.id FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text LIMIT 1),
         LEFT(w.item_class, 100), LEFT(w.manufacturer, 150), LEFT(w.pack_size, 50), LEFT(w.sample_pack_size, 50),
         LEFT(w.item_code_main, 100), LEFT(w.set_id, 100), LEFT(w.new_set_id, 100), w.status, 'import', 'import', 0
  FROM item_import_work w
  WHERE w.rn = 1 AND NOT EXISTS (SELECT 1 FROM item_master i WHERE i.item_code = LEFT(w.code, 50));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_name is blank - item_code used as the name' FROM item_import_work WHERE rn = 1 AND name = '';

  -- 4) rates: one per old row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('state_main ', w.state_text, ' not found in state_master - rate not imported')
  FROM item_import_work w
  WHERE w.state_text IS NOT NULL AND NOT EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('price order wrong (needs MRP >= PTR >= PTS): PTS ', IFNULL(w.pts, '-'), ', PTR ', IFNULL(w.ptr, '-'),
                                     ', MRP ', IFNULL(w.mrp, '-'), ' - rate not imported')
  FROM item_import_work w
  WHERE (w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
     OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0;

  INSERT IGNORE INTO item_rate (item_id, state_id, effective_from, pts, ptr, mrp, purchase_rate, sample_rate, discount_pct,
                                tax_included, legacy_item_id, status, created_by, updated_by)
  SELECT i.id, CAST(w.state_text AS UNSIGNED), p_from, w.pts, w.ptr, w.mrp, w.purchase_rate, w.sample_rate,
         IF(w.discount_pct BETWEEN 0 AND 100, w.discount_pct, NULL), w.tax_included, w.legacy_id, 1, 'import', 'import'
  FROM item_import_work w
  JOIN item_master i ON i.item_code = LEFT(w.code, 50)
  WHERE (w.state_text IS NULL OR EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text))
    AND NOT ((w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
             OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0)
    AND NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
  ORDER BY w.rn;   -- the latest row wins when two rows have the same item_code + state

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'Product rate without PTS / PTR / MRP - imported, fill the prices on the Item page'
  FROM item_import_work w
  JOIN item_rate r ON r.legacy_item_id = w.legacy_id
  JOIN item_master i ON i.id = r.item_id
  JOIN item_type_master t ON t.id = i.item_type_id AND t.item_type_code = 'PRODUCT'
  WHERE r.pts IS NULL OR r.ptr IS NULL OR r.mrp IS NULL;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'another row has the same item_code + state - only the latest row''s rate was imported'
  FROM item_import_work w
  WHERE NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
    AND NOT EXISTS (SELECT 1 FROM item_import_log g WHERE g.legacy_item_id = w.legacy_id AND g.problem LIKE '%rate not imported');

  SELECT DATABASE() AS db,
         (SELECT COUNT(*) FROM item_details)                                         AS old_rows,
         (SELECT COUNT(DISTINCT code) FROM item_import_work)                          AS item_codes,
         (SELECT COUNT(*) FROM item_master)                                          AS items_now,
         (SELECT COUNT(*) FROM item_rate WHERE legacy_item_id IS NOT NULL)            AS imported_rates,
         (SELECT COUNT(*) FROM item_import_log)                                      AS problems;
  SELECT legacy_item_id, item_code, problem FROM item_import_log ORDER BY item_code, legacy_item_id;
  DROP TABLE item_import_work;
END$$
DELIMITER ;
CALL sfa_import_item_details(@item_rates_from);
DROP PROCEDURE sfa_import_item_details;

-- ==================== initech_db ====================
USE initech_db;
DROP PROCEDURE IF EXISTS sfa_import_item_details;
DELIMITER $$
CREATE PROCEDURE sfa_import_item_details(IN p_from DATE)
proc: BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item_details') = 0 THEN
    SELECT CONCAT(DATABASE(), ': item_details not found - nothing to import') AS result;
    LEAVE proc;
  END IF;

  CREATE TABLE IF NOT EXISTS item_import_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    legacy_item_id  VARCHAR(255) NULL,
    item_code       VARCHAR(255) NULL,
    problem         VARCHAR(500) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  DELETE FROM item_import_log;   -- keeps the latest run only

  -- 1) cleaned copy of the old rows
  DROP TABLE IF EXISTS item_import_work;
  CREATE TABLE item_import_work ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AS
  SELECT TRIM(d.item_newid)                                   AS legacy_id,
         UPPER(TRIM(d.item_code))                             AS code,
         TRIM(d.item_name)                                    AS name,
         NULLIF(TRIM(d.item_short_name), '')                  AS short_name,
         TRIM(d.`TYPE`)                                       AS type_text,
         CAST(NULL AS UNSIGNED)                               AS type_id,
         NULLIF(TRIM(d.division_id), '')                      AS division_text,
         NULLIF(TRIM(d.brand_id), '')                         AS brand_text,
         COALESCE(NULLIF(TRIM(d.item_class), ''), NULLIF(TRIM(d.`class`), '')) AS item_class,
         NULLIF(TRIM(d.Company_Name), '')                     AS manufacturer,
         NULLIF(TRIM(d.item_pack_size), '')                   AS pack_size,
         NULLIF(TRIM(d.sample_pack_size), '')                 AS sample_pack_size,
         NULLIF(TRIM(d.item_code_main), '')                   AS item_code_main,
         NULLIF(TRIM(d.set_id), '')                           AS set_id,
         NULLIF(TRIM(d.new_set_id), '')                       AS new_set_id,
         IF(LOWER(TRIM(d.item_status)) IN ('active', 'y', 'yes', '1'), 1, 0) AS status,
         NULLIF(NULLIF(TRIM(d.state_main), ''), '0')          AS state_text,
         CAST(NULLIF(d.pts, 0) AS DECIMAL(12,2))              AS pts,
         CAST(NULLIF(d.PTR, 0) AS DECIMAL(12,2))              AS ptr,
         CAST(NULLIF(d.MRP, 0) AS DECIMAL(12,2))              AS mrp,
         CAST(NULLIF(d.purchase_rate, 0) AS DECIMAL(12,2))    AS purchase_rate,
         IF(REGEXP_LIKE(TRIM(d.sample_rate), '^[0-9]+(\\.[0-9]+)?$') AND TRIM(d.sample_rate) + 0 > 0,
            CAST(TRIM(d.sample_rate) AS DECIMAL(12,2)), NULL)  AS sample_rate,
         CAST(NULLIF(d.discount, 0) AS DECIMAL(5,2))          AS discount_pct,
         IF(LOWER(TRIM(d.include_sttx)) IN ('y', 'yes', '1', 'true', 'include', 'included'), 1, 0) AS tax_included,
         ROW_NUMBER() OVER (PARTITION BY UPPER(TRIM(d.item_code))
                            ORDER BY d.last_modified_date_time DESC, d.item_newid DESC) AS rn
  FROM item_details d;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_code is blank - row not imported' FROM item_import_work WHERE code = '';
  DELETE FROM item_import_work WHERE code = '';

  -- 2) item types: match by name or code, create the missing ones, blank = Product
  INSERT IGNORE INTO item_type_master (item_type_code, item_type_name, display_order, is_active, created_by)
  SELECT LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30), LEFT(MIN(w.type_text), 100), 50, 1, 'import'
  FROM item_import_work w
  WHERE w.type_text <> ''
    AND NOT EXISTS (SELECT 1 FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text)
  GROUP BY LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30);

  UPDATE item_import_work w
  SET w.type_id = COALESCE(
        (SELECT t.id FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text
         ORDER BY t.item_type_name = w.type_text DESC LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30) LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = 'PRODUCT' LIMIT 1));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'TYPE is blank - taken as Product' FROM item_import_work WHERE type_text = '' AND rn = 1;

  -- 3) one item per item_code, values from the latest row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('rows of this item_code differ (name / pack size / type) - kept the latest row: ', LEFT(l.name, 150))
  FROM item_import_work w JOIN item_import_work l ON l.code = w.code AND l.rn = 1
  WHERE w.rn > 1 AND (w.name <> l.name OR NOT (w.pack_size <=> l.pack_size) OR NOT (w.type_id <=> l.type_id));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('division ', w.division_text, ' not found in division_master - left blank')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.division_text IS NOT NULL AND w.division_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('brand ', w.brand_text, ' not found in brand_master - left blank (add the brand, then set it on the item)')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.brand_text IS NOT NULL AND w.brand_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text);

  INSERT INTO item_master (item_code, item_name, short_name, item_type_id, division_id, brand_id, item_class, manufacturer,
                           pack_size, sample_pack_size, item_code_main, legacy_set_id, legacy_new_set_id, status, created_by, updated_by, version)
  SELECT LEFT(w.code, 50), LEFT(IF(w.name = '', w.code, w.name), 200), LEFT(w.short_name, 100), w.type_id,
         (SELECT m.oid FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text LIMIT 1),
         (SELECT b.id FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text LIMIT 1),
         LEFT(w.item_class, 100), LEFT(w.manufacturer, 150), LEFT(w.pack_size, 50), LEFT(w.sample_pack_size, 50),
         LEFT(w.item_code_main, 100), LEFT(w.set_id, 100), LEFT(w.new_set_id, 100), w.status, 'import', 'import', 0
  FROM item_import_work w
  WHERE w.rn = 1 AND NOT EXISTS (SELECT 1 FROM item_master i WHERE i.item_code = LEFT(w.code, 50));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_name is blank - item_code used as the name' FROM item_import_work WHERE rn = 1 AND name = '';

  -- 4) rates: one per old row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('state_main ', w.state_text, ' not found in state_master - rate not imported')
  FROM item_import_work w
  WHERE w.state_text IS NOT NULL AND NOT EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('price order wrong (needs MRP >= PTR >= PTS): PTS ', IFNULL(w.pts, '-'), ', PTR ', IFNULL(w.ptr, '-'),
                                     ', MRP ', IFNULL(w.mrp, '-'), ' - rate not imported')
  FROM item_import_work w
  WHERE (w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
     OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0;

  INSERT IGNORE INTO item_rate (item_id, state_id, effective_from, pts, ptr, mrp, purchase_rate, sample_rate, discount_pct,
                                tax_included, legacy_item_id, status, created_by, updated_by)
  SELECT i.id, CAST(w.state_text AS UNSIGNED), p_from, w.pts, w.ptr, w.mrp, w.purchase_rate, w.sample_rate,
         IF(w.discount_pct BETWEEN 0 AND 100, w.discount_pct, NULL), w.tax_included, w.legacy_id, 1, 'import', 'import'
  FROM item_import_work w
  JOIN item_master i ON i.item_code = LEFT(w.code, 50)
  WHERE (w.state_text IS NULL OR EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text))
    AND NOT ((w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
             OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0)
    AND NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
  ORDER BY w.rn;   -- the latest row wins when two rows have the same item_code + state

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'Product rate without PTS / PTR / MRP - imported, fill the prices on the Item page'
  FROM item_import_work w
  JOIN item_rate r ON r.legacy_item_id = w.legacy_id
  JOIN item_master i ON i.id = r.item_id
  JOIN item_type_master t ON t.id = i.item_type_id AND t.item_type_code = 'PRODUCT'
  WHERE r.pts IS NULL OR r.ptr IS NULL OR r.mrp IS NULL;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'another row has the same item_code + state - only the latest row''s rate was imported'
  FROM item_import_work w
  WHERE NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
    AND NOT EXISTS (SELECT 1 FROM item_import_log g WHERE g.legacy_item_id = w.legacy_id AND g.problem LIKE '%rate not imported');

  SELECT DATABASE() AS db,
         (SELECT COUNT(*) FROM item_details)                                         AS old_rows,
         (SELECT COUNT(DISTINCT code) FROM item_import_work)                          AS item_codes,
         (SELECT COUNT(*) FROM item_master)                                          AS items_now,
         (SELECT COUNT(*) FROM item_rate WHERE legacy_item_id IS NOT NULL)            AS imported_rates,
         (SELECT COUNT(*) FROM item_import_log)                                      AS problems;
  SELECT legacy_item_id, item_code, problem FROM item_import_log ORDER BY item_code, legacy_item_id;
  DROP TABLE item_import_work;
END$$
DELIMITER ;
CALL sfa_import_item_details(@item_rates_from);
DROP PROCEDURE sfa_import_item_details;

-- ==================== sfa_demo ====================
USE sfa_demo;
DROP PROCEDURE IF EXISTS sfa_import_item_details;
DELIMITER $$
CREATE PROCEDURE sfa_import_item_details(IN p_from DATE)
proc: BEGIN
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item_details') = 0 THEN
    SELECT CONCAT(DATABASE(), ': item_details not found - nothing to import') AS result;
    LEAVE proc;
  END IF;

  CREATE TABLE IF NOT EXISTS item_import_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    legacy_item_id  VARCHAR(255) NULL,
    item_code       VARCHAR(255) NULL,
    problem         VARCHAR(500) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  DELETE FROM item_import_log;   -- keeps the latest run only

  -- 1) cleaned copy of the old rows
  DROP TABLE IF EXISTS item_import_work;
  CREATE TABLE item_import_work ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AS
  SELECT TRIM(d.item_newid)                                   AS legacy_id,
         UPPER(TRIM(d.item_code))                             AS code,
         TRIM(d.item_name)                                    AS name,
         NULLIF(TRIM(d.item_short_name), '')                  AS short_name,
         TRIM(d.`TYPE`)                                       AS type_text,
         CAST(NULL AS UNSIGNED)                               AS type_id,
         NULLIF(TRIM(d.division_id), '')                      AS division_text,
         NULLIF(TRIM(d.brand_id), '')                         AS brand_text,
         COALESCE(NULLIF(TRIM(d.item_class), ''), NULLIF(TRIM(d.`class`), '')) AS item_class,
         NULLIF(TRIM(d.Company_Name), '')                     AS manufacturer,
         NULLIF(TRIM(d.item_pack_size), '')                   AS pack_size,
         NULLIF(TRIM(d.sample_pack_size), '')                 AS sample_pack_size,
         NULLIF(TRIM(d.item_code_main), '')                   AS item_code_main,
         NULLIF(TRIM(d.set_id), '')                           AS set_id,
         NULLIF(TRIM(d.new_set_id), '')                       AS new_set_id,
         IF(LOWER(TRIM(d.item_status)) IN ('active', 'y', 'yes', '1'), 1, 0) AS status,
         NULLIF(NULLIF(TRIM(d.state_main), ''), '0')          AS state_text,
         CAST(NULLIF(d.pts, 0) AS DECIMAL(12,2))              AS pts,
         CAST(NULLIF(d.PTR, 0) AS DECIMAL(12,2))              AS ptr,
         CAST(NULLIF(d.MRP, 0) AS DECIMAL(12,2))              AS mrp,
         CAST(NULLIF(d.purchase_rate, 0) AS DECIMAL(12,2))    AS purchase_rate,
         IF(REGEXP_LIKE(TRIM(d.sample_rate), '^[0-9]+(\\.[0-9]+)?$') AND TRIM(d.sample_rate) + 0 > 0,
            CAST(TRIM(d.sample_rate) AS DECIMAL(12,2)), NULL)  AS sample_rate,
         CAST(NULLIF(d.discount, 0) AS DECIMAL(5,2))          AS discount_pct,
         IF(LOWER(TRIM(d.include_sttx)) IN ('y', 'yes', '1', 'true', 'include', 'included'), 1, 0) AS tax_included,
         ROW_NUMBER() OVER (PARTITION BY UPPER(TRIM(d.item_code))
                            ORDER BY d.last_modified_date_time DESC, d.item_newid DESC) AS rn
  FROM item_details d;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_code is blank - row not imported' FROM item_import_work WHERE code = '';
  DELETE FROM item_import_work WHERE code = '';

  -- 2) item types: match by name or code, create the missing ones, blank = Product
  INSERT IGNORE INTO item_type_master (item_type_code, item_type_name, display_order, is_active, created_by)
  SELECT LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30), LEFT(MIN(w.type_text), 100), 50, 1, 'import'
  FROM item_import_work w
  WHERE w.type_text <> ''
    AND NOT EXISTS (SELECT 1 FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text)
  GROUP BY LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30);

  UPDATE item_import_work w
  SET w.type_id = COALESCE(
        (SELECT t.id FROM item_type_master t WHERE t.item_type_name = w.type_text OR t.item_type_code = w.type_text
         ORDER BY t.item_type_name = w.type_text DESC LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = LEFT(UPPER(REGEXP_REPLACE(w.type_text, '[^A-Za-z0-9]+', '_')), 30) LIMIT 1),
        (SELECT t.id FROM item_type_master t WHERE t.item_type_code = 'PRODUCT' LIMIT 1));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'TYPE is blank - taken as Product' FROM item_import_work WHERE type_text = '' AND rn = 1;

  -- 3) one item per item_code, values from the latest row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('rows of this item_code differ (name / pack size / type) - kept the latest row: ', LEFT(l.name, 150))
  FROM item_import_work w JOIN item_import_work l ON l.code = w.code AND l.rn = 1
  WHERE w.rn > 1 AND (w.name <> l.name OR NOT (w.pack_size <=> l.pack_size) OR NOT (w.type_id <=> l.type_id));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('division ', w.division_text, ' not found in division_master - left blank')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.division_text IS NOT NULL AND w.division_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('brand ', w.brand_text, ' not found in brand_master - left blank (add the brand, then set it on the item)')
  FROM item_import_work w
  WHERE w.rn = 1 AND w.brand_text IS NOT NULL AND w.brand_text <> '0'
    AND NOT EXISTS (SELECT 1 FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text);

  INSERT INTO item_master (item_code, item_name, short_name, item_type_id, division_id, brand_id, item_class, manufacturer,
                           pack_size, sample_pack_size, item_code_main, legacy_set_id, legacy_new_set_id, status, created_by, updated_by, version)
  SELECT LEFT(w.code, 50), LEFT(IF(w.name = '', w.code, w.name), 200), LEFT(w.short_name, 100), w.type_id,
         (SELECT m.oid FROM division_master m WHERE CAST(m.oid AS CHAR) = w.division_text OR m.division_code = w.division_text OR m.division_name = w.division_text LIMIT 1),
         (SELECT b.id FROM brand_master b WHERE CAST(b.id AS CHAR) = w.brand_text OR b.brand_code = w.brand_text OR b.brand_name = w.brand_text LIMIT 1),
         LEFT(w.item_class, 100), LEFT(w.manufacturer, 150), LEFT(w.pack_size, 50), LEFT(w.sample_pack_size, 50),
         LEFT(w.item_code_main, 100), LEFT(w.set_id, 100), LEFT(w.new_set_id, 100), w.status, 'import', 'import', 0
  FROM item_import_work w
  WHERE w.rn = 1 AND NOT EXISTS (SELECT 1 FROM item_master i WHERE i.item_code = LEFT(w.code, 50));

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT legacy_id, code, 'item_name is blank - item_code used as the name' FROM item_import_work WHERE rn = 1 AND name = '';

  -- 4) rates: one per old row
  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('state_main ', w.state_text, ' not found in state_master - rate not imported')
  FROM item_import_work w
  WHERE w.state_text IS NOT NULL AND NOT EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text);

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, CONCAT('price order wrong (needs MRP >= PTR >= PTS): PTS ', IFNULL(w.pts, '-'), ', PTR ', IFNULL(w.ptr, '-'),
                                     ', MRP ', IFNULL(w.mrp, '-'), ' - rate not imported')
  FROM item_import_work w
  WHERE (w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
     OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0;

  INSERT IGNORE INTO item_rate (item_id, state_id, effective_from, pts, ptr, mrp, purchase_rate, sample_rate, discount_pct,
                                tax_included, legacy_item_id, status, created_by, updated_by)
  SELECT i.id, CAST(w.state_text AS UNSIGNED), p_from, w.pts, w.ptr, w.mrp, w.purchase_rate, w.sample_rate,
         IF(w.discount_pct BETWEEN 0 AND 100, w.discount_pct, NULL), w.tax_included, w.legacy_id, 1, 'import', 'import'
  FROM item_import_work w
  JOIN item_master i ON i.item_code = LEFT(w.code, 50)
  WHERE (w.state_text IS NULL OR EXISTS (SELECT 1 FROM state_master s WHERE CAST(s.oid AS CHAR) = w.state_text))
    AND NOT ((w.mrp IS NOT NULL AND w.ptr IS NOT NULL AND w.mrp < w.ptr) OR (w.ptr IS NOT NULL AND w.pts IS NOT NULL AND w.ptr < w.pts)
             OR LEAST(IFNULL(w.pts, 0), IFNULL(w.ptr, 0), IFNULL(w.mrp, 0), IFNULL(w.purchase_rate, 0)) < 0)
    AND NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
  ORDER BY w.rn;   -- the latest row wins when two rows have the same item_code + state

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'Product rate without PTS / PTR / MRP - imported, fill the prices on the Item page'
  FROM item_import_work w
  JOIN item_rate r ON r.legacy_item_id = w.legacy_id
  JOIN item_master i ON i.id = r.item_id
  JOIN item_type_master t ON t.id = i.item_type_id AND t.item_type_code = 'PRODUCT'
  WHERE r.pts IS NULL OR r.ptr IS NULL OR r.mrp IS NULL;

  INSERT INTO item_import_log (legacy_item_id, item_code, problem)
  SELECT w.legacy_id, w.code, 'another row has the same item_code + state - only the latest row''s rate was imported'
  FROM item_import_work w
  WHERE NOT EXISTS (SELECT 1 FROM item_rate r WHERE r.legacy_item_id = w.legacy_id)
    AND NOT EXISTS (SELECT 1 FROM item_import_log g WHERE g.legacy_item_id = w.legacy_id AND g.problem LIKE '%rate not imported');

  SELECT DATABASE() AS db,
         (SELECT COUNT(*) FROM item_details)                                         AS old_rows,
         (SELECT COUNT(DISTINCT code) FROM item_import_work)                          AS item_codes,
         (SELECT COUNT(*) FROM item_master)                                          AS items_now,
         (SELECT COUNT(*) FROM item_rate WHERE legacy_item_id IS NOT NULL)            AS imported_rates,
         (SELECT COUNT(*) FROM item_import_log)                                      AS problems;
  SELECT legacy_item_id, item_code, problem FROM item_import_log ORDER BY item_code, legacy_item_id;
  DROP TABLE item_import_work;
END$$
DELIMITER ;
CALL sfa_import_item_details(@item_rates_from);
DROP PROCEDURE sfa_import_item_details;
