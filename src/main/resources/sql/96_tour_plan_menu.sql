-- 96: menus Tour Plan, Tour Plan Approval and DCR Approval (next to the DCR). Idempotent. (Holiday Master menu is in 95.)

USE acme_db;
-- parent: next to the DCR; else the Sales menu; else Reports; else the first top-level menu
SET @tp_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE parent_id IS NULL ORDER BY sort_order, id LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan Approval', 'Tour Plan Approval', 'patch-check', 'tourplanapproval', 'tp/tour-plan.html?tab=team', '_SELF', 7, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplanapproval');
-- make sure an earlier row is visible / active
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('tourplan', 'tourplanapproval', 'holidaymaster');
-- DCR Approval: the DCR page opened on "Waiting for my approval"
SET @ap_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'tourplan' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @ap_parent, p.emp_id, 'WEB', 'DCR Approval', 'DCR Approval', 'patch-check', 'dcrapproval', 'dcr/dcr-submit.html?view=approval', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @ap_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @ap_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrapproval');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplan') AS tour_plan, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplanapproval') AS tp_approval, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'holidaymaster') AS holiday_master;

USE globex_db;
-- parent: next to the DCR; else the Sales menu; else Reports; else the first top-level menu
SET @tp_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE parent_id IS NULL ORDER BY sort_order, id LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan Approval', 'Tour Plan Approval', 'patch-check', 'tourplanapproval', 'tp/tour-plan.html?tab=team', '_SELF', 7, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplanapproval');
-- make sure an earlier row is visible / active
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('tourplan', 'tourplanapproval', 'holidaymaster');
-- DCR Approval: the DCR page opened on "Waiting for my approval"
SET @ap_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'tourplan' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @ap_parent, p.emp_id, 'WEB', 'DCR Approval', 'DCR Approval', 'patch-check', 'dcrapproval', 'dcr/dcr-submit.html?view=approval', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @ap_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @ap_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrapproval');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplan') AS tour_plan, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplanapproval') AS tp_approval, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'holidaymaster') AS holiday_master;

USE initech_db;
-- parent: next to the DCR; else the Sales menu; else Reports; else the first top-level menu
SET @tp_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE parent_id IS NULL ORDER BY sort_order, id LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan Approval', 'Tour Plan Approval', 'patch-check', 'tourplanapproval', 'tp/tour-plan.html?tab=team', '_SELF', 7, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplanapproval');
-- make sure an earlier row is visible / active
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('tourplan', 'tourplanapproval', 'holidaymaster');
-- DCR Approval: the DCR page opened on "Waiting for my approval"
SET @ap_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'tourplan' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @ap_parent, p.emp_id, 'WEB', 'DCR Approval', 'DCR Approval', 'patch-check', 'dcrapproval', 'dcr/dcr-submit.html?view=approval', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @ap_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @ap_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrapproval');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplan') AS tour_plan, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplanapproval') AS tp_approval, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'holidaymaster') AS holiday_master;

USE sfa_demo;
-- parent: next to the DCR; else the Sales menu; else Reports; else the first top-level menu
SET @tp_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1),
                           (SELECT id FROM company_menu_self WHERE parent_id IS NULL ORDER BY sort_order, id LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan', 'Tour Plan', 'calendar3', 'tourplan', 'tp/tour-plan.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplan');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @tp_parent, p.emp_id, 'WEB', 'Tour Plan Approval', 'Tour Plan Approval', 'patch-check', 'tourplanapproval', 'tp/tour-plan.html?tab=team', '_SELF', 7, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @tp_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @tp_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'tourplanapproval');
-- make sure an earlier row is visible / active
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('tourplan', 'tourplanapproval', 'holidaymaster');
-- DCR Approval: the DCR page opened on "Waiting for my approval"
SET @ap_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1), (SELECT parent_id FROM company_menu_self WHERE page = 'tourplan' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @ap_parent, p.emp_id, 'WEB', 'DCR Approval', 'DCR Approval', 'patch-check', 'dcrapproval', 'dcr/dcr-submit.html?view=approval', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @ap_parent LIMIT 1) p CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @ap_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrapproval');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplan') AS tour_plan, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'tourplanapproval') AS tp_approval, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'holidaymaster') AS holiday_master;
