-- ============================================================================
-- 56_company_details_to_common.sql   (run against sfa_central)
-- ----------------------------------------------------------------------------
-- Renames company_profile -> company_details and moves it OUT of each tenant db
-- and INTO the common db (sfa_central) as ONE shared table keyed by tenant_id
-- (one row per company). Existing per-tenant rows are copied across.
-- The old tenant-side company_profile tables are left unused and droppable later.
-- ============================================================================

CREATE TABLE IF NOT EXISTS company_details (
    tenant_id    VARCHAR(64)  NOT NULL,
    company_name VARCHAR(160) NULL,
    address      VARCHAR(255) NULL,
    city         VARCHAR(96)  NULL,
    phone        VARCHAR(40)  NULL,
    email        VARCHAR(128) NULL,
    website      VARCHAR(128) NULL,
    logo         MEDIUMTEXT   NULL,
    updated_at   DATETIME     NULL,
    updated_by   VARCHAR(128) NULL,
    PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Copy the single company_profile row (id = 1) from each tenant db (idempotent).
INSERT INTO sfa_central.company_details (tenant_id, company_name, address, city, phone, email, website, logo, updated_at, updated_by)
SELECT 'acme', company_name, address, city, phone, email, website, logo, updated_at, updated_by FROM acme_db.company_profile
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), logo = VALUES(logo), updated_at = VALUES(updated_at);

INSERT INTO sfa_central.company_details (tenant_id, company_name, address, city, phone, email, website, logo, updated_at, updated_by)
SELECT 'demo', company_name, address, city, phone, email, website, logo, updated_at, updated_by FROM sfa_demo.company_profile
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), logo = VALUES(logo), updated_at = VALUES(updated_at);

INSERT INTO sfa_central.company_details (tenant_id, company_name, address, city, phone, email, website, logo, updated_at, updated_by)
SELECT 'globex', company_name, address, city, phone, email, website, logo, updated_at, updated_by FROM globex_db.company_profile
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), logo = VALUES(logo), updated_at = VALUES(updated_at);

INSERT INTO sfa_central.company_details (tenant_id, company_name, address, city, phone, email, website, logo, updated_at, updated_by)
SELECT 'initech', company_name, address, city, phone, email, website, logo, updated_at, updated_by FROM initech_db.company_profile
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), logo = VALUES(logo), updated_at = VALUES(updated_at);
