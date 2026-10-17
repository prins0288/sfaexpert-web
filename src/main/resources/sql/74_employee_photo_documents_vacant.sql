-- ===========================================================================
-- 74_employee_photo_documents_vacant.sql — PER TENANT database
--
-- Create Employee additions:
--   1. emp_detail.photo_thumbnail_path  photo uploaded from the employee form (same
--      photo_path the top-right avatar uses after login) gets a thumbnail too
--   2. emp_detail.is_vacant             1 = vacant position: that login is refused
--   3. emp_document (from 61)           many documents per employee, type from
--                                       document_master; adds document_no, remarks, thumbnail
--   4. permission USER_PASSWORD_RESET (default DENIED, SUPER_ADMIN always passes) + menu "Password Reset" (next to Create Employee)
--
-- emp_document.emp_id copies emp_detail.emp_id's exact charset/collation (else the FK fails).
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/74_employee_photo_documents_vacant.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'is_vacant');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN is_vacant TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = vacant position, login refused''', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
-- same structure as 61_employee_v2_schema.sql (created there already in most databases)
SET @sql := CONCAT('CREATE TABLE IF NOT EXISTS emp_document (',
  ' id                BIGINT NOT NULL AUTO_INCREMENT,',
  ' emp_id            VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NOT NULL,',
  ' document_type_id  BIGINT UNSIGNED NOT NULL COMMENT ''-> document_master.id'',',
  ' original_name     VARCHAR(255) NOT NULL,',
  ' storage_key       VARCHAR(500) NOT NULL COMMENT ''object key / relative path'',',
  ' mime_type         VARCHAR(100) NULL,',
  ' file_size_bytes   INT UNSIGNED NULL,',
  ' sha256            CHAR(64) NULL,',
  ' is_active         BOOLEAN NOT NULL DEFAULT TRUE,',
  ' uploaded_by       VARCHAR(30) NULL,',
  ' uploaded_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
  ' PRIMARY KEY (id),',
  ' KEY idx_doc_emp_type (emp_id, document_type_id, is_active),',
  ' KEY idx_doc_type (document_type_id),',
  ' CONSTRAINT fk_doc_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),',
  ' CONSTRAINT fk_doc_type FOREIGN KEY (document_type_id) REFERENCES document_master (id)',
  ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'document_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN document_no VARCHAR(50) NULL COMMENT ''number printed on the document'' AFTER document_type_id', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'remarks');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN remarks VARCHAR(255) NULL AFTER document_no', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_path VARCHAR(255) NULL AFTER storage_key', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_type VARCHAR(100) NULL AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- default DENIED (like COLUMN_SETTINGS): grant it to the admins who may reset passwords
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('USER_PASSWORD_RESET', 'Password Reset', 'Reset the login password of other users', 1, 0, NOW(), 'seed');
UPDATE permission_master SET default_allowed = 0 WHERE permission_code = 'USER_PASSWORD_RESET';

-- menu: same parent as Create Employee, else "Master Entry", else Masters (2)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Password Reset', 'User Password Reset', 'key',
       'passwordreset', 'master/password-reset.html', '_SELF', 60, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'passwordreset');

-- ==================== globex_db ====================
USE globex_db;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'is_vacant');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN is_vacant TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = vacant position, login refused''', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
-- same structure as 61_employee_v2_schema.sql (created there already in most databases)
SET @sql := CONCAT('CREATE TABLE IF NOT EXISTS emp_document (',
  ' id                BIGINT NOT NULL AUTO_INCREMENT,',
  ' emp_id            VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NOT NULL,',
  ' document_type_id  BIGINT UNSIGNED NOT NULL COMMENT ''-> document_master.id'',',
  ' original_name     VARCHAR(255) NOT NULL,',
  ' storage_key       VARCHAR(500) NOT NULL COMMENT ''object key / relative path'',',
  ' mime_type         VARCHAR(100) NULL,',
  ' file_size_bytes   INT UNSIGNED NULL,',
  ' sha256            CHAR(64) NULL,',
  ' is_active         BOOLEAN NOT NULL DEFAULT TRUE,',
  ' uploaded_by       VARCHAR(30) NULL,',
  ' uploaded_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
  ' PRIMARY KEY (id),',
  ' KEY idx_doc_emp_type (emp_id, document_type_id, is_active),',
  ' KEY idx_doc_type (document_type_id),',
  ' CONSTRAINT fk_doc_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),',
  ' CONSTRAINT fk_doc_type FOREIGN KEY (document_type_id) REFERENCES document_master (id)',
  ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'document_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN document_no VARCHAR(50) NULL COMMENT ''number printed on the document'' AFTER document_type_id', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'remarks');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN remarks VARCHAR(255) NULL AFTER document_no', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_path VARCHAR(255) NULL AFTER storage_key', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_type VARCHAR(100) NULL AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- default DENIED (like COLUMN_SETTINGS): grant it to the admins who may reset passwords
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('USER_PASSWORD_RESET', 'Password Reset', 'Reset the login password of other users', 1, 0, NOW(), 'seed');
UPDATE permission_master SET default_allowed = 0 WHERE permission_code = 'USER_PASSWORD_RESET';

-- menu: same parent as Create Employee, else "Master Entry", else Masters (2)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Password Reset', 'User Password Reset', 'key',
       'passwordreset', 'master/password-reset.html', '_SELF', 60, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'passwordreset');

-- ==================== initech_db ====================
USE initech_db;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'is_vacant');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN is_vacant TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = vacant position, login refused''', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
-- same structure as 61_employee_v2_schema.sql (created there already in most databases)
SET @sql := CONCAT('CREATE TABLE IF NOT EXISTS emp_document (',
  ' id                BIGINT NOT NULL AUTO_INCREMENT,',
  ' emp_id            VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NOT NULL,',
  ' document_type_id  BIGINT UNSIGNED NOT NULL COMMENT ''-> document_master.id'',',
  ' original_name     VARCHAR(255) NOT NULL,',
  ' storage_key       VARCHAR(500) NOT NULL COMMENT ''object key / relative path'',',
  ' mime_type         VARCHAR(100) NULL,',
  ' file_size_bytes   INT UNSIGNED NULL,',
  ' sha256            CHAR(64) NULL,',
  ' is_active         BOOLEAN NOT NULL DEFAULT TRUE,',
  ' uploaded_by       VARCHAR(30) NULL,',
  ' uploaded_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
  ' PRIMARY KEY (id),',
  ' KEY idx_doc_emp_type (emp_id, document_type_id, is_active),',
  ' KEY idx_doc_type (document_type_id),',
  ' CONSTRAINT fk_doc_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),',
  ' CONSTRAINT fk_doc_type FOREIGN KEY (document_type_id) REFERENCES document_master (id)',
  ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'document_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN document_no VARCHAR(50) NULL COMMENT ''number printed on the document'' AFTER document_type_id', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'remarks');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN remarks VARCHAR(255) NULL AFTER document_no', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_path VARCHAR(255) NULL AFTER storage_key', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_type VARCHAR(100) NULL AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- default DENIED (like COLUMN_SETTINGS): grant it to the admins who may reset passwords
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('USER_PASSWORD_RESET', 'Password Reset', 'Reset the login password of other users', 1, 0, NOW(), 'seed');
UPDATE permission_master SET default_allowed = 0 WHERE permission_code = 'USER_PASSWORD_RESET';

-- menu: same parent as Create Employee, else "Master Entry", else Masters (2)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Password Reset', 'User Password Reset', 'key',
       'passwordreset', 'master/password-reset.html', '_SELF', 60, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'passwordreset');

-- ==================== sfa_demo ====================
USE sfa_demo;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'is_vacant');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_detail ADD COLUMN is_vacant TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = vacant position, login refused''', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
-- same structure as 61_employee_v2_schema.sql (created there already in most databases)
SET @sql := CONCAT('CREATE TABLE IF NOT EXISTS emp_document (',
  ' id                BIGINT NOT NULL AUTO_INCREMENT,',
  ' emp_id            VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NOT NULL,',
  ' document_type_id  BIGINT UNSIGNED NOT NULL COMMENT ''-> document_master.id'',',
  ' original_name     VARCHAR(255) NOT NULL,',
  ' storage_key       VARCHAR(500) NOT NULL COMMENT ''object key / relative path'',',
  ' mime_type         VARCHAR(100) NULL,',
  ' file_size_bytes   INT UNSIGNED NULL,',
  ' sha256            CHAR(64) NULL,',
  ' is_active         BOOLEAN NOT NULL DEFAULT TRUE,',
  ' uploaded_by       VARCHAR(30) NULL,',
  ' uploaded_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),',
  ' PRIMARY KEY (id),',
  ' KEY idx_doc_emp_type (emp_id, document_type_id, is_active),',
  ' KEY idx_doc_type (document_type_id),',
  ' CONSTRAINT fk_doc_emp FOREIGN KEY (emp_id) REFERENCES emp_detail (emp_id),',
  ' CONSTRAINT fk_doc_type FOREIGN KEY (document_type_id) REFERENCES document_master (id)',
  ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'document_no');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN document_no VARCHAR(50) NULL COMMENT ''number printed on the document'' AFTER document_type_id', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'remarks');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN remarks VARCHAR(255) NULL AFTER document_no', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_path VARCHAR(255) NULL AFTER storage_key', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_document' AND COLUMN_NAME = 'thumbnail_type');
SET @sql := IF(@has = 0, 'ALTER TABLE emp_document ADD COLUMN thumbnail_type VARCHAR(100) NULL AFTER thumbnail_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- default DENIED (like COLUMN_SETTINGS): grant it to the admins who may reset passwords
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('USER_PASSWORD_RESET', 'Password Reset', 'Reset the login password of other users', 1, 0, NOW(), 'seed');
UPDATE permission_master SET default_allowed = 0 WHERE permission_code = 'USER_PASSWORD_RESET';

-- menu: same parent as Create Employee, else "Master Entry", else Masters (2)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Password Reset', 'User Password Reset', 'key',
       'passwordreset', 'master/password-reset.html', '_SELF', 60, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'passwordreset');
