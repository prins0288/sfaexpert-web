-- 125: menu for the Company Stock report (Inventory > Company Stock). Safe to re-run.

USE acme_db;

SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Company Stock', 'Stock of the company: item x state / HQ / designation / employee / party, qty and value', 'table', 'stockreport', 'inventory/stock-report.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'stockreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'stockreport';

USE globex_db;

SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Company Stock', 'Stock of the company: item x state / HQ / designation / employee / party, qty and value', 'table', 'stockreport', 'inventory/stock-report.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'stockreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'stockreport';

USE initech_db;

SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Company Stock', 'Stock of the company: item x state / HQ / designation / employee / party, qty and value', 'table', 'stockreport', 'inventory/stock-report.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'stockreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'stockreport';

USE sfa_demo;

SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Company Stock', 'Stock of the company: item x state / HQ / designation / employee / party, qty and value', 'table', 'stockreport', 'inventory/stock-report.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'stockreport');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page = 'stockreport';
