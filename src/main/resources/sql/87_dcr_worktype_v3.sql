-- ===========================================================================
-- 87: app_dcr_worktype v3 (2026 layout of the old table).
--   * one row = one thing the DCR offers a user: a day type for Start Day (row_type wd / attn)
--     or a tab of the form (av = client check-in, tab = unlisted / joint / expense / day end)
--   * worktype_code = backend code (stored in DCR rows), display_name = label the user sees
--   * emp_id NULL  = MASTER rows; a user with no rows of their own gets the master rows;
--     a user with rows of their own gets ONLY those (copied from master when the employee
--     is created, edited per employee afterwards)
--   Dropped from the old table: type_id, sub_form_type, status (is_enabled replaces it).
-- Safe to re-run (the old v2 table is replaced only once).
-- ===========================================================================
-- Runs for every tenant DB below. Run: mysql -u myroot < 87_dcr_worktype_v3.sql

-- ==================== acme_db ====================
USE acme_db;

SET @old := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'target_type');
SET @s := IF(@old > 0, 'DROP TABLE app_dcr_worktype', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'NULL = master row; else this employee only',
  worktype_code  VARCHAR(30)  NOT NULL COMMENT 'backend code, e.g. WORKING, LEAVE, CLIENT, RETAILER',
  display_name   VARCHAR(60)  NOT NULL COMMENT 'label shown to the user',
  row_type       VARCHAR(5)   NOT NULL COMMENT 'wd = day type, attn = attendance type, av = check-in tab, tab = other tab',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'WORK / ATTN for day types; CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND for tabs',
  client_type_id BIGINT       NULL COMMENT 'check-in tab: only clients of this type',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT '' COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown / tab strip',
  icon           VARCHAR(40)  NULL COMMENT 'bootstrap-icons name shown on the tab / app tile, e.g. person-badge',
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = shown to the user, 0 = hidden (row kept)',
  created_by     VARCHAR(40)  NULL COMMENT 'who added the row (migration / admin / auto-copy)',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when the row was added',
  updated_by     VARCHAR(40)  NULL COMMENT 'last editor',
  updated_at     DATETIME     NULL COMMENT 'last edit time',
  emp_key        VARCHAR(40)  AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows',
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype (emp_key, worktype_code),
  KEY ix_worktype_emp (emp_id),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order) VALUES
  (NULL, 'WORKING',    'Field WORKING', 'wd',   'WORK', NULL, '', 1),
  (NULL, 'ATTENDANCE', 'ATTENDANCE',    'wd',   'ATTN', NULL, '', 2),
  (NULL, 'LEAVE',      'LEAVE',         'wd',   'ATTN', NULL, '', 3),
  (NULL, 'PNR_DAY',    'PNR DAY',       'attn', 'ATTN', NULL, '', 4),
  (NULL, 'R_TRAINING', 'R TRAINING',    'attn', 'ATTN', NULL, '', 5),
  (NULL, 'TRANSIT',    'Transit',       'attn', 'ATTN', NULL, '', 6),
  (NULL, 'CAMP',       'Camp',          'attn', 'ATTN', NULL, '', 7),
  (NULL, 'STUDY',      'STUDY',         'attn', 'ATTN', NULL, '', 8),
  (NULL, 'CDE',        'CDE',           'attn', 'ATTN', NULL, '', 9),
  (NULL, 'RTM',        'RTM',           'attn', 'ATTN', NULL, '', 10),
  (NULL, 'CLIENT',     'Doctor Checkin',    'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'DR' LIMIT 1), 'ITEMS,LEAFLETS,TREATMENT', 10),
  (NULL, 'RETAILER',   'Chemist Check In',  'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'RT' LIMIT 1), 'ITEMS', 20),
  (NULL, 'PARTY',      'Stockist Check in', 'av',  'CHECKIN',  NULL, 'ITEMS', 30),
  (NULL, 'UNLISTED',   'Unlisted Check-in', 'tab', 'UNLISTED', NULL, 'ITEMS,LEAFLETS', 40),
  (NULL, 'JOINT',      'Joint Working',     'tab', 'JOINT',    NULL, '', 50),
  (NULL, 'EXPENSE',    'Daily Expense',     'tab', 'EXPENSE',  NULL, '', 60),
  (NULL, 'DAYEND',     'Day End',           'tab', 'DAYEND',   NULL, '', 70);

-- employees that already exist get their own copy of the master rows
INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order, is_enabled, created_by)
SELECT e.emp_id, w.worktype_code, w.display_name, w.row_type, w.form_open, w.client_type_id, w.sub_forms, w.sort_order, w.is_enabled, 'migration'
FROM emp_detail e JOIN app_dcr_worktype w ON w.emp_id IS NULL;

-- tab icons (only the master rows, once)
UPDATE app_dcr_worktype SET icon = CASE worktype_code WHEN 'CLIENT' THEN 'person-badge' WHEN 'RETAILER' THEN 'shop' WHEN 'PARTY' THEN 'truck'
  WHEN 'UNLISTED' THEN 'person-plus' WHEN 'JOINT' THEN 'people' WHEN 'EXPENSE' THEN 'wallet2' WHEN 'DAYEND' THEN 'moon' END
 WHERE row_type IN ('av','tab') AND icon IS NULL;

-- ==================== globex_db ====================
USE globex_db;

SET @old := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'target_type');
SET @s := IF(@old > 0, 'DROP TABLE app_dcr_worktype', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'NULL = master row; else this employee only',
  worktype_code  VARCHAR(30)  NOT NULL COMMENT 'backend code, e.g. WORKING, LEAVE, CLIENT, RETAILER',
  display_name   VARCHAR(60)  NOT NULL COMMENT 'label shown to the user',
  row_type       VARCHAR(5)   NOT NULL COMMENT 'wd = day type, attn = attendance type, av = check-in tab, tab = other tab',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'WORK / ATTN for day types; CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND for tabs',
  client_type_id BIGINT       NULL COMMENT 'check-in tab: only clients of this type',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT '' COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown / tab strip',
  icon           VARCHAR(40)  NULL COMMENT 'bootstrap-icons name shown on the tab / app tile, e.g. person-badge',
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = shown to the user, 0 = hidden (row kept)',
  created_by     VARCHAR(40)  NULL COMMENT 'who added the row (migration / admin / auto-copy)',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when the row was added',
  updated_by     VARCHAR(40)  NULL COMMENT 'last editor',
  updated_at     DATETIME     NULL COMMENT 'last edit time',
  emp_key        VARCHAR(40)  AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows',
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype (emp_key, worktype_code),
  KEY ix_worktype_emp (emp_id),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order) VALUES
  (NULL, 'WORKING',    'Field WORKING', 'wd',   'WORK', NULL, '', 1),
  (NULL, 'ATTENDANCE', 'ATTENDANCE',    'wd',   'ATTN', NULL, '', 2),
  (NULL, 'LEAVE',      'LEAVE',         'wd',   'ATTN', NULL, '', 3),
  (NULL, 'PNR_DAY',    'PNR DAY',       'attn', 'ATTN', NULL, '', 4),
  (NULL, 'R_TRAINING', 'R TRAINING',    'attn', 'ATTN', NULL, '', 5),
  (NULL, 'TRANSIT',    'Transit',       'attn', 'ATTN', NULL, '', 6),
  (NULL, 'CAMP',       'Camp',          'attn', 'ATTN', NULL, '', 7),
  (NULL, 'STUDY',      'STUDY',         'attn', 'ATTN', NULL, '', 8),
  (NULL, 'CDE',        'CDE',           'attn', 'ATTN', NULL, '', 9),
  (NULL, 'RTM',        'RTM',           'attn', 'ATTN', NULL, '', 10),
  (NULL, 'CLIENT',     'Doctor Checkin',    'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'DR' LIMIT 1), 'ITEMS,LEAFLETS,TREATMENT', 10),
  (NULL, 'RETAILER',   'Chemist Check In',  'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'RT' LIMIT 1), 'ITEMS', 20),
  (NULL, 'PARTY',      'Stockist Check in', 'av',  'CHECKIN',  NULL, 'ITEMS', 30),
  (NULL, 'UNLISTED',   'Unlisted Check-in', 'tab', 'UNLISTED', NULL, 'ITEMS,LEAFLETS', 40),
  (NULL, 'JOINT',      'Joint Working',     'tab', 'JOINT',    NULL, '', 50),
  (NULL, 'EXPENSE',    'Daily Expense',     'tab', 'EXPENSE',  NULL, '', 60),
  (NULL, 'DAYEND',     'Day End',           'tab', 'DAYEND',   NULL, '', 70);

-- employees that already exist get their own copy of the master rows
INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order, is_enabled, created_by)
SELECT e.emp_id, w.worktype_code, w.display_name, w.row_type, w.form_open, w.client_type_id, w.sub_forms, w.sort_order, w.is_enabled, 'migration'
FROM emp_detail e JOIN app_dcr_worktype w ON w.emp_id IS NULL;

-- tab icons (only the master rows, once)
UPDATE app_dcr_worktype SET icon = CASE worktype_code WHEN 'CLIENT' THEN 'person-badge' WHEN 'RETAILER' THEN 'shop' WHEN 'PARTY' THEN 'truck'
  WHEN 'UNLISTED' THEN 'person-plus' WHEN 'JOINT' THEN 'people' WHEN 'EXPENSE' THEN 'wallet2' WHEN 'DAYEND' THEN 'moon' END
 WHERE row_type IN ('av','tab') AND icon IS NULL;

-- ==================== initech_db ====================
USE initech_db;

SET @old := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'target_type');
SET @s := IF(@old > 0, 'DROP TABLE app_dcr_worktype', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'NULL = master row; else this employee only',
  worktype_code  VARCHAR(30)  NOT NULL COMMENT 'backend code, e.g. WORKING, LEAVE, CLIENT, RETAILER',
  display_name   VARCHAR(60)  NOT NULL COMMENT 'label shown to the user',
  row_type       VARCHAR(5)   NOT NULL COMMENT 'wd = day type, attn = attendance type, av = check-in tab, tab = other tab',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'WORK / ATTN for day types; CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND for tabs',
  client_type_id BIGINT       NULL COMMENT 'check-in tab: only clients of this type',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT '' COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown / tab strip',
  icon           VARCHAR(40)  NULL COMMENT 'bootstrap-icons name shown on the tab / app tile, e.g. person-badge',
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = shown to the user, 0 = hidden (row kept)',
  created_by     VARCHAR(40)  NULL COMMENT 'who added the row (migration / admin / auto-copy)',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when the row was added',
  updated_by     VARCHAR(40)  NULL COMMENT 'last editor',
  updated_at     DATETIME     NULL COMMENT 'last edit time',
  emp_key        VARCHAR(40)  AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows',
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype (emp_key, worktype_code),
  KEY ix_worktype_emp (emp_id),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order) VALUES
  (NULL, 'WORKING',    'Field WORKING', 'wd',   'WORK', NULL, '', 1),
  (NULL, 'ATTENDANCE', 'ATTENDANCE',    'wd',   'ATTN', NULL, '', 2),
  (NULL, 'LEAVE',      'LEAVE',         'wd',   'ATTN', NULL, '', 3),
  (NULL, 'PNR_DAY',    'PNR DAY',       'attn', 'ATTN', NULL, '', 4),
  (NULL, 'R_TRAINING', 'R TRAINING',    'attn', 'ATTN', NULL, '', 5),
  (NULL, 'TRANSIT',    'Transit',       'attn', 'ATTN', NULL, '', 6),
  (NULL, 'CAMP',       'Camp',          'attn', 'ATTN', NULL, '', 7),
  (NULL, 'STUDY',      'STUDY',         'attn', 'ATTN', NULL, '', 8),
  (NULL, 'CDE',        'CDE',           'attn', 'ATTN', NULL, '', 9),
  (NULL, 'RTM',        'RTM',           'attn', 'ATTN', NULL, '', 10),
  (NULL, 'CLIENT',     'Doctor Checkin',    'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'DR' LIMIT 1), 'ITEMS,LEAFLETS,TREATMENT', 10),
  (NULL, 'RETAILER',   'Chemist Check In',  'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'RT' LIMIT 1), 'ITEMS', 20),
  (NULL, 'PARTY',      'Stockist Check in', 'av',  'CHECKIN',  NULL, 'ITEMS', 30),
  (NULL, 'UNLISTED',   'Unlisted Check-in', 'tab', 'UNLISTED', NULL, 'ITEMS,LEAFLETS', 40),
  (NULL, 'JOINT',      'Joint Working',     'tab', 'JOINT',    NULL, '', 50),
  (NULL, 'EXPENSE',    'Daily Expense',     'tab', 'EXPENSE',  NULL, '', 60),
  (NULL, 'DAYEND',     'Day End',           'tab', 'DAYEND',   NULL, '', 70);

-- employees that already exist get their own copy of the master rows
INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order, is_enabled, created_by)
SELECT e.emp_id, w.worktype_code, w.display_name, w.row_type, w.form_open, w.client_type_id, w.sub_forms, w.sort_order, w.is_enabled, 'migration'
FROM emp_detail e JOIN app_dcr_worktype w ON w.emp_id IS NULL;

-- tab icons (only the master rows, once)
UPDATE app_dcr_worktype SET icon = CASE worktype_code WHEN 'CLIENT' THEN 'person-badge' WHEN 'RETAILER' THEN 'shop' WHEN 'PARTY' THEN 'truck'
  WHEN 'UNLISTED' THEN 'person-plus' WHEN 'JOINT' THEN 'people' WHEN 'EXPENSE' THEN 'wallet2' WHEN 'DAYEND' THEN 'moon' END
 WHERE row_type IN ('av','tab') AND icon IS NULL;

-- ==================== sfa_demo ====================
USE sfa_demo;

SET @old := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'target_type');
SET @s := IF(@old > 0, 'DROP TABLE app_dcr_worktype', 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

CREATE TABLE IF NOT EXISTS app_dcr_worktype (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'row id',
  emp_id         VARCHAR(40)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'NULL = master row; else this employee only',
  worktype_code  VARCHAR(30)  NOT NULL COMMENT 'backend code, e.g. WORKING, LEAVE, CLIENT, RETAILER',
  display_name   VARCHAR(60)  NOT NULL COMMENT 'label shown to the user',
  row_type       VARCHAR(5)   NOT NULL COMMENT 'wd = day type, attn = attendance type, av = check-in tab, tab = other tab',
  form_open      VARCHAR(12)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'WORK / ATTN for day types; CHECKIN / UNLISTED / EXPENSE / JOINT / DAYEND for tabs',
  client_type_id BIGINT       NULL COMMENT 'check-in tab: only clients of this type',
  sub_forms      VARCHAR(80)  NOT NULL DEFAULT '' COMMENT 'inside a check-in: ITEMS,LEAFLETS,TREATMENT',
  sort_order     SMALLINT     NOT NULL DEFAULT 0 COMMENT 'order in the dropdown / tab strip',
  icon           VARCHAR(40)  NULL COMMENT 'bootstrap-icons name shown on the tab / app tile, e.g. person-badge',
  is_enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 = shown to the user, 0 = hidden (row kept)',
  created_by     VARCHAR(40)  NULL COMMENT 'who added the row (migration / admin / auto-copy)',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'when the row was added',
  updated_by     VARCHAR(40)  NULL COMMENT 'last editor',
  updated_at     DATETIME     NULL COMMENT 'last edit time',
  emp_key        VARCHAR(40)  AS (COALESCE(emp_id, '')) STORED COMMENT 'emp_id or empty - makes (employee, code) unique even for master rows',
  PRIMARY KEY (id),
  UNIQUE KEY uq_worktype (emp_key, worktype_code),
  KEY ix_worktype_emp (emp_id),
  CONSTRAINT fk_worktype_ctype FOREIGN KEY (client_type_id) REFERENCES client_type (oid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order) VALUES
  (NULL, 'WORKING',    'Field WORKING', 'wd',   'WORK', NULL, '', 1),
  (NULL, 'ATTENDANCE', 'ATTENDANCE',    'wd',   'ATTN', NULL, '', 2),
  (NULL, 'LEAVE',      'LEAVE',         'wd',   'ATTN', NULL, '', 3),
  (NULL, 'PNR_DAY',    'PNR DAY',       'attn', 'ATTN', NULL, '', 4),
  (NULL, 'R_TRAINING', 'R TRAINING',    'attn', 'ATTN', NULL, '', 5),
  (NULL, 'TRANSIT',    'Transit',       'attn', 'ATTN', NULL, '', 6),
  (NULL, 'CAMP',       'Camp',          'attn', 'ATTN', NULL, '', 7),
  (NULL, 'STUDY',      'STUDY',         'attn', 'ATTN', NULL, '', 8),
  (NULL, 'CDE',        'CDE',           'attn', 'ATTN', NULL, '', 9),
  (NULL, 'RTM',        'RTM',           'attn', 'ATTN', NULL, '', 10),
  (NULL, 'CLIENT',     'Doctor Checkin',    'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'DR' LIMIT 1), 'ITEMS,LEAFLETS,TREATMENT', 10),
  (NULL, 'RETAILER',   'Chemist Check In',  'av',  'CHECKIN',  (SELECT oid FROM client_type WHERE type_code = 'RT' LIMIT 1), 'ITEMS', 20),
  (NULL, 'PARTY',      'Stockist Check in', 'av',  'CHECKIN',  NULL, 'ITEMS', 30),
  (NULL, 'UNLISTED',   'Unlisted Check-in', 'tab', 'UNLISTED', NULL, 'ITEMS,LEAFLETS', 40),
  (NULL, 'JOINT',      'Joint Working',     'tab', 'JOINT',    NULL, '', 50),
  (NULL, 'EXPENSE',    'Daily Expense',     'tab', 'EXPENSE',  NULL, '', 60),
  (NULL, 'DAYEND',     'Day End',           'tab', 'DAYEND',   NULL, '', 70);

-- employees that already exist get their own copy of the master rows
INSERT IGNORE INTO app_dcr_worktype (emp_id, worktype_code, display_name, row_type, form_open, client_type_id, sub_forms, sort_order, is_enabled, created_by)
SELECT e.emp_id, w.worktype_code, w.display_name, w.row_type, w.form_open, w.client_type_id, w.sub_forms, w.sort_order, w.is_enabled, 'migration'
FROM emp_detail e JOIN app_dcr_worktype w ON w.emp_id IS NULL;

-- tab icons (only the master rows, once)
UPDATE app_dcr_worktype SET icon = CASE worktype_code WHEN 'CLIENT' THEN 'person-badge' WHEN 'RETAILER' THEN 'shop' WHEN 'PARTY' THEN 'truck'
  WHEN 'UNLISTED' THEN 'person-plus' WHEN 'JOINT' THEN 'people' WHEN 'EXPENSE' THEN 'wallet2' WHEN 'DAYEND' THEN 'moon' END
 WHERE row_type IN ('av','tab') AND icon IS NULL;

