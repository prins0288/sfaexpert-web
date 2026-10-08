-- ===========================================================================
-- New top-level (L1) menu parents, added as empty branches (no href/children
-- yet — pages will be built later): Inventory, Sales, Requests, Utilities,
-- Updations, Location, Mail, AI.
--
-- Requested sequence: Master, Inventory, Sales, Requests, Utilities,
-- Updations, Location, Mail, AI. "Master" already exists (id=2, "Masters"),
-- so it's left untouched and simply keeps its place right after Dashboard.
-- Reports/Administration weren't part of the requested sequence, so they're
-- pushed to the end (sort_order bumped) rather than interleaved.
--
-- Final top-level order: Dashboard(10) < Masters(20) < Inventory(30) <
-- Sales(40) < Requests(50) < Utilities(60) < Updations(70) < Location(80) <
-- Mail(90) < AI(100) < Reports(110) < Administration(120).
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/24_top_level_menu_expansion.sql
-- ===========================================================================

USE acme_db;

INSERT INTO menu_item (id, parent_id, label, title, description, icon, page, href, sort_order, roles, enabled) VALUES
    (410, NULL, 'Inventory', NULL, NULL, 'boxes', NULL, NULL, 30, NULL, 1),
    (411, NULL, 'Sales', NULL, NULL, 'cart3', NULL, NULL, 40, NULL, 1),
    (412, NULL, 'Requests', NULL, NULL, 'clipboard-check', NULL, NULL, 50, NULL, 1),
    (413, NULL, 'Utilities', NULL, NULL, 'tools', NULL, NULL, 60, NULL, 1),
    (414, NULL, 'Updations', NULL, NULL, 'pencil-square', NULL, NULL, 70, NULL, 1),
    (415, NULL, 'Location', NULL, NULL, 'geo-alt', NULL, NULL, 80, NULL, 1),
    (416, NULL, 'Mail', NULL, NULL, 'envelope', NULL, NULL, 90, NULL, 1),
    (417, NULL, 'AI', NULL, NULL, 'robot', NULL, NULL, 100, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), description=VALUES(description),
    icon=VALUES(icon), page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order),
    roles=VALUES(roles), enabled=VALUES(enabled);

-- push Reports/Administration to the end so the new sequence sits right after Masters
UPDATE menu_item SET sort_order = 110 WHERE id = 4;  -- Reports
UPDATE menu_item SET sort_order = 120 WHERE id = 3;  -- Administration
