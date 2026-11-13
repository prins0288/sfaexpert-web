-- ===========================================================================
-- 83_team_hierarchy_menu.sql — PER TENANT database
--
-- Menu "Team Hierarchy" (Master Entry, next to Password Reset): who reports to
-- whom — add a member under a manager, move, or remove from the team. Admin
-- handles everyone; a manager only his own team. Changes are applied directly
-- and recorded in the promotion / transfer history (MANAGER_CHANGE).
-- Permission TEAM_HIERARCHY_SAVE (allowed by default).
-- Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/83_team_hierarchy_menu.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('TEAM_HIERARCHY_SAVE', 'Team Hierarchy', 'Add / move / remove team members (manager changes)', 1, NOW(), 'seed');
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Team Hierarchy', 'Team Hierarchy', 'diagram-3',
       'teamhierarchy', 'master/team-hierarchy.html', '_SELF', 22, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'teamhierarchy');

-- ==================== globex_db ====================
USE globex_db;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('TEAM_HIERARCHY_SAVE', 'Team Hierarchy', 'Add / move / remove team members (manager changes)', 1, NOW(), 'seed');
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Team Hierarchy', 'Team Hierarchy', 'diagram-3',
       'teamhierarchy', 'master/team-hierarchy.html', '_SELF', 22, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'teamhierarchy');

-- ==================== initech_db ====================
USE initech_db;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('TEAM_HIERARCHY_SAVE', 'Team Hierarchy', 'Add / move / remove team members (manager changes)', 1, NOW(), 'seed');
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Team Hierarchy', 'Team Hierarchy', 'diagram-3',
       'teamhierarchy', 'master/team-hierarchy.html', '_SELF', 22, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'teamhierarchy');

-- ==================== sfa_demo ====================
USE sfa_demo;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('TEAM_HIERARCHY_SAVE', 'Team Hierarchy', 'Add / move / remove team members (manager changes)', 1, NOW(), 'seed');
SET @parent := COALESCE((SELECT parent_id FROM company_menu_self WHERE page = 'passwordreset' LIMIT 1), 500);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Team Hierarchy', 'Team Hierarchy', 'diagram-3',
       'teamhierarchy', 'master/team-hierarchy.html', '_SELF', 22, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'teamhierarchy');
