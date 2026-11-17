-- 114: menu for the Calendar Report (month calendar: state / HQ / designation / employee wise). Safe to re-run.

USE acme_db;

SET @cp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'attendancereport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @cp, p.emp_id, 'WEB', 'Calendar Report', 'Month calendar: state / HQ / designation / employee wise', 'calendar3', 'calendarreport', 'report/calendar-report.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @cp LIMIT 1) p WHERE @cp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'calendarreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'calendarreport';

USE globex_db;

SET @cp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'attendancereport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @cp, p.emp_id, 'WEB', 'Calendar Report', 'Month calendar: state / HQ / designation / employee wise', 'calendar3', 'calendarreport', 'report/calendar-report.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @cp LIMIT 1) p WHERE @cp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'calendarreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'calendarreport';

USE initech_db;

SET @cp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'attendancereport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @cp, p.emp_id, 'WEB', 'Calendar Report', 'Month calendar: state / HQ / designation / employee wise', 'calendar3', 'calendarreport', 'report/calendar-report.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @cp LIMIT 1) p WHERE @cp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'calendarreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'calendarreport';

USE sfa_demo;

SET @cp := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'attendancereport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @cp, p.emp_id, 'WEB', 'Calendar Report', 'Month calendar: state / HQ / designation / employee wise', 'calendar3', 'calendarreport', 'report/calendar-report.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @cp LIMIT 1) p WHERE @cp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'calendarreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'calendarreport';
