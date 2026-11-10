-- ===========================================================================
-- 85_dcr_menu.sql — PER TENANT database
--
-- Menus for the new DCR (also added by 84 when the parent menu exists — this one
-- adds the fallback to Reports and is safe to run any number of times):
--   "DCR"      (dcr/dcr-submit.html)      next to DCR Report, else under Sales, else Reports
--   "DCR Tabs" (master/dcr-worktype.html) Master Entry, next to Password Reset
-- Run:  mysql -u myroot < src/main/resources/sql/85_dcr_menu.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
-- DCR (fill / submit / approve): next to the DCR Report; else under Sales; else under Reports
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
-- DCR Tabs (who gets which tab): Master Entry, next to Password Reset / Team Hierarchy
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'designationmaster' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrsubmit') AS dcr_menu, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrworktype') AS dcr_tabs_menu;

-- ==================== globex_db ====================
USE globex_db;
-- DCR (fill / submit / approve): next to the DCR Report; else under Sales; else under Reports
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
-- DCR Tabs (who gets which tab): Master Entry, next to Password Reset / Team Hierarchy
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'designationmaster' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrsubmit') AS dcr_menu, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrworktype') AS dcr_tabs_menu;

-- ==================== initech_db ====================
USE initech_db;
-- DCR (fill / submit / approve): next to the DCR Report; else under Sales; else under Reports
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
-- DCR Tabs (who gets which tab): Master Entry, next to Password Reset / Team Hierarchy
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'designationmaster' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrsubmit') AS dcr_menu, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrworktype') AS dcr_tabs_menu;

-- ==================== sfa_demo ====================
USE sfa_demo;
-- DCR (fill / submit / approve): next to the DCR Report; else under Sales; else under Reports
SET @dcr_parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'dcrreport' LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Sales' AND parent_id IS NULL LIMIT 1),
                            (SELECT id FROM company_menu_self WHERE label = 'Reports' AND parent_id IS NULL LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_parent, p.emp_id, 'WEB', 'DCR', 'Daily Call Report', 'journal-check', 'dcrsubmit', 'dcr/dcr-submit.html', '_SELF', 5, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_parent LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_parent IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrsubmit');
-- DCR Tabs (who gets which tab): Master Entry, next to Password Reset / Team Hierarchy
SET @dcr_entry := COALESCE((SELECT parent_id FROM company_menu_self WHERE page IN ('passwordreset', 'teamhierarchy') LIMIT 1),
                           (SELECT parent_id FROM company_menu_self WHERE page = 'designationmaster' LIMIT 1));
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @dcr_entry, p.emp_id, 'WEB', 'DCR Tabs', 'DCR Tabs', 'ui-checks-grid', 'dcrworktype', 'master/dcr-worktype.html', '_SELF', 23, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @dcr_entry LIMIT 1) p
CROSS JOIN (SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM company_menu_self) nx
WHERE @dcr_entry IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'dcrworktype');
SELECT DATABASE() AS db, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrsubmit') AS dcr_menu, (SELECT COUNT(*) FROM company_menu_self WHERE page = 'dcrworktype') AS dcr_tabs_menu;
