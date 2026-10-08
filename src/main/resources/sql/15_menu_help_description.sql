-- ===========================================================================
-- menu_item.description — per-page help text shown by a header help button
-- that appears ONLY when this column is non-null/non-blank for the active
-- page's menu row. HTML tags are allowed (rendered as-is by the tooltip).
--
-- TENANT db, acme_db ONLY (per request).
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/15_menu_help_description.sql
-- ===========================================================================

USE acme_db;

SET @has_description := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'menu_item' AND column_name = 'description');
SET @ddl := IF(@has_description = 0,
    'ALTER TABLE menu_item ADD COLUMN description TEXT NULL AFTER title', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- example: DCR Report (id 400, added in 14_country_and_dcr.sql)
UPDATE menu_item SET description =
  '<b>DCR Report</b> shows every Daily Call Report entry.<br/>Pick a <b>Division</b> first — the filter panel narrows Route &rarr; Area &rarr; Employee from there.<br/>Nepal clients show their Nepali (Bikram Sambat) date instead of the regular date.'
WHERE id = 400;
