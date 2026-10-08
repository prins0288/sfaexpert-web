-- ===========================================================================
-- Renames the 6 geography tables to the "_master" naming convention:
--   country  -> country_master
--   state    -> state_master
--   hq       -> hq_master
--   area     -> city_master   (Area IS the city level in this app's hierarchy)
--   zone     -> zone_master
--   division -> division_master
--
-- RENAME TABLE is atomic and preserves all data/indexes/auto_increment — no
-- data is touched. Column names (state_oid, area_oid, hq_oid, etc.) are
-- UNCHANGED; only the table names themselves move. Safe to run once; re-running
-- after the rename has already happened will simply error "table doesn't
-- exist" on the old name (harmless — it means it's already done).
--
-- TENANT db, acme_db ONLY.
--
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/18_rename_geography_to_master_naming.sql
-- ===========================================================================

USE acme_db;

RENAME TABLE
  country  TO country_master,
  state    TO state_master,
  hq       TO hq_master,
  area     TO city_master,
  zone     TO zone_master,
  division TO division_master;
