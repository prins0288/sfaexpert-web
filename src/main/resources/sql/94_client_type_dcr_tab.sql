-- 94: Client / Retailer / Party check-in tabs are fixed codes; client_type.dcr_tab says which client types each tab lists. Labels (Doctor / Chemist / Stockist ...) stay editable in app_dcr_worktype.display_name. Safe to re-run.

USE acme_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_type' AND column_name = 'dcr_tab') = 0,
 "ALTER TABLE client_type ADD dcr_tab VARCHAR(10) NULL COMMENT 'DCR check-in tab this client type belongs to: CLIENT / RETAILER / PARTY (tab codes are fixed, only their labels change)' AFTER plural_label", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE UPPER(type_code) IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
UPDATE client_type SET dcr_tab = 'CLIENT'   WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('DR', 'CLIENT', 'DOCTOR') OR type_name IN ('Doctor', 'Client'));
UPDATE client_type SET dcr_tab = 'RETAILER' WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('RT', 'RETAILER', 'CHEMIST') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%');
UPDATE client_type SET dcr_tab = 'PARTY'    WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('PT', 'PARTY', 'STOCKIST') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- the tab rows keep their label (display_name); no client type is needed on them any more
UPDATE app_dcr_worktype SET client_type_id = NULL WHERE row_type = 'av';

USE globex_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_type' AND column_name = 'dcr_tab') = 0,
 "ALTER TABLE client_type ADD dcr_tab VARCHAR(10) NULL COMMENT 'DCR check-in tab this client type belongs to: CLIENT / RETAILER / PARTY (tab codes are fixed, only their labels change)' AFTER plural_label", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE UPPER(type_code) IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
UPDATE client_type SET dcr_tab = 'CLIENT'   WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('DR', 'CLIENT', 'DOCTOR') OR type_name IN ('Doctor', 'Client'));
UPDATE client_type SET dcr_tab = 'RETAILER' WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('RT', 'RETAILER', 'CHEMIST') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%');
UPDATE client_type SET dcr_tab = 'PARTY'    WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('PT', 'PARTY', 'STOCKIST') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- the tab rows keep their label (display_name); no client type is needed on them any more
UPDATE app_dcr_worktype SET client_type_id = NULL WHERE row_type = 'av';

USE initech_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_type' AND column_name = 'dcr_tab') = 0,
 "ALTER TABLE client_type ADD dcr_tab VARCHAR(10) NULL COMMENT 'DCR check-in tab this client type belongs to: CLIENT / RETAILER / PARTY (tab codes are fixed, only their labels change)' AFTER plural_label", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE UPPER(type_code) IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
UPDATE client_type SET dcr_tab = 'CLIENT'   WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('DR', 'CLIENT', 'DOCTOR') OR type_name IN ('Doctor', 'Client'));
UPDATE client_type SET dcr_tab = 'RETAILER' WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('RT', 'RETAILER', 'CHEMIST') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%');
UPDATE client_type SET dcr_tab = 'PARTY'    WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('PT', 'PARTY', 'STOCKIST') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- the tab rows keep their label (display_name); no client type is needed on them any more
UPDATE app_dcr_worktype SET client_type_id = NULL WHERE row_type = 'av';

USE sfa_demo;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client_type' AND column_name = 'dcr_tab') = 0,
 "ALTER TABLE client_type ADD dcr_tab VARCHAR(10) NULL COMMENT 'DCR check-in tab this client type belongs to: CLIENT / RETAILER / PARTY (tab codes are fixed, only their labels change)' AFTER plural_label", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE UPPER(type_code) IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
UPDATE client_type SET dcr_tab = 'CLIENT'   WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('DR', 'CLIENT', 'DOCTOR') OR type_name IN ('Doctor', 'Client'));
UPDATE client_type SET dcr_tab = 'RETAILER' WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('RT', 'RETAILER', 'CHEMIST') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%');
UPDATE client_type SET dcr_tab = 'PARTY'    WHERE dcr_tab IS NULL AND (UPPER(type_code) IN ('PT', 'PARTY', 'STOCKIST') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- the tab rows keep their label (display_name); no client type is needed on them any more
UPDATE app_dcr_worktype SET client_type_id = NULL WHERE row_type = 'av';
