-- 92: DCR_SAVE / DCR_APPROVE must be allowed by default (migration 84 had left default_allowed to the column default,
-- which is 0 in some databases -> 'Requires permission DCR_SAVE'). Safe to re-run.

USE acme_db;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');

USE globex_db;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');

USE initech_db;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');

USE sfa_demo;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');

