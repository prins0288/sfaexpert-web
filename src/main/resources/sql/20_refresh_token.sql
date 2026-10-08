-- ===========================================================================
-- refresh_token — COMMON db (sfa_central). Opaque, revocable, rotated-on-use
-- long-lived credential (see in.opt.sfa.auth.AuthService.refresh/logout).
-- Lifetime is controlled by application.yml's app.jwt.refresh-expiration-days
-- (NOT stored per-row as a duration — each row's own expires_at is computed
-- at issue time from whatever that setting was then).
--
-- Run:  mysql -u myroot sfa_central < src/main/resources/sql/20_refresh_token.sql
-- ===========================================================================

USE sfa_central;

CREATE TABLE IF NOT EXISTS refresh_token (
    oid          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token        VARCHAR(128) NOT NULL,
    app_user_id  BIGINT       NOT NULL,
    expires_at   DATETIME     NOT NULL,
    revoked      TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL,
    UNIQUE KEY uq_refresh_token_token (token),
    KEY idx_refresh_token_user (app_user_id),
    KEY idx_refresh_token_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
