-- ===========================================================================
-- 65_permission_oid_to_id.sql — PER TENANT database
--
-- Renames the primary key of the permission tables from `oid` to `id`
-- (permission_master, permission_assignment). Data and AUTO_INCREMENT are kept.
-- Requires 13_permission_master.sql; the app code from this change onward
-- expects `id`.
--
-- Run ONCE:  mysql -u myroot < src/main/resources/sql/65_permission_oid_to_id.sql
-- ===========================================================================

USE acme_db;
ALTER TABLE permission_master     RENAME COLUMN oid TO id;
ALTER TABLE permission_assignment RENAME COLUMN oid TO id;

USE globex_db;
ALTER TABLE permission_master     RENAME COLUMN oid TO id;
ALTER TABLE permission_assignment RENAME COLUMN oid TO id;

USE initech_db;
ALTER TABLE permission_master     RENAME COLUMN oid TO id;
ALTER TABLE permission_assignment RENAME COLUMN oid TO id;

USE sfa_demo;
ALTER TABLE permission_master     RENAME COLUMN oid TO id;
ALTER TABLE permission_assignment RENAME COLUMN oid TO id;
