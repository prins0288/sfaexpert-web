-- 127: Expense module - expense_setting, expense_da_rate, expense_fare_rate, expense_statement (one per employee per month, unique), expense_statement_day, expense_change_log, menu Expense. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS expense_setting (
  setting_key VARCHAR(40) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='START_FROM_STAY_AFTER_OFF = Y | N: after a holiday / weekly off the next working day starts from the stay area (Y) or from the starting point (N)';
INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('START_FROM_STAY_AFTER_OFF','N');

CREATE TABLE IF NOT EXISTS expense_da_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', area_type VARCHAR(10) NOT NULL COMMENT 'HQ / EX / OUT (area_master.area_type of the places visited)', amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_da (desig_key, area_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='daily allowance per designation and area type';
CREATE TABLE IF NOT EXISTS expense_fare_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', travel_mode VARCHAR(100) NOT NULL COMMENT 'travel_type_master.travel_type_name', rate_per_km DECIMAL(8,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_fare (desig_key, travel_mode)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='fare per km per designation and travel mode';

CREATE TABLE IF NOT EXISTS expense_statement (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL, exp_month TINYINT UNSIGNED NOT NULL, exp_year SMALLINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / SUBMITTED / MGR_APPROVED / APPROVED / REJECTED',
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  submitted_at DATETIME NULL, mgr_by VARCHAR(40) NULL, mgr_at DATETIME NULL, admin_by VARCHAR(40) NULL, admin_at DATETIME NULL,
  rejected_by VARCHAR(40) NULL, rejected_at DATETIME NULL, reject_reason VARCHAR(250) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_stmt (emp_id, exp_month, exp_year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one expense statement per employee per month: user submits, the manager approves, then the admin approves';

CREATE TABLE IF NOT EXISTS expense_statement_day (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, exp_date DATE NOT NULL,
  dcr_id BIGINT UNSIGNED NULL, day_type VARCHAR(30) NULL, tp_area VARCHAR(150) NULL, route VARCHAR(500) NULL, visits INT NOT NULL DEFAULT 0, stay_area VARCHAR(150) NULL,
  km DECIMAL(9,1) NOT NULL DEFAULT 0, fare DECIMAL(12,2) NOT NULL DEFAULT 0, da DECIMAL(12,2) NOT NULL DEFAULT 0, travel_exp DECIMAL(12,2) NOT NULL DEFAULT 0,
  heads_json TEXT NULL COMMENT 'amount per expense head: {"expense_id": amount}', bills INT NOT NULL DEFAULT 0, remark VARCHAR(250) NULL, work_with VARCHAR(250) NULL,
  total DECIMAL(12,2) NOT NULL DEFAULT 0,
  orig_json TEXT NULL COMMENT 'what the system calculated (km, fare, da, travel_exp, heads)',
  edited_level CHAR(1) NULL COMMENT 'who changed the amounts last: U user, M manager, A admin; NULL = as calculated',
  edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_day (statement_id, exp_date), CONSTRAINT fk_expday_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS expense_change_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, day_id BIGINT UNSIGNED NULL, exp_date DATE NULL,
  field_name VARCHAR(40) NOT NULL, old_value VARCHAR(100) NULL, new_value VARCHAR(100) NULL,
  changed_by VARCHAR(40) NOT NULL, level CHAR(1) NOT NULL COMMENT 'U / M / A', reason VARCHAR(250) NULL, changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_expchg_stmt (statement_id, day_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every change of an amount, who did it and why - shown to everyone who sees the statement';

-- menu: Expense (root, created when the company has none) > Expense Statement, Expense Setup
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement', 'expensesetup');

USE globex_db;

CREATE TABLE IF NOT EXISTS expense_setting (
  setting_key VARCHAR(40) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='START_FROM_STAY_AFTER_OFF = Y | N: after a holiday / weekly off the next working day starts from the stay area (Y) or from the starting point (N)';
INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('START_FROM_STAY_AFTER_OFF','N');

CREATE TABLE IF NOT EXISTS expense_da_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', area_type VARCHAR(10) NOT NULL COMMENT 'HQ / EX / OUT (area_master.area_type of the places visited)', amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_da (desig_key, area_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='daily allowance per designation and area type';
CREATE TABLE IF NOT EXISTS expense_fare_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', travel_mode VARCHAR(100) NOT NULL COMMENT 'travel_type_master.travel_type_name', rate_per_km DECIMAL(8,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_fare (desig_key, travel_mode)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='fare per km per designation and travel mode';

CREATE TABLE IF NOT EXISTS expense_statement (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL, exp_month TINYINT UNSIGNED NOT NULL, exp_year SMALLINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / SUBMITTED / MGR_APPROVED / APPROVED / REJECTED',
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  submitted_at DATETIME NULL, mgr_by VARCHAR(40) NULL, mgr_at DATETIME NULL, admin_by VARCHAR(40) NULL, admin_at DATETIME NULL,
  rejected_by VARCHAR(40) NULL, rejected_at DATETIME NULL, reject_reason VARCHAR(250) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_stmt (emp_id, exp_month, exp_year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one expense statement per employee per month: user submits, the manager approves, then the admin approves';

CREATE TABLE IF NOT EXISTS expense_statement_day (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, exp_date DATE NOT NULL,
  dcr_id BIGINT UNSIGNED NULL, day_type VARCHAR(30) NULL, tp_area VARCHAR(150) NULL, route VARCHAR(500) NULL, visits INT NOT NULL DEFAULT 0, stay_area VARCHAR(150) NULL,
  km DECIMAL(9,1) NOT NULL DEFAULT 0, fare DECIMAL(12,2) NOT NULL DEFAULT 0, da DECIMAL(12,2) NOT NULL DEFAULT 0, travel_exp DECIMAL(12,2) NOT NULL DEFAULT 0,
  heads_json TEXT NULL COMMENT 'amount per expense head: {"expense_id": amount}', bills INT NOT NULL DEFAULT 0, remark VARCHAR(250) NULL, work_with VARCHAR(250) NULL,
  total DECIMAL(12,2) NOT NULL DEFAULT 0,
  orig_json TEXT NULL COMMENT 'what the system calculated (km, fare, da, travel_exp, heads)',
  edited_level CHAR(1) NULL COMMENT 'who changed the amounts last: U user, M manager, A admin; NULL = as calculated',
  edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_day (statement_id, exp_date), CONSTRAINT fk_expday_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS expense_change_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, day_id BIGINT UNSIGNED NULL, exp_date DATE NULL,
  field_name VARCHAR(40) NOT NULL, old_value VARCHAR(100) NULL, new_value VARCHAR(100) NULL,
  changed_by VARCHAR(40) NOT NULL, level CHAR(1) NOT NULL COMMENT 'U / M / A', reason VARCHAR(250) NULL, changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_expchg_stmt (statement_id, day_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every change of an amount, who did it and why - shown to everyone who sees the statement';

-- menu: Expense (root, created when the company has none) > Expense Statement, Expense Setup
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement', 'expensesetup');

USE initech_db;

CREATE TABLE IF NOT EXISTS expense_setting (
  setting_key VARCHAR(40) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='START_FROM_STAY_AFTER_OFF = Y | N: after a holiday / weekly off the next working day starts from the stay area (Y) or from the starting point (N)';
INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('START_FROM_STAY_AFTER_OFF','N');

CREATE TABLE IF NOT EXISTS expense_da_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', area_type VARCHAR(10) NOT NULL COMMENT 'HQ / EX / OUT (area_master.area_type of the places visited)', amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_da (desig_key, area_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='daily allowance per designation and area type';
CREATE TABLE IF NOT EXISTS expense_fare_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', travel_mode VARCHAR(100) NOT NULL COMMENT 'travel_type_master.travel_type_name', rate_per_km DECIMAL(8,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_fare (desig_key, travel_mode)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='fare per km per designation and travel mode';

CREATE TABLE IF NOT EXISTS expense_statement (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL, exp_month TINYINT UNSIGNED NOT NULL, exp_year SMALLINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / SUBMITTED / MGR_APPROVED / APPROVED / REJECTED',
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  submitted_at DATETIME NULL, mgr_by VARCHAR(40) NULL, mgr_at DATETIME NULL, admin_by VARCHAR(40) NULL, admin_at DATETIME NULL,
  rejected_by VARCHAR(40) NULL, rejected_at DATETIME NULL, reject_reason VARCHAR(250) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_stmt (emp_id, exp_month, exp_year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one expense statement per employee per month: user submits, the manager approves, then the admin approves';

CREATE TABLE IF NOT EXISTS expense_statement_day (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, exp_date DATE NOT NULL,
  dcr_id BIGINT UNSIGNED NULL, day_type VARCHAR(30) NULL, tp_area VARCHAR(150) NULL, route VARCHAR(500) NULL, visits INT NOT NULL DEFAULT 0, stay_area VARCHAR(150) NULL,
  km DECIMAL(9,1) NOT NULL DEFAULT 0, fare DECIMAL(12,2) NOT NULL DEFAULT 0, da DECIMAL(12,2) NOT NULL DEFAULT 0, travel_exp DECIMAL(12,2) NOT NULL DEFAULT 0,
  heads_json TEXT NULL COMMENT 'amount per expense head: {"expense_id": amount}', bills INT NOT NULL DEFAULT 0, remark VARCHAR(250) NULL, work_with VARCHAR(250) NULL,
  total DECIMAL(12,2) NOT NULL DEFAULT 0,
  orig_json TEXT NULL COMMENT 'what the system calculated (km, fare, da, travel_exp, heads)',
  edited_level CHAR(1) NULL COMMENT 'who changed the amounts last: U user, M manager, A admin; NULL = as calculated',
  edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_day (statement_id, exp_date), CONSTRAINT fk_expday_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS expense_change_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, day_id BIGINT UNSIGNED NULL, exp_date DATE NULL,
  field_name VARCHAR(40) NOT NULL, old_value VARCHAR(100) NULL, new_value VARCHAR(100) NULL,
  changed_by VARCHAR(40) NOT NULL, level CHAR(1) NOT NULL COMMENT 'U / M / A', reason VARCHAR(250) NULL, changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_expchg_stmt (statement_id, day_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every change of an amount, who did it and why - shown to everyone who sees the statement';

-- menu: Expense (root, created when the company has none) > Expense Statement, Expense Setup
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement', 'expensesetup');

USE sfa_demo;

CREATE TABLE IF NOT EXISTS expense_setting (
  setting_key VARCHAR(40) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='START_FROM_STAY_AFTER_OFF = Y | N: after a holiday / weekly off the next working day starts from the stay area (Y) or from the starting point (N)';
INSERT IGNORE INTO expense_setting (setting_key, setting_value) VALUES ('START_FROM_STAY_AFTER_OFF','N');

CREATE TABLE IF NOT EXISTS expense_da_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', area_type VARCHAR(10) NOT NULL COMMENT 'HQ / EX / OUT (area_master.area_type of the places visited)', amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_da (desig_key, area_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='daily allowance per designation and area type';
CREATE TABLE IF NOT EXISTS expense_fare_rate (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, designation_id INT NULL COMMENT 'NULL = every designation', travel_mode VARCHAR(100) NOT NULL COMMENT 'travel_type_master.travel_type_name', rate_per_km DECIMAL(8,2) NOT NULL DEFAULT 0,
  desig_key INT GENERATED ALWAYS AS (COALESCE(designation_id, 0)) STORED,
  PRIMARY KEY (id), UNIQUE KEY uq_fare (desig_key, travel_mode)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='fare per km per designation and travel mode';

CREATE TABLE IF NOT EXISTS expense_statement (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL, exp_month TINYINT UNSIGNED NOT NULL, exp_year SMALLINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / SUBMITTED / MGR_APPROVED / APPROVED / REJECTED',
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  submitted_at DATETIME NULL, mgr_by VARCHAR(40) NULL, mgr_at DATETIME NULL, admin_by VARCHAR(40) NULL, admin_at DATETIME NULL,
  rejected_by VARCHAR(40) NULL, rejected_at DATETIME NULL, reject_reason VARCHAR(250) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_stmt (emp_id, exp_month, exp_year)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one expense statement per employee per month: user submits, the manager approves, then the admin approves';

CREATE TABLE IF NOT EXISTS expense_statement_day (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, exp_date DATE NOT NULL,
  dcr_id BIGINT UNSIGNED NULL, day_type VARCHAR(30) NULL, tp_area VARCHAR(150) NULL, route VARCHAR(500) NULL, visits INT NOT NULL DEFAULT 0, stay_area VARCHAR(150) NULL,
  km DECIMAL(9,1) NOT NULL DEFAULT 0, fare DECIMAL(12,2) NOT NULL DEFAULT 0, da DECIMAL(12,2) NOT NULL DEFAULT 0, travel_exp DECIMAL(12,2) NOT NULL DEFAULT 0,
  heads_json TEXT NULL COMMENT 'amount per expense head: {"expense_id": amount}', bills INT NOT NULL DEFAULT 0, remark VARCHAR(250) NULL, work_with VARCHAR(250) NULL,
  total DECIMAL(12,2) NOT NULL DEFAULT 0,
  orig_json TEXT NULL COMMENT 'what the system calculated (km, fare, da, travel_exp, heads)',
  edited_level CHAR(1) NULL COMMENT 'who changed the amounts last: U user, M manager, A admin; NULL = as calculated',
  edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_exp_day (statement_id, exp_date), CONSTRAINT fk_expday_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS expense_change_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, day_id BIGINT UNSIGNED NULL, exp_date DATE NULL,
  field_name VARCHAR(40) NOT NULL, old_value VARCHAR(100) NULL, new_value VARCHAR(100) NULL,
  changed_by VARCHAR(40) NOT NULL, level CHAR(1) NOT NULL COMMENT 'U / M / A', reason VARCHAR(250) NULL, changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_expchg_stmt (statement_id, day_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='every change of an amount, who did it and why - shown to everyone who sees the statement';

-- menu: Expense (root, created when the company has none) > Expense Statement, Expense Setup
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement', 'expensesetup');
