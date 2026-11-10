-- 108: menu for Mark Attendance / Leave (admin marks attendance or leave for several employees). Safe to re-run.

USE acme_db;

SET @mk_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mk_parent, p.emp_id, 'WEB', 'Mark Attendance / Leave', 'Mark attendance or leave for several employees', 'calendar2-check', 'markattendance', 'hr/mark-attendance.html', '_SELF', 51, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mk_parent LIMIT 1) p
WHERE @mk_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'markattendance');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'markattendance';

USE globex_db;

SET @mk_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mk_parent, p.emp_id, 'WEB', 'Mark Attendance / Leave', 'Mark attendance or leave for several employees', 'calendar2-check', 'markattendance', 'hr/mark-attendance.html', '_SELF', 51, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mk_parent LIMIT 1) p
WHERE @mk_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'markattendance');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'markattendance';

USE initech_db;

SET @mk_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mk_parent, p.emp_id, 'WEB', 'Mark Attendance / Leave', 'Mark attendance or leave for several employees', 'calendar2-check', 'markattendance', 'hr/mark-attendance.html', '_SELF', 51, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mk_parent LIMIT 1) p
WHERE @mk_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'markattendance');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'markattendance';

USE sfa_demo;

SET @mk_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'hrsetup' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'marqueemessage' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @mk_parent, p.emp_id, 'WEB', 'Mark Attendance / Leave', 'Mark attendance or leave for several employees', 'calendar2-check', 'markattendance', 'hr/mark-attendance.html', '_SELF', 51, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @mk_parent LIMIT 1) p
WHERE @mk_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'markattendance');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'markattendance';
