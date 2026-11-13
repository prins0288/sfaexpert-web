-- 102: leave master (policy per leave type), leave applications (from the DCR screen or the My Leave page), manual balance adjustments. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS hr_leave_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  code VARCHAR(12) NOT NULL, name VARCHAR(80) NOT NULL, is_paid TINYINT(1) NOT NULL DEFAULT 1,
  annual_quota DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'days per leave year',
  accrual VARCHAR(8) NOT NULL DEFAULT 'UPFRONT' COMMENT 'UPFRONT = whole quota at year start, MONTHLY = quota/12 each month',
  carry_forward_max DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'unused days carried to next year, at most this many',
  allow_half_day TINYINT(1) NOT NULL DEFAULT 1, max_consecutive SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit (calendar days of one application)',
  max_per_month DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '0 = no limit', min_notice_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'apply at least this many days before the first day',
  backdate_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'may apply for a day up to this many days back', future_days SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit on how far ahead',
  requires_reason TINYINT(1) NOT NULL DEFAULT 1, sandwich TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'weekly offs / holidays between leave days are counted as leave',
  allow_negative TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'balance may run out: the shortfall becomes loss-of-pay days (always on for LWP)',
  auto_approve TINYINT(1) NOT NULL DEFAULT 0, min_service_months SMALLINT NOT NULL DEFAULT 0 COMMENT 'eligible after this many months of service',
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_leave_type (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='leave master = the policy of each leave type';
INSERT IGNORE INTO hr_leave_type (company_id, code, name, is_paid, annual_quota, accrual, carry_forward_max, allow_half_day, max_consecutive, max_per_month, min_notice_days, backdate_days, future_days, requires_reason, sandwich, allow_negative, auto_approve, min_service_months, sort_order) VALUES
 (1,'CL','Casual Leave',1,12,'MONTHLY',0,1,3,3,0,7,0,1,0,0,0,0,1),
 (1,'SL','Sick Leave',1,12,'UPFRONT',0,1,0,0,0,7,0,1,0,0,0,0,2),
 (1,'PL','Privilege Leave',1,18,'MONTHLY',30,0,0,0,7,0,0,1,0,0,0,6,3),
 (1,'LWP','Leave Without Pay',0,0,'UPFRONT',0,1,0,0,0,7,0,1,0,1,0,0,4);

CREATE TABLE IF NOT EXISTS hr_leave_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL, from_date DATE NOT NULL, to_date DATE NOT NULL,
  day_part VARCHAR(8) NOT NULL DEFAULT 'FULL' COMMENT 'FULL / FIRST / SECOND (half day)', days DECIMAL(6,2) NOT NULL, lwp_days DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'part of days that is unpaid because the balance ran out',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / CANCELLED',
  source VARCHAR(6) NOT NULL DEFAULT 'WEB' COMMENT 'DCR / WEB', dcr_id BIGINT UNSIGNED NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_hr_la_emp (emp_id, from_date), KEY ix_hr_la_status (status, from_date),
  CONSTRAINT fk_hr_la_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_leave_adjustment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL,
  leave_year SMALLINT NOT NULL COMMENT 'year in which the leave year starts', days DECIMAL(6,2) NOT NULL COMMENT '+ credit / - debit', reason VARCHAR(200) NOT NULL,
  by_emp_id VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_ladj (emp_id, leave_type_id, leave_year),
  CONSTRAINT fk_hr_ladj_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='manual balance corrections (opening balance, encashment, grants)';

SET @lv_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @lv_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @lv_parent LIMIT 1) p
JOIN (SELECT 1 n,'Leave Master' label,'Leave types and policy' title,'calendar2-x' icon,'hrleavemaster' page,'hr/leave-master.html' href,44 so
      UNION ALL SELECT 2,'My Leave','Balance, apply, approvals','calendar-check','myleave','hr/leave-my.html',45) m
WHERE @lv_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);

USE globex_db;

CREATE TABLE IF NOT EXISTS hr_leave_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  code VARCHAR(12) NOT NULL, name VARCHAR(80) NOT NULL, is_paid TINYINT(1) NOT NULL DEFAULT 1,
  annual_quota DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'days per leave year',
  accrual VARCHAR(8) NOT NULL DEFAULT 'UPFRONT' COMMENT 'UPFRONT = whole quota at year start, MONTHLY = quota/12 each month',
  carry_forward_max DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'unused days carried to next year, at most this many',
  allow_half_day TINYINT(1) NOT NULL DEFAULT 1, max_consecutive SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit (calendar days of one application)',
  max_per_month DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '0 = no limit', min_notice_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'apply at least this many days before the first day',
  backdate_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'may apply for a day up to this many days back', future_days SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit on how far ahead',
  requires_reason TINYINT(1) NOT NULL DEFAULT 1, sandwich TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'weekly offs / holidays between leave days are counted as leave',
  allow_negative TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'balance may run out: the shortfall becomes loss-of-pay days (always on for LWP)',
  auto_approve TINYINT(1) NOT NULL DEFAULT 0, min_service_months SMALLINT NOT NULL DEFAULT 0 COMMENT 'eligible after this many months of service',
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_leave_type (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='leave master = the policy of each leave type';
INSERT IGNORE INTO hr_leave_type (company_id, code, name, is_paid, annual_quota, accrual, carry_forward_max, allow_half_day, max_consecutive, max_per_month, min_notice_days, backdate_days, future_days, requires_reason, sandwich, allow_negative, auto_approve, min_service_months, sort_order) VALUES
 (1,'CL','Casual Leave',1,12,'MONTHLY',0,1,3,3,0,7,0,1,0,0,0,0,1),
 (1,'SL','Sick Leave',1,12,'UPFRONT',0,1,0,0,0,7,0,1,0,0,0,0,2),
 (1,'PL','Privilege Leave',1,18,'MONTHLY',30,0,0,0,7,0,0,1,0,0,0,6,3),
 (1,'LWP','Leave Without Pay',0,0,'UPFRONT',0,1,0,0,0,7,0,1,0,1,0,0,4);

CREATE TABLE IF NOT EXISTS hr_leave_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL, from_date DATE NOT NULL, to_date DATE NOT NULL,
  day_part VARCHAR(8) NOT NULL DEFAULT 'FULL' COMMENT 'FULL / FIRST / SECOND (half day)', days DECIMAL(6,2) NOT NULL, lwp_days DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'part of days that is unpaid because the balance ran out',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / CANCELLED',
  source VARCHAR(6) NOT NULL DEFAULT 'WEB' COMMENT 'DCR / WEB', dcr_id BIGINT UNSIGNED NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_hr_la_emp (emp_id, from_date), KEY ix_hr_la_status (status, from_date),
  CONSTRAINT fk_hr_la_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_leave_adjustment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL,
  leave_year SMALLINT NOT NULL COMMENT 'year in which the leave year starts', days DECIMAL(6,2) NOT NULL COMMENT '+ credit / - debit', reason VARCHAR(200) NOT NULL,
  by_emp_id VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_ladj (emp_id, leave_type_id, leave_year),
  CONSTRAINT fk_hr_ladj_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='manual balance corrections (opening balance, encashment, grants)';

SET @lv_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @lv_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @lv_parent LIMIT 1) p
JOIN (SELECT 1 n,'Leave Master' label,'Leave types and policy' title,'calendar2-x' icon,'hrleavemaster' page,'hr/leave-master.html' href,44 so
      UNION ALL SELECT 2,'My Leave','Balance, apply, approvals','calendar-check','myleave','hr/leave-my.html',45) m
WHERE @lv_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);

USE initech_db;

CREATE TABLE IF NOT EXISTS hr_leave_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  code VARCHAR(12) NOT NULL, name VARCHAR(80) NOT NULL, is_paid TINYINT(1) NOT NULL DEFAULT 1,
  annual_quota DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'days per leave year',
  accrual VARCHAR(8) NOT NULL DEFAULT 'UPFRONT' COMMENT 'UPFRONT = whole quota at year start, MONTHLY = quota/12 each month',
  carry_forward_max DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'unused days carried to next year, at most this many',
  allow_half_day TINYINT(1) NOT NULL DEFAULT 1, max_consecutive SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit (calendar days of one application)',
  max_per_month DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '0 = no limit', min_notice_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'apply at least this many days before the first day',
  backdate_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'may apply for a day up to this many days back', future_days SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit on how far ahead',
  requires_reason TINYINT(1) NOT NULL DEFAULT 1, sandwich TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'weekly offs / holidays between leave days are counted as leave',
  allow_negative TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'balance may run out: the shortfall becomes loss-of-pay days (always on for LWP)',
  auto_approve TINYINT(1) NOT NULL DEFAULT 0, min_service_months SMALLINT NOT NULL DEFAULT 0 COMMENT 'eligible after this many months of service',
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_leave_type (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='leave master = the policy of each leave type';
INSERT IGNORE INTO hr_leave_type (company_id, code, name, is_paid, annual_quota, accrual, carry_forward_max, allow_half_day, max_consecutive, max_per_month, min_notice_days, backdate_days, future_days, requires_reason, sandwich, allow_negative, auto_approve, min_service_months, sort_order) VALUES
 (1,'CL','Casual Leave',1,12,'MONTHLY',0,1,3,3,0,7,0,1,0,0,0,0,1),
 (1,'SL','Sick Leave',1,12,'UPFRONT',0,1,0,0,0,7,0,1,0,0,0,0,2),
 (1,'PL','Privilege Leave',1,18,'MONTHLY',30,0,0,0,7,0,0,1,0,0,0,6,3),
 (1,'LWP','Leave Without Pay',0,0,'UPFRONT',0,1,0,0,0,7,0,1,0,1,0,0,4);

CREATE TABLE IF NOT EXISTS hr_leave_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL, from_date DATE NOT NULL, to_date DATE NOT NULL,
  day_part VARCHAR(8) NOT NULL DEFAULT 'FULL' COMMENT 'FULL / FIRST / SECOND (half day)', days DECIMAL(6,2) NOT NULL, lwp_days DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'part of days that is unpaid because the balance ran out',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / CANCELLED',
  source VARCHAR(6) NOT NULL DEFAULT 'WEB' COMMENT 'DCR / WEB', dcr_id BIGINT UNSIGNED NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_hr_la_emp (emp_id, from_date), KEY ix_hr_la_status (status, from_date),
  CONSTRAINT fk_hr_la_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_leave_adjustment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL,
  leave_year SMALLINT NOT NULL COMMENT 'year in which the leave year starts', days DECIMAL(6,2) NOT NULL COMMENT '+ credit / - debit', reason VARCHAR(200) NOT NULL,
  by_emp_id VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_ladj (emp_id, leave_type_id, leave_year),
  CONSTRAINT fk_hr_ladj_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='manual balance corrections (opening balance, encashment, grants)';

SET @lv_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @lv_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @lv_parent LIMIT 1) p
JOIN (SELECT 1 n,'Leave Master' label,'Leave types and policy' title,'calendar2-x' icon,'hrleavemaster' page,'hr/leave-master.html' href,44 so
      UNION ALL SELECT 2,'My Leave','Balance, apply, approvals','calendar-check','myleave','hr/leave-my.html',45) m
WHERE @lv_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);

USE sfa_demo;

CREATE TABLE IF NOT EXISTS hr_leave_type (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  code VARCHAR(12) NOT NULL, name VARCHAR(80) NOT NULL, is_paid TINYINT(1) NOT NULL DEFAULT 1,
  annual_quota DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'days per leave year',
  accrual VARCHAR(8) NOT NULL DEFAULT 'UPFRONT' COMMENT 'UPFRONT = whole quota at year start, MONTHLY = quota/12 each month',
  carry_forward_max DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'unused days carried to next year, at most this many',
  allow_half_day TINYINT(1) NOT NULL DEFAULT 1, max_consecutive SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit (calendar days of one application)',
  max_per_month DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '0 = no limit', min_notice_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'apply at least this many days before the first day',
  backdate_days SMALLINT NOT NULL DEFAULT 0 COMMENT 'may apply for a day up to this many days back', future_days SMALLINT NOT NULL DEFAULT 0 COMMENT '0 = no limit on how far ahead',
  requires_reason TINYINT(1) NOT NULL DEFAULT 1, sandwich TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'weekly offs / holidays between leave days are counted as leave',
  allow_negative TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'balance may run out: the shortfall becomes loss-of-pay days (always on for LWP)',
  auto_approve TINYINT(1) NOT NULL DEFAULT 0, min_service_months SMALLINT NOT NULL DEFAULT 0 COMMENT 'eligible after this many months of service',
  sort_order SMALLINT NOT NULL DEFAULT 0, is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_hr_leave_type (company_id, code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='leave master = the policy of each leave type';
INSERT IGNORE INTO hr_leave_type (company_id, code, name, is_paid, annual_quota, accrual, carry_forward_max, allow_half_day, max_consecutive, max_per_month, min_notice_days, backdate_days, future_days, requires_reason, sandwich, allow_negative, auto_approve, min_service_months, sort_order) VALUES
 (1,'CL','Casual Leave',1,12,'MONTHLY',0,1,3,3,0,7,0,1,0,0,0,0,1),
 (1,'SL','Sick Leave',1,12,'UPFRONT',0,1,0,0,0,7,0,1,0,0,0,0,2),
 (1,'PL','Privilege Leave',1,18,'MONTHLY',30,0,0,0,7,0,0,1,0,0,0,6,3),
 (1,'LWP','Leave Without Pay',0,0,'UPFRONT',0,1,0,0,0,7,0,1,0,1,0,0,4);

CREATE TABLE IF NOT EXISTS hr_leave_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, company_id INT UNSIGNED NOT NULL DEFAULT 1,
  emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL, from_date DATE NOT NULL, to_date DATE NOT NULL,
  day_part VARCHAR(8) NOT NULL DEFAULT 'FULL' COMMENT 'FULL / FIRST / SECOND (half day)', days DECIMAL(6,2) NOT NULL, lwp_days DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'part of days that is unpaid because the balance ran out',
  reason VARCHAR(300) NULL, status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / CANCELLED',
  source VARCHAR(6) NOT NULL DEFAULT 'WEB' COMMENT 'DCR / WEB', dcr_id BIGINT UNSIGNED NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, decided_by VARCHAR(40) NULL, decided_at DATETIME NULL, decision_remarks VARCHAR(200) NULL,
  PRIMARY KEY (id), KEY ix_hr_la_emp (emp_id, from_date), KEY ix_hr_la_status (status, from_date),
  CONSTRAINT fk_hr_la_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hr_leave_adjustment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, emp_id VARCHAR(40) NOT NULL, leave_type_id INT UNSIGNED NOT NULL,
  leave_year SMALLINT NOT NULL COMMENT 'year in which the leave year starts', days DECIMAL(6,2) NOT NULL COMMENT '+ credit / - debit', reason VARCHAR(200) NOT NULL,
  by_emp_id VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_hr_ladj (emp_id, leave_type_id, leave_year),
  CONSTRAINT fk_hr_ladj_type FOREIGN KEY (leave_type_id) REFERENCES hr_leave_type (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='manual balance corrections (opening balance, encashment, grants)';

SET @lv_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + m.n, @lv_parent, p.emp_id, 'WEB', m.label, m.title, m.icon, m.page, m.href, '_SELF', m.so, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @lv_parent LIMIT 1) p
JOIN (SELECT 1 n,'Leave Master' label,'Leave types and policy' title,'calendar2-x' icon,'hrleavemaster' page,'hr/leave-master.html' href,44 so
      UNION ALL SELECT 2,'My Leave','Balance, apply, approvals','calendar-check','myleave','hr/leave-my.html',45) m
WHERE @lv_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = m.page);
