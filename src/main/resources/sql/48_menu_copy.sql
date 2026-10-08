-- ============================================================================
-- 48_menu_copy.sql   (TENANT db: run in acme_db, globex_db, initech_db, sfa_demo)
--
-- Menu Copy (multi-tenant) — Masters > Master Entry > Menu Copy — pick one or
-- more menu items (with their full sub-tree, any depth) in one tenant and
-- copy them into one or more other tenants.
--
-- Idempotent: only runs if a row with page='menucopy' doesn't already exist.
-- ============================================================================

INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 517, 500, me.emp_id, 'WEB', 'Menu Copy', NULL, 'diagram-2',
       'menucopy', 'utilities/menu-copy.html', '_SELF', 7, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 500 LIMIT 1) me
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'menucopy');
