-- ===========================================================================
-- Language / i18n support.
--
-- 1) user_setting (COMMON db) — a generic per-user key/value preference table,
--    the per-user counterpart to per-tenant company_setting_master. The chosen
--    UI language is stored here as key 'lang'. Follows the user across tenants
--    and devices.
--
-- 2) label_master (TENANT db) — ALREADY EXISTS in every tenant DB
--    (label_key, label_value, lang, status, unique(label_key,lang)); it is the
--    per-tenant OVERRIDE layer over the shipped i18n/lang_*.properties files.
--    Nothing to create here — this note just documents where it fits. A tenant
--    that wants to reword a label inserts a row, e.g.:
--       INSERT INTO label_master (label_key, label_value, lang)
--       VALUES ('common.save', 'Store', 'en');
--    Base translations (no override needed) live in the app's .properties.
--
-- Run (COMMON db):
--   mysql -u myroot sfa_central < src/main/resources/sql/28_i18n_user_setting.sql
-- ===========================================================================

USE sfa_central;

CREATE TABLE IF NOT EXISTS user_setting (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(128) NOT NULL,
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255) NULL,
    updated_at    DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_setting (username, setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
