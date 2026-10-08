-- ============================================================================
-- 59_app_branding_to_company_setting.sql   (run against sfa_central)
-- ----------------------------------------------------------------------------
-- Retires the app_branding table. The app-wide default branding now lives in
-- company_setting_master under the GLOBAL pseudo-tenant '*':
--     branding.appName  -> the application name (e.g. StarSFA)
--     branding.logo     -> the default logo FILE NAME in the external storage
--                          folder (<base>/__global__/<file>), served publicly by
--                          /api/public/branding-image?file=...  (NOT base64)
--
-- The old default_logo was a base64 data URI in the DB; it is exported to a file
-- (<STORAGE_BASE_DIR>/__global__/default-logo.png) by the deploy step that runs
-- this migration. On a fresh environment the file may be absent — a super-admin
-- re-uploads it from Settings -> Branding; the login page falls back to the app
-- name text until then.
--
-- Run this while the app is stopped, deploying the build that no longer has the
-- AppBranding entity.
-- ============================================================================

INSERT INTO company_setting_master (tenant_id, setting_key, setting_value, updated_at, updated_by)
SELECT '*', 'branding.appName', COALESCE(NULLIF(app_name, ''), 'StarSFA'), NOW(), 'migration'
FROM app_branding WHERE id = 1
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at);

-- point the global logo at the exported file (only when the old row actually had a logo)
INSERT INTO company_setting_master (tenant_id, setting_key, setting_value, updated_at, updated_by)
SELECT '*', 'branding.logo', 'default-logo.png', NOW(), 'migration'
FROM app_branding WHERE id = 1 AND default_logo IS NOT NULL AND default_logo <> ''
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at);

DROP TABLE IF EXISTS app_branding;
