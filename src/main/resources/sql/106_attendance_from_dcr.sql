-- 106: payroll attendance comes from the DCR (settings ATTENDANCE_FROM_DCR and PAYROLL_REQUIRES_DCR, both on). Safe to re-run.

USE acme_db;

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('ATTENDANCE_FROM_DCR','Attendance','Attendance from the DCR','A working day with no submitted DCR (and no leave) is absent and its pay is cut. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,60),
 ('PAYROLL_REQUIRES_DCR','Attendance','Salary needs at least one DCR in the month','No salary slip for an employee who submitted no DCR (leave, working, attendance ... any) in the month. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,61);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, d.setting_key, 'true', '2000-04-01', 'USER', 'Default: attendance from the DCR' FROM hr_setting_def d WHERE d.setting_key IN ('ATTENDANCE_FROM_DCR','PAYROLL_REQUIRES_DCR')
 AND NOT EXISTS (SELECT 1 FROM hr_setting s WHERE s.setting_key = d.setting_key);

USE globex_db;

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('ATTENDANCE_FROM_DCR','Attendance','Attendance from the DCR','A working day with no submitted DCR (and no leave) is absent and its pay is cut. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,60),
 ('PAYROLL_REQUIRES_DCR','Attendance','Salary needs at least one DCR in the month','No salary slip for an employee who submitted no DCR (leave, working, attendance ... any) in the month. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,61);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, d.setting_key, 'true', '2000-04-01', 'USER', 'Default: attendance from the DCR' FROM hr_setting_def d WHERE d.setting_key IN ('ATTENDANCE_FROM_DCR','PAYROLL_REQUIRES_DCR')
 AND NOT EXISTS (SELECT 1 FROM hr_setting s WHERE s.setting_key = d.setting_key);

USE initech_db;

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('ATTENDANCE_FROM_DCR','Attendance','Attendance from the DCR','A working day with no submitted DCR (and no leave) is absent and its pay is cut. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,60),
 ('PAYROLL_REQUIRES_DCR','Attendance','Salary needs at least one DCR in the month','No salary slip for an employee who submitted no DCR (leave, working, attendance ... any) in the month. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,61);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, d.setting_key, 'true', '2000-04-01', 'USER', 'Default: attendance from the DCR' FROM hr_setting_def d WHERE d.setting_key IN ('ATTENDANCE_FROM_DCR','PAYROLL_REQUIRES_DCR')
 AND NOT EXISTS (SELECT 1 FROM hr_setting s WHERE s.setting_key = d.setting_key);

USE sfa_demo;

INSERT IGNORE INTO hr_setting_def (setting_key, grp, label, help_text, value_type, unit, min_value, max_value, enum_values, sort_order) VALUES
 ('ATTENDANCE_FROM_DCR','Attendance','Attendance from the DCR','A working day with no submitted DCR (and no leave) is absent and its pay is cut. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,60),
 ('PAYROLL_REQUIRES_DCR','Attendance','Salary needs at least one DCR in the month','No salary slip for an employee who submitted no DCR (leave, working, attendance ... any) in the month. Office staff are not checked','BOOL',NULL,NULL,NULL,NULL,61);
INSERT INTO hr_setting (company_id, setting_key, value_text, effective_from, source, reason)
SELECT 1, d.setting_key, 'true', '2000-04-01', 'USER', 'Default: attendance from the DCR' FROM hr_setting_def d WHERE d.setting_key IN ('ATTENDANCE_FROM_DCR','PAYROLL_REQUIRES_DCR')
 AND NOT EXISTS (SELECT 1 FROM hr_setting s WHERE s.setting_key = d.setting_key);
