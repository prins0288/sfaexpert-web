-- ===========================================================================
-- 3 more leaf items under Mapping (id=502, see 25_master_submenu_tree.sql):
-- Client Product Mapping, Area Route Mapping, Area Route Mapping View.
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/26_mapping_more_items.sql
-- ===========================================================================

USE acme_db;

INSERT INTO menu_item (id, parent_id, label, title, description, icon, page, href, sort_order, roles, enabled) VALUES
    (542, 502, 'Client Product Mapping', NULL, NULL, 'link-45deg', NULL, NULL, 13, NULL, 1),
    (543, 502, 'Area Route Mapping', NULL, NULL, 'link-45deg', NULL, NULL, 14, NULL, 1),
    (544, 502, 'Area Route Mapping View', NULL, NULL, 'eye', NULL, NULL, 15, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), description=VALUES(description),
    icon=VALUES(icon), page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order),
    roles=VALUES(roles), enabled=VALUES(enabled);
