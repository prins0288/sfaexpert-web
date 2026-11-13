-- ===========================================================================
-- 75_sample_multi_create_menu.sql — PER TENANT database
--
-- Menu "Create Multiple Sample" under Masters > Catalog (next to Item):
-- tick Product items, set pack size + PTS and states, and one Sample item is
-- made per product (item_code_main = the product's item_code). Uses ITEM_SAVE.
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/75_sample_multi_create_menu.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'itemmaster' LIMIT 1), 12);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Create Multiple Sample', 'Create Multiple Sample From Item', 'collection',
       'samplemulticreate', 'master/sample-multi-create.html', '_SELF', 57, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'samplemulticreate');

-- ==================== globex_db ====================
USE globex_db;
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'itemmaster' LIMIT 1), 12);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Create Multiple Sample', 'Create Multiple Sample From Item', 'collection',
       'samplemulticreate', 'master/sample-multi-create.html', '_SELF', 57, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'samplemulticreate');

-- ==================== initech_db ====================
USE initech_db;
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'itemmaster' LIMIT 1), 12);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Create Multiple Sample', 'Create Multiple Sample From Item', 'collection',
       'samplemulticreate', 'master/sample-multi-create.html', '_SELF', 57, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'samplemulticreate');

-- ==================== sfa_demo ====================
USE sfa_demo;
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'itemmaster' LIMIT 1), 12);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Create Multiple Sample', 'Create Multiple Sample From Item', 'collection',
       'samplemulticreate', 'master/sample-multi-create.html', '_SELF', 57, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'samplemulticreate');
