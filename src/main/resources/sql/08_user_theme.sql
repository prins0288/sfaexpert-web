-- ===========================================================================
-- Per-user appearance / theme configuration (COMMON database: sfa_central).
--
-- Two tables:
--   user_preference   -> one row/user: nav layout, light/dark, preset, density...
--   user_theme_token  -> fine-grained colour/size overrides, per user + mode
--
-- Colours themselves are NOT seeded here: the application resolves
-- (mode default -> preset delta -> these overrides) at runtime, so a user with
-- no rows automatically gets the original StarSFA look. These tables only hold
-- what the user has explicitly changed.
--
-- Run:  mysql -u myroot < src/main/resources/sql/08_user_theme.sql
-- ===========================================================================
USE sfa_central;

CREATE TABLE IF NOT EXISTS user_preference (
    username          VARCHAR(128) NOT NULL PRIMARY KEY,
    nav_layout        VARCHAR(16)  NOT NULL DEFAULT 'vertical',      -- vertical | horizontal
    theme_mode        VARCHAR(16)  NOT NULL DEFAULT 'light',         -- light | dark
    preset            VARCHAR(32)  NOT NULL DEFAULT 'default',       -- default | teal | indigo | slate | emerald
    font_scale        VARCHAR(8)   NOT NULL DEFAULT '1.0',           -- 0.85 – 1.30
    density           VARCHAR(16)  NOT NULL DEFAULT 'comfortable',   -- comfortable | compact
    sidebar_collapsed TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_preference_user
        FOREIGN KEY (username) REFERENCES app_user (username)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_theme_token (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(128) NOT NULL,
    mode         VARCHAR(16)  NOT NULL,                              -- light | dark
    token_key    VARCHAR(64)  NOT NULL,                             -- e.g. sidebar-bg
    token_value  VARCHAR(128) NOT NULL,                             -- e.g. #12294d / 260px
    UNIQUE KEY uq_user_theme_token (username, mode, token_key),
    KEY idx_user_theme_token_user (username),
    CONSTRAINT fk_user_theme_token_user
        FOREIGN KEY (username) REFERENCES app_user (username)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
