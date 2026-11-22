-- 129: fixed and variable expense - expense_master.exp_kind / bill_required, expense_fixed_assign, expense_entry (daily feed with bill), expense_statement_fixed, menus Daily Expense / Expense Master / Fixed Expense. Safe to re-run.

USE acme_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'exp_kind') = 0, 'ALTER TABLE expense_master ADD COLUMN exp_kind VARCHAR(8) NOT NULL DEFAULT ''VARIABLE'' COMMENT ''FIXED = a fixed amount assigned to the employees every month (mobile / internet bill ...); VARIABLE = entered when it happens, with the bill (courier, birthday ...)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'bill_required') = 0, 'ALTER TABLE expense_master ADD COLUMN bill_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = a bill / invoice file is required with a variable expense of this type''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order, is_active) VALUES ('MOBILE','Mobile bill',0,20,1),('INTERNET','Internet bill',0,21,1),('BIRTHDAY','Birthday / wishes',0,22,1);
UPDATE expense_master SET exp_kind = 'FIXED' WHERE expense_code IN ('MOBILE','INTERNET');

CREATE TABLE IF NOT EXISTS expense_fixed_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind FIXED',
  amount DECIMAL(12,2) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_fixed (emp_id, expense_id, effective_from), KEY ix_fixed_exp (expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a fixed monthly expense (mobile bill, internet bill ...) given to an employee; it comes into his monthly statement';

CREATE TABLE IF NOT EXISTS expense_entry (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, exp_date DATE NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind VARIABLE',
  amount DECIMAL(12,2) NOT NULL, description VARCHAR(150) NULL, file_name VARCHAR(255) NULL, file_type VARCHAR(100) NULL, file_path VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_entry_emp (emp_id, exp_date)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a variable expense fed on its day outside the DCR, with its bill; it comes into the statement of the month';

CREATE TABLE IF NOT EXISTS expense_statement_fixed (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, expense_id INT UNSIGNED NOT NULL,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0, orig_amount DECIMAL(12,2) NOT NULL DEFAULT 0, edited_level CHAR(1) NULL, edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_stmt_fixed (statement_id, expense_id), CONSTRAINT fk_stfixed_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the fixed expenses of the month in a statement; the manager / admin can change the amount (logged)';

SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('dailyexpense', 'expensemaster', 'fixedexpense');

USE globex_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'exp_kind') = 0, 'ALTER TABLE expense_master ADD COLUMN exp_kind VARCHAR(8) NOT NULL DEFAULT ''VARIABLE'' COMMENT ''FIXED = a fixed amount assigned to the employees every month (mobile / internet bill ...); VARIABLE = entered when it happens, with the bill (courier, birthday ...)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'bill_required') = 0, 'ALTER TABLE expense_master ADD COLUMN bill_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = a bill / invoice file is required with a variable expense of this type''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order, is_active) VALUES ('MOBILE','Mobile bill',0,20,1),('INTERNET','Internet bill',0,21,1),('BIRTHDAY','Birthday / wishes',0,22,1);
UPDATE expense_master SET exp_kind = 'FIXED' WHERE expense_code IN ('MOBILE','INTERNET');

CREATE TABLE IF NOT EXISTS expense_fixed_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind FIXED',
  amount DECIMAL(12,2) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_fixed (emp_id, expense_id, effective_from), KEY ix_fixed_exp (expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a fixed monthly expense (mobile bill, internet bill ...) given to an employee; it comes into his monthly statement';

CREATE TABLE IF NOT EXISTS expense_entry (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, exp_date DATE NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind VARIABLE',
  amount DECIMAL(12,2) NOT NULL, description VARCHAR(150) NULL, file_name VARCHAR(255) NULL, file_type VARCHAR(100) NULL, file_path VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_entry_emp (emp_id, exp_date)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a variable expense fed on its day outside the DCR, with its bill; it comes into the statement of the month';

CREATE TABLE IF NOT EXISTS expense_statement_fixed (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, expense_id INT UNSIGNED NOT NULL,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0, orig_amount DECIMAL(12,2) NOT NULL DEFAULT 0, edited_level CHAR(1) NULL, edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_stmt_fixed (statement_id, expense_id), CONSTRAINT fk_stfixed_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the fixed expenses of the month in a statement; the manager / admin can change the amount (logged)';

SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('dailyexpense', 'expensemaster', 'fixedexpense');

USE initech_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'exp_kind') = 0, 'ALTER TABLE expense_master ADD COLUMN exp_kind VARCHAR(8) NOT NULL DEFAULT ''VARIABLE'' COMMENT ''FIXED = a fixed amount assigned to the employees every month (mobile / internet bill ...); VARIABLE = entered when it happens, with the bill (courier, birthday ...)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'bill_required') = 0, 'ALTER TABLE expense_master ADD COLUMN bill_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = a bill / invoice file is required with a variable expense of this type''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order, is_active) VALUES ('MOBILE','Mobile bill',0,20,1),('INTERNET','Internet bill',0,21,1),('BIRTHDAY','Birthday / wishes',0,22,1);
UPDATE expense_master SET exp_kind = 'FIXED' WHERE expense_code IN ('MOBILE','INTERNET');

CREATE TABLE IF NOT EXISTS expense_fixed_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind FIXED',
  amount DECIMAL(12,2) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_fixed (emp_id, expense_id, effective_from), KEY ix_fixed_exp (expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a fixed monthly expense (mobile bill, internet bill ...) given to an employee; it comes into his monthly statement';

CREATE TABLE IF NOT EXISTS expense_entry (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, exp_date DATE NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind VARIABLE',
  amount DECIMAL(12,2) NOT NULL, description VARCHAR(150) NULL, file_name VARCHAR(255) NULL, file_type VARCHAR(100) NULL, file_path VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_entry_emp (emp_id, exp_date)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a variable expense fed on its day outside the DCR, with its bill; it comes into the statement of the month';

CREATE TABLE IF NOT EXISTS expense_statement_fixed (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, expense_id INT UNSIGNED NOT NULL,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0, orig_amount DECIMAL(12,2) NOT NULL DEFAULT 0, edited_level CHAR(1) NULL, edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_stmt_fixed (statement_id, expense_id), CONSTRAINT fk_stfixed_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the fixed expenses of the month in a statement; the manager / admin can change the amount (logged)';

SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('dailyexpense', 'expensemaster', 'fixedexpense');

USE sfa_demo;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'exp_kind') = 0, 'ALTER TABLE expense_master ADD COLUMN exp_kind VARCHAR(8) NOT NULL DEFAULT ''VARIABLE'' COMMENT ''FIXED = a fixed amount assigned to the employees every month (mobile / internet bill ...); VARIABLE = entered when it happens, with the bill (courier, birthday ...)''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'expense_master' AND column_name = 'bill_required') = 0, 'ALTER TABLE expense_master ADD COLUMN bill_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = a bill / invoice file is required with a variable expense of this type''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO expense_master (expense_code, expense_name, is_travel, sort_order, is_active) VALUES ('MOBILE','Mobile bill',0,20,1),('INTERNET','Internet bill',0,21,1),('BIRTHDAY','Birthday / wishes',0,22,1);
UPDATE expense_master SET exp_kind = 'FIXED' WHERE expense_code IN ('MOBILE','INTERNET');

CREATE TABLE IF NOT EXISTS expense_fixed_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind FIXED',
  amount DECIMAL(12,2) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_fixed (emp_id, expense_id, effective_from), KEY ix_fixed_exp (expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a fixed monthly expense (mobile bill, internet bill ...) given to an employee; it comes into his monthly statement';

CREATE TABLE IF NOT EXISTS expense_entry (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, exp_date DATE NOT NULL, expense_id INT UNSIGNED NOT NULL COMMENT '-> expense_master.id of kind VARIABLE',
  amount DECIMAL(12,2) NOT NULL, description VARCHAR(150) NULL, file_name VARCHAR(255) NULL, file_type VARCHAR(100) NULL, file_path VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_entry_emp (emp_id, exp_date)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='a variable expense fed on its day outside the DCR, with its bill; it comes into the statement of the month';

CREATE TABLE IF NOT EXISTS expense_statement_fixed (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, statement_id BIGINT UNSIGNED NOT NULL, expense_id INT UNSIGNED NOT NULL,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0, orig_amount DECIMAL(12,2) NOT NULL DEFAULT 0, edited_level CHAR(1) NULL, edited_by VARCHAR(40) NULL, edited_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_stmt_fixed (statement_id, expense_id), CONSTRAINT fk_stfixed_stmt FOREIGN KEY (statement_id) REFERENCES expense_statement (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the fixed expenses of the month in a statement; the manager / admin can change the amount (logged)';

SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('dailyexpense', 'expensemaster', 'fixedexpense');
