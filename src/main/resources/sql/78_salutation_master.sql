-- ===========================================================================
-- 78_salutation_master.sql — PER TENANT database
--
-- Salutation (title before a name): Dr., Mr., Mrs., Ms., Miss, Prof. ...
--   salutation_master       the list shown in dropdowns (Master Entry > Salutation)
--   client_master.salutation, emp_detail.salutation
--                             the chosen VALUE itself ("Dr."), not an id — a
--                             value later renamed / deactivated in the master
--                             stays as it was saved on old clients
-- Permission SALUTATION_SAVE, menu "Salutation" next to Client Master.
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/78_salutation_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;

CREATE TABLE IF NOT EXISTS salutation_master (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  salutation     VARCHAR(20)  NOT NULL COMMENT 'saved as text where used, e.g. Dr.',
  display_order  INT          NOT NULL DEFAULT 0,
  is_active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by     VARCHAR(40)  NULL,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by     VARCHAR(40)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_salutation (salutation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO salutation_master (salutation, display_order, created_by) VALUES
  ('Dr.', 1, 'seed'), ('Mr.', 2, 'seed'), ('Mrs.', 3, 'seed'), ('Ms.', 4, 'seed'), ('Miss', 5, 'seed'), ('Prof.', 6, 'seed');

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Dr.'' AFTER client_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Mr.'' AFTER emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('SALUTATION_SAVE', 'Salutation', 'Add / edit / activate Salutation (Dr., Mr. ...)', 1, NOW(), 'seed');

SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE page = 'clientmaster' LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Salutation', 'Salutation Master', 'person-badge',
       'salutationmaster', 'master/salutation-master.html', '_SELF', 21, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'salutationmaster');

-- ==================== globex_db ====================
USE globex_db;

CREATE TABLE IF NOT EXISTS salutation_master (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  salutation     VARCHAR(20)  NOT NULL COMMENT 'saved as text where used, e.g. Dr.',
  display_order  INT          NOT NULL DEFAULT 0,
  is_active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by     VARCHAR(40)  NULL,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by     VARCHAR(40)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_salutation (salutation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO salutation_master (salutation, display_order, created_by) VALUES
  ('Dr.', 1, 'seed'), ('Mr.', 2, 'seed'), ('Mrs.', 3, 'seed'), ('Ms.', 4, 'seed'), ('Miss', 5, 'seed'), ('Prof.', 6, 'seed');

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Dr.'' AFTER client_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Mr.'' AFTER emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('SALUTATION_SAVE', 'Salutation', 'Add / edit / activate Salutation (Dr., Mr. ...)', 1, NOW(), 'seed');

SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE page = 'clientmaster' LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Salutation', 'Salutation Master', 'person-badge',
       'salutationmaster', 'master/salutation-master.html', '_SELF', 21, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'salutationmaster');

-- ==================== initech_db ====================
USE initech_db;

CREATE TABLE IF NOT EXISTS salutation_master (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  salutation     VARCHAR(20)  NOT NULL COMMENT 'saved as text where used, e.g. Dr.',
  display_order  INT          NOT NULL DEFAULT 0,
  is_active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by     VARCHAR(40)  NULL,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by     VARCHAR(40)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_salutation (salutation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO salutation_master (salutation, display_order, created_by) VALUES
  ('Dr.', 1, 'seed'), ('Mr.', 2, 'seed'), ('Mrs.', 3, 'seed'), ('Ms.', 4, 'seed'), ('Miss', 5, 'seed'), ('Prof.', 6, 'seed');

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Dr.'' AFTER client_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Mr.'' AFTER emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('SALUTATION_SAVE', 'Salutation', 'Add / edit / activate Salutation (Dr., Mr. ...)', 1, NOW(), 'seed');

SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE page = 'clientmaster' LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Salutation', 'Salutation Master', 'person-badge',
       'salutationmaster', 'master/salutation-master.html', '_SELF', 21, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'salutationmaster');

-- ==================== sfa_demo ====================
USE sfa_demo;

CREATE TABLE IF NOT EXISTS salutation_master (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  salutation     VARCHAR(20)  NOT NULL COMMENT 'saved as text where used, e.g. Dr.',
  display_order  INT          NOT NULL DEFAULT 0,
  is_active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by     VARCHAR(40)  NULL,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by     VARCHAR(40)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_salutation (salutation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO salutation_master (salutation, display_order, created_by) VALUES
  ('Dr.', 1, 'seed'), ('Mr.', 2, 'seed'), ('Mrs.', 3, 'seed'), ('Ms.', 4, 'seed'), ('Miss', 5, 'seed'), ('Prof.', 6, 'seed');

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Dr.'' AFTER client_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'salutation');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN salutation VARCHAR(20) NULL COMMENT ''value from salutation_master, e.g. Mr.'' AFTER emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('SALUTATION_SAVE', 'Salutation', 'Add / edit / activate Salutation (Dr., Mr. ...)', 1, NOW(), 'seed');

SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE page = 'clientmaster' LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Salutation', 'Salutation Master', 'person-badge',
       'salutationmaster', 'master/salutation-master.html', '_SELF', 21, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'salutationmaster');
