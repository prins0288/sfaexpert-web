-- 123: manager_client_geo - which clients of his team a senior may check in, and whether the geo check is ON for that manager + client (unique per manager + client). Safe to re-run.

USE acme_db;

-- the rule row of the first version (app_validations is not used for this)
DELETE FROM app_validations WHERE config_code = 'SENIOR_CLIENT_CHECKIN';

CREATE TABLE IF NOT EXISTS manager_client_geo (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the senior / manager who may check in this client',
  client_id BIGINT NOT NULL COMMENT '-> client_master.id (a client of his team)',
  geo_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = his check-in must be at a tagged place of the client (far-away message otherwise); 0 = no location check for him at this client',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mcg_emp_client (emp_id, client_id), KEY ix_mcg_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='clients of the team that a senior may check in, with the geo check on / off for that manager and client';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Senior Client Access', 'Clients of the team a senior can check in, with geo check on / off', 'person-lines-fill', 'seniorclient', 'master/senior-client.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'seniorclient');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'seniorclient';

USE globex_db;

-- the rule row of the first version (app_validations is not used for this)
DELETE FROM app_validations WHERE config_code = 'SENIOR_CLIENT_CHECKIN';

CREATE TABLE IF NOT EXISTS manager_client_geo (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the senior / manager who may check in this client',
  client_id BIGINT NOT NULL COMMENT '-> client_master.id (a client of his team)',
  geo_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = his check-in must be at a tagged place of the client (far-away message otherwise); 0 = no location check for him at this client',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mcg_emp_client (emp_id, client_id), KEY ix_mcg_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='clients of the team that a senior may check in, with the geo check on / off for that manager and client';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Senior Client Access', 'Clients of the team a senior can check in, with geo check on / off', 'person-lines-fill', 'seniorclient', 'master/senior-client.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'seniorclient');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'seniorclient';

USE initech_db;

-- the rule row of the first version (app_validations is not used for this)
DELETE FROM app_validations WHERE config_code = 'SENIOR_CLIENT_CHECKIN';

CREATE TABLE IF NOT EXISTS manager_client_geo (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the senior / manager who may check in this client',
  client_id BIGINT NOT NULL COMMENT '-> client_master.id (a client of his team)',
  geo_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = his check-in must be at a tagged place of the client (far-away message otherwise); 0 = no location check for him at this client',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mcg_emp_client (emp_id, client_id), KEY ix_mcg_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='clients of the team that a senior may check in, with the geo check on / off for that manager and client';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Senior Client Access', 'Clients of the team a senior can check in, with geo check on / off', 'person-lines-fill', 'seniorclient', 'master/senior-client.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'seniorclient');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'seniorclient';

USE sfa_demo;

-- the rule row of the first version (app_validations is not used for this)
DELETE FROM app_validations WHERE config_code = 'SENIOR_CLIENT_CHECKIN';

CREATE TABLE IF NOT EXISTS manager_client_geo (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the senior / manager who may check in this client',
  client_id BIGINT NOT NULL COMMENT '-> client_master.id (a client of his team)',
  geo_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = his check-in must be at a tagged place of the client (far-away message otherwise); 0 = no location check for him at this client',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_mcg_emp_client (emp_id, client_id), KEY ix_mcg_client (client_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='clients of the team that a senior may check in, with the geo check on / off for that manager and client';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrpermission' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Senior Client Access', 'Clients of the team a senior can check in, with geo check on / off', 'person-lines-fill', 'seniorclient', 'master/senior-client.html', '_SELF', 55, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'seniorclient');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'seniorclient';
