-- 133: smtp_setting (company mail server), expense_reminder_log, menu Expense Dashboard. Safe to re-run.

USE acme_db;
CREATE TABLE IF NOT EXISTS smtp_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, host VARCHAR(120) NULL, port INT NOT NULL DEFAULT 587, username VARCHAR(150) NULL, password VARCHAR(200) NULL,
  from_email VARCHAR(150) NULL, from_name VARCHAR(100) NULL, security VARCHAR(5) NOT NULL DEFAULT 'TLS' COMMENT 'TLS (STARTTLS) / SSL / NONE', is_enabled TINYINT(1) NOT NULL DEFAULT 0,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the company mail server (one row): used for the expense reminders and the other system mails';
CREATE TABLE IF NOT EXISTS expense_reminder_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NULL, exp_month TINYINT NOT NULL, exp_year SMALLINT NOT NULL, kind VARCHAR(20) NOT NULL COMMENT 'NOT_SUBMITTED / PENDING',
  sent_to VARCHAR(150) NULL, ok TINYINT(1) NOT NULL DEFAULT 1, error VARCHAR(300) NULL, sent_by VARCHAR(40) NULL, sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_remind (exp_year, exp_month, emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='reminder mails sent from the Expense Dashboard';
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Dashboard', 'All employees of the month: submit, approve, send reminders', 'speedometer2', 'expensedashboard', 'expense/expense-dashboard.html', '_SELF', 0, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensedashboard');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'expensedashboard';

USE globex_db;
CREATE TABLE IF NOT EXISTS smtp_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, host VARCHAR(120) NULL, port INT NOT NULL DEFAULT 587, username VARCHAR(150) NULL, password VARCHAR(200) NULL,
  from_email VARCHAR(150) NULL, from_name VARCHAR(100) NULL, security VARCHAR(5) NOT NULL DEFAULT 'TLS' COMMENT 'TLS (STARTTLS) / SSL / NONE', is_enabled TINYINT(1) NOT NULL DEFAULT 0,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the company mail server (one row): used for the expense reminders and the other system mails';
CREATE TABLE IF NOT EXISTS expense_reminder_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NULL, exp_month TINYINT NOT NULL, exp_year SMALLINT NOT NULL, kind VARCHAR(20) NOT NULL COMMENT 'NOT_SUBMITTED / PENDING',
  sent_to VARCHAR(150) NULL, ok TINYINT(1) NOT NULL DEFAULT 1, error VARCHAR(300) NULL, sent_by VARCHAR(40) NULL, sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_remind (exp_year, exp_month, emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='reminder mails sent from the Expense Dashboard';
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Dashboard', 'All employees of the month: submit, approve, send reminders', 'speedometer2', 'expensedashboard', 'expense/expense-dashboard.html', '_SELF', 0, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensedashboard');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'expensedashboard';

USE initech_db;
CREATE TABLE IF NOT EXISTS smtp_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, host VARCHAR(120) NULL, port INT NOT NULL DEFAULT 587, username VARCHAR(150) NULL, password VARCHAR(200) NULL,
  from_email VARCHAR(150) NULL, from_name VARCHAR(100) NULL, security VARCHAR(5) NOT NULL DEFAULT 'TLS' COMMENT 'TLS (STARTTLS) / SSL / NONE', is_enabled TINYINT(1) NOT NULL DEFAULT 0,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the company mail server (one row): used for the expense reminders and the other system mails';
CREATE TABLE IF NOT EXISTS expense_reminder_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NULL, exp_month TINYINT NOT NULL, exp_year SMALLINT NOT NULL, kind VARCHAR(20) NOT NULL COMMENT 'NOT_SUBMITTED / PENDING',
  sent_to VARCHAR(150) NULL, ok TINYINT(1) NOT NULL DEFAULT 1, error VARCHAR(300) NULL, sent_by VARCHAR(40) NULL, sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_remind (exp_year, exp_month, emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='reminder mails sent from the Expense Dashboard';
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Dashboard', 'All employees of the month: submit, approve, send reminders', 'speedometer2', 'expensedashboard', 'expense/expense-dashboard.html', '_SELF', 0, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensedashboard');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'expensedashboard';

USE sfa_demo;
CREATE TABLE IF NOT EXISTS smtp_setting (
  id TINYINT UNSIGNED NOT NULL DEFAULT 1, host VARCHAR(120) NULL, port INT NOT NULL DEFAULT 587, username VARCHAR(150) NULL, password VARCHAR(200) NULL,
  from_email VARCHAR(150) NULL, from_name VARCHAR(100) NULL, security VARCHAR(5) NOT NULL DEFAULT 'TLS' COMMENT 'TLS (STARTTLS) / SSL / NONE', is_enabled TINYINT(1) NOT NULL DEFAULT 0,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the company mail server (one row): used for the expense reminders and the other system mails';
CREATE TABLE IF NOT EXISTS expense_reminder_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NULL, exp_month TINYINT NOT NULL, exp_year SMALLINT NOT NULL, kind VARCHAR(20) NOT NULL COMMENT 'NOT_SUBMITTED / PENDING',
  sent_to VARCHAR(150) NULL, ok TINYINT(1) NOT NULL DEFAULT 1, error VARCHAR(300) NULL, sent_by VARCHAR(40) NULL, sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_remind (exp_year, exp_month, emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='reminder mails sent from the Expense Dashboard';
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Dashboard', 'All employees of the month: submit, approve, send reminders', 'speedometer2', 'expensedashboard', 'expense/expense-dashboard.html', '_SELF', 0, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensedashboard');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'expensedashboard';
