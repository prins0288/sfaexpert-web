-- 131: the whole Expense menu (Expense Statement, Expense Setup, Daily Expense, Expense Master, Fixed Expense, Area Type Master) - added only where missing, made visible. Safe to re-run.

USE acme_db;
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @eroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement','expensesetup','dailyexpense','expensemaster','fixedexpense','areatype');

USE globex_db;
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @eroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement','expensesetup','dailyexpense','expensemaster','fixedexpense','areatype');

USE initech_db;
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @eroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement','expensesetup','dailyexpense','expensemaster','fixedexpense','areatype');

USE sfa_demo;
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Expense', 'Expense statements and approval', 'wallet2', NULL, NULL, '_SELF', 8, 1, 1
FROM DUAL WHERE @eroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @eroot := (SELECT id FROM company_menu_self WHERE label = 'Expense' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE id = @eroot;
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Statement', 'Monthly expense from the DCR: submit, manager and admin approval', 'receipt', 'expensestatement', 'expense/expense-statement.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensestatement');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Setup', 'Route setting, daily allowance and fare rates', 'sliders', 'expensesetup', 'expense/expense-setup.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensesetup');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Daily Expense', 'Feed a variable expense of the day with its bill', 'journal-plus', 'dailyexpense', 'expense/daily-expense.html', '_SELF', 3, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'dailyexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Expense Master', 'Fixed and variable expense types', 'tags', 'expensemaster', 'expense/expense-master.html', '_SELF', 4, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'expensemaster');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Fixed Expense', 'Give a fixed monthly expense to the employees', 'person-check', 'fixedexpense', 'expense/fixed-expense.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'fixedexpense');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @eroot, p.emp_id, 'WEB', 'Area Type Master', 'HQ / EX / OUT EX / OUT with their km range', 'geo', 'areatype', 'master/area-type.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @eroot LIMIT 1) p WHERE @eroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'areatype');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('expensestatement','expensesetup','dailyexpense','expensemaster','fixedexpense','areatype');
