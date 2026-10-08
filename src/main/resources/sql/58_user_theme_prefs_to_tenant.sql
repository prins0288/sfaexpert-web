-- ============================================================================
-- 58_user_theme_prefs_to_tenant.sql   (run against EACH tenant db)
-- ----------------------------------------------------------------------------
-- Moves the two legacy per-user appearance tables OUT of the common db and INTO
-- each tenant db:
--    user_preference   (one row/user: nav layout, mode, preset, density, ...)
--    user_theme_token  (per user+mode colour/size overrides)
--
-- These were superseded by per-tenant company_setting_master (theme.* keys) in
-- migration 27 and are currently unused by the application, so no rows are
-- carried over — the tables are (re)created empty in the tenant db. The FK to
-- the common user table is dropped (there is no user table in a tenant db).
--
-- After creating them here in every tenant db, drop the common copies:
--    USE sfa_central;
--    DROP TABLE IF EXISTS user_theme_token;
--    DROP TABLE IF EXISTS user_preference;
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_preference (
    username          VARCHAR(128) NOT NULL PRIMARY KEY,
    nav_layout        VARCHAR(16)  NOT NULL DEFAULT 'vertical',
    theme_mode        VARCHAR(16)  NOT NULL DEFAULT 'light',
    preset            VARCHAR(32)  NOT NULL DEFAULT 'default',
    font_scale        VARCHAR(8)   NOT NULL DEFAULT '1.0',
    density           VARCHAR(16)  NOT NULL DEFAULT 'comfortable',
    sidebar_collapsed TINYINT(1)   NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_theme_token (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(128) NOT NULL,
    mode         VARCHAR(16)  NOT NULL,
    token_key    VARCHAR(64)  NOT NULL,
    token_value  VARCHAR(128) NOT NULL,
    UNIQUE KEY uq_user_theme_token (username, mode, token_key),
    KEY idx_user_theme_token_user (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
