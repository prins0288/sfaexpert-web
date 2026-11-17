-- 89: menu entry for the DCR Rules page. Safe to re-run.

USE acme_db;
-- DCR Rules (validations: who has which rule): Master Entry, next to DCR Tabs
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Rules', 'DCR Rules (validations)', 'shield-check', 'dcrrules', 'master/dcr-rules.html', '_SELF', 24, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrrules');

USE globex_db;
-- DCR Rules (validations: who has which rule): Master Entry, next to DCR Tabs
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Rules', 'DCR Rules (validations)', 'shield-check', 'dcrrules', 'master/dcr-rules.html', '_SELF', 24, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrrules');

USE initech_db;
-- DCR Rules (validations: who has which rule): Master Entry, next to DCR Tabs
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Rules', 'DCR Rules (validations)', 'shield-check', 'dcrrules', 'master/dcr-rules.html', '_SELF', 24, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrrules');

USE sfa_demo;
-- DCR Rules (validations: who has which rule): Master Entry, next to DCR Tabs
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrworktype' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Rules', 'DCR Rules (validations)', 'shield-check', 'dcrrules', 'master/dcr-rules.html', '_SELF', 24, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrrules');

