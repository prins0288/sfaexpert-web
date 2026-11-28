-- 132: expense_var_assign - the variable expense types given to an employee. Safe to re-run.

USE acme_db;
CREATE TABLE IF NOT EXISTS expense_var_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_var_assign (emp_id, expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='which variable expense types an employee can use (DCR, Daily Expense, statement); an employee with no row here can use every variable type';

USE globex_db;
CREATE TABLE IF NOT EXISTS expense_var_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_var_assign (emp_id, expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='which variable expense types an employee can use (DCR, Daily Expense, statement); an employee with no row here can use every variable type';

USE initech_db;
CREATE TABLE IF NOT EXISTS expense_var_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_var_assign (emp_id, expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='which variable expense types an employee can use (DCR, Daily Expense, statement); an employee with no row here can use every variable type';

USE sfa_demo;
CREATE TABLE IF NOT EXISTS expense_var_assign (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, expense_id INT UNSIGNED NOT NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_var_assign (emp_id, expense_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='which variable expense types an employee can use (DCR, Daily Expense, statement); an employee with no row here can use every variable type';
