-- ============================================================================
-- 49_designation_master.sql
-- Rename `designation` -> `designation_master` and add an emp_level column
-- (hierarchy: 1, 2, 3, … — higher = more senior).
--
-- Run ONCE on EVERY tenant database (acme_db, and each other tenant DB in
-- tenant_config). The common DB (sfa_central) is NOT affected.
-- ============================================================================

RENAME TABLE designation TO designation_master;

ALTER TABLE designation_master
    ADD COLUMN emp_level INT NULL COMMENT 'Hierarchy / employee level: 1,2,3,...';

-- Optional: seed levels for existing rows, e.g.
-- UPDATE designation_master SET emp_level = 1 WHERE designation_code = 'MR';
-- UPDATE designation_master SET emp_level = 2 WHERE designation_code = 'ABM';
