-- ===========================================================================
-- user_table_preferences (COMMON db) — COMPANY-WIDE column layout for any
-- report / grid: column order, visibility and width, keyed by
-- (company_code, screen_key). Every user of the company sees the same layout;
-- only ADMIN / SUPER_ADMIN can change or reset it.
--
--   company_code  same column name/type as user_login_master.company_code
--   screen_key    a key registered in DefaultColumnRegistry, e.g. DCR_REPORT
--   column_config JSON array of
--                 {field, labelKey, order, visible, width, extraSettings}
--
-- No row = the company never customised that screen -> the registry's default
-- columns are served. Rows are always read with company_code from the
-- verified JWT, so one company never sees another company's layout.
--
-- Run (COMMON db):
--   mysql -u myroot < src/main/resources/sql/64_user_table_preferences.sql
-- ===========================================================================

USE sfa_central;

CREATE TABLE IF NOT EXISTS user_table_preferences (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    company_code  VARCHAR(64)  NOT NULL,
    screen_key    VARCHAR(100) NOT NULL,
    column_config JSON         NOT NULL,
    updated_at    DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_table_preferences (company_code, screen_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
