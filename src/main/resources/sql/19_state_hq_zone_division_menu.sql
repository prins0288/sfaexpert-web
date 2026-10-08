-- ===========================================================================
-- Menu entries for the 4 new geography masters (Division, Zone, State, HQ)
-- built on top of the country_master/state_master/hq_master/zone_master/
-- division_master rename (18_rename_geography_to_master_naming.sql). Placed
-- under Masters > Geography (parent id 10), ordered top-to-bottom by
-- hierarchy: Division > Zone > State > HQ > Country > City(Area) > Route.
-- Each gets a `description` so its header help button shows (see
-- 15_menu_help_description.sql for how that feature works).
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/19_state_hq_zone_division_menu.sql
-- ===========================================================================

USE acme_db;

INSERT INTO menu_item (id, parent_id, label, title, description, icon, page, href, sort_order, roles, enabled) VALUES
    (104, 10, 'Division', 'Division Master', '<b>Division</b> is the top of the geography hierarchy.<br/>Division &rarr; Zone &rarr; State &rarr; HQ &rarr; City.', 'diagram-3', 'division', 'master/division.html', 1, NULL, 1),
    (105, 10, 'Zone', 'Zone Master', '<b>Zone</b> groups states/regions under a <b>Division</b>.', 'map', 'zone', 'master/zone.html', 2, NULL, 1),
    (106, 10, 'State', 'State Master', '<b>State</b> belongs to a <b>Country</b> and can be assigned a <b>Zone</b>-mapped region.<br/>Nepal states drive the DCR report''s Nepali-date behaviour.', 'signpost', 'state', 'master/state.html', 3, NULL, 1),
    (107, 10, 'HQ', 'HQ Master', '<b>HQ</b> (headquarters/district) belongs to a <b>State</b> and is the parent of City/Area.', 'building', 'hq', 'master/hq.html', 4, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), description=VALUES(description),
    icon=VALUES(icon), page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order),
    roles=VALUES(roles), enabled=VALUES(enabled);

-- bump Country's own sort_order to sit after HQ (was 5, now the last of the 5 "master" entries before Area/City=10)
UPDATE menu_item SET sort_order = 5 WHERE id = 103;
