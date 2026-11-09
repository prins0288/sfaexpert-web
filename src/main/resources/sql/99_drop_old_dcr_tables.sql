-- 99: drop the old DCR tables (old `dcr`, old `dcr_travle_expense`); the report now reads the 2026 tables only. Safe to re-run.

USE acme_db;
-- the old per-visit table and the old travel-expense table (merged into dcr_expense) are not used by anything any more
DROP TABLE IF EXISTS dcr;
DROP TABLE IF EXISTS dcr_travle_expense;

USE globex_db;
-- the old per-visit table and the old travel-expense table (merged into dcr_expense) are not used by anything any more
DROP TABLE IF EXISTS dcr;
DROP TABLE IF EXISTS dcr_travle_expense;

USE initech_db;
-- the old per-visit table and the old travel-expense table (merged into dcr_expense) are not used by anything any more
DROP TABLE IF EXISTS dcr;
DROP TABLE IF EXISTS dcr_travle_expense;

USE sfa_demo;
-- the old per-visit table and the old travel-expense table (merged into dcr_expense) are not used by anything any more
DROP TABLE IF EXISTS dcr;
DROP TABLE IF EXISTS dcr_travle_expense;
