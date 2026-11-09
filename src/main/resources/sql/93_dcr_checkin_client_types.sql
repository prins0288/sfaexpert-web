-- 93: Doctor / Retailer / Party check-in tabs list only clients of their own type. Safe to re-run.

USE acme_db;
-- Party client type (Client = Doctor DR, Retailer RT, Party PT)
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- every check-in tab lists only its own client type
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('DR', 'CLIENT') OR type_name IN ('Doctor', 'Client') ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'CLIENT' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('RT', 'RETAILER') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'RETAILER' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'PARTY' AND w.row_type = 'av' AND w.client_type_id IS NULL;

USE globex_db;
-- Party client type (Client = Doctor DR, Retailer RT, Party PT)
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- every check-in tab lists only its own client type
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('DR', 'CLIENT') OR type_name IN ('Doctor', 'Client') ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'CLIENT' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('RT', 'RETAILER') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'RETAILER' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'PARTY' AND w.row_type = 'av' AND w.client_type_id IS NULL;

USE initech_db;
-- Party client type (Client = Doctor DR, Retailer RT, Party PT)
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- every check-in tab lists only its own client type
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('DR', 'CLIENT') OR type_name IN ('Doctor', 'Client') ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'CLIENT' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('RT', 'RETAILER') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'RETAILER' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'PARTY' AND w.row_type = 'av' AND w.client_type_id IS NULL;

USE sfa_demo;
-- Party client type (Client = Doctor DR, Retailer RT, Party PT)
INSERT INTO client_type (type_code, type_name, singular_label, plural_label, is_active)
SELECT 'PT', 'Party', 'Party', 'Parties', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%');
-- every check-in tab lists only its own client type
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('DR', 'CLIENT') OR type_name IN ('Doctor', 'Client') ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'CLIENT' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('RT', 'RETAILER') OR type_name LIKE 'Retailer%' OR type_name LIKE 'Chemist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'RETAILER' AND w.row_type = 'av' AND w.client_type_id IS NULL;
UPDATE app_dcr_worktype w SET w.client_type_id = (SELECT oid FROM client_type WHERE type_code IN ('PT', 'PARTY') OR type_name LIKE 'Party%' OR type_name LIKE 'Stockist%' ORDER BY oid LIMIT 1)
 WHERE w.worktype_code = 'PARTY' AND w.row_type = 'av' AND w.client_type_id IS NULL;
