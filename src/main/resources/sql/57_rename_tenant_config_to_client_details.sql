-- ============================================================================
-- 57_rename_tenant_config_to_client_details.sql   (run against sfa_central)
-- ----------------------------------------------------------------------------
-- Renames the tenant registry / datasource-config table tenant_config ->
-- client_details. Nothing references it by raw name (only the JPA @Table on
-- TenantConfig, updated in the same change), so a plain RENAME is enough.
-- IMPORTANT: run this while the app is stopped, between deploying the old and
-- the new build, since the old build maps the entity to the old name.
-- ============================================================================

RENAME TABLE tenant_config TO client_details;
