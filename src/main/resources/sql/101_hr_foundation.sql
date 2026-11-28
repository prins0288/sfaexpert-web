-- 101: HR foundation (Phase 1 of HR + Leave + Payroll): company / branch / department / grade / cost centre / employee type, HR employment facts,
-- roles + permission matrix, append-only audit log, effective-dated settings framework (+ default values from the requirements, to verify with a CA). Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS hr_company (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, legal_name VARCHAR(150) NOT NULL, trade_name VARCHAR(150) NULL,
  pan VARCHAR(10) NULL, tan VARCHAR(10) NULL, gstin VARCHAR(15) NULL, cin VARCHAR(25) NULL,
  pf_code VARCHAR(30) NULL COMMENT 'PF establishment code', esi_code VARCHAR(30) NULL COMMENT 'ESI employer code',
  address VARCHAR(300) NULL, fy_start_month TINYINT NOT NULL DEFAULT 4 COMMENT 'financial year starts in this month (1-12)',
  leave_year_mode VARCHAR(12) NOT NULL DEFAULT 'FINANCIAL' COMMENT 'FINANCIAL / CALENDAR / JOINING',
  payroll_start_day TINYINT NOT NULL DEFAULT 1 COMMENT 'payroll cycle starts on this day of month',
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='legal entity (more than one can live in one tenant DB)';
INSERT INTO hr_company (id, legal_name) SELECT 1, 'My Company' WHERE NOT EXISTS (SELECT 1 FROM hr_company);

CREATE TABLE IF NOT EXISTS hr_branch (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  state_id BIGINT NULL COMMENT '-> state_master.oid; PT / LWF depend on it', city VARCHAR(80) NULL, address VARCHAR(300) NULL,
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', pt_reg_no VARCHAR(40) NULL, lwf_reg_no VARCHAR(40) NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_branch (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_department (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_department (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_grade (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_grade (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_cost_center (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_cost_center (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_employee_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  pf_applicable TINYINT(1) NOT NULL DEFAULT 1, esi_applicable TINYINT(1) NOT NULL DEFAULT 1, leave_applicable TINYINT(1) NOT NULL DEFAULT 1,
  compoff_applicable TINYINT(1) NOT NULL DEFAULT 1, probation_applicable TINYINT(1) NOT NULL DEFAULT 0, bonus_applicable TINYINT(1) NOT NULL DEFAULT 1,
  gratuity_applicable TINYINT(1) NOT NULL DEFAULT 1,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_emptype (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO hr_employee_type (company_id, code, name, pf_applicable, esi_applicable, leave_applicable, compoff_applicable, probation_applicable, bonus_applicable, gratuity_applicable, sort_order) VALUES
 (1,'PERM','Permanent',1,1,1,1,0,1,1,1),(1,'PROB','Probation',1,1,1,0,1,1,0,2),(1,'CONTRACT','Contract',1,1,0,0,0,0,0,3),
 (1,'FIXED','Fixed-term',1,1,1,1,0,1,0,4),(1,'INTERN','Intern',0,0,0,0,0,0,0,5),(1,'CONSULT','Consultant',0,0,0,0,0,0,0,6);

CREATE TABLE IF NOT EXISTS hr_emp_employment (
  emp_id VARCHAR(40) NOT NULL COMMENT '-> emp_detail.emp_id', company_id INT UNSIGNED NOT NULL DEFAULT 1,
  branch_id INT UNSIGNED NULL, department_id INT UNSIGNED NULL, grade_id INT UNSIGNED NULL, cost_center_id INT UNSIGNED NULL, employee_type_id INT UNSIGNED NULL,
  second_manager_id VARCHAR(40) NULL COMMENT 'optional second-level approver',
  probation_months TINYINT NULL, probation_end_date DATE NULL, notice_days SMALLINT NULL,
  tax_regime VARCHAR(4) NOT NULL DEFAULT 'NEW' COMMENT 'NEW / OLD', is_disabled TINYINT(1) NOT NULL DEFAULT 0,
  pf_member_since DATE NULL, prev_pf_member TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'was a PF member before this company (affects the EPS / ceiling rule)',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (emp_id), KEY ix_hr_emp_branch (branch_id), KEY ix_hr_emp_dept (department_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR facts of an employee; the person stays in emp_detail';

CREATE TABLE IF NOT EXISTS hr_role (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, code VARCHAR(30) NOT NULL, name VARCHAR(80) NOT NULL, description VARCHAR(255) NULL,
  is_system TINYINT(1) NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_role (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_permission_def (
  code VARCHAR(40) NOT NULL, name VARCHAR(120) NOT NULL, module VARCHAR(30) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_role_permission (
  role_id INT UNSIGNED NOT NULL, permission_code VARCHAR(40) NOT NULL,
  PRIMARY KEY (role_id, permission_code), CONSTRAINT fk_hrrp_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_emp_role (
  emp_id VARCHAR(40) NOT NULL, role_id INT UNSIGNED NOT NULL, granted_by VARCHAR(40) NULL, granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (emp_id, role_id), CONSTRAINT fk_hrer_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO hr_role (code, name, description, is_system) VALUES
 ('SUPER_ADMIN','Super Admin','Everything, including role assignment',1),
 ('HR_ADMIN','HR Admin','Company set-up, employees, leave, attendance, salary set-up',1),
 ('PAYROLL_APPROVER','Payroll Approver','Approves payroll runs (maker-checker: cannot approve a run they processed)',1),
 ('MANAGER','Manager','Approves the team\'s leave / attendance, sees the team',1),
 ('EMPLOYEE','Employee','Own profile, leave, payslips',1);
INSERT IGNORE INTO hr_permission_def (code, name, module, sort_order) VALUES
 ('HR_SETUP','Company, branch, department, grade, cost centre, employee type','SETUP',1),
 ('HR_SETTINGS','Change effective-dated settings','SETUP',2),
 ('HR_SETTINGS_VIEW','View settings and their history','SETUP',3),
 ('HR_EMPLOYEE_EDIT','Edit HR details of employees','EMPLOYEE',4),
 ('HR_EMPLOYEE_VIEW','View HR details of employees','EMPLOYEE',5),
 ('HR_ROLES','Assign roles and edit the permission matrix','ACCESS',6),
 ('HR_AUDIT_VIEW','View the audit log','ACCESS',7),
 ('LEAVE_APPLY','Apply for leave','LEAVE',10),
 ('LEAVE_APPROVE','Approve leave of the team','LEAVE',11),
 ('LEAVE_ADMIN','Leave set-up, balances, adjustments','LEAVE',12),
 ('ATTENDANCE_ADMIN','Attendance corrections and lock','ATTENDANCE',20),
 ('SALARY_EDIT','Salary structure and employee salary','PAYROLL',30),
 ('SALARY_VIEW','View salary','PAYROLL',31),
 ('PAYROLL_PROCESS','Create and process payroll runs','PAYROLL',32),
 ('PAYROLL_APPROVE','Approve payroll runs','PAYROLL',33),
 ('PAYROLL_REPORTS','Payroll reports and statutory files','PAYROLL',34),
 ('SELF_SERVICE','Employee self-service portal','SELF',40);
INSERT IGNORE INTO hr_role_permission (role_id, permission_code)
SELECT r.id, p.code FROM hr_role r JOIN hr_permission_def p ON
 (r.code = 'HR_ADMIN' AND p.code IN ('HR_SETUP','HR_SETTINGS','HR_SETTINGS_VIEW','HR_EMPLOYEE_EDIT','HR_EMPLOYEE_VIEW','HR_AUDIT_VIEW','LEAVE_APPLY','LEAVE_APPROVE','LEAVE_ADMIN','ATTENDANCE_ADMIN','SALARY_EDIT','SALARY_VIEW','PAYROLL_PROCESS','PAYROLL_REPORTS','SELF_SERVICE'))
 OR (r.code = 'PAYROLL_APPROVER' AND p.code IN ('HR_SETTINGS_VIEW','SALARY_VIEW','PAYROLL_APPROVE','PAYROLL_REPORTS','HR_AUDIT_VIEW'))
 OR (r.code = 'MANAGER' AND p.code IN ('LEAVE_APPLY','LEAVE_APPROVE','HR_EMPLOYEE_VIEW','SELF_SERVICE'))
 OR (r.code = 'EMPLOYEE' AND p.code IN ('LEAVE_APPLY','SELF_SERVICE'));

CREATE TABLE IF NOT EXISTS hr_audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  at_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, by_emp_id VARCHAR(40) NULL, by_name VARCHAR(120) NULL,
  entity VARCHAR(40) NOT NULL COMMENT 'SETTING / BRANCH / DEPARTMENT / ... / LEAVE / SALARY / PAYROLL', entity_id VARCHAR(60) NULL,
  subject_emp_id VARCHAR(40) NULL COMMENT 'employee the change is about, if any',
  action VARCHAR(12) NOT NULL COMMENT 'CREATE / UPDATE / DELETE', field_name VARCHAR(60) NULL, old_value TEXT NULL, new_value TEXT NULL, reason VARCHAR(300) NULL,
  PRIMARY KEY (id), KEY ix_hr_audit_entity (entity, entity_id), KEY ix_hr_audit_time (at_time), KEY ix_hr_audit_subject (subject_emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='append-only: rows are never updated or deleted';

CREATE TABLE IF NOT EXISTS hr_setting_def (
  setting_key VARCHAR(60) NOT NULL, grp VARCHAR(30) NOT NULL, label VARCHAR(160) NOT NULL, help_text VARCHAR(400) NULL,
  value_type VARCHAR(8) NOT NULL COMMENT 'INT / DECIMAL / BOOL / STRING / ENUM / JSON', unit VARCHAR(12) NULL,
  min_value VARCHAR(20) NULL, max_value VARCHAR(20) NULL, enum_values VARCHAR(200) NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='catalogue of every setting the HR engine reads';
CREATE TABLE IF NOT EXISTS hr_setting (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, setting_key VARCHAR(60) NOT NULL,
  scope_type VARCHAR(10) NOT NULL DEFAULT 'COMPANY' COMMENT 'COMPANY / STATE / BRANCH', scope_id INT NOT NULL DEFAULT 0 COMMENT '0 = whole company',
  value_text VARCHAR(2000) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  source VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT 'BRIEF = seeded default from the requirements, to be verified; USER = entered here',
  reason VARCHAR(300) NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_setting (company_id, setting_key, scope_type, scope_id, effective_from),
  CONSTRAINT fk_hrs_def FOREIGN KEY (setting_key) REFERENCES hr_setting_def (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='effective-dated values; the row valid on a date with the latest effective_from wins';

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('MONEY_ROUNDING','General','Rounding of money amounts','How a fraction of a rupee is rounded','ENUM',NULL,NULL,NULL,'NEAREST,FLOOR,CEIL',1),
 ('MAKER_CHECKER_ON','General','Maker-checker for payroll','The person who processes a run cannot approve it','BOOL',NULL,NULL,NULL,NULL,2),
 ('PROBATION_MONTHS','Employee','Default probation period','Months, used when an employee has none of their own','INT','months','0','24',NULL,10),
 ('NOTICE_DAYS','Employee','Default notice period','Days, used when an employee has none of their own','INT','days','0','365',NULL,11),
 ('PF_WAGE_CEILING','PF','PF wage ceiling','Monthly wage up to which PF is computed (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,20),
 ('PF_EMPLOYEE_RATE','PF','PF - employee share','Percent of PF wage','DECIMAL','%','0','100',NULL,21),
 ('PF_EMPLOYER_EPF_RATE','PF','PF - employer EPF share','Percent of PF wage (EPF a/c 01)','DECIMAL','%','0','100',NULL,22),
 ('PF_EMPLOYER_EPS_RATE','PF','PF - employer EPS share','Percent of PF wage (pension a/c 10)','DECIMAL','%','0','100',NULL,23),
 ('PF_EDLI_RATE','PF','EDLI rate','Percent, employer cost','DECIMAL','%','0','100',NULL,24),
 ('PF_ADMIN_RATE','PF','PF admin charge rate','Percent, employer cost','DECIMAL','%','0','100',NULL,25),
 ('ESI_WAGE_LIMIT','ESI','ESI wage limit','ESI applies while gross is at or below this (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,30),
 ('ESI_WAGE_LIMIT_DISABLED','ESI','ESI wage limit - person with disability','Higher limit for employees marked disabled','DECIMAL','Rs','0',NULL,NULL,31),
 ('ESI_EMPLOYEE_RATE','ESI','ESI - employee share','Percent of gross','DECIMAL','%','0','100',NULL,32),
 ('ESI_EMPLOYER_RATE','ESI','ESI - employer share','Percent of gross','DECIMAL','%','0','100',NULL,33),
 ('PT_ANNUAL_CAP','PT','Professional tax - yearly cap','Most PT a person pays in a year','DECIMAL','Rs','0',NULL,NULL,40),
 ('WAGE_RULE_MIN_PERCENT','Wages','Wage rule - minimum percent','Basic + DA must be at least this percent of total remuneration (verify with a CA)','DECIMAL','%','0','100',NULL,50),
 ('WAGE_RULE_MODE','Wages','Wage rule - what to do on breach','OFF / WARN / BLOCK','ENUM',NULL,NULL,NULL,'OFF,WARN,BLOCK',51);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, v.k, v.val, v.fr, 'BRIEF', 'Seeded default from the requirements - verify with a CA' FROM (
 SELECT 'MONEY_ROUNDING' k, 'NEAREST' val, DATE '2000-04-01' fr UNION ALL SELECT 'MAKER_CHECKER_ON','true','2000-04-01'
 UNION ALL SELECT 'PROBATION_MONTHS','6','2000-04-01' UNION ALL SELECT 'NOTICE_DAYS','30','2000-04-01'
 UNION ALL SELECT 'PF_WAGE_CEILING','15000','2000-04-01' UNION ALL SELECT 'PF_WAGE_CEILING','25000','2026-09-17'
 UNION ALL SELECT 'PF_EMPLOYEE_RATE','12','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPF_RATE','3.67','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPS_RATE','8.33','2000-04-01'
 UNION ALL SELECT 'PF_EDLI_RATE','0.5','2000-04-01' UNION ALL SELECT 'PF_ADMIN_RATE','0.5','2000-04-01'
 UNION ALL SELECT 'ESI_WAGE_LIMIT','21000','2000-04-01' UNION ALL SELECT 'ESI_WAGE_LIMIT_DISABLED','25000','2000-04-01'
 UNION ALL SELECT 'ESI_EMPLOYEE_RATE','0.75','2000-04-01' UNION ALL SELECT 'ESI_EMPLOYER_RATE','3.25','2000-04-01'
 UNION ALL SELECT 'PT_ANNUAL_CAP','2500','2000-04-01'
 UNION ALL SELECT 'WAGE_RULE_MIN_PERCENT','50','2025-11-21' UNION ALL SELECT 'WAGE_RULE_MODE','WARN','2025-11-21') v
WHERE NOT EXISTS (SELECT 1 FROM hr_setting);
UPDATE hr_setting SET effective_to = '2026-09-16' WHERE setting_key = 'PF_WAGE_CEILING' AND effective_from = '2000-04-01' AND effective_to IS NULL AND source = 'BRIEF';

-- menu: HR & Payroll group + 4 pages, under the same parent as the other masters
SET @hr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset','teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @hr_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @hr_parent LIMIT 1) p
JOIN (SELECT 1 n,'HR Setup' label,'Company, branches, departments, grades' title,'building' icon,'hrsetup' page,'hr/hr-setup.html' href,40 so
      UNION ALL SELECT 2,'HR Settings','Effective-dated rules and rates','sliders','hrsettings','hr/hr-settings.html',41
      UNION ALL SELECT 3,'HR Roles & Access','Roles and permission matrix','shield-lock','hrroles','hr/hr-roles.html',42
      UNION ALL SELECT 4,'HR Audit Log','Who changed what','journal-text','hraudit','hr/hr-audit.html',43) m
WHERE @hr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);

USE globex_db;

CREATE TABLE IF NOT EXISTS hr_company (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, legal_name VARCHAR(150) NOT NULL, trade_name VARCHAR(150) NULL,
  pan VARCHAR(10) NULL, tan VARCHAR(10) NULL, gstin VARCHAR(15) NULL, cin VARCHAR(25) NULL,
  pf_code VARCHAR(30) NULL COMMENT 'PF establishment code', esi_code VARCHAR(30) NULL COMMENT 'ESI employer code',
  address VARCHAR(300) NULL, fy_start_month TINYINT NOT NULL DEFAULT 4 COMMENT 'financial year starts in this month (1-12)',
  leave_year_mode VARCHAR(12) NOT NULL DEFAULT 'FINANCIAL' COMMENT 'FINANCIAL / CALENDAR / JOINING',
  payroll_start_day TINYINT NOT NULL DEFAULT 1 COMMENT 'payroll cycle starts on this day of month',
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='legal entity (more than one can live in one tenant DB)';
INSERT INTO hr_company (id, legal_name) SELECT 1, 'My Company' WHERE NOT EXISTS (SELECT 1 FROM hr_company);

CREATE TABLE IF NOT EXISTS hr_branch (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  state_id BIGINT NULL COMMENT '-> state_master.oid; PT / LWF depend on it', city VARCHAR(80) NULL, address VARCHAR(300) NULL,
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', pt_reg_no VARCHAR(40) NULL, lwf_reg_no VARCHAR(40) NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_branch (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_department (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_department (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_grade (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_grade (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_cost_center (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_cost_center (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_employee_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  pf_applicable TINYINT(1) NOT NULL DEFAULT 1, esi_applicable TINYINT(1) NOT NULL DEFAULT 1, leave_applicable TINYINT(1) NOT NULL DEFAULT 1,
  compoff_applicable TINYINT(1) NOT NULL DEFAULT 1, probation_applicable TINYINT(1) NOT NULL DEFAULT 0, bonus_applicable TINYINT(1) NOT NULL DEFAULT 1,
  gratuity_applicable TINYINT(1) NOT NULL DEFAULT 1,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_emptype (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO hr_employee_type (company_id, code, name, pf_applicable, esi_applicable, leave_applicable, compoff_applicable, probation_applicable, bonus_applicable, gratuity_applicable, sort_order) VALUES
 (1,'PERM','Permanent',1,1,1,1,0,1,1,1),(1,'PROB','Probation',1,1,1,0,1,1,0,2),(1,'CONTRACT','Contract',1,1,0,0,0,0,0,3),
 (1,'FIXED','Fixed-term',1,1,1,1,0,1,0,4),(1,'INTERN','Intern',0,0,0,0,0,0,0,5),(1,'CONSULT','Consultant',0,0,0,0,0,0,0,6);

CREATE TABLE IF NOT EXISTS hr_emp_employment (
  emp_id VARCHAR(40) NOT NULL COMMENT '-> emp_detail.emp_id', company_id INT UNSIGNED NOT NULL DEFAULT 1,
  branch_id INT UNSIGNED NULL, department_id INT UNSIGNED NULL, grade_id INT UNSIGNED NULL, cost_center_id INT UNSIGNED NULL, employee_type_id INT UNSIGNED NULL,
  second_manager_id VARCHAR(40) NULL COMMENT 'optional second-level approver',
  probation_months TINYINT NULL, probation_end_date DATE NULL, notice_days SMALLINT NULL,
  tax_regime VARCHAR(4) NOT NULL DEFAULT 'NEW' COMMENT 'NEW / OLD', is_disabled TINYINT(1) NOT NULL DEFAULT 0,
  pf_member_since DATE NULL, prev_pf_member TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'was a PF member before this company (affects the EPS / ceiling rule)',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (emp_id), KEY ix_hr_emp_branch (branch_id), KEY ix_hr_emp_dept (department_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR facts of an employee; the person stays in emp_detail';

CREATE TABLE IF NOT EXISTS hr_role (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, code VARCHAR(30) NOT NULL, name VARCHAR(80) NOT NULL, description VARCHAR(255) NULL,
  is_system TINYINT(1) NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_role (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_permission_def (
  code VARCHAR(40) NOT NULL, name VARCHAR(120) NOT NULL, module VARCHAR(30) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_role_permission (
  role_id INT UNSIGNED NOT NULL, permission_code VARCHAR(40) NOT NULL,
  PRIMARY KEY (role_id, permission_code), CONSTRAINT fk_hrrp_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_emp_role (
  emp_id VARCHAR(40) NOT NULL, role_id INT UNSIGNED NOT NULL, granted_by VARCHAR(40) NULL, granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (emp_id, role_id), CONSTRAINT fk_hrer_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO hr_role (code, name, description, is_system) VALUES
 ('SUPER_ADMIN','Super Admin','Everything, including role assignment',1),
 ('HR_ADMIN','HR Admin','Company set-up, employees, leave, attendance, salary set-up',1),
 ('PAYROLL_APPROVER','Payroll Approver','Approves payroll runs (maker-checker: cannot approve a run they processed)',1),
 ('MANAGER','Manager','Approves the team\'s leave / attendance, sees the team',1),
 ('EMPLOYEE','Employee','Own profile, leave, payslips',1);
INSERT IGNORE INTO hr_permission_def (code, name, module, sort_order) VALUES
 ('HR_SETUP','Company, branch, department, grade, cost centre, employee type','SETUP',1),
 ('HR_SETTINGS','Change effective-dated settings','SETUP',2),
 ('HR_SETTINGS_VIEW','View settings and their history','SETUP',3),
 ('HR_EMPLOYEE_EDIT','Edit HR details of employees','EMPLOYEE',4),
 ('HR_EMPLOYEE_VIEW','View HR details of employees','EMPLOYEE',5),
 ('HR_ROLES','Assign roles and edit the permission matrix','ACCESS',6),
 ('HR_AUDIT_VIEW','View the audit log','ACCESS',7),
 ('LEAVE_APPLY','Apply for leave','LEAVE',10),
 ('LEAVE_APPROVE','Approve leave of the team','LEAVE',11),
 ('LEAVE_ADMIN','Leave set-up, balances, adjustments','LEAVE',12),
 ('ATTENDANCE_ADMIN','Attendance corrections and lock','ATTENDANCE',20),
 ('SALARY_EDIT','Salary structure and employee salary','PAYROLL',30),
 ('SALARY_VIEW','View salary','PAYROLL',31),
 ('PAYROLL_PROCESS','Create and process payroll runs','PAYROLL',32),
 ('PAYROLL_APPROVE','Approve payroll runs','PAYROLL',33),
 ('PAYROLL_REPORTS','Payroll reports and statutory files','PAYROLL',34),
 ('SELF_SERVICE','Employee self-service portal','SELF',40);
INSERT IGNORE INTO hr_role_permission (role_id, permission_code)
SELECT r.id, p.code FROM hr_role r JOIN hr_permission_def p ON
 (r.code = 'HR_ADMIN' AND p.code IN ('HR_SETUP','HR_SETTINGS','HR_SETTINGS_VIEW','HR_EMPLOYEE_EDIT','HR_EMPLOYEE_VIEW','HR_AUDIT_VIEW','LEAVE_APPLY','LEAVE_APPROVE','LEAVE_ADMIN','ATTENDANCE_ADMIN','SALARY_EDIT','SALARY_VIEW','PAYROLL_PROCESS','PAYROLL_REPORTS','SELF_SERVICE'))
 OR (r.code = 'PAYROLL_APPROVER' AND p.code IN ('HR_SETTINGS_VIEW','SALARY_VIEW','PAYROLL_APPROVE','PAYROLL_REPORTS','HR_AUDIT_VIEW'))
 OR (r.code = 'MANAGER' AND p.code IN ('LEAVE_APPLY','LEAVE_APPROVE','HR_EMPLOYEE_VIEW','SELF_SERVICE'))
 OR (r.code = 'EMPLOYEE' AND p.code IN ('LEAVE_APPLY','SELF_SERVICE'));

CREATE TABLE IF NOT EXISTS hr_audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  at_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, by_emp_id VARCHAR(40) NULL, by_name VARCHAR(120) NULL,
  entity VARCHAR(40) NOT NULL COMMENT 'SETTING / BRANCH / DEPARTMENT / ... / LEAVE / SALARY / PAYROLL', entity_id VARCHAR(60) NULL,
  subject_emp_id VARCHAR(40) NULL COMMENT 'employee the change is about, if any',
  action VARCHAR(12) NOT NULL COMMENT 'CREATE / UPDATE / DELETE', field_name VARCHAR(60) NULL, old_value TEXT NULL, new_value TEXT NULL, reason VARCHAR(300) NULL,
  PRIMARY KEY (id), KEY ix_hr_audit_entity (entity, entity_id), KEY ix_hr_audit_time (at_time), KEY ix_hr_audit_subject (subject_emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='append-only: rows are never updated or deleted';

CREATE TABLE IF NOT EXISTS hr_setting_def (
  setting_key VARCHAR(60) NOT NULL, grp VARCHAR(30) NOT NULL, label VARCHAR(160) NOT NULL, help_text VARCHAR(400) NULL,
  value_type VARCHAR(8) NOT NULL COMMENT 'INT / DECIMAL / BOOL / STRING / ENUM / JSON', unit VARCHAR(12) NULL,
  min_value VARCHAR(20) NULL, max_value VARCHAR(20) NULL, enum_values VARCHAR(200) NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='catalogue of every setting the HR engine reads';
CREATE TABLE IF NOT EXISTS hr_setting (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, setting_key VARCHAR(60) NOT NULL,
  scope_type VARCHAR(10) NOT NULL DEFAULT 'COMPANY' COMMENT 'COMPANY / STATE / BRANCH', scope_id INT NOT NULL DEFAULT 0 COMMENT '0 = whole company',
  value_text VARCHAR(2000) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  source VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT 'BRIEF = seeded default from the requirements, to be verified; USER = entered here',
  reason VARCHAR(300) NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_setting (company_id, setting_key, scope_type, scope_id, effective_from),
  CONSTRAINT fk_hrs_def FOREIGN KEY (setting_key) REFERENCES hr_setting_def (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='effective-dated values; the row valid on a date with the latest effective_from wins';

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('MONEY_ROUNDING','General','Rounding of money amounts','How a fraction of a rupee is rounded','ENUM',NULL,NULL,NULL,'NEAREST,FLOOR,CEIL',1),
 ('MAKER_CHECKER_ON','General','Maker-checker for payroll','The person who processes a run cannot approve it','BOOL',NULL,NULL,NULL,NULL,2),
 ('PROBATION_MONTHS','Employee','Default probation period','Months, used when an employee has none of their own','INT','months','0','24',NULL,10),
 ('NOTICE_DAYS','Employee','Default notice period','Days, used when an employee has none of their own','INT','days','0','365',NULL,11),
 ('PF_WAGE_CEILING','PF','PF wage ceiling','Monthly wage up to which PF is computed (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,20),
 ('PF_EMPLOYEE_RATE','PF','PF - employee share','Percent of PF wage','DECIMAL','%','0','100',NULL,21),
 ('PF_EMPLOYER_EPF_RATE','PF','PF - employer EPF share','Percent of PF wage (EPF a/c 01)','DECIMAL','%','0','100',NULL,22),
 ('PF_EMPLOYER_EPS_RATE','PF','PF - employer EPS share','Percent of PF wage (pension a/c 10)','DECIMAL','%','0','100',NULL,23),
 ('PF_EDLI_RATE','PF','EDLI rate','Percent, employer cost','DECIMAL','%','0','100',NULL,24),
 ('PF_ADMIN_RATE','PF','PF admin charge rate','Percent, employer cost','DECIMAL','%','0','100',NULL,25),
 ('ESI_WAGE_LIMIT','ESI','ESI wage limit','ESI applies while gross is at or below this (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,30),
 ('ESI_WAGE_LIMIT_DISABLED','ESI','ESI wage limit - person with disability','Higher limit for employees marked disabled','DECIMAL','Rs','0',NULL,NULL,31),
 ('ESI_EMPLOYEE_RATE','ESI','ESI - employee share','Percent of gross','DECIMAL','%','0','100',NULL,32),
 ('ESI_EMPLOYER_RATE','ESI','ESI - employer share','Percent of gross','DECIMAL','%','0','100',NULL,33),
 ('PT_ANNUAL_CAP','PT','Professional tax - yearly cap','Most PT a person pays in a year','DECIMAL','Rs','0',NULL,NULL,40),
 ('WAGE_RULE_MIN_PERCENT','Wages','Wage rule - minimum percent','Basic + DA must be at least this percent of total remuneration (verify with a CA)','DECIMAL','%','0','100',NULL,50),
 ('WAGE_RULE_MODE','Wages','Wage rule - what to do on breach','OFF / WARN / BLOCK','ENUM',NULL,NULL,NULL,'OFF,WARN,BLOCK',51);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, v.k, v.val, v.fr, 'BRIEF', 'Seeded default from the requirements - verify with a CA' FROM (
 SELECT 'MONEY_ROUNDING' k, 'NEAREST' val, DATE '2000-04-01' fr UNION ALL SELECT 'MAKER_CHECKER_ON','true','2000-04-01'
 UNION ALL SELECT 'PROBATION_MONTHS','6','2000-04-01' UNION ALL SELECT 'NOTICE_DAYS','30','2000-04-01'
 UNION ALL SELECT 'PF_WAGE_CEILING','15000','2000-04-01' UNION ALL SELECT 'PF_WAGE_CEILING','25000','2026-09-17'
 UNION ALL SELECT 'PF_EMPLOYEE_RATE','12','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPF_RATE','3.67','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPS_RATE','8.33','2000-04-01'
 UNION ALL SELECT 'PF_EDLI_RATE','0.5','2000-04-01' UNION ALL SELECT 'PF_ADMIN_RATE','0.5','2000-04-01'
 UNION ALL SELECT 'ESI_WAGE_LIMIT','21000','2000-04-01' UNION ALL SELECT 'ESI_WAGE_LIMIT_DISABLED','25000','2000-04-01'
 UNION ALL SELECT 'ESI_EMPLOYEE_RATE','0.75','2000-04-01' UNION ALL SELECT 'ESI_EMPLOYER_RATE','3.25','2000-04-01'
 UNION ALL SELECT 'PT_ANNUAL_CAP','2500','2000-04-01'
 UNION ALL SELECT 'WAGE_RULE_MIN_PERCENT','50','2025-11-21' UNION ALL SELECT 'WAGE_RULE_MODE','WARN','2025-11-21') v
WHERE NOT EXISTS (SELECT 1 FROM hr_setting);
UPDATE hr_setting SET effective_to = '2026-09-16' WHERE setting_key = 'PF_WAGE_CEILING' AND effective_from = '2000-04-01' AND effective_to IS NULL AND source = 'BRIEF';

-- menu: HR & Payroll group + 4 pages, under the same parent as the other masters
SET @hr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset','teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @hr_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @hr_parent LIMIT 1) p
JOIN (SELECT 1 n,'HR Setup' label,'Company, branches, departments, grades' title,'building' icon,'hrsetup' page,'hr/hr-setup.html' href,40 so
      UNION ALL SELECT 2,'HR Settings','Effective-dated rules and rates','sliders','hrsettings','hr/hr-settings.html',41
      UNION ALL SELECT 3,'HR Roles & Access','Roles and permission matrix','shield-lock','hrroles','hr/hr-roles.html',42
      UNION ALL SELECT 4,'HR Audit Log','Who changed what','journal-text','hraudit','hr/hr-audit.html',43) m
WHERE @hr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);

USE initech_db;

CREATE TABLE IF NOT EXISTS hr_company (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, legal_name VARCHAR(150) NOT NULL, trade_name VARCHAR(150) NULL,
  pan VARCHAR(10) NULL, tan VARCHAR(10) NULL, gstin VARCHAR(15) NULL, cin VARCHAR(25) NULL,
  pf_code VARCHAR(30) NULL COMMENT 'PF establishment code', esi_code VARCHAR(30) NULL COMMENT 'ESI employer code',
  address VARCHAR(300) NULL, fy_start_month TINYINT NOT NULL DEFAULT 4 COMMENT 'financial year starts in this month (1-12)',
  leave_year_mode VARCHAR(12) NOT NULL DEFAULT 'FINANCIAL' COMMENT 'FINANCIAL / CALENDAR / JOINING',
  payroll_start_day TINYINT NOT NULL DEFAULT 1 COMMENT 'payroll cycle starts on this day of month',
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='legal entity (more than one can live in one tenant DB)';
INSERT INTO hr_company (id, legal_name) SELECT 1, 'My Company' WHERE NOT EXISTS (SELECT 1 FROM hr_company);

CREATE TABLE IF NOT EXISTS hr_branch (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  state_id BIGINT NULL COMMENT '-> state_master.oid; PT / LWF depend on it', city VARCHAR(80) NULL, address VARCHAR(300) NULL,
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', pt_reg_no VARCHAR(40) NULL, lwf_reg_no VARCHAR(40) NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_branch (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_department (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_department (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_grade (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_grade (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_cost_center (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_cost_center (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_employee_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  pf_applicable TINYINT(1) NOT NULL DEFAULT 1, esi_applicable TINYINT(1) NOT NULL DEFAULT 1, leave_applicable TINYINT(1) NOT NULL DEFAULT 1,
  compoff_applicable TINYINT(1) NOT NULL DEFAULT 1, probation_applicable TINYINT(1) NOT NULL DEFAULT 0, bonus_applicable TINYINT(1) NOT NULL DEFAULT 1,
  gratuity_applicable TINYINT(1) NOT NULL DEFAULT 1,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_emptype (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO hr_employee_type (company_id, code, name, pf_applicable, esi_applicable, leave_applicable, compoff_applicable, probation_applicable, bonus_applicable, gratuity_applicable, sort_order) VALUES
 (1,'PERM','Permanent',1,1,1,1,0,1,1,1),(1,'PROB','Probation',1,1,1,0,1,1,0,2),(1,'CONTRACT','Contract',1,1,0,0,0,0,0,3),
 (1,'FIXED','Fixed-term',1,1,1,1,0,1,0,4),(1,'INTERN','Intern',0,0,0,0,0,0,0,5),(1,'CONSULT','Consultant',0,0,0,0,0,0,0,6);

CREATE TABLE IF NOT EXISTS hr_emp_employment (
  emp_id VARCHAR(40) NOT NULL COMMENT '-> emp_detail.emp_id', company_id INT UNSIGNED NOT NULL DEFAULT 1,
  branch_id INT UNSIGNED NULL, department_id INT UNSIGNED NULL, grade_id INT UNSIGNED NULL, cost_center_id INT UNSIGNED NULL, employee_type_id INT UNSIGNED NULL,
  second_manager_id VARCHAR(40) NULL COMMENT 'optional second-level approver',
  probation_months TINYINT NULL, probation_end_date DATE NULL, notice_days SMALLINT NULL,
  tax_regime VARCHAR(4) NOT NULL DEFAULT 'NEW' COMMENT 'NEW / OLD', is_disabled TINYINT(1) NOT NULL DEFAULT 0,
  pf_member_since DATE NULL, prev_pf_member TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'was a PF member before this company (affects the EPS / ceiling rule)',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (emp_id), KEY ix_hr_emp_branch (branch_id), KEY ix_hr_emp_dept (department_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR facts of an employee; the person stays in emp_detail';

CREATE TABLE IF NOT EXISTS hr_role (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, code VARCHAR(30) NOT NULL, name VARCHAR(80) NOT NULL, description VARCHAR(255) NULL,
  is_system TINYINT(1) NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_role (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_permission_def (
  code VARCHAR(40) NOT NULL, name VARCHAR(120) NOT NULL, module VARCHAR(30) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_role_permission (
  role_id INT UNSIGNED NOT NULL, permission_code VARCHAR(40) NOT NULL,
  PRIMARY KEY (role_id, permission_code), CONSTRAINT fk_hrrp_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_emp_role (
  emp_id VARCHAR(40) NOT NULL, role_id INT UNSIGNED NOT NULL, granted_by VARCHAR(40) NULL, granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (emp_id, role_id), CONSTRAINT fk_hrer_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO hr_role (code, name, description, is_system) VALUES
 ('SUPER_ADMIN','Super Admin','Everything, including role assignment',1),
 ('HR_ADMIN','HR Admin','Company set-up, employees, leave, attendance, salary set-up',1),
 ('PAYROLL_APPROVER','Payroll Approver','Approves payroll runs (maker-checker: cannot approve a run they processed)',1),
 ('MANAGER','Manager','Approves the team\'s leave / attendance, sees the team',1),
 ('EMPLOYEE','Employee','Own profile, leave, payslips',1);
INSERT IGNORE INTO hr_permission_def (code, name, module, sort_order) VALUES
 ('HR_SETUP','Company, branch, department, grade, cost centre, employee type','SETUP',1),
 ('HR_SETTINGS','Change effective-dated settings','SETUP',2),
 ('HR_SETTINGS_VIEW','View settings and their history','SETUP',3),
 ('HR_EMPLOYEE_EDIT','Edit HR details of employees','EMPLOYEE',4),
 ('HR_EMPLOYEE_VIEW','View HR details of employees','EMPLOYEE',5),
 ('HR_ROLES','Assign roles and edit the permission matrix','ACCESS',6),
 ('HR_AUDIT_VIEW','View the audit log','ACCESS',7),
 ('LEAVE_APPLY','Apply for leave','LEAVE',10),
 ('LEAVE_APPROVE','Approve leave of the team','LEAVE',11),
 ('LEAVE_ADMIN','Leave set-up, balances, adjustments','LEAVE',12),
 ('ATTENDANCE_ADMIN','Attendance corrections and lock','ATTENDANCE',20),
 ('SALARY_EDIT','Salary structure and employee salary','PAYROLL',30),
 ('SALARY_VIEW','View salary','PAYROLL',31),
 ('PAYROLL_PROCESS','Create and process payroll runs','PAYROLL',32),
 ('PAYROLL_APPROVE','Approve payroll runs','PAYROLL',33),
 ('PAYROLL_REPORTS','Payroll reports and statutory files','PAYROLL',34),
 ('SELF_SERVICE','Employee self-service portal','SELF',40);
INSERT IGNORE INTO hr_role_permission (role_id, permission_code)
SELECT r.id, p.code FROM hr_role r JOIN hr_permission_def p ON
 (r.code = 'HR_ADMIN' AND p.code IN ('HR_SETUP','HR_SETTINGS','HR_SETTINGS_VIEW','HR_EMPLOYEE_EDIT','HR_EMPLOYEE_VIEW','HR_AUDIT_VIEW','LEAVE_APPLY','LEAVE_APPROVE','LEAVE_ADMIN','ATTENDANCE_ADMIN','SALARY_EDIT','SALARY_VIEW','PAYROLL_PROCESS','PAYROLL_REPORTS','SELF_SERVICE'))
 OR (r.code = 'PAYROLL_APPROVER' AND p.code IN ('HR_SETTINGS_VIEW','SALARY_VIEW','PAYROLL_APPROVE','PAYROLL_REPORTS','HR_AUDIT_VIEW'))
 OR (r.code = 'MANAGER' AND p.code IN ('LEAVE_APPLY','LEAVE_APPROVE','HR_EMPLOYEE_VIEW','SELF_SERVICE'))
 OR (r.code = 'EMPLOYEE' AND p.code IN ('LEAVE_APPLY','SELF_SERVICE'));

CREATE TABLE IF NOT EXISTS hr_audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  at_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, by_emp_id VARCHAR(40) NULL, by_name VARCHAR(120) NULL,
  entity VARCHAR(40) NOT NULL COMMENT 'SETTING / BRANCH / DEPARTMENT / ... / LEAVE / SALARY / PAYROLL', entity_id VARCHAR(60) NULL,
  subject_emp_id VARCHAR(40) NULL COMMENT 'employee the change is about, if any',
  action VARCHAR(12) NOT NULL COMMENT 'CREATE / UPDATE / DELETE', field_name VARCHAR(60) NULL, old_value TEXT NULL, new_value TEXT NULL, reason VARCHAR(300) NULL,
  PRIMARY KEY (id), KEY ix_hr_audit_entity (entity, entity_id), KEY ix_hr_audit_time (at_time), KEY ix_hr_audit_subject (subject_emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='append-only: rows are never updated or deleted';

CREATE TABLE IF NOT EXISTS hr_setting_def (
  setting_key VARCHAR(60) NOT NULL, grp VARCHAR(30) NOT NULL, label VARCHAR(160) NOT NULL, help_text VARCHAR(400) NULL,
  value_type VARCHAR(8) NOT NULL COMMENT 'INT / DECIMAL / BOOL / STRING / ENUM / JSON', unit VARCHAR(12) NULL,
  min_value VARCHAR(20) NULL, max_value VARCHAR(20) NULL, enum_values VARCHAR(200) NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='catalogue of every setting the HR engine reads';
CREATE TABLE IF NOT EXISTS hr_setting (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, setting_key VARCHAR(60) NOT NULL,
  scope_type VARCHAR(10) NOT NULL DEFAULT 'COMPANY' COMMENT 'COMPANY / STATE / BRANCH', scope_id INT NOT NULL DEFAULT 0 COMMENT '0 = whole company',
  value_text VARCHAR(2000) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  source VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT 'BRIEF = seeded default from the requirements, to be verified; USER = entered here',
  reason VARCHAR(300) NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_setting (company_id, setting_key, scope_type, scope_id, effective_from),
  CONSTRAINT fk_hrs_def FOREIGN KEY (setting_key) REFERENCES hr_setting_def (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='effective-dated values; the row valid on a date with the latest effective_from wins';

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('MONEY_ROUNDING','General','Rounding of money amounts','How a fraction of a rupee is rounded','ENUM',NULL,NULL,NULL,'NEAREST,FLOOR,CEIL',1),
 ('MAKER_CHECKER_ON','General','Maker-checker for payroll','The person who processes a run cannot approve it','BOOL',NULL,NULL,NULL,NULL,2),
 ('PROBATION_MONTHS','Employee','Default probation period','Months, used when an employee has none of their own','INT','months','0','24',NULL,10),
 ('NOTICE_DAYS','Employee','Default notice period','Days, used when an employee has none of their own','INT','days','0','365',NULL,11),
 ('PF_WAGE_CEILING','PF','PF wage ceiling','Monthly wage up to which PF is computed (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,20),
 ('PF_EMPLOYEE_RATE','PF','PF - employee share','Percent of PF wage','DECIMAL','%','0','100',NULL,21),
 ('PF_EMPLOYER_EPF_RATE','PF','PF - employer EPF share','Percent of PF wage (EPF a/c 01)','DECIMAL','%','0','100',NULL,22),
 ('PF_EMPLOYER_EPS_RATE','PF','PF - employer EPS share','Percent of PF wage (pension a/c 10)','DECIMAL','%','0','100',NULL,23),
 ('PF_EDLI_RATE','PF','EDLI rate','Percent, employer cost','DECIMAL','%','0','100',NULL,24),
 ('PF_ADMIN_RATE','PF','PF admin charge rate','Percent, employer cost','DECIMAL','%','0','100',NULL,25),
 ('ESI_WAGE_LIMIT','ESI','ESI wage limit','ESI applies while gross is at or below this (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,30),
 ('ESI_WAGE_LIMIT_DISABLED','ESI','ESI wage limit - person with disability','Higher limit for employees marked disabled','DECIMAL','Rs','0',NULL,NULL,31),
 ('ESI_EMPLOYEE_RATE','ESI','ESI - employee share','Percent of gross','DECIMAL','%','0','100',NULL,32),
 ('ESI_EMPLOYER_RATE','ESI','ESI - employer share','Percent of gross','DECIMAL','%','0','100',NULL,33),
 ('PT_ANNUAL_CAP','PT','Professional tax - yearly cap','Most PT a person pays in a year','DECIMAL','Rs','0',NULL,NULL,40),
 ('WAGE_RULE_MIN_PERCENT','Wages','Wage rule - minimum percent','Basic + DA must be at least this percent of total remuneration (verify with a CA)','DECIMAL','%','0','100',NULL,50),
 ('WAGE_RULE_MODE','Wages','Wage rule - what to do on breach','OFF / WARN / BLOCK','ENUM',NULL,NULL,NULL,'OFF,WARN,BLOCK',51);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, v.k, v.val, v.fr, 'BRIEF', 'Seeded default from the requirements - verify with a CA' FROM (
 SELECT 'MONEY_ROUNDING' k, 'NEAREST' val, DATE '2000-04-01' fr UNION ALL SELECT 'MAKER_CHECKER_ON','true','2000-04-01'
 UNION ALL SELECT 'PROBATION_MONTHS','6','2000-04-01' UNION ALL SELECT 'NOTICE_DAYS','30','2000-04-01'
 UNION ALL SELECT 'PF_WAGE_CEILING','15000','2000-04-01' UNION ALL SELECT 'PF_WAGE_CEILING','25000','2026-09-17'
 UNION ALL SELECT 'PF_EMPLOYEE_RATE','12','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPF_RATE','3.67','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPS_RATE','8.33','2000-04-01'
 UNION ALL SELECT 'PF_EDLI_RATE','0.5','2000-04-01' UNION ALL SELECT 'PF_ADMIN_RATE','0.5','2000-04-01'
 UNION ALL SELECT 'ESI_WAGE_LIMIT','21000','2000-04-01' UNION ALL SELECT 'ESI_WAGE_LIMIT_DISABLED','25000','2000-04-01'
 UNION ALL SELECT 'ESI_EMPLOYEE_RATE','0.75','2000-04-01' UNION ALL SELECT 'ESI_EMPLOYER_RATE','3.25','2000-04-01'
 UNION ALL SELECT 'PT_ANNUAL_CAP','2500','2000-04-01'
 UNION ALL SELECT 'WAGE_RULE_MIN_PERCENT','50','2025-11-21' UNION ALL SELECT 'WAGE_RULE_MODE','WARN','2025-11-21') v
WHERE NOT EXISTS (SELECT 1 FROM hr_setting);
UPDATE hr_setting SET effective_to = '2026-09-16' WHERE setting_key = 'PF_WAGE_CEILING' AND effective_from = '2000-04-01' AND effective_to IS NULL AND source = 'BRIEF';

-- menu: HR & Payroll group + 4 pages, under the same parent as the other masters
SET @hr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset','teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @hr_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @hr_parent LIMIT 1) p
JOIN (SELECT 1 n,'HR Setup' label,'Company, branches, departments, grades' title,'building' icon,'hrsetup' page,'hr/hr-setup.html' href,40 so
      UNION ALL SELECT 2,'HR Settings','Effective-dated rules and rates','sliders','hrsettings','hr/hr-settings.html',41
      UNION ALL SELECT 3,'HR Roles & Access','Roles and permission matrix','shield-lock','hrroles','hr/hr-roles.html',42
      UNION ALL SELECT 4,'HR Audit Log','Who changed what','journal-text','hraudit','hr/hr-audit.html',43) m
WHERE @hr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);

USE sfa_demo;

CREATE TABLE IF NOT EXISTS hr_company (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, legal_name VARCHAR(150) NOT NULL, trade_name VARCHAR(150) NULL,
  pan VARCHAR(10) NULL, tan VARCHAR(10) NULL, gstin VARCHAR(15) NULL, cin VARCHAR(25) NULL,
  pf_code VARCHAR(30) NULL COMMENT 'PF establishment code', esi_code VARCHAR(30) NULL COMMENT 'ESI employer code',
  address VARCHAR(300) NULL, fy_start_month TINYINT NOT NULL DEFAULT 4 COMMENT 'financial year starts in this month (1-12)',
  leave_year_mode VARCHAR(12) NOT NULL DEFAULT 'FINANCIAL' COMMENT 'FINANCIAL / CALENDAR / JOINING',
  payroll_start_day TINYINT NOT NULL DEFAULT 1 COMMENT 'payroll cycle starts on this day of month',
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='legal entity (more than one can live in one tenant DB)';
INSERT INTO hr_company (id, legal_name) SELECT 1, 'My Company' WHERE NOT EXISTS (SELECT 1 FROM hr_company);

CREATE TABLE IF NOT EXISTS hr_branch (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  state_id BIGINT NULL COMMENT '-> state_master.oid; PT / LWF depend on it', city VARCHAR(80) NULL, address VARCHAR(300) NULL,
  timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Kolkata', pt_reg_no VARCHAR(40) NULL, lwf_reg_no VARCHAR(40) NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_branch (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_department (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_department (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_grade (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_grade (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_cost_center (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_cost_center (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_employee_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(120) NOT NULL,
  pf_applicable TINYINT(1) NOT NULL DEFAULT 1, esi_applicable TINYINT(1) NOT NULL DEFAULT 1, leave_applicable TINYINT(1) NOT NULL DEFAULT 1,
  compoff_applicable TINYINT(1) NOT NULL DEFAULT 1, probation_applicable TINYINT(1) NOT NULL DEFAULT 0, bonus_applicable TINYINT(1) NOT NULL DEFAULT 1,
  gratuity_applicable TINYINT(1) NOT NULL DEFAULT 1,
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_emptype (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO hr_employee_type (company_id, code, name, pf_applicable, esi_applicable, leave_applicable, compoff_applicable, probation_applicable, bonus_applicable, gratuity_applicable, sort_order) VALUES
 (1,'PERM','Permanent',1,1,1,1,0,1,1,1),(1,'PROB','Probation',1,1,1,0,1,1,0,2),(1,'CONTRACT','Contract',1,1,0,0,0,0,0,3),
 (1,'FIXED','Fixed-term',1,1,1,1,0,1,0,4),(1,'INTERN','Intern',0,0,0,0,0,0,0,5),(1,'CONSULT','Consultant',0,0,0,0,0,0,0,6);

CREATE TABLE IF NOT EXISTS hr_emp_employment (
  emp_id VARCHAR(40) NOT NULL COMMENT '-> emp_detail.emp_id', company_id INT UNSIGNED NOT NULL DEFAULT 1,
  branch_id INT UNSIGNED NULL, department_id INT UNSIGNED NULL, grade_id INT UNSIGNED NULL, cost_center_id INT UNSIGNED NULL, employee_type_id INT UNSIGNED NULL,
  second_manager_id VARCHAR(40) NULL COMMENT 'optional second-level approver',
  probation_months TINYINT NULL, probation_end_date DATE NULL, notice_days SMALLINT NULL,
  tax_regime VARCHAR(4) NOT NULL DEFAULT 'NEW' COMMENT 'NEW / OLD', is_disabled TINYINT(1) NOT NULL DEFAULT 0,
  pf_member_since DATE NULL, prev_pf_member TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'was a PF member before this company (affects the EPS / ceiling rule)',
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (emp_id), KEY ix_hr_emp_branch (branch_id), KEY ix_hr_emp_dept (department_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR facts of an employee; the person stays in emp_detail';

CREATE TABLE IF NOT EXISTS hr_role (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, code VARCHAR(30) NOT NULL, name VARCHAR(80) NOT NULL, description VARCHAR(255) NULL,
  is_system TINYINT(1) NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_role (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_permission_def (
  code VARCHAR(40) NOT NULL, name VARCHAR(120) NOT NULL, module VARCHAR(30) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_role_permission (
  role_id INT UNSIGNED NOT NULL, permission_code VARCHAR(40) NOT NULL,
  PRIMARY KEY (role_id, permission_code), CONSTRAINT fk_hrrp_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_emp_role (
  emp_id VARCHAR(40) NOT NULL, role_id INT UNSIGNED NOT NULL, granted_by VARCHAR(40) NULL, granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (emp_id, role_id), CONSTRAINT fk_hrer_role FOREIGN KEY (role_id) REFERENCES hr_role (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO hr_role (code, name, description, is_system) VALUES
 ('SUPER_ADMIN','Super Admin','Everything, including role assignment',1),
 ('HR_ADMIN','HR Admin','Company set-up, employees, leave, attendance, salary set-up',1),
 ('PAYROLL_APPROVER','Payroll Approver','Approves payroll runs (maker-checker: cannot approve a run they processed)',1),
 ('MANAGER','Manager','Approves the team\'s leave / attendance, sees the team',1),
 ('EMPLOYEE','Employee','Own profile, leave, payslips',1);
INSERT IGNORE INTO hr_permission_def (code, name, module, sort_order) VALUES
 ('HR_SETUP','Company, branch, department, grade, cost centre, employee type','SETUP',1),
 ('HR_SETTINGS','Change effective-dated settings','SETUP',2),
 ('HR_SETTINGS_VIEW','View settings and their history','SETUP',3),
 ('HR_EMPLOYEE_EDIT','Edit HR details of employees','EMPLOYEE',4),
 ('HR_EMPLOYEE_VIEW','View HR details of employees','EMPLOYEE',5),
 ('HR_ROLES','Assign roles and edit the permission matrix','ACCESS',6),
 ('HR_AUDIT_VIEW','View the audit log','ACCESS',7),
 ('LEAVE_APPLY','Apply for leave','LEAVE',10),
 ('LEAVE_APPROVE','Approve leave of the team','LEAVE',11),
 ('LEAVE_ADMIN','Leave set-up, balances, adjustments','LEAVE',12),
 ('ATTENDANCE_ADMIN','Attendance corrections and lock','ATTENDANCE',20),
 ('SALARY_EDIT','Salary structure and employee salary','PAYROLL',30),
 ('SALARY_VIEW','View salary','PAYROLL',31),
 ('PAYROLL_PROCESS','Create and process payroll runs','PAYROLL',32),
 ('PAYROLL_APPROVE','Approve payroll runs','PAYROLL',33),
 ('PAYROLL_REPORTS','Payroll reports and statutory files','PAYROLL',34),
 ('SELF_SERVICE','Employee self-service portal','SELF',40);
INSERT IGNORE INTO hr_role_permission (role_id, permission_code)
SELECT r.id, p.code FROM hr_role r JOIN hr_permission_def p ON
 (r.code = 'HR_ADMIN' AND p.code IN ('HR_SETUP','HR_SETTINGS','HR_SETTINGS_VIEW','HR_EMPLOYEE_EDIT','HR_EMPLOYEE_VIEW','HR_AUDIT_VIEW','LEAVE_APPLY','LEAVE_APPROVE','LEAVE_ADMIN','ATTENDANCE_ADMIN','SALARY_EDIT','SALARY_VIEW','PAYROLL_PROCESS','PAYROLL_REPORTS','SELF_SERVICE'))
 OR (r.code = 'PAYROLL_APPROVER' AND p.code IN ('HR_SETTINGS_VIEW','SALARY_VIEW','PAYROLL_APPROVE','PAYROLL_REPORTS','HR_AUDIT_VIEW'))
 OR (r.code = 'MANAGER' AND p.code IN ('LEAVE_APPLY','LEAVE_APPROVE','HR_EMPLOYEE_VIEW','SELF_SERVICE'))
 OR (r.code = 'EMPLOYEE' AND p.code IN ('LEAVE_APPLY','SELF_SERVICE'));

CREATE TABLE IF NOT EXISTS hr_audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  at_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, by_emp_id VARCHAR(40) NULL, by_name VARCHAR(120) NULL,
  entity VARCHAR(40) NOT NULL COMMENT 'SETTING / BRANCH / DEPARTMENT / ... / LEAVE / SALARY / PAYROLL', entity_id VARCHAR(60) NULL,
  subject_emp_id VARCHAR(40) NULL COMMENT 'employee the change is about, if any',
  action VARCHAR(12) NOT NULL COMMENT 'CREATE / UPDATE / DELETE', field_name VARCHAR(60) NULL, old_value TEXT NULL, new_value TEXT NULL, reason VARCHAR(300) NULL,
  PRIMARY KEY (id), KEY ix_hr_audit_entity (entity, entity_id), KEY ix_hr_audit_time (at_time), KEY ix_hr_audit_subject (subject_emp_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='append-only: rows are never updated or deleted';

CREATE TABLE IF NOT EXISTS hr_setting_def (
  setting_key VARCHAR(60) NOT NULL, grp VARCHAR(30) NOT NULL, label VARCHAR(160) NOT NULL, help_text VARCHAR(400) NULL,
  value_type VARCHAR(8) NOT NULL COMMENT 'INT / DECIMAL / BOOL / STRING / ENUM / JSON', unit VARCHAR(12) NULL,
  min_value VARCHAR(20) NULL, max_value VARCHAR(20) NULL, enum_values VARCHAR(200) NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='catalogue of every setting the HR engine reads';
CREATE TABLE IF NOT EXISTS hr_setting (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, setting_key VARCHAR(60) NOT NULL,
  scope_type VARCHAR(10) NOT NULL DEFAULT 'COMPANY' COMMENT 'COMPANY / STATE / BRANCH', scope_id INT NOT NULL DEFAULT 0 COMMENT '0 = whole company',
  value_text VARCHAR(2000) NOT NULL, effective_from DATE NOT NULL, effective_to DATE NULL,
  source VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT 'BRIEF = seeded default from the requirements, to be verified; USER = entered here',
  reason VARCHAR(300) NULL, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_setting (company_id, setting_key, scope_type, scope_id, effective_from),
  CONSTRAINT fk_hrs_def FOREIGN KEY (setting_key) REFERENCES hr_setting_def (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='effective-dated values; the row valid on a date with the latest effective_from wins';

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('MONEY_ROUNDING','General','Rounding of money amounts','How a fraction of a rupee is rounded','ENUM',NULL,NULL,NULL,'NEAREST,FLOOR,CEIL',1),
 ('MAKER_CHECKER_ON','General','Maker-checker for payroll','The person who processes a run cannot approve it','BOOL',NULL,NULL,NULL,NULL,2),
 ('PROBATION_MONTHS','Employee','Default probation period','Months, used when an employee has none of their own','INT','months','0','24',NULL,10),
 ('NOTICE_DAYS','Employee','Default notice period','Days, used when an employee has none of their own','INT','days','0','365',NULL,11),
 ('PF_WAGE_CEILING','PF','PF wage ceiling','Monthly wage up to which PF is computed (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,20),
 ('PF_EMPLOYEE_RATE','PF','PF - employee share','Percent of PF wage','DECIMAL','%','0','100',NULL,21),
 ('PF_EMPLOYER_EPF_RATE','PF','PF - employer EPF share','Percent of PF wage (EPF a/c 01)','DECIMAL','%','0','100',NULL,22),
 ('PF_EMPLOYER_EPS_RATE','PF','PF - employer EPS share','Percent of PF wage (pension a/c 10)','DECIMAL','%','0','100',NULL,23),
 ('PF_EDLI_RATE','PF','EDLI rate','Percent, employer cost','DECIMAL','%','0','100',NULL,24),
 ('PF_ADMIN_RATE','PF','PF admin charge rate','Percent, employer cost','DECIMAL','%','0','100',NULL,25),
 ('ESI_WAGE_LIMIT','ESI','ESI wage limit','ESI applies while gross is at or below this (verify with a CA)','DECIMAL','Rs','0',NULL,NULL,30),
 ('ESI_WAGE_LIMIT_DISABLED','ESI','ESI wage limit - person with disability','Higher limit for employees marked disabled','DECIMAL','Rs','0',NULL,NULL,31),
 ('ESI_EMPLOYEE_RATE','ESI','ESI - employee share','Percent of gross','DECIMAL','%','0','100',NULL,32),
 ('ESI_EMPLOYER_RATE','ESI','ESI - employer share','Percent of gross','DECIMAL','%','0','100',NULL,33),
 ('PT_ANNUAL_CAP','PT','Professional tax - yearly cap','Most PT a person pays in a year','DECIMAL','Rs','0',NULL,NULL,40),
 ('WAGE_RULE_MIN_PERCENT','Wages','Wage rule - minimum percent','Basic + DA must be at least this percent of total remuneration (verify with a CA)','DECIMAL','%','0','100',NULL,50),
 ('WAGE_RULE_MODE','Wages','Wage rule - what to do on breach','OFF / WARN / BLOCK','ENUM',NULL,NULL,NULL,'OFF,WARN,BLOCK',51);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, v.k, v.val, v.fr, 'BRIEF', 'Seeded default from the requirements - verify with a CA' FROM (
 SELECT 'MONEY_ROUNDING' k, 'NEAREST' val, DATE '2000-04-01' fr UNION ALL SELECT 'MAKER_CHECKER_ON','true','2000-04-01'
 UNION ALL SELECT 'PROBATION_MONTHS','6','2000-04-01' UNION ALL SELECT 'NOTICE_DAYS','30','2000-04-01'
 UNION ALL SELECT 'PF_WAGE_CEILING','15000','2000-04-01' UNION ALL SELECT 'PF_WAGE_CEILING','25000','2026-09-17'
 UNION ALL SELECT 'PF_EMPLOYEE_RATE','12','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPF_RATE','3.67','2000-04-01' UNION ALL SELECT 'PF_EMPLOYER_EPS_RATE','8.33','2000-04-01'
 UNION ALL SELECT 'PF_EDLI_RATE','0.5','2000-04-01' UNION ALL SELECT 'PF_ADMIN_RATE','0.5','2000-04-01'
 UNION ALL SELECT 'ESI_WAGE_LIMIT','21000','2000-04-01' UNION ALL SELECT 'ESI_WAGE_LIMIT_DISABLED','25000','2000-04-01'
 UNION ALL SELECT 'ESI_EMPLOYEE_RATE','0.75','2000-04-01' UNION ALL SELECT 'ESI_EMPLOYER_RATE','3.25','2000-04-01'
 UNION ALL SELECT 'PT_ANNUAL_CAP','2500','2000-04-01'
 UNION ALL SELECT 'WAGE_RULE_MIN_PERCENT','50','2025-11-21' UNION ALL SELECT 'WAGE_RULE_MODE','WARN','2025-11-21') v
WHERE NOT EXISTS (SELECT 1 FROM hr_setting);
UPDATE hr_setting SET effective_to = '2026-09-16' WHERE setting_key = 'PF_WAGE_CEILING' AND effective_from = '2000-04-01' AND effective_to IS NULL AND source = 'BRIEF';

-- menu: HR & Payroll group + 4 pages, under the same parent as the other masters
SET @hr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset','teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @hr_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @hr_parent LIMIT 1) p
JOIN (SELECT 1 n,'HR Setup' label,'Company, branches, departments, grades' title,'building' icon,'hrsetup' page,'hr/hr-setup.html' href,40 so
      UNION ALL SELECT 2,'HR Settings','Effective-dated rules and rates','sliders','hrsettings','hr/hr-settings.html',41
      UNION ALL SELECT 3,'HR Roles & Access','Roles and permission matrix','shield-lock','hrroles','hr/hr-roles.html',42
      UNION ALL SELECT 4,'HR Audit Log','Who changed what','journal-text','hraudit','hr/hr-audit.html',43) m
WHERE @hr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);
