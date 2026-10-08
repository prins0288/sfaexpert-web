-- ===========================================================================
-- Full "Master" submenu tree, added as empty branches (no href yet — actual
-- pages will be built later, same as 24_top_level_menu_expansion.sql). Sits
-- alongside the existing Geography/Parties/Catalog groups under Masters (id=2).
--
-- Structure (L2 groups under Masters, L3 leaf actions, L4 for Expense's two
-- sub-groups):
--   Master Entry     > Create Employee/Designation/District/State/Promoitem/Items
--   Feed Clients     > Multi Client Add Single Area / In Detail / Multi Area
--   Mapping          > Party Client Mapping (+View/Approve), Employee Retailer
--                       Mapping (+View), CNF Creation/View/Primary/Closing/Party
--                       Mapping, Client Degree Mapping (+View)
--   Copy Data        > Copy Client / Party to New Employee
--   Data Transfer    > District Transfer, Transfer Standard Tour Plan, Transfer
--                       Data/Clients/Area/Party to New Employee
--   Expense          > Inputs > Issue Samples - Employee/State, Multi Employee
--                       Sample Issue
--                     > Sample > (same 3 as Inputs — two separate menu paths
--                       to the same sample-issue actions)
--   Sponsor Data     > Feed Investment, Update CRM Status, Feed Client Business,
--                       Feed CRM Budget Yearly
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/25_master_submenu_tree.sql
-- ===========================================================================

USE acme_db;

INSERT INTO menu_item (id, parent_id, label, title, description, icon, page, href, sort_order, roles, enabled) VALUES
    -- L2 groups under Masters (id=2) — after Geography(10)/Parties(20)/Catalog(30)
    (500, 2, 'Master Entry', NULL, NULL, 'pencil-square', NULL, NULL, 40, NULL, 1),
    (501, 2, 'Feed Clients', NULL, NULL, 'person-plus', NULL, NULL, 50, NULL, 1),
    (502, 2, 'Mapping', NULL, NULL, 'diagram-3', NULL, NULL, 60, NULL, 1),
    (503, 2, 'Copy Data', NULL, NULL, 'files', NULL, NULL, 70, NULL, 1),
    (504, 2, 'Data Transfer', NULL, NULL, 'arrow-left-right', NULL, NULL, 80, NULL, 1),
    (505, 2, 'Expense', NULL, NULL, 'cash-stack', NULL, NULL, 90, NULL, 1),
    (506, 2, 'Sponsor Data', NULL, NULL, 'briefcase', NULL, NULL, 100, NULL, 1),

    -- Master Entry (500)
    (510, 500, 'Create Employee', NULL, NULL, 'person-badge', NULL, NULL, 1, NULL, 1),
    (511, 500, 'Create Designation', NULL, NULL, 'person-workspace', NULL, NULL, 2, NULL, 1),
    (512, 500, 'Create District', NULL, NULL, 'geo', NULL, NULL, 3, NULL, 1),
    (513, 500, 'Create State', NULL, NULL, 'signpost', NULL, NULL, 4, NULL, 1),
    (514, 500, 'Create Promoitem', NULL, NULL, 'megaphone', NULL, NULL, 5, NULL, 1),
    (515, 500, 'Create Items', NULL, NULL, 'box', NULL, NULL, 6, NULL, 1),

    -- Feed Clients (501)
    (520, 501, 'Multi Client Add Single Area', NULL, NULL, 'geo-alt', NULL, NULL, 1, NULL, 1),
    (521, 501, 'Multi Client Add In Detail', NULL, NULL, 'list-check', NULL, NULL, 2, NULL, 1),
    (522, 501, 'Multi Client Add Multi Area', NULL, NULL, 'geo-alt-fill', NULL, NULL, 3, NULL, 1),

    -- Mapping (502)
    (530, 502, 'Party Client Mapping', NULL, NULL, 'link-45deg', NULL, NULL, 1, NULL, 1),
    (531, 502, 'Party Client Mapping View', NULL, NULL, 'eye', NULL, NULL, 2, NULL, 1),
    (532, 502, 'Party Client Mapping Approve', NULL, NULL, 'check2-circle', NULL, NULL, 3, NULL, 1),
    (533, 502, 'Employee Retailer Mapping', NULL, NULL, 'link-45deg', NULL, NULL, 4, NULL, 1),
    (534, 502, 'Employee Retailer Mapping View', NULL, NULL, 'eye', NULL, NULL, 5, NULL, 1),
    (535, 502, 'CNF Creation', NULL, NULL, 'plus-square', NULL, NULL, 6, NULL, 1),
    (536, 502, 'CNF View', NULL, NULL, 'eye', NULL, NULL, 7, NULL, 1),
    (537, 502, 'CNF Primary', NULL, NULL, 'star', NULL, NULL, 8, NULL, 1),
    (538, 502, 'CNF Closing', NULL, NULL, 'x-square', NULL, NULL, 9, NULL, 1),
    (539, 502, 'CNF Party Mapping', NULL, NULL, 'link-45deg', NULL, NULL, 10, NULL, 1),
    (540, 502, 'Client Degree Mapping', NULL, NULL, 'mortarboard', NULL, NULL, 11, NULL, 1),
    (541, 502, 'Client Degree Mapping View', NULL, NULL, 'eye', NULL, NULL, 12, NULL, 1),

    -- Copy Data (503)
    (550, 503, 'Copy Client to New Employee', NULL, NULL, 'person-lines-fill', NULL, NULL, 1, NULL, 1),
    (551, 503, 'Copy Party to New Employee', NULL, NULL, 'person-lines-fill', NULL, NULL, 2, NULL, 1),

    -- Data Transfer (504)
    (560, 504, 'District Transfer', NULL, NULL, 'arrow-left-right', NULL, NULL, 1, NULL, 1),
    (561, 504, 'Transfer Standard Tour Plan', NULL, NULL, 'signpost-split', NULL, NULL, 2, NULL, 1),
    (562, 504, 'Transfer Data to New Employee', NULL, NULL, 'arrow-right', NULL, NULL, 3, NULL, 1),
    (563, 504, 'Transfer Clients to New Employee', NULL, NULL, 'arrow-right', NULL, NULL, 4, NULL, 1),
    (564, 504, 'Transfer Area to New Employee', NULL, NULL, 'arrow-right', NULL, NULL, 5, NULL, 1),
    (565, 504, 'Transfer Party to New Employee', NULL, NULL, 'arrow-right', NULL, NULL, 6, NULL, 1),

    -- Expense (505) sub-groups
    (570, 505, 'Inputs', NULL, NULL, 'input-cursor', NULL, NULL, 1, NULL, 1),
    (571, 505, 'Sample', NULL, NULL, 'box-seam', NULL, NULL, 2, NULL, 1),

    -- Inputs (570)
    (580, 570, 'Issue Samples - Employee', NULL, NULL, 'person-check', NULL, NULL, 1, NULL, 1),
    (581, 570, 'Issue Samples - State', NULL, NULL, 'geo-alt', NULL, NULL, 2, NULL, 1),
    (582, 570, 'Multi Employee Sample Issue', NULL, NULL, 'people', NULL, NULL, 3, NULL, 1),

    -- Sample (571) — same 3 actions, a second menu path to them
    (590, 571, 'Issue Samples - Employee', NULL, NULL, 'person-check', NULL, NULL, 1, NULL, 1),
    (591, 571, 'Issue Samples - State', NULL, NULL, 'geo-alt', NULL, NULL, 2, NULL, 1),
    (592, 571, 'Multi Employee Sample Issue', NULL, NULL, 'people', NULL, NULL, 3, NULL, 1),

    -- Sponsor Data (506)
    (600, 506, 'Feed Investment', NULL, NULL, 'cash-coin', NULL, NULL, 1, NULL, 1),
    (601, 506, 'Update CRM Status', NULL, NULL, 'arrow-repeat', NULL, NULL, 2, NULL, 1),
    (602, 506, 'Feed Client Business', NULL, NULL, 'graph-up', NULL, NULL, 3, NULL, 1),
    (603, 506, 'Feed CRM Budget Yearly', NULL, NULL, 'calendar4-range', NULL, NULL, 4, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), description=VALUES(description),
    icon=VALUES(icon), page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order),
    roles=VALUES(roles), enabled=VALUES(enabled);
