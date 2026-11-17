-- 128: area_type_master (HQ 0-50, EX 51-100, OUT EX 101-250, OUT 251+ km), expense_emp_rate (DA / fare per employee per area type), expense settings TYPE_RULE / STAY_FORCES_TYPE, menu Area Type Master. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS area_type_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(10) NOT NULL COMMENT 'stored in area_master.area_type: HQ / EX / OUTEX / OUT ...',
  type_name VARCHAR(40) NOT NULL,
  km_from INT NOT NULL DEFAULT 0 COMMENT 'distance of the area from the HQ: from (km, inclusive)',
  km_to INT NULL COMMENT 'to (km, inclusive); NULL = unlimited',
  sort_order SMALLINT NOT NULL DEFAULT 100 COMMENT 'the higher the order the higher the type (HQ < EX < OUT EX < OUT)',
  is_stay_type TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = the type a day gets when the user stays the night (one type)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_areatype_code (type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='area types with their km range - used in Area Master and for DA / fare in the expense';
INSERT IGNORE INTO area_type_master (type_code, type_name, km_from, km_to, sort_order, is_stay_type) VALUES
 ('HQ','HQ',0,50,10,0),('EX','EX',51,100,20,0),('OUTEX','OUT EX',101,250,30,0),('OUT','OUT',251,NULL,40,1);

CREATE TABLE IF NOT EXISTS expense_emp_rate (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, type_code VARCHAR(10) NOT NULL,
  da_amount DECIMAL(10,2) NULL COMMENT 'daily allowance of this employee on a day of this area type; NULL = the designation / company rate',
  fare_per_km DECIMAL(8,2) NULL COMMENT 'fare per km of this employee on a day of this area type; NULL = the travel mode rate',
  PRIMARY KEY (id), UNIQUE KEY uq_emprate (emp_id, type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DA and fare given to an employee by area type (set in Create Employee); wins over the designation rates';

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('TYPE_RULE','MAX'),('STAY_FORCES_TYPE','Y');

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'areatype';

USE globex_db;

CREATE TABLE IF NOT EXISTS area_type_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(10) NOT NULL COMMENT 'stored in area_master.area_type: HQ / EX / OUTEX / OUT ...',
  type_name VARCHAR(40) NOT NULL,
  km_from INT NOT NULL DEFAULT 0 COMMENT 'distance of the area from the HQ: from (km, inclusive)',
  km_to INT NULL COMMENT 'to (km, inclusive); NULL = unlimited',
  sort_order SMALLINT NOT NULL DEFAULT 100 COMMENT 'the higher the order the higher the type (HQ < EX < OUT EX < OUT)',
  is_stay_type TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = the type a day gets when the user stays the night (one type)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_areatype_code (type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='area types with their km range - used in Area Master and for DA / fare in the expense';
INSERT IGNORE INTO area_type_master (type_code, type_name, km_from, km_to, sort_order, is_stay_type) VALUES
 ('HQ','HQ',0,50,10,0),('EX','EX',51,100,20,0),('OUTEX','OUT EX',101,250,30,0),('OUT','OUT',251,NULL,40,1);

CREATE TABLE IF NOT EXISTS expense_emp_rate (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, type_code VARCHAR(10) NOT NULL,
  da_amount DECIMAL(10,2) NULL COMMENT 'daily allowance of this employee on a day of this area type; NULL = the designation / company rate',
  fare_per_km DECIMAL(8,2) NULL COMMENT 'fare per km of this employee on a day of this area type; NULL = the travel mode rate',
  PRIMARY KEY (id), UNIQUE KEY uq_emprate (emp_id, type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DA and fare given to an employee by area type (set in Create Employee); wins over the designation rates';

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('TYPE_RULE','MAX'),('STAY_FORCES_TYPE','Y');

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'areatype';

USE initech_db;

CREATE TABLE IF NOT EXISTS area_type_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(10) NOT NULL COMMENT 'stored in area_master.area_type: HQ / EX / OUTEX / OUT ...',
  type_name VARCHAR(40) NOT NULL,
  km_from INT NOT NULL DEFAULT 0 COMMENT 'distance of the area from the HQ: from (km, inclusive)',
  km_to INT NULL COMMENT 'to (km, inclusive); NULL = unlimited',
  sort_order SMALLINT NOT NULL DEFAULT 100 COMMENT 'the higher the order the higher the type (HQ < EX < OUT EX < OUT)',
  is_stay_type TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = the type a day gets when the user stays the night (one type)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_areatype_code (type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='area types with their km range - used in Area Master and for DA / fare in the expense';
INSERT IGNORE INTO area_type_master (type_code, type_name, km_from, km_to, sort_order, is_stay_type) VALUES
 ('HQ','HQ',0,50,10,0),('EX','EX',51,100,20,0),('OUTEX','OUT EX',101,250,30,0),('OUT','OUT',251,NULL,40,1);

CREATE TABLE IF NOT EXISTS expense_emp_rate (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, type_code VARCHAR(10) NOT NULL,
  da_amount DECIMAL(10,2) NULL COMMENT 'daily allowance of this employee on a day of this area type; NULL = the designation / company rate',
  fare_per_km DECIMAL(8,2) NULL COMMENT 'fare per km of this employee on a day of this area type; NULL = the travel mode rate',
  PRIMARY KEY (id), UNIQUE KEY uq_emprate (emp_id, type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DA and fare given to an employee by area type (set in Create Employee); wins over the designation rates';

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('TYPE_RULE','MAX'),('STAY_FORCES_TYPE','Y');

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'areatype';

USE sfa_demo;

CREATE TABLE IF NOT EXISTS area_type_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(10) NOT NULL COMMENT 'stored in area_master.area_type: HQ / EX / OUTEX / OUT ...',
  type_name VARCHAR(40) NOT NULL,
  km_from INT NOT NULL DEFAULT 0 COMMENT 'distance of the area from the HQ: from (km, inclusive)',
  km_to INT NULL COMMENT 'to (km, inclusive); NULL = unlimited',
  sort_order SMALLINT NOT NULL DEFAULT 100 COMMENT 'the higher the order the higher the type (HQ < EX < OUT EX < OUT)',
  is_stay_type TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = the type a day gets when the user stays the night (one type)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_areatype_code (type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='area types with their km range - used in Area Master and for DA / fare in the expense';
INSERT IGNORE INTO area_type_master (type_code, type_name, km_from, km_to, sort_order, is_stay_type) VALUES
 ('HQ','HQ',0,50,10,0),('EX','EX',51,100,20,0),('OUTEX','OUT EX',101,250,30,0),('OUT','OUT',251,NULL,40,1);

CREATE TABLE IF NOT EXISTS expense_emp_rate (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, type_code VARCHAR(10) NOT NULL,
  da_amount DECIMAL(10,2) NULL COMMENT 'daily allowance of this employee on a day of this area type; NULL = the designation / company rate',
  fare_per_km DECIMAL(8,2) NULL COMMENT 'fare per km of this employee on a day of this area type; NULL = the travel mode rate',
  PRIMARY KEY (id), UNIQUE KEY uq_emprate (emp_id, type_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DA and fare given to an employee by area type (set in Create Employee); wins over the designation rates';

INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('TYPE_RULE','MAX'),('STAY_FORCES_TYPE','Y');

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 56, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'areatype';
