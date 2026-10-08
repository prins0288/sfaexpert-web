-- ===========================================================================
-- app_setting — generic key/value company-wide settings (TENANT db). Starts
-- with "dateFormat" (drives every date picker + report date column via
-- js/core/settings.js) but is schema-less on purpose so new settings added
-- from the Settings page never need a migration.
--
-- Also registers the "Settings" page under Administration > Preferences,
-- next to Appearance.
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/21_app_setting_and_date_format.sql
-- ===========================================================================

USE acme_db;

CREATE TABLE IF NOT EXISTS app_setting (
    oid           BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255) NULL,
    updated_at    DATETIME     NULL,
    updated_by    VARCHAR(128) NULL,
    UNIQUE KEY uq_app_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO app_setting (setting_key, setting_value, updated_at, updated_by) VALUES
    ('dateFormat', 'dd-MM-yyyy', NOW(), 'system')
ON DUPLICATE KEY UPDATE setting_key = setting_key;   -- don't overwrite if already customised

-- "Settings" under Administration > Preferences (21), next to Appearance (210)
INSERT INTO menu_item (id, parent_id, label, title, description, icon, page, href, sort_order, roles, enabled) VALUES
    (211, 21, 'Settings', 'Settings', '<b>Settings</b> holds company-wide configuration.<br/>Currently: <b>Date Format</b> — controls every date picker and report date column across the app.', 'gear', 'settings', 'settings/general.html', 20, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), description=VALUES(description),
    icon=VALUES(icon), page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order),
    roles=VALUES(roles), enabled=VALUES(enabled);
