-- 111: geo fencing: location + address stored at Start Day and every check-in, client tag (latitude / longitude / gps_address / has_geo_tag), retag requests with approval, rules GEO_FENCING_ALLOW / GEO_FENCING_DISTANCE. Safe to re-run.

USE acme_db;

-- columns (each added only when missing)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_master' AND column_name = 'gps_address') = 0,
  'ALTER TABLE client_master ADD COLUMN gps_address VARCHAR(500) NULL COMMENT "address of the tagged location" AFTER has_geo_tag, ADD COLUMN tagged_at DATETIME NULL, ADD COLUMN tagged_by VARCHAR(40) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE client_master SET has_geo_tag = 1 WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND COALESCE(has_geo_tag, 0) = 0;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN start_lat DECIMAL(10,7) NULL, ADD COLUMN start_lng DECIMAL(10,7) NULL, ADD COLUMN start_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL, ADD COLUMN checkin_distance_m INT NULL COMMENT "metres from the tagged client location at check-in"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

CREATE TABLE IF NOT EXISTS client_retag_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, client_id BIGINT UNSIGNED NOT NULL,
  requested_by VARCHAR(40) NOT NULL, manager_id VARCHAR(40) NULL, requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  new_lat DECIMAL(10,7) NOT NULL, new_lng DECIMAL(10,7) NOT NULL, new_address VARCHAR(500) NULL,
  old_lat DECIMAL(10,7) NULL, old_lng DECIMAL(10,7) NULL, old_address VARCHAR(500) NULL, moved_m INT NULL COMMENT 'metres between the old and the new tag',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
  decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_retag_status (status, requested_at), KEY ix_retag_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a request to (re)tag the location of a client; approved by the manager / admin';

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'GEO_FENCING_ALLOW','Location check (geo fencing)','DCR','GEO','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the current location is captured and checked at Start Day and at every client / unlisted check-in (and stored with address). FALSE = no location check.',30),
 (NULL,'GEO_FENCING_DISTANCE','Allowed distance from the client (metres)','DCR','GEO','APP','LIMIT','INTEGER','200','200','A client check-in is allowed only when the user is within this many metres of the client tagged location (client_master latitude / longitude). Works only while GEO_FENCING_ALLOW is TRUE.',31);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('GEO_FENCING_ALLOW','GEO_FENCING_DISTANCE');

SET @gp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @gp, p.emp_id, 'WEB', 'Client Retag Approval', 'Approve client location (re)tag requests', 'geo-alt', 'clientretag', 'master/client-retag.html', '_SELF', 52, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @gp LIMIT 1) p
WHERE @gp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'clientretag');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'clientretag';

USE globex_db;

-- columns (each added only when missing)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_master' AND column_name = 'gps_address') = 0,
  'ALTER TABLE client_master ADD COLUMN gps_address VARCHAR(500) NULL COMMENT "address of the tagged location" AFTER has_geo_tag, ADD COLUMN tagged_at DATETIME NULL, ADD COLUMN tagged_by VARCHAR(40) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE client_master SET has_geo_tag = 1 WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND COALESCE(has_geo_tag, 0) = 0;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN start_lat DECIMAL(10,7) NULL, ADD COLUMN start_lng DECIMAL(10,7) NULL, ADD COLUMN start_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL, ADD COLUMN checkin_distance_m INT NULL COMMENT "metres from the tagged client location at check-in"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

CREATE TABLE IF NOT EXISTS client_retag_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, client_id BIGINT UNSIGNED NOT NULL,
  requested_by VARCHAR(40) NOT NULL, manager_id VARCHAR(40) NULL, requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  new_lat DECIMAL(10,7) NOT NULL, new_lng DECIMAL(10,7) NOT NULL, new_address VARCHAR(500) NULL,
  old_lat DECIMAL(10,7) NULL, old_lng DECIMAL(10,7) NULL, old_address VARCHAR(500) NULL, moved_m INT NULL COMMENT 'metres between the old and the new tag',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
  decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_retag_status (status, requested_at), KEY ix_retag_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a request to (re)tag the location of a client; approved by the manager / admin';

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'GEO_FENCING_ALLOW','Location check (geo fencing)','DCR','GEO','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the current location is captured and checked at Start Day and at every client / unlisted check-in (and stored with address). FALSE = no location check.',30),
 (NULL,'GEO_FENCING_DISTANCE','Allowed distance from the client (metres)','DCR','GEO','APP','LIMIT','INTEGER','200','200','A client check-in is allowed only when the user is within this many metres of the client tagged location (client_master latitude / longitude). Works only while GEO_FENCING_ALLOW is TRUE.',31);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('GEO_FENCING_ALLOW','GEO_FENCING_DISTANCE');

SET @gp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @gp, p.emp_id, 'WEB', 'Client Retag Approval', 'Approve client location (re)tag requests', 'geo-alt', 'clientretag', 'master/client-retag.html', '_SELF', 52, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @gp LIMIT 1) p
WHERE @gp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'clientretag');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'clientretag';

USE initech_db;

-- columns (each added only when missing)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_master' AND column_name = 'gps_address') = 0,
  'ALTER TABLE client_master ADD COLUMN gps_address VARCHAR(500) NULL COMMENT "address of the tagged location" AFTER has_geo_tag, ADD COLUMN tagged_at DATETIME NULL, ADD COLUMN tagged_by VARCHAR(40) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE client_master SET has_geo_tag = 1 WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND COALESCE(has_geo_tag, 0) = 0;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN start_lat DECIMAL(10,7) NULL, ADD COLUMN start_lng DECIMAL(10,7) NULL, ADD COLUMN start_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL, ADD COLUMN checkin_distance_m INT NULL COMMENT "metres from the tagged client location at check-in"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

CREATE TABLE IF NOT EXISTS client_retag_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, client_id BIGINT UNSIGNED NOT NULL,
  requested_by VARCHAR(40) NOT NULL, manager_id VARCHAR(40) NULL, requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  new_lat DECIMAL(10,7) NOT NULL, new_lng DECIMAL(10,7) NOT NULL, new_address VARCHAR(500) NULL,
  old_lat DECIMAL(10,7) NULL, old_lng DECIMAL(10,7) NULL, old_address VARCHAR(500) NULL, moved_m INT NULL COMMENT 'metres between the old and the new tag',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
  decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_retag_status (status, requested_at), KEY ix_retag_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a request to (re)tag the location of a client; approved by the manager / admin';

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'GEO_FENCING_ALLOW','Location check (geo fencing)','DCR','GEO','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the current location is captured and checked at Start Day and at every client / unlisted check-in (and stored with address). FALSE = no location check.',30),
 (NULL,'GEO_FENCING_DISTANCE','Allowed distance from the client (metres)','DCR','GEO','APP','LIMIT','INTEGER','200','200','A client check-in is allowed only when the user is within this many metres of the client tagged location (client_master latitude / longitude). Works only while GEO_FENCING_ALLOW is TRUE.',31);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('GEO_FENCING_ALLOW','GEO_FENCING_DISTANCE');

SET @gp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @gp, p.emp_id, 'WEB', 'Client Retag Approval', 'Approve client location (re)tag requests', 'geo-alt', 'clientretag', 'master/client-retag.html', '_SELF', 52, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @gp LIMIT 1) p
WHERE @gp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'clientretag');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'clientretag';

USE sfa_demo;

-- columns (each added only when missing)
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_master' AND column_name = 'gps_address') = 0,
  'ALTER TABLE client_master ADD COLUMN gps_address VARCHAR(500) NULL COMMENT "address of the tagged location" AFTER has_geo_tag, ADD COLUMN tagged_at DATETIME NULL, ADD COLUMN tagged_by VARCHAR(40) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE client_master SET has_geo_tag = 1 WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND COALESCE(has_geo_tag, 0) = 0;

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_parent_summary' AND column_name = 'start_lat') = 0,
  'ALTER TABLE dcr_parent_summary ADD COLUMN start_lat DECIMAL(10,7) NULL, ADD COLUMN start_lng DECIMAL(10,7) NULL, ADD COLUMN start_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL, ADD COLUMN checkin_distance_m INT NULL COMMENT "metres from the tagged client location at check-in"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcr_unlisted_client_summary' AND column_name = 'checkin_address') = 0,
  'ALTER TABLE dcr_unlisted_client_summary ADD COLUMN checkin_address VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

CREATE TABLE IF NOT EXISTS client_retag_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, client_id BIGINT UNSIGNED NOT NULL,
  requested_by VARCHAR(40) NOT NULL, manager_id VARCHAR(40) NULL, requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  new_lat DECIMAL(10,7) NOT NULL, new_lng DECIMAL(10,7) NOT NULL, new_address VARCHAR(500) NULL,
  old_lat DECIMAL(10,7) NULL, old_lng DECIMAL(10,7) NULL, old_address VARCHAR(500) NULL, moved_m INT NULL COMMENT 'metres between the old and the new tag',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
  decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_retag_status (status, requested_at), KEY ix_retag_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a request to (re)tag the location of a client; approved by the manager / admin';

INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order) VALUES
 (NULL,'GEO_FENCING_ALLOW','Location check (geo fencing)','DCR','GEO','APP','VALIDATION','BOOLEAN','FALSE','FALSE','TRUE = the current location is captured and checked at Start Day and at every client / unlisted check-in (and stored with address). FALSE = no location check.',30),
 (NULL,'GEO_FENCING_DISTANCE','Allowed distance from the client (metres)','DCR','GEO','APP','LIMIT','INTEGER','200','200','A client check-in is allowed only when the user is within this many metres of the client tagged location (client_master latitude / longitude). Works only while GEO_FENCING_ALLOW is TRUE.',31);
INSERT IGNORE INTO app_validations (emp_id, config_code, display_name, module_name, screen_name, platform, config_type, data_type, config_value, default_value, description, display_order)
SELECT e.emp_id, v.config_code, v.display_name, v.module_name, v.screen_name, v.platform, v.config_type, v.data_type, v.config_value, v.default_value, v.description, v.display_order
FROM emp_detail e JOIN app_validations v ON v.emp_id IS NULL AND v.module_name = 'DCR' AND v.config_code IN ('GEO_FENCING_ALLOW','GEO_FENCING_DISTANCE');

SET @gp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @gp, p.emp_id, 'WEB', 'Client Retag Approval', 'Approve client location (re)tag requests', 'geo-alt', 'clientretag', 'master/client-retag.html', '_SELF', 52, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @gp LIMIT 1) p
WHERE @gp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'clientretag');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'clientretag';
