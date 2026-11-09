-- 91: DCR page - nothing hard-coded: item type -> DCR panel, visit priorities. Safe to re-run.

USE acme_db;
-- item types: which DCR check-out panel offers them (was hard-coded PRODUCT / SAMPLE / INPUT in the page)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'item_type_master' AND column_name = 'dcr_panel') = 0,
 "ALTER TABLE item_type_master ADD dcr_panel VARCHAR(10) NULL COMMENT 'DCR check-out panel that lists this item type: POB (detailed + POB qty) / SAMPLE / INPUT; NULL = not offered in the DCR' AFTER description", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
UPDATE item_type_master SET dcr_panel = CASE item_type_code WHEN 'PRODUCT' THEN 'POB' WHEN 'SAMPLE' THEN 'SAMPLE' WHEN 'INPUT' THEN 'INPUT' END WHERE dcr_panel IS NULL;

-- next-visit priorities (was hard-coded LOW / MEDIUM / HIGH in the page)
CREATE TABLE IF NOT EXISTS visit_priority_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  priority_name VARCHAR(30)  NOT NULL COMMENT 'shown and stored on the visit',
  sort_order    SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = offered',
  PRIMARY KEY (id),
  UNIQUE KEY uq_priority (priority_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO visit_priority_master (priority_name, sort_order) VALUES ('LOW', 1), ('MEDIUM', 2), ('HIGH', 3);

USE globex_db;
-- item types: which DCR check-out panel offers them (was hard-coded PRODUCT / SAMPLE / INPUT in the page)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'item_type_master' AND column_name = 'dcr_panel') = 0,
 "ALTER TABLE item_type_master ADD dcr_panel VARCHAR(10) NULL COMMENT 'DCR check-out panel that lists this item type: POB (detailed + POB qty) / SAMPLE / INPUT; NULL = not offered in the DCR' AFTER description", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
UPDATE item_type_master SET dcr_panel = CASE item_type_code WHEN 'PRODUCT' THEN 'POB' WHEN 'SAMPLE' THEN 'SAMPLE' WHEN 'INPUT' THEN 'INPUT' END WHERE dcr_panel IS NULL;

-- next-visit priorities (was hard-coded LOW / MEDIUM / HIGH in the page)
CREATE TABLE IF NOT EXISTS visit_priority_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  priority_name VARCHAR(30)  NOT NULL COMMENT 'shown and stored on the visit',
  sort_order    SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = offered',
  PRIMARY KEY (id),
  UNIQUE KEY uq_priority (priority_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO visit_priority_master (priority_name, sort_order) VALUES ('LOW', 1), ('MEDIUM', 2), ('HIGH', 3);

USE initech_db;
-- item types: which DCR check-out panel offers them (was hard-coded PRODUCT / SAMPLE / INPUT in the page)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'item_type_master' AND column_name = 'dcr_panel') = 0,
 "ALTER TABLE item_type_master ADD dcr_panel VARCHAR(10) NULL COMMENT 'DCR check-out panel that lists this item type: POB (detailed + POB qty) / SAMPLE / INPUT; NULL = not offered in the DCR' AFTER description", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
UPDATE item_type_master SET dcr_panel = CASE item_type_code WHEN 'PRODUCT' THEN 'POB' WHEN 'SAMPLE' THEN 'SAMPLE' WHEN 'INPUT' THEN 'INPUT' END WHERE dcr_panel IS NULL;

-- next-visit priorities (was hard-coded LOW / MEDIUM / HIGH in the page)
CREATE TABLE IF NOT EXISTS visit_priority_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  priority_name VARCHAR(30)  NOT NULL COMMENT 'shown and stored on the visit',
  sort_order    SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = offered',
  PRIMARY KEY (id),
  UNIQUE KEY uq_priority (priority_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO visit_priority_master (priority_name, sort_order) VALUES ('LOW', 1), ('MEDIUM', 2), ('HIGH', 3);

USE sfa_demo;
-- item types: which DCR check-out panel offers them (was hard-coded PRODUCT / SAMPLE / INPUT in the page)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'item_type_master' AND column_name = 'dcr_panel') = 0,
 "ALTER TABLE item_type_master ADD dcr_panel VARCHAR(10) NULL COMMENT 'DCR check-out panel that lists this item type: POB (detailed + POB qty) / SAMPLE / INPUT; NULL = not offered in the DCR' AFTER description", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
UPDATE item_type_master SET dcr_panel = CASE item_type_code WHEN 'PRODUCT' THEN 'POB' WHEN 'SAMPLE' THEN 'SAMPLE' WHEN 'INPUT' THEN 'INPUT' END WHERE dcr_panel IS NULL;

-- next-visit priorities (was hard-coded LOW / MEDIUM / HIGH in the page)
CREATE TABLE IF NOT EXISTS visit_priority_master (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  priority_name VARCHAR(30)  NOT NULL COMMENT 'shown and stored on the visit',
  sort_order    SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = offered',
  PRIMARY KEY (id),
  UNIQUE KEY uq_priority (priority_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
INSERT IGNORE INTO visit_priority_master (priority_name, sort_order) VALUES ('LOW', 1), ('MEDIUM', 2), ('HIGH', 3);
