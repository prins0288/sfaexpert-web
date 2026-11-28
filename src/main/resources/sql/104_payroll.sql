-- 104: salary head master, employee salary (revisions), payroll run + payslips, salary hold (state / HQ / designation / employee), slip design. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS hr_salary_head (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(80) NOT NULL,
  head_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION', calc_type VARCHAR(8) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED amount / PERCENT of a base / BALANCE (earning that fills the rest of the gross)',
  calc_base VARCHAR(20) NULL COMMENT 'PERCENT: GROSS or the code of an earlier earning head', default_value DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'suggested value when a salary is set up',
  prorata TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'reduced for loss-of-pay days', pf_wage TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'counts in the PF wage', esi_wage TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'counts in the ESI wage',
  taxable TINYINT(1) NOT NULL DEFAULT 1, show_in_slip TINYINT(1) NOT NULL DEFAULT 1, sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_head (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary head master (earnings and deductions); PF / ESI are worked out by the engine from settings';
INSERT IGNORE INTO hr_salary_head (company_id, code, name, head_type, calc_type, calc_base, default_value, prorata, pf_wage, esi_wage, taxable, sort_order) VALUES
 (1,'BASIC','Basic','EARNING','PERCENT','GROSS',50,1,1,1,1,1),
 (1,'HRA','House Rent Allowance','EARNING','PERCENT','BASIC',40,1,0,1,1,2),
 (1,'CONV','Conveyance','EARNING','FIXED',NULL,1600,1,0,1,1,3),
 (1,'SPECIAL','Special Allowance','EARNING','BALANCE',NULL,0,1,0,1,1,4),
 (1,'PT','Professional Tax','DEDUCTION','FIXED',NULL,200,0,0,0,0,10);

CREATE TABLE IF NOT EXISTS hr_emp_salary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, emp_id VARCHAR(40) NOT NULL,
  effective_from DATE NOT NULL, effective_to DATE NULL, gross_monthly DECIMAL(12,2) NOT NULL, remarks VARCHAR(200) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_es_emp (emp_id, effective_from)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary revisions of an employee (effective dated; the one valid in the payroll month is used)';
CREATE TABLE IF NOT EXISTS hr_emp_salary_head (
  salary_id BIGINT UNSIGNED NOT NULL, head_id INT UNSIGNED NOT NULL, head_value DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'rupees (FIXED) or percent (PERCENT)',
  PRIMARY KEY (salary_id, head_id), CONSTRAINT fk_hr_esh_sal FOREIGN KEY (salary_id) REFERENCES hr_emp_salary (id) ON DELETE CASCADE,
  CONSTRAINT fk_hr_esh_head FOREIGN KEY (head_id) REFERENCES hr_salary_head (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_payroll_run (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, period_year SMALLINT NOT NULL, period_month TINYINT NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / APPROVED / PAID', generated_by VARCHAR(40) NULL, generated_at DATETIME NULL,
  approved_by VARCHAR(40) NULL, approved_at DATETIME NULL, paid_at DATETIME NULL, remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_run (company_id, period_year, period_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, run_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, salary_id BIGINT UNSIGNED NULL,
  days_in_month TINYINT NOT NULL, lop_days DECIMAL(6,2) NOT NULL DEFAULT 0, paid_days DECIMAL(6,2) NOT NULL, lop_note VARCHAR(200) NULL,
  gross_earnings DECIMAL(12,2) NOT NULL, total_deductions DECIMAL(12,2) NOT NULL, net_pay DECIMAL(12,2) NOT NULL, employer_cost DECIMAL(12,2) NOT NULL, pf_wage DECIMAL(12,2) NOT NULL DEFAULT 0, esi_wage DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(10) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED / HELD / RELEASED', hold_reason VARCHAR(200) NULL, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_slip (run_id, emp_id), KEY ix_hr_slip_emp (emp_id),
  CONSTRAINT fk_hr_slip_run FOREIGN KEY (run_id) REFERENCES hr_payroll_run (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip_line (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, payslip_id BIGINT UNSIGNED NOT NULL, head_code VARCHAR(20) NOT NULL, head_name VARCHAR(80) NOT NULL,
  line_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION / EMPLOYER', amount DECIMAL(12,2) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_hr_pl (payslip_id), CONSTRAINT fk_hr_pl_slip FOREIGN KEY (payslip_id) REFERENCES hr_payslip (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the computed lines are kept as they were: an old payslip never changes when a head or a rate changes later';

CREATE TABLE IF NOT EXISTS hr_salary_hold (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, from_period INT NOT NULL COMMENT 'YYYYMM the hold starts', to_period INT NULL COMMENT 'YYYYMM it ends (empty = until released)',
  reason VARCHAR(200) NOT NULL, is_active TINYINT(1) NOT NULL DEFAULT 1, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_hr_hold (is_active, from_period)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='hold the salary of the chosen states / HQs / designations / employees';
CREATE TABLE IF NOT EXISTS hr_salary_hold_target (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hold_id BIGINT UNSIGNED NOT NULL, target_type VARCHAR(12) NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP', target_value VARCHAR(40) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_ht (hold_id, target_type, target_value), CONSTRAINT fk_hr_ht_hold FOREIGN KEY (hold_id) REFERENCES hr_salary_hold (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_slip_config (
  company_id INT UNSIGNED NOT NULL DEFAULT 1, title VARCHAR(80) NOT NULL DEFAULT 'Salary Slip', header_note VARCHAR(300) NULL, footer_note VARCHAR(300) NULL DEFAULT 'This is a computer generated payslip and does not need a signature.',
  show_logo TINYINT(1) NOT NULL DEFAULT 1, show_address TINYINT(1) NOT NULL DEFAULT 1, show_employer_contrib TINYINT(1) NOT NULL DEFAULT 0, show_bank TINYINT(1) NOT NULL DEFAULT 1, show_statutory_ids TINYINT(1) NOT NULL DEFAULT 1,
  show_attendance TINYINT(1) NOT NULL DEFAULT 1, show_net_in_words TINYINT(1) NOT NULL DEFAULT 1, accent_color VARCHAR(9) NOT NULL DEFAULT '#d9441a', signatory_label VARCHAR(60) NULL,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (company_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what the salary slip shows and how it looks';
INSERT IGNORE INTO hr_slip_config (company_id) VALUES (1);

SET @sl_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
SET @sl_dcr := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), @sl_parent);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, IF(m.mine = 1, @sl_dcr, @sl_parent), p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sl_parent LIMIT 1) p
JOIN (SELECT 1 n, 0 mine,'Salary Head Master' label,'Earnings and deductions' title,'cash-coin' icon,'salaryhead' page,'hr/salary-head.html' href,46 so
      UNION ALL SELECT 2,0,'Employee Salary','Salary of each employee','person-vcard','empsalary','hr/emp-salary.html',47
      UNION ALL SELECT 3,0,'Payroll','Generate, hold, approve salary','wallet2','payroll','hr/payroll.html',48
      UNION ALL SELECT 4,0,'Salary Hold','Hold salary state / HQ / designation / employee wise','pause-circle','salaryhold','hr/salary-hold.html',49
      UNION ALL SELECT 5,0,'Salary Slip Design','What the slip shows, logo, notes','file-earmark-text','slipdesign','hr/slip-design.html',50
      UNION ALL SELECT 6,1,'My Salary Slip','My payslips','receipt','mypayslip','hr/my-payslip.html',10) m
WHERE @sl_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('salaryhead','empsalary','payroll','salaryhold','slipdesign','mypayslip');

USE globex_db;

CREATE TABLE IF NOT EXISTS hr_salary_head (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(80) NOT NULL,
  head_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION', calc_type VARCHAR(8) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED amount / PERCENT of a base / BALANCE (earning that fills the rest of the gross)',
  calc_base VARCHAR(20) NULL COMMENT 'PERCENT: GROSS or the code of an earlier earning head', default_value DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'suggested value when a salary is set up',
  prorata TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'reduced for loss-of-pay days', pf_wage TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'counts in the PF wage', esi_wage TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'counts in the ESI wage',
  taxable TINYINT(1) NOT NULL DEFAULT 1, show_in_slip TINYINT(1) NOT NULL DEFAULT 1, sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_head (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary head master (earnings and deductions); PF / ESI are worked out by the engine from settings';
INSERT IGNORE INTO hr_salary_head (company_id, code, name, head_type, calc_type, calc_base, default_value, prorata, pf_wage, esi_wage, taxable, sort_order) VALUES
 (1,'BASIC','Basic','EARNING','PERCENT','GROSS',50,1,1,1,1,1),
 (1,'HRA','House Rent Allowance','EARNING','PERCENT','BASIC',40,1,0,1,1,2),
 (1,'CONV','Conveyance','EARNING','FIXED',NULL,1600,1,0,1,1,3),
 (1,'SPECIAL','Special Allowance','EARNING','BALANCE',NULL,0,1,0,1,1,4),
 (1,'PT','Professional Tax','DEDUCTION','FIXED',NULL,200,0,0,0,0,10);

CREATE TABLE IF NOT EXISTS hr_emp_salary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, emp_id VARCHAR(40) NOT NULL,
  effective_from DATE NOT NULL, effective_to DATE NULL, gross_monthly DECIMAL(12,2) NOT NULL, remarks VARCHAR(200) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_es_emp (emp_id, effective_from)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary revisions of an employee (effective dated; the one valid in the payroll month is used)';
CREATE TABLE IF NOT EXISTS hr_emp_salary_head (
  salary_id BIGINT UNSIGNED NOT NULL, head_id INT UNSIGNED NOT NULL, head_value DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'rupees (FIXED) or percent (PERCENT)',
  PRIMARY KEY (salary_id, head_id), CONSTRAINT fk_hr_esh_sal FOREIGN KEY (salary_id) REFERENCES hr_emp_salary (id) ON DELETE CASCADE,
  CONSTRAINT fk_hr_esh_head FOREIGN KEY (head_id) REFERENCES hr_salary_head (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_payroll_run (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, period_year SMALLINT NOT NULL, period_month TINYINT NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / APPROVED / PAID', generated_by VARCHAR(40) NULL, generated_at DATETIME NULL,
  approved_by VARCHAR(40) NULL, approved_at DATETIME NULL, paid_at DATETIME NULL, remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_run (company_id, period_year, period_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, run_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, salary_id BIGINT UNSIGNED NULL,
  days_in_month TINYINT NOT NULL, lop_days DECIMAL(6,2) NOT NULL DEFAULT 0, paid_days DECIMAL(6,2) NOT NULL, lop_note VARCHAR(200) NULL,
  gross_earnings DECIMAL(12,2) NOT NULL, total_deductions DECIMAL(12,2) NOT NULL, net_pay DECIMAL(12,2) NOT NULL, employer_cost DECIMAL(12,2) NOT NULL, pf_wage DECIMAL(12,2) NOT NULL DEFAULT 0, esi_wage DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(10) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED / HELD / RELEASED', hold_reason VARCHAR(200) NULL, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_slip (run_id, emp_id), KEY ix_hr_slip_emp (emp_id),
  CONSTRAINT fk_hr_slip_run FOREIGN KEY (run_id) REFERENCES hr_payroll_run (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip_line (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, payslip_id BIGINT UNSIGNED NOT NULL, head_code VARCHAR(20) NOT NULL, head_name VARCHAR(80) NOT NULL,
  line_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION / EMPLOYER', amount DECIMAL(12,2) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_hr_pl (payslip_id), CONSTRAINT fk_hr_pl_slip FOREIGN KEY (payslip_id) REFERENCES hr_payslip (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the computed lines are kept as they were: an old payslip never changes when a head or a rate changes later';

CREATE TABLE IF NOT EXISTS hr_salary_hold (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, from_period INT NOT NULL COMMENT 'YYYYMM the hold starts', to_period INT NULL COMMENT 'YYYYMM it ends (empty = until released)',
  reason VARCHAR(200) NOT NULL, is_active TINYINT(1) NOT NULL DEFAULT 1, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_hr_hold (is_active, from_period)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='hold the salary of the chosen states / HQs / designations / employees';
CREATE TABLE IF NOT EXISTS hr_salary_hold_target (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hold_id BIGINT UNSIGNED NOT NULL, target_type VARCHAR(12) NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP', target_value VARCHAR(40) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_ht (hold_id, target_type, target_value), CONSTRAINT fk_hr_ht_hold FOREIGN KEY (hold_id) REFERENCES hr_salary_hold (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_slip_config (
  company_id INT UNSIGNED NOT NULL DEFAULT 1, title VARCHAR(80) NOT NULL DEFAULT 'Salary Slip', header_note VARCHAR(300) NULL, footer_note VARCHAR(300) NULL DEFAULT 'This is a computer generated payslip and does not need a signature.',
  show_logo TINYINT(1) NOT NULL DEFAULT 1, show_address TINYINT(1) NOT NULL DEFAULT 1, show_employer_contrib TINYINT(1) NOT NULL DEFAULT 0, show_bank TINYINT(1) NOT NULL DEFAULT 1, show_statutory_ids TINYINT(1) NOT NULL DEFAULT 1,
  show_attendance TINYINT(1) NOT NULL DEFAULT 1, show_net_in_words TINYINT(1) NOT NULL DEFAULT 1, accent_color VARCHAR(9) NOT NULL DEFAULT '#d9441a', signatory_label VARCHAR(60) NULL,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (company_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what the salary slip shows and how it looks';
INSERT IGNORE INTO hr_slip_config (company_id) VALUES (1);

SET @sl_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
SET @sl_dcr := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), @sl_parent);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, IF(m.mine = 1, @sl_dcr, @sl_parent), p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sl_parent LIMIT 1) p
JOIN (SELECT 1 n, 0 mine,'Salary Head Master' label,'Earnings and deductions' title,'cash-coin' icon,'salaryhead' page,'hr/salary-head.html' href,46 so
      UNION ALL SELECT 2,0,'Employee Salary','Salary of each employee','person-vcard','empsalary','hr/emp-salary.html',47
      UNION ALL SELECT 3,0,'Payroll','Generate, hold, approve salary','wallet2','payroll','hr/payroll.html',48
      UNION ALL SELECT 4,0,'Salary Hold','Hold salary state / HQ / designation / employee wise','pause-circle','salaryhold','hr/salary-hold.html',49
      UNION ALL SELECT 5,0,'Salary Slip Design','What the slip shows, logo, notes','file-earmark-text','slipdesign','hr/slip-design.html',50
      UNION ALL SELECT 6,1,'My Salary Slip','My payslips','receipt','mypayslip','hr/my-payslip.html',10) m
WHERE @sl_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('salaryhead','empsalary','payroll','salaryhold','slipdesign','mypayslip');

USE initech_db;

CREATE TABLE IF NOT EXISTS hr_salary_head (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(80) NOT NULL,
  head_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION', calc_type VARCHAR(8) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED amount / PERCENT of a base / BALANCE (earning that fills the rest of the gross)',
  calc_base VARCHAR(20) NULL COMMENT 'PERCENT: GROSS or the code of an earlier earning head', default_value DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'suggested value when a salary is set up',
  prorata TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'reduced for loss-of-pay days', pf_wage TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'counts in the PF wage', esi_wage TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'counts in the ESI wage',
  taxable TINYINT(1) NOT NULL DEFAULT 1, show_in_slip TINYINT(1) NOT NULL DEFAULT 1, sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_head (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary head master (earnings and deductions); PF / ESI are worked out by the engine from settings';
INSERT IGNORE INTO hr_salary_head (company_id, code, name, head_type, calc_type, calc_base, default_value, prorata, pf_wage, esi_wage, taxable, sort_order) VALUES
 (1,'BASIC','Basic','EARNING','PERCENT','GROSS',50,1,1,1,1,1),
 (1,'HRA','House Rent Allowance','EARNING','PERCENT','BASIC',40,1,0,1,1,2),
 (1,'CONV','Conveyance','EARNING','FIXED',NULL,1600,1,0,1,1,3),
 (1,'SPECIAL','Special Allowance','EARNING','BALANCE',NULL,0,1,0,1,1,4),
 (1,'PT','Professional Tax','DEDUCTION','FIXED',NULL,200,0,0,0,0,10);

CREATE TABLE IF NOT EXISTS hr_emp_salary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, emp_id VARCHAR(40) NOT NULL,
  effective_from DATE NOT NULL, effective_to DATE NULL, gross_monthly DECIMAL(12,2) NOT NULL, remarks VARCHAR(200) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_es_emp (emp_id, effective_from)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary revisions of an employee (effective dated; the one valid in the payroll month is used)';
CREATE TABLE IF NOT EXISTS hr_emp_salary_head (
  salary_id BIGINT UNSIGNED NOT NULL, head_id INT UNSIGNED NOT NULL, head_value DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'rupees (FIXED) or percent (PERCENT)',
  PRIMARY KEY (salary_id, head_id), CONSTRAINT fk_hr_esh_sal FOREIGN KEY (salary_id) REFERENCES hr_emp_salary (id) ON DELETE CASCADE,
  CONSTRAINT fk_hr_esh_head FOREIGN KEY (head_id) REFERENCES hr_salary_head (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_payroll_run (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, period_year SMALLINT NOT NULL, period_month TINYINT NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / APPROVED / PAID', generated_by VARCHAR(40) NULL, generated_at DATETIME NULL,
  approved_by VARCHAR(40) NULL, approved_at DATETIME NULL, paid_at DATETIME NULL, remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_run (company_id, period_year, period_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, run_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, salary_id BIGINT UNSIGNED NULL,
  days_in_month TINYINT NOT NULL, lop_days DECIMAL(6,2) NOT NULL DEFAULT 0, paid_days DECIMAL(6,2) NOT NULL, lop_note VARCHAR(200) NULL,
  gross_earnings DECIMAL(12,2) NOT NULL, total_deductions DECIMAL(12,2) NOT NULL, net_pay DECIMAL(12,2) NOT NULL, employer_cost DECIMAL(12,2) NOT NULL, pf_wage DECIMAL(12,2) NOT NULL DEFAULT 0, esi_wage DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(10) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED / HELD / RELEASED', hold_reason VARCHAR(200) NULL, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_slip (run_id, emp_id), KEY ix_hr_slip_emp (emp_id),
  CONSTRAINT fk_hr_slip_run FOREIGN KEY (run_id) REFERENCES hr_payroll_run (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip_line (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, payslip_id BIGINT UNSIGNED NOT NULL, head_code VARCHAR(20) NOT NULL, head_name VARCHAR(80) NOT NULL,
  line_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION / EMPLOYER', amount DECIMAL(12,2) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_hr_pl (payslip_id), CONSTRAINT fk_hr_pl_slip FOREIGN KEY (payslip_id) REFERENCES hr_payslip (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the computed lines are kept as they were: an old payslip never changes when a head or a rate changes later';

CREATE TABLE IF NOT EXISTS hr_salary_hold (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, from_period INT NOT NULL COMMENT 'YYYYMM the hold starts', to_period INT NULL COMMENT 'YYYYMM it ends (empty = until released)',
  reason VARCHAR(200) NOT NULL, is_active TINYINT(1) NOT NULL DEFAULT 1, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_hr_hold (is_active, from_period)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='hold the salary of the chosen states / HQs / designations / employees';
CREATE TABLE IF NOT EXISTS hr_salary_hold_target (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hold_id BIGINT UNSIGNED NOT NULL, target_type VARCHAR(12) NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP', target_value VARCHAR(40) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_ht (hold_id, target_type, target_value), CONSTRAINT fk_hr_ht_hold FOREIGN KEY (hold_id) REFERENCES hr_salary_hold (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_slip_config (
  company_id INT UNSIGNED NOT NULL DEFAULT 1, title VARCHAR(80) NOT NULL DEFAULT 'Salary Slip', header_note VARCHAR(300) NULL, footer_note VARCHAR(300) NULL DEFAULT 'This is a computer generated payslip and does not need a signature.',
  show_logo TINYINT(1) NOT NULL DEFAULT 1, show_address TINYINT(1) NOT NULL DEFAULT 1, show_employer_contrib TINYINT(1) NOT NULL DEFAULT 0, show_bank TINYINT(1) NOT NULL DEFAULT 1, show_statutory_ids TINYINT(1) NOT NULL DEFAULT 1,
  show_attendance TINYINT(1) NOT NULL DEFAULT 1, show_net_in_words TINYINT(1) NOT NULL DEFAULT 1, accent_color VARCHAR(9) NOT NULL DEFAULT '#d9441a', signatory_label VARCHAR(60) NULL,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (company_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what the salary slip shows and how it looks';
INSERT IGNORE INTO hr_slip_config (company_id) VALUES (1);

SET @sl_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
SET @sl_dcr := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), @sl_parent);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, IF(m.mine = 1, @sl_dcr, @sl_parent), p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sl_parent LIMIT 1) p
JOIN (SELECT 1 n, 0 mine,'Salary Head Master' label,'Earnings and deductions' title,'cash-coin' icon,'salaryhead' page,'hr/salary-head.html' href,46 so
      UNION ALL SELECT 2,0,'Employee Salary','Salary of each employee','person-vcard','empsalary','hr/emp-salary.html',47
      UNION ALL SELECT 3,0,'Payroll','Generate, hold, approve salary','wallet2','payroll','hr/payroll.html',48
      UNION ALL SELECT 4,0,'Salary Hold','Hold salary state / HQ / designation / employee wise','pause-circle','salaryhold','hr/salary-hold.html',49
      UNION ALL SELECT 5,0,'Salary Slip Design','What the slip shows, logo, notes','file-earmark-text','slipdesign','hr/slip-design.html',50
      UNION ALL SELECT 6,1,'My Salary Slip','My payslips','receipt','mypayslip','hr/my-payslip.html',10) m
WHERE @sl_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('salaryhead','empsalary','payroll','salaryhold','slipdesign','mypayslip');

USE sfa_demo;

CREATE TABLE IF NOT EXISTS hr_salary_head (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, code VARCHAR(20) NOT NULL, name VARCHAR(80) NOT NULL,
  head_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION', calc_type VARCHAR(8) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED amount / PERCENT of a base / BALANCE (earning that fills the rest of the gross)',
  calc_base VARCHAR(20) NULL COMMENT 'PERCENT: GROSS or the code of an earlier earning head', default_value DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'suggested value when a salary is set up',
  prorata TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'reduced for loss-of-pay days', pf_wage TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'counts in the PF wage', esi_wage TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'counts in the ESI wage',
  taxable TINYINT(1) NOT NULL DEFAULT 1, show_in_slip TINYINT(1) NOT NULL DEFAULT 1, sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_head (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary head master (earnings and deductions); PF / ESI are worked out by the engine from settings';
INSERT IGNORE INTO hr_salary_head (company_id, code, name, head_type, calc_type, calc_base, default_value, prorata, pf_wage, esi_wage, taxable, sort_order) VALUES
 (1,'BASIC','Basic','EARNING','PERCENT','GROSS',50,1,1,1,1,1),
 (1,'HRA','House Rent Allowance','EARNING','PERCENT','BASIC',40,1,0,1,1,2),
 (1,'CONV','Conveyance','EARNING','FIXED',NULL,1600,1,0,1,1,3),
 (1,'SPECIAL','Special Allowance','EARNING','BALANCE',NULL,0,1,0,1,1,4),
 (1,'PT','Professional Tax','DEDUCTION','FIXED',NULL,200,0,0,0,0,10);

CREATE TABLE IF NOT EXISTS hr_emp_salary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, emp_id VARCHAR(40) NOT NULL,
  effective_from DATE NOT NULL, effective_to DATE NULL, gross_monthly DECIMAL(12,2) NOT NULL, remarks VARCHAR(200) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_es_emp (emp_id, effective_from)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='salary revisions of an employee (effective dated; the one valid in the payroll month is used)';
CREATE TABLE IF NOT EXISTS hr_emp_salary_head (
  salary_id BIGINT UNSIGNED NOT NULL, head_id INT UNSIGNED NOT NULL, head_value DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT 'rupees (FIXED) or percent (PERCENT)',
  PRIMARY KEY (salary_id, head_id), CONSTRAINT fk_hr_esh_sal FOREIGN KEY (salary_id) REFERENCES hr_emp_salary (id) ON DELETE CASCADE,
  CONSTRAINT fk_hr_esh_head FOREIGN KEY (head_id) REFERENCES hr_salary_head (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_payroll_run (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, period_year SMALLINT NOT NULL, period_month TINYINT NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / APPROVED / PAID', generated_by VARCHAR(40) NULL, generated_at DATETIME NULL,
  approved_by VARCHAR(40) NULL, approved_at DATETIME NULL, paid_at DATETIME NULL, remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_run (company_id, period_year, period_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, run_id BIGINT UNSIGNED NOT NULL, emp_id VARCHAR(40) NOT NULL, salary_id BIGINT UNSIGNED NULL,
  days_in_month TINYINT NOT NULL, lop_days DECIMAL(6,2) NOT NULL DEFAULT 0, paid_days DECIMAL(6,2) NOT NULL, lop_note VARCHAR(200) NULL,
  gross_earnings DECIMAL(12,2) NOT NULL, total_deductions DECIMAL(12,2) NOT NULL, net_pay DECIMAL(12,2) NOT NULL, employer_cost DECIMAL(12,2) NOT NULL, pf_wage DECIMAL(12,2) NOT NULL DEFAULT 0, esi_wage DECIMAL(12,2) NOT NULL DEFAULT 0,
  status VARCHAR(10) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED / HELD / RELEASED', hold_reason VARCHAR(200) NULL, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_slip (run_id, emp_id), KEY ix_hr_slip_emp (emp_id),
  CONSTRAINT fk_hr_slip_run FOREIGN KEY (run_id) REFERENCES hr_payroll_run (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS hr_payslip_line (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, payslip_id BIGINT UNSIGNED NOT NULL, head_code VARCHAR(20) NOT NULL, head_name VARCHAR(80) NOT NULL,
  line_type VARCHAR(10) NOT NULL COMMENT 'EARNING / DEDUCTION / EMPLOYER', amount DECIMAL(12,2) NOT NULL, sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), KEY ix_hr_pl (payslip_id), CONSTRAINT fk_hr_pl_slip FOREIGN KEY (payslip_id) REFERENCES hr_payslip (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='the computed lines are kept as they were: an old payslip never changes when a head or a rate changes later';

CREATE TABLE IF NOT EXISTS hr_salary_hold (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1, from_period INT NOT NULL COMMENT 'YYYYMM the hold starts', to_period INT NULL COMMENT 'YYYYMM it ends (empty = until released)',
  reason VARCHAR(200) NOT NULL, is_active TINYINT(1) NOT NULL DEFAULT 1, created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, released_by VARCHAR(40) NULL, released_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_hr_hold (is_active, from_period)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='hold the salary of the chosen states / HQs / designations / employees';
CREATE TABLE IF NOT EXISTS hr_salary_hold_target (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, hold_id BIGINT UNSIGNED NOT NULL, target_type VARCHAR(12) NOT NULL COMMENT 'STATE / HQ / DESIGNATION / EMP', target_value VARCHAR(40) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_ht (hold_id, target_type, target_value), CONSTRAINT fk_hr_ht_hold FOREIGN KEY (hold_id) REFERENCES hr_salary_hold (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_slip_config (
  company_id INT UNSIGNED NOT NULL DEFAULT 1, title VARCHAR(80) NOT NULL DEFAULT 'Salary Slip', header_note VARCHAR(300) NULL, footer_note VARCHAR(300) NULL DEFAULT 'This is a computer generated payslip and does not need a signature.',
  show_logo TINYINT(1) NOT NULL DEFAULT 1, show_address TINYINT(1) NOT NULL DEFAULT 1, show_employer_contrib TINYINT(1) NOT NULL DEFAULT 0, show_bank TINYINT(1) NOT NULL DEFAULT 1, show_statutory_ids TINYINT(1) NOT NULL DEFAULT 1,
  show_attendance TINYINT(1) NOT NULL DEFAULT 1, show_net_in_words TINYINT(1) NOT NULL DEFAULT 1, accent_color VARCHAR(9) NOT NULL DEFAULT '#d9441a', signatory_label VARCHAR(60) NULL,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL, PRIMARY KEY (company_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what the salary slip shows and how it looks';
INSERT IGNORE INTO hr_slip_config (company_id) VALUES (1);

SET @sl_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
SET @sl_dcr := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), @sl_parent);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, IF(m.mine = 1, @sl_dcr, @sl_parent), p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @sl_parent LIMIT 1) p
JOIN (SELECT 1 n, 0 mine,'Salary Head Master' label,'Earnings and deductions' title,'cash-coin' icon,'salaryhead' page,'hr/salary-head.html' href,46 so
      UNION ALL SELECT 2,0,'Employee Salary','Salary of each employee','person-vcard','empsalary','hr/emp-salary.html',47
      UNION ALL SELECT 3,0,'Payroll','Generate, hold, approve salary','wallet2','payroll','hr/payroll.html',48
      UNION ALL SELECT 4,0,'Salary Hold','Hold salary state / HQ / designation / employee wise','pause-circle','salaryhold','hr/salary-hold.html',49
      UNION ALL SELECT 5,0,'Salary Slip Design','What the slip shows, logo, notes','file-earmark-text','slipdesign','hr/slip-design.html',50
      UNION ALL SELECT 6,1,'My Salary Slip','My payslips','receipt','mypayslip','hr/my-payslip.html',10) m
WHERE @sl_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('salaryhead','empsalary','payroll','salaryhold','slipdesign','mypayslip');
