-- ===========================================================================
-- Renames app_setting -> company_setting_master (RENAME TABLE preserves the
-- existing dateFormat row/data, indexes, auto_increment). This is now the ONE
-- table for every per-tenant company-wide setting.
--
-- Also seeds "contentProtection" = 'N' (off by default) — a SINGLE toggle
-- that, when 'Y', turns on ALL of: hiding every report/master's Excel export
-- button, blocking right-click / copy / cut / text-selection, and a
-- best-effort DevTools-open deterrent. See js/core/settings.js +
-- js/app.js (sfaApplyContentProtection) for the enforcement, and IMPORTANT:
-- none of this is real security — see the code comments for why DevTools
-- specifically cannot be blocked by any web page, ever.
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/22_company_setting_master_and_content_protection.sql
-- ===========================================================================

USE acme_db;

RENAME TABLE app_setting TO company_setting_master;

INSERT INTO company_setting_master (setting_key, setting_value, updated_at, updated_by) VALUES
    ('contentProtection', 'N', NOW(), 'system')
ON DUPLICATE KEY UPDATE setting_key = setting_key;   -- don't overwrite if already set
