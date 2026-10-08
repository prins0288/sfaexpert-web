-- ===========================================================================
-- app_user.last_login_at — COMMON db (sfa_central — see application.yml's
-- app.datasource.common.jdbc-url; the "common_db" name used in the OLDER
-- 01_common_db.sql seed file is stale and not what this project actually runs
-- against). Stamped by AuthService on every successful login; the value
-- returned to the client is the PREVIOUS login (read before the overwrite),
-- so the UI can show "Last login: ..." for the session that just ended, not
-- the one starting right now.
--
-- Run:  mysql -u myroot sfa_central < src/main/resources/sql/16_last_login.sql
-- ===========================================================================

USE sfa_central;

SET @has_last_login := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'app_user' AND column_name = 'last_login_at');
SET @ddl := IF(@has_last_login = 0,
    'ALTER TABLE app_user ADD COLUMN last_login_at DATETIME NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
