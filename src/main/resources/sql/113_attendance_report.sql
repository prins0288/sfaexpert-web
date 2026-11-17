-- 113: Attendance Report - rules (attendance_rule: minimum calls / hours / start / end conditions per designation), short code of a work type, menus. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS attendance_rule (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_name VARCHAR(80) NOT NULL DEFAULT 'All designations',
  designation_id INT NULL COMMENT 'NULL = every designation; a rule of the designation wins over the general one',
  apply_types VARCHAR(200) NOT NULL DEFAULT 'WORKING' COMMENT 'DCR work types (app_dcr_worktype codes, comma list) the call / time conditions are checked on; other types count as present once submitted',
  min_calls SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'fewer calls than this -> calls_result (0 = not checked)',
  calls_result VARCHAR(6) NOT NULL DEFAULT 'ABSENT' COMMENT 'ABSENT / HALF',
  count_unlisted TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'unlisted check-ins count as calls',
  min_hours DECIMAL(4,2) NOT NULL DEFAULT 0 COMMENT 'working hours (day start to day end) less than this -> hours_result (0 = not checked)',
  hours_result VARCHAR(6) NOT NULL DEFAULT 'HALF' COMMENT 'ABSENT / HALF',
  late_start_after TIME NULL COMMENT 'day started after this time -> late_result',
  late_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  early_end_before TIME NULL COMMENT 'day ended before this time -> early_result',
  early_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  need_approved TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = only an APPROVED DCR counts; 0 = a submitted DCR counts (a draft or rejected DCR never counts)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_attrule_des (designation_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='conditions that decide whether a submitted DCR day counts as present / half day / absent in the Attendance Report';
INSERT INTO attendance_rule (rule_name) SELECT 'All designations' WHERE NOT EXISTS (SELECT 1 FROM attendance_rule);

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'short_code') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN short_code VARCHAR(8) NULL COMMENT "short code shown in the Attendance Report cells and legend"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE app_dcr_worktype SET short_code = CASE worktype_code WHEN 'WORKING' THEN 'WO' WHEN 'ATTENDANCE' THEN 'AT' WHEN 'LEAVE' THEN 'L' WHEN 'PNR_DAY' THEN 'PNR' WHEN 'R_TRAINING' THEN 'TRN' WHEN 'TRANSIT' THEN 'TRL' WHEN 'CAMP' THEN 'CA' WHEN 'STUDY' THEN 'ST' WHEN 'CDE' THEN 'CDE' WHEN 'RTM' THEN 'RTM' ELSE short_code END WHERE short_code IS NULL;

SET @rp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @rp, p.emp_id, 'WEB', 'Attendance Report', 'Employee attendance sheet of a month / period', 'calendar-check', 'attendancereport', 'report/attendance-report.html', '_SELF', 9, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @rp LIMIT 1) p WHERE @rp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancereport');
SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Attendance Rules', 'Calls / hours conditions for the attendance report', 'rulers', 'attendancerules', 'master/attendance-rules.html', '_SELF', 53, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancerules');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('attendancereport', 'attendancerules');

USE globex_db;

CREATE TABLE IF NOT EXISTS attendance_rule (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_name VARCHAR(80) NOT NULL DEFAULT 'All designations',
  designation_id INT NULL COMMENT 'NULL = every designation; a rule of the designation wins over the general one',
  apply_types VARCHAR(200) NOT NULL DEFAULT 'WORKING' COMMENT 'DCR work types (app_dcr_worktype codes, comma list) the call / time conditions are checked on; other types count as present once submitted',
  min_calls SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'fewer calls than this -> calls_result (0 = not checked)',
  calls_result VARCHAR(6) NOT NULL DEFAULT 'ABSENT' COMMENT 'ABSENT / HALF',
  count_unlisted TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'unlisted check-ins count as calls',
  min_hours DECIMAL(4,2) NOT NULL DEFAULT 0 COMMENT 'working hours (day start to day end) less than this -> hours_result (0 = not checked)',
  hours_result VARCHAR(6) NOT NULL DEFAULT 'HALF' COMMENT 'ABSENT / HALF',
  late_start_after TIME NULL COMMENT 'day started after this time -> late_result',
  late_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  early_end_before TIME NULL COMMENT 'day ended before this time -> early_result',
  early_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  need_approved TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = only an APPROVED DCR counts; 0 = a submitted DCR counts (a draft or rejected DCR never counts)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_attrule_des (designation_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='conditions that decide whether a submitted DCR day counts as present / half day / absent in the Attendance Report';
INSERT INTO attendance_rule (rule_name) SELECT 'All designations' WHERE NOT EXISTS (SELECT 1 FROM attendance_rule);

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'short_code') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN short_code VARCHAR(8) NULL COMMENT "short code shown in the Attendance Report cells and legend"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE app_dcr_worktype SET short_code = CASE worktype_code WHEN 'WORKING' THEN 'WO' WHEN 'ATTENDANCE' THEN 'AT' WHEN 'LEAVE' THEN 'L' WHEN 'PNR_DAY' THEN 'PNR' WHEN 'R_TRAINING' THEN 'TRN' WHEN 'TRANSIT' THEN 'TRL' WHEN 'CAMP' THEN 'CA' WHEN 'STUDY' THEN 'ST' WHEN 'CDE' THEN 'CDE' WHEN 'RTM' THEN 'RTM' ELSE short_code END WHERE short_code IS NULL;

SET @rp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @rp, p.emp_id, 'WEB', 'Attendance Report', 'Employee attendance sheet of a month / period', 'calendar-check', 'attendancereport', 'report/attendance-report.html', '_SELF', 9, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @rp LIMIT 1) p WHERE @rp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancereport');
SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Attendance Rules', 'Calls / hours conditions for the attendance report', 'rulers', 'attendancerules', 'master/attendance-rules.html', '_SELF', 53, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancerules');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('attendancereport', 'attendancerules');

USE initech_db;

CREATE TABLE IF NOT EXISTS attendance_rule (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_name VARCHAR(80) NOT NULL DEFAULT 'All designations',
  designation_id INT NULL COMMENT 'NULL = every designation; a rule of the designation wins over the general one',
  apply_types VARCHAR(200) NOT NULL DEFAULT 'WORKING' COMMENT 'DCR work types (app_dcr_worktype codes, comma list) the call / time conditions are checked on; other types count as present once submitted',
  min_calls SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'fewer calls than this -> calls_result (0 = not checked)',
  calls_result VARCHAR(6) NOT NULL DEFAULT 'ABSENT' COMMENT 'ABSENT / HALF',
  count_unlisted TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'unlisted check-ins count as calls',
  min_hours DECIMAL(4,2) NOT NULL DEFAULT 0 COMMENT 'working hours (day start to day end) less than this -> hours_result (0 = not checked)',
  hours_result VARCHAR(6) NOT NULL DEFAULT 'HALF' COMMENT 'ABSENT / HALF',
  late_start_after TIME NULL COMMENT 'day started after this time -> late_result',
  late_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  early_end_before TIME NULL COMMENT 'day ended before this time -> early_result',
  early_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  need_approved TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = only an APPROVED DCR counts; 0 = a submitted DCR counts (a draft or rejected DCR never counts)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_attrule_des (designation_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='conditions that decide whether a submitted DCR day counts as present / half day / absent in the Attendance Report';
INSERT INTO attendance_rule (rule_name) SELECT 'All designations' WHERE NOT EXISTS (SELECT 1 FROM attendance_rule);

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'short_code') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN short_code VARCHAR(8) NULL COMMENT "short code shown in the Attendance Report cells and legend"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE app_dcr_worktype SET short_code = CASE worktype_code WHEN 'WORKING' THEN 'WO' WHEN 'ATTENDANCE' THEN 'AT' WHEN 'LEAVE' THEN 'L' WHEN 'PNR_DAY' THEN 'PNR' WHEN 'R_TRAINING' THEN 'TRN' WHEN 'TRANSIT' THEN 'TRL' WHEN 'CAMP' THEN 'CA' WHEN 'STUDY' THEN 'ST' WHEN 'CDE' THEN 'CDE' WHEN 'RTM' THEN 'RTM' ELSE short_code END WHERE short_code IS NULL;

SET @rp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @rp, p.emp_id, 'WEB', 'Attendance Report', 'Employee attendance sheet of a month / period', 'calendar-check', 'attendancereport', 'report/attendance-report.html', '_SELF', 9, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @rp LIMIT 1) p WHERE @rp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancereport');
SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Attendance Rules', 'Calls / hours conditions for the attendance report', 'rulers', 'attendancerules', 'master/attendance-rules.html', '_SELF', 53, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancerules');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('attendancereport', 'attendancerules');

USE sfa_demo;

CREATE TABLE IF NOT EXISTS attendance_rule (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_name VARCHAR(80) NOT NULL DEFAULT 'All designations',
  designation_id INT NULL COMMENT 'NULL = every designation; a rule of the designation wins over the general one',
  apply_types VARCHAR(200) NOT NULL DEFAULT 'WORKING' COMMENT 'DCR work types (app_dcr_worktype codes, comma list) the call / time conditions are checked on; other types count as present once submitted',
  min_calls SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'fewer calls than this -> calls_result (0 = not checked)',
  calls_result VARCHAR(6) NOT NULL DEFAULT 'ABSENT' COMMENT 'ABSENT / HALF',
  count_unlisted TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'unlisted check-ins count as calls',
  min_hours DECIMAL(4,2) NOT NULL DEFAULT 0 COMMENT 'working hours (day start to day end) less than this -> hours_result (0 = not checked)',
  hours_result VARCHAR(6) NOT NULL DEFAULT 'HALF' COMMENT 'ABSENT / HALF',
  late_start_after TIME NULL COMMENT 'day started after this time -> late_result',
  late_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  early_end_before TIME NULL COMMENT 'day ended before this time -> early_result',
  early_result VARCHAR(6) NOT NULL DEFAULT 'HALF',
  need_approved TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = only an APPROVED DCR counts; 0 = a submitted DCR counts (a draft or rejected DCR never counts)',
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), KEY ix_attrule_des (designation_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='conditions that decide whether a submitted DCR day counts as present / half day / absent in the Attendance Report';
INSERT INTO attendance_rule (rule_name) SELECT 'All designations' WHERE NOT EXISTS (SELECT 1 FROM attendance_rule);

SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_dcr_worktype' AND column_name = 'short_code') = 0,
  'ALTER TABLE app_dcr_worktype ADD COLUMN short_code VARCHAR(8) NULL COMMENT "short code shown in the Attendance Report cells and legend"', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
UPDATE app_dcr_worktype SET short_code = CASE worktype_code WHEN 'WORKING' THEN 'WO' WHEN 'ATTENDANCE' THEN 'AT' WHEN 'LEAVE' THEN 'L' WHEN 'PNR_DAY' THEN 'PNR' WHEN 'R_TRAINING' THEN 'TRN' WHEN 'TRANSIT' THEN 'TRL' WHEN 'CAMP' THEN 'CA' WHEN 'STUDY' THEN 'ST' WHEN 'CDE' THEN 'CDE' WHEN 'RTM' THEN 'RTM' ELSE short_code END WHERE short_code IS NULL;

SET @rp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @rp, p.emp_id, 'WEB', 'Attendance Report', 'Employee attendance sheet of a month / period', 'calendar-check', 'attendancereport', 'report/attendance-report.html', '_SELF', 9, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @rp LIMIT 1) p WHERE @rp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancereport');
SET @mp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mp, p.emp_id, 'WEB', 'Attendance Rules', 'Calls / hours conditions for the attendance report', 'rulers', 'attendancerules', 'master/attendance-rules.html', '_SELF', 53, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mp LIMIT 1) p WHERE @mp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'attendancerules');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('attendancereport', 'attendancerules');
