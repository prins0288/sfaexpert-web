-- ============================================================================
-- 55_company_setting_master_to_common.sql   (run against sfa_central)
-- ----------------------------------------------------------------------------
-- Moves company_setting_master OUT of each tenant db and INTO the common db
-- (sfa_central) as ONE shared table with a tenant_id discriminator. Every read
-- and write is scoped to the current tenant in code (CompanySettingStore), so a
-- company can never see or overwrite another company's settings.
--
-- Existing per-tenant rows are copied across, stamped with their tenant_id. The
-- old tenant-side tables are left in place (unused) and can be dropped later.
-- ============================================================================

CREATE TABLE IF NOT EXISTS company_setting_master (
    oid           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id     VARCHAR(64)  NOT NULL,
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255) NULL,
    updated_at    DATETIME     NULL,
    updated_by    VARCHAR(128) NULL,
    PRIMARY KEY (oid),
    UNIQUE KEY uq_company_setting_tenant_key (tenant_id, setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Copy existing settings from each tenant db (idempotent).
INSERT INTO sfa_central.company_setting_master (tenant_id, setting_key, setting_value, updated_at, updated_by)
SELECT 'acme', setting_key, setting_value, updated_at, updated_by FROM acme_db.company_setting_master
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at), updated_by = VALUES(updated_by);

INSERT INTO sfa_central.company_setting_master (tenant_id, setting_key, setting_value, updated_at, updated_by)
SELECT 'demo', setting_key, setting_value, updated_at, updated_by FROM sfa_demo.company_setting_master
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at), updated_by = VALUES(updated_by);

INSERT INTO sfa_central.company_setting_master (tenant_id, setting_key, setting_value, updated_at, updated_by)
SELECT 'globex', setting_key, setting_value, updated_at, updated_by FROM globex_db.company_setting_master
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at), updated_by = VALUES(updated_by);

INSERT INTO sfa_central.company_setting_master (tenant_id, setting_key, setting_value, updated_at, updated_by)
SELECT 'initech', setting_key, setting_value, updated_at, updated_by FROM initech_db.company_setting_master
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at), updated_by = VALUES(updated_by);
