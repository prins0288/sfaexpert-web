-- ===========================================================================
-- Fixes country.status / dcr.status: these tables were first created (by an
-- earlier run of 14_country_and_dcr.sql, before it was corrected) as
-- VARCHAR(1) 'Y'/'N'. CREATE TABLE IF NOT EXISTS is a no-op on an existing
-- table, so simply re-running 14 never applied the intended TINYINT(1) type.
-- This converts the data ('Y'->1, 'N'->0) BEFORE changing the column type, so
-- no row is silently zeroed out by MySQL's string->number cast.
--
-- Safe to run even if the columns are already TINYINT (the data UPDATE only
-- matches 'Y'/'N', and the MODIFY is a no-op if already TINYINT(1)).
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/17_fix_country_dcr_status_tinyint.sql
-- ===========================================================================

USE acme_db;

UPDATE country SET status = IF(status = 'Y', '1', '0') WHERE status IN ('Y', 'N');
ALTER TABLE country MODIFY COLUMN status TINYINT(1) NOT NULL DEFAULT 1;

UPDATE dcr SET status = IF(status = 'Y', '1', '0') WHERE status IN ('Y', 'N');
ALTER TABLE dcr MODIFY COLUMN status TINYINT(1) NOT NULL DEFAULT 1;
