-- 119: area_master: oid -> id, state_oid -> state_id, hq_oid -> hq_id, status char(1) Y/N -> boolean (1 = active). Safe to re-run. Run BEFORE 120.

USE acme_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN oid id BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'state_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN state_oid state_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'hq_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN hq_oid hq_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- status: char(1) 'Y' / 'N'  ->  boolean (tinyint 1): 1 = active, 0 = inactive
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'UPDATE area_master SET status = CASE WHEN status = ''Y'' THEN ''1'' ELSE ''0'' END', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'ALTER TABLE area_master MODIFY COLUMN status TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = active, 0 = inactive"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE globex_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN oid id BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'state_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN state_oid state_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'hq_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN hq_oid hq_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- status: char(1) 'Y' / 'N'  ->  boolean (tinyint 1): 1 = active, 0 = inactive
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'UPDATE area_master SET status = CASE WHEN status = ''Y'' THEN ''1'' ELSE ''0'' END', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'ALTER TABLE area_master MODIFY COLUMN status TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = active, 0 = inactive"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE initech_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN oid id BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'state_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN state_oid state_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'hq_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN hq_oid hq_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- status: char(1) 'Y' / 'N'  ->  boolean (tinyint 1): 1 = active, 0 = inactive
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'UPDATE area_master SET status = CASE WHEN status = ''Y'' THEN ''1'' ELSE ''0'' END', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'ALTER TABLE area_master MODIFY COLUMN status TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = active, 0 = inactive"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

USE sfa_demo;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN oid id BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'state_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN state_oid state_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'hq_oid') > 0, 'ALTER TABLE area_master CHANGE COLUMN hq_oid hq_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- status: char(1) 'Y' / 'N'  ->  boolean (tinyint 1): 1 = active, 0 = inactive
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'UPDATE area_master SET status = CASE WHEN status = ''Y'' THEN ''1'' ELSE ''0'' END', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'status') = 'char',
  'ALTER TABLE area_master MODIFY COLUMN status TINYINT(1) NOT NULL DEFAULT 1 COMMENT "1 = active, 0 = inactive"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
