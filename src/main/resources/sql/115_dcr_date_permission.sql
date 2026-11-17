-- 115: DCR lock period (emp_app_setting.lock_period_days) + per employee back-date submit / DCR edit permissions (dcr_date_permission) + menu. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS dcr_date_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL,
  perm_type VARCHAR(10) NOT NULL COMMENT 'BACKDATE = may fill the DCR of dcr_from..dcr_to although it is past the lock period; EDIT = may change an already submitted DCR of dcr_from..dcr_to',
  dcr_from DATE NOT NULL, dcr_to DATE NOT NULL COMMENT 'the DCR dates the permission covers',
  allow_from DATE NOT NULL, allow_to DATE NOT NULL COMMENT 'the days on which the employee may use it; after allow_to it is expired and not listed',
  reason VARCHAR(255) NOT NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_dcrperm_emp (emp_id, perm_type, allow_to)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='per employee back-date submit / DCR edit permissions with a validity window';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrrules' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'DCR Permissions', 'Back-date submit and DCR edit permissions', 'unlock', 'dcrpermission', 'master/dcr-permission.html', '_SELF', 54, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dcrpermission');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'dcrpermission';

USE globex_db;

CREATE TABLE IF NOT EXISTS dcr_date_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL,
  perm_type VARCHAR(10) NOT NULL COMMENT 'BACKDATE = may fill the DCR of dcr_from..dcr_to although it is past the lock period; EDIT = may change an already submitted DCR of dcr_from..dcr_to',
  dcr_from DATE NOT NULL, dcr_to DATE NOT NULL COMMENT 'the DCR dates the permission covers',
  allow_from DATE NOT NULL, allow_to DATE NOT NULL COMMENT 'the days on which the employee may use it; after allow_to it is expired and not listed',
  reason VARCHAR(255) NOT NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_dcrperm_emp (emp_id, perm_type, allow_to)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='per employee back-date submit / DCR edit permissions with a validity window';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrrules' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'DCR Permissions', 'Back-date submit and DCR edit permissions', 'unlock', 'dcrpermission', 'master/dcr-permission.html', '_SELF', 54, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dcrpermission');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'dcrpermission';

USE initech_db;

CREATE TABLE IF NOT EXISTS dcr_date_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL,
  perm_type VARCHAR(10) NOT NULL COMMENT 'BACKDATE = may fill the DCR of dcr_from..dcr_to although it is past the lock period; EDIT = may change an already submitted DCR of dcr_from..dcr_to',
  dcr_from DATE NOT NULL, dcr_to DATE NOT NULL COMMENT 'the DCR dates the permission covers',
  allow_from DATE NOT NULL, allow_to DATE NOT NULL COMMENT 'the days on which the employee may use it; after allow_to it is expired and not listed',
  reason VARCHAR(255) NOT NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_dcrperm_emp (emp_id, perm_type, allow_to)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='per employee back-date submit / DCR edit permissions with a validity window';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrrules' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'DCR Permissions', 'Back-date submit and DCR edit permissions', 'unlock', 'dcrpermission', 'master/dcr-permission.html', '_SELF', 54, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dcrpermission');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'dcrpermission';

USE sfa_demo;

CREATE TABLE IF NOT EXISTS dcr_date_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL,
  perm_type VARCHAR(10) NOT NULL COMMENT 'BACKDATE = may fill the DCR of dcr_from..dcr_to although it is past the lock period; EDIT = may change an already submitted DCR of dcr_from..dcr_to',
  dcr_from DATE NOT NULL, dcr_to DATE NOT NULL COMMENT 'the DCR dates the permission covers',
  allow_from DATE NOT NULL, allow_to DATE NOT NULL COMMENT 'the days on which the employee may use it; after allow_to it is expired and not listed',
  reason VARCHAR(255) NOT NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_dcrperm_emp (emp_id, perm_type, allow_to)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='per employee back-date submit / DCR edit permissions with a validity window';

SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrrules' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'attendancerules' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'DCR Permissions', 'Back-date submit and DCR edit permissions', 'unlock', 'dcrpermission', 'master/dcr-permission.html', '_SELF', 54, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dcrpermission');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'dcrpermission';
