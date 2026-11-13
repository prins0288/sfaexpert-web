-- 92: DCR_SAVE / DCR_APPROVE are allowed by default (84 left default_allowed to the column default, 0 in some DBs ->
-- 'Requires permission DCR_SAVE'), and EVERY existing employee gets their own permission_assignment rows for every
-- active permission: what their designation / hierarchy level gives, else the permission's default. Rows that already
-- exist are never touched or duplicated (unique key + NOT EXISTS). New employees get the same rows when created.
-- Safe to re-run.

USE acme_db;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code,
       COALESCE((SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'DESIGNATION' AND a.target_value = CAST(e.designation AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                (SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'HIERARCHY_LEVEL' AND a.target_value = CAST(d.hierarchy_level AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                m.default_allowed),
       NOW(), 'migration-92'
FROM emp_detail e
LEFT JOIN designation_master d ON d.id = e.designation
JOIN permission_master m ON m.status = 1
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

USE globex_db;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code,
       COALESCE((SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'DESIGNATION' AND a.target_value = CAST(e.designation AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                (SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'HIERARCHY_LEVEL' AND a.target_value = CAST(d.hierarchy_level AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                m.default_allowed),
       NOW(), 'migration-92'
FROM emp_detail e
LEFT JOIN designation_master d ON d.id = e.designation
JOIN permission_master m ON m.status = 1
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

USE initech_db;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code,
       COALESCE((SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'DESIGNATION' AND a.target_value = CAST(e.designation AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                (SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'HIERARCHY_LEVEL' AND a.target_value = CAST(d.hierarchy_level AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                m.default_allowed),
       NOW(), 'migration-92'
FROM emp_detail e
LEFT JOIN designation_master d ON d.id = e.designation
JOIN permission_master m ON m.status = 1
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

USE sfa_demo;
UPDATE permission_master SET default_allowed = 1 WHERE permission_code IN ('DCR_SAVE', 'DCR_APPROVE');
INSERT IGNORE INTO permission_assignment (target_type, target_value, permission_code, allowed, created_at, created_by)
SELECT 'EMP_ID', e.emp_id COLLATE utf8mb4_unicode_ci, m.permission_code,
       COALESCE((SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'DESIGNATION' AND a.target_value = CAST(e.designation AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                (SELECT a.allowed FROM permission_assignment a WHERE a.target_type = 'HIERARCHY_LEVEL' AND a.target_value = CAST(d.hierarchy_level AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AND a.permission_code = m.permission_code),
                m.default_allowed),
       NOW(), 'migration-92'
FROM emp_detail e
LEFT JOIN designation_master d ON d.id = e.designation
JOIN permission_master m ON m.status = 1
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment x WHERE x.target_type = 'EMP_ID' AND x.target_value = e.emp_id COLLATE utf8mb4_unicode_ci AND x.permission_code = m.permission_code);

