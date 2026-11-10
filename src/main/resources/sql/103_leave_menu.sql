-- 103: put the My Leave menu next to the DCR (every employee uses it), not under Master Entry; make the HR / leave menus visible. Safe to re-run.

USE acme_db;
SET @lv_dcr := (SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1);
UPDATE company_menu_self SET parent_id = @lv_dcr, sort_order = 9 WHERE page = 'myleave' AND @lv_dcr IS NOT NULL;
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('myleave','hrleavemaster','hrsetup','hrsettings','hrroles','hraudit');

USE globex_db;
SET @lv_dcr := (SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1);
UPDATE company_menu_self SET parent_id = @lv_dcr, sort_order = 9 WHERE page = 'myleave' AND @lv_dcr IS NOT NULL;
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('myleave','hrleavemaster','hrsetup','hrsettings','hrroles','hraudit');

USE initech_db;
SET @lv_dcr := (SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1);
UPDATE company_menu_self SET parent_id = @lv_dcr, sort_order = 9 WHERE page = 'myleave' AND @lv_dcr IS NOT NULL;
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('myleave','hrleavemaster','hrsetup','hrsettings','hrroles','hraudit');

USE sfa_demo;
SET @lv_dcr := (SELECT parent_id FROM company_menu_self WHERE page = 'dcrsubmit' LIMIT 1);
UPDATE company_menu_self SET parent_id = @lv_dcr, sort_order = 9 WHERE page = 'myleave' AND @lv_dcr IS NOT NULL;
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('myleave','hrleavemaster','hrsetup','hrsettings','hrroles','hraudit');
