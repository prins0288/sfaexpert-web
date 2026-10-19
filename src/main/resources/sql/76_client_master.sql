-- ===========================================================================
-- 76_client_master.sql — PER TENANT database
--
-- Client Master (production version of the old `client` table), like emp_detail:
--   client_master        one row per doctor / chemist / stockist ... (your DDL)
--     + photo_thumbnail_path, approved_on, delete_requested_by, delete_request_reason
--     + foreign keys to client_type, category_master, speciality_master,
--       degree_master, state_master, district, city_master (= Area master), emp_detail
--     (degree_id is BIGINT UNSIGNED to match degree_master.id; mapped_emp_code
--      copies emp_detail.emp_id's charset/collation — else the FK cannot be made)
--   client_master_child  social handles (1 row per client)
--   permissions CLIENT_MASTER_SAVE / _STATUS / _UPLOAD / _APPROVE, menu "Client Master"
--   existing rows of the old `client` table are copied ONCE with the same id
--   (so dcr.client_oid still points at the same client). The old table is kept.
--
-- If you already created client_master from the DDL, the missing columns and
-- keys are added. Safe to re-run.
-- Run:  mysql -u myroot < src/main/resources/sql/76_client_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;

CREATE TABLE IF NOT EXISTS client_master (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    client_code               VARCHAR(30)   NOT NULL,
    client_name               VARCHAR(150)  NOT NULL,
    client_type_id            BIGINT        NULL COMMENT '-> client_type.oid (Doctor / Chemist / Stockist ...)',
    category_id               BIGINT        NULL COMMENT '-> category_master.oid',
    sub_category              VARCHAR(100)  NULL,
    speciality_id             BIGINT        NULL COMMENT '-> speciality_master.oid',
    sub_speciality            VARCHAR(100)  NULL,
    degree_id                 BIGINT UNSIGNED NULL COMMENT '-> degree_master.id',
    qualification             VARCHAR(150)  NULL,
    designation               VARCHAR(100)  NULL,
    is_prescriber             TINYINT(1)    NOT NULL DEFAULT 0,
    experience_years          VARCHAR(100)  NULL,
    mapped_emp_code           VARCHAR(40)   NULL COMMENT '-> emp_detail.emp_id (field employee)',
    state_id                  BIGINT        NULL COMMENT '-> state_master.oid',
    district_id               BIGINT        NULL COMMENT '-> district.oid',
    city_id                   BIGINT        NULL COMMENT '-> city_master.oid (Area master)',
    locality_type             ENUM('Urban','Semi-Urban','Rural') NULL,
    pin_code                  VARCHAR(10)   NULL,
    address                   VARCHAR(500)  NULL,
    landmark                  VARCHAR(200)  NULL,
    latitude                  DECIMAL(10,7) NULL,
    longitude                 DECIMAL(10,7) NULL,
    has_geo_tag               TINYINT(1)    NOT NULL DEFAULT 0,
    sitting_place             VARCHAR(100)  NULL COMMENT 'Clinic/Hospital/etc',
    property_name             VARCHAR(150)  NULL,
    star_rating               TINYINT UNSIGNED NOT NULL DEFAULT 0,
    visit_frequency           VARCHAR(50)   NULL,
    visit_days                SET('MON','TUE','WED','THU','FRI','SAT','SUN') NULL,
    preferred_visit_time      VARCHAR(100)  NULL,
    patients_per_day          SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    no_of_machines            SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    is_multi_location         TINYINT(1)    NOT NULL DEFAULT 0,
    is_pool_client            TINYINT(1)    NOT NULL DEFAULT 0,
    mapped_campaign_code      VARCHAR(100)  NULL,
    mobile_number_1           VARCHAR(15)   NULL,
    mobile_number_2           VARCHAR(15)   NULL,
    email                     VARCHAR(120)  NULL,
    contact_person_1_name     VARCHAR(150)  NULL,
    contact_person_1_mobile   VARCHAR(15)   NULL,
    contact_person_2_name     VARCHAR(150)  NULL,
    contact_person_2_mobile   VARCHAR(15)   NULL,
    gender                    ENUM('Male','Female','Other') NULL,
    date_of_birth             DATE          NULL,
    anniversary_date          DATE          NULL,
    spouse_date_of_birth      DATE          NULL,
    gst_number                VARCHAR(20)   NULL,
    pan_number                CHAR(10)      NULL,
    aadhaar_number            CHAR(12)      NULL,
    drug_license_no           VARCHAR(80)   NULL,
    medical_registration_no   VARCHAR(100)  NULL,
    photo_path                VARCHAR(255)  NULL,
    photo_thumbnail_path      VARCHAR(255)  NULL,
    remarks                   TEXT          NULL,
    extra_info_1              VARCHAR(255)  NULL,
    extra_info_2              VARCHAR(255)  NULL,
    extra_info_3              VARCHAR(255)  NULL,
    is_active                 TINYINT(1)    NOT NULL DEFAULT 1,
    is_delete_requested       TINYINT(1)    NOT NULL DEFAULT 0,
    delete_requested_by       VARCHAR(40)   NULL,
    delete_request_reason     VARCHAR(255)  NULL,
    is_approved               TINYINT(1)    NOT NULL DEFAULT 0,
    approved_by_emp_code      VARCHAR(40)   NULL,
    approved_on               DATETIME      NULL,
    has_fund_support          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by                VARCHAR(40)   NULL,
    updated_by                VARCHAR(40)   NULL,
    created_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_code (client_code),
    KEY idx_client_master_name (client_name),
    KEY idx_client_master_emp (mapped_emp_code),
    KEY idx_client_master_state (state_id),
    KEY idx_client_master_district (district_id),
    KEY idx_client_master_city (city_id),
    KEY idx_client_master_category (category_id),
    KEY idx_client_master_speciality (speciality_id),
    KEY idx_client_master_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- table created earlier from the plain DDL: add what is missing
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_requested_by');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_request_reason');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approved_on');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by_emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'degree_id');
SET @sql := IF(@t <> 'bigint unsigned', 'ALTER TABLE client_master MODIFY degree_id BIGINT UNSIGNED NULL', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- mapped_emp_code must match emp_detail.emp_id exactly for the FK
SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
SET @cur := (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'mapped_emp_code');
SET @sql := IF(@cur <> @co, CONCAT('ALTER TABLE client_master MODIFY mapped_emp_code VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_client_type');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_client_type FOREIGN KEY (client_type_id) REFERENCES client_type (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_category');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_category FOREIGN KEY (category_id) REFERENCES category_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_speciality');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_speciality FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_degree');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_degree FOREIGN KEY (degree_id) REFERENCES degree_master (id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_state');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_state FOREIGN KEY (state_id) REFERENCES state_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_district');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_district FOREIGN KEY (district_id) REFERENCES district (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_city');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_city FOREIGN KEY (city_id) REFERENCES city_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_emp');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_emp FOREIGN KEY (mapped_emp_code) REFERENCES emp_detail (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS client_master_child (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    client_id           BIGINT        NOT NULL,
    mapped_emp_code     VARCHAR(40)   NULL,
    instagram_handle    VARCHAR(150)  NULL,
    facebook_handle     VARCHAR(150)  NULL,
    twitter_handle      VARCHAR(150)  NULL,
    is_active           TINYINT(1)    NOT NULL DEFAULT 1,
    created_by          VARCHAR(40)   NULL,
    created_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_child_client (client_id),
    KEY idx_client_master_child_emp (mapped_emp_code),
    CONSTRAINT fk_cmc_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- one-time copy of the old `client` table (same id; masters only when they exist)
SET @has := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client');
SET @sql := IF(@has = 0, 'DO 0', 'INSERT INTO client_master (id, client_code, client_name, client_type_id, category_id, speciality_id, degree_id,
       city_id, state_id, address, mobile_number_1, email, date_of_birth, anniversary_date, drug_license_no, gst_number, remarks,
       property_name, is_active, is_approved, created_by, created_on)
SELECT c.oid, LEFT(c.client_code, 30), LEFT(TRIM(CONCAT(IFNULL(CONCAT(c.prefix, '' ''), ''''), c.name)), 150),
       (SELECT t.oid FROM client_type t WHERE t.oid = c.client_type_oid),
       (SELECT x.oid FROM category_master x WHERE x.oid = c.category_oid),
       (SELECT x.oid FROM speciality_master x WHERE x.oid = c.speciality_oid),
       (SELECT x.id FROM degree_master x WHERE x.id = c.degree_oid),
       (SELECT a.oid FROM city_master a WHERE a.oid = c.area_oid),
       (SELECT a.state_oid FROM city_master a JOIN state_master s ON s.oid = a.state_oid WHERE a.oid = c.area_oid),
       c.address, LEFT(c.mobile, 15), c.email, c.dob, c.anniversary, c.drug_license_no, LEFT(c.gst_no, 20), c.remarks,
       c.firm_name, IF(c.status = ''Y'', 1, 0), 1, ''import'', c.created_on
FROM client c
WHERE NOT EXISTS (SELECT 1 FROM client_master m WHERE m.id = c.oid OR m.client_code = LEFT(c.client_code, 30))');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('CLIENT_MASTER_SAVE', 'Client Master', 'Add / edit Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_STATUS', 'Client Master', 'Activate / deactivate Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_UPLOAD', 'Client Master', 'Bulk Excel upload of Client Master', 1, NOW(), 'seed');
-- approving clients / delete requests is for managers: default DENIED
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_MASTER_APPROVE', 'Client Master', 'Approve clients and their delete requests', 1, 0, NOW(), 'seed');

-- menu: next to Create Employee (else Master Entry, else Masters)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Client Master', 'Client Master', 'hospital',
       'clientmaster', 'master/client-master.html', '_SELF', 20, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'clientmaster');

-- ==================== globex_db ====================
USE globex_db;

CREATE TABLE IF NOT EXISTS client_master (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    client_code               VARCHAR(30)   NOT NULL,
    client_name               VARCHAR(150)  NOT NULL,
    client_type_id            BIGINT        NULL COMMENT '-> client_type.oid (Doctor / Chemist / Stockist ...)',
    category_id               BIGINT        NULL COMMENT '-> category_master.oid',
    sub_category              VARCHAR(100)  NULL,
    speciality_id             BIGINT        NULL COMMENT '-> speciality_master.oid',
    sub_speciality            VARCHAR(100)  NULL,
    degree_id                 BIGINT UNSIGNED NULL COMMENT '-> degree_master.id',
    qualification             VARCHAR(150)  NULL,
    designation               VARCHAR(100)  NULL,
    is_prescriber             TINYINT(1)    NOT NULL DEFAULT 0,
    experience_years          VARCHAR(100)  NULL,
    mapped_emp_code           VARCHAR(40)   NULL COMMENT '-> emp_detail.emp_id (field employee)',
    state_id                  BIGINT        NULL COMMENT '-> state_master.oid',
    district_id               BIGINT        NULL COMMENT '-> district.oid',
    city_id                   BIGINT        NULL COMMENT '-> city_master.oid (Area master)',
    locality_type             ENUM('Urban','Semi-Urban','Rural') NULL,
    pin_code                  VARCHAR(10)   NULL,
    address                   VARCHAR(500)  NULL,
    landmark                  VARCHAR(200)  NULL,
    latitude                  DECIMAL(10,7) NULL,
    longitude                 DECIMAL(10,7) NULL,
    has_geo_tag               TINYINT(1)    NOT NULL DEFAULT 0,
    sitting_place             VARCHAR(100)  NULL COMMENT 'Clinic/Hospital/etc',
    property_name             VARCHAR(150)  NULL,
    star_rating               TINYINT UNSIGNED NOT NULL DEFAULT 0,
    visit_frequency           VARCHAR(50)   NULL,
    visit_days                SET('MON','TUE','WED','THU','FRI','SAT','SUN') NULL,
    preferred_visit_time      VARCHAR(100)  NULL,
    patients_per_day          SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    no_of_machines            SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    is_multi_location         TINYINT(1)    NOT NULL DEFAULT 0,
    is_pool_client            TINYINT(1)    NOT NULL DEFAULT 0,
    mapped_campaign_code      VARCHAR(100)  NULL,
    mobile_number_1           VARCHAR(15)   NULL,
    mobile_number_2           VARCHAR(15)   NULL,
    email                     VARCHAR(120)  NULL,
    contact_person_1_name     VARCHAR(150)  NULL,
    contact_person_1_mobile   VARCHAR(15)   NULL,
    contact_person_2_name     VARCHAR(150)  NULL,
    contact_person_2_mobile   VARCHAR(15)   NULL,
    gender                    ENUM('Male','Female','Other') NULL,
    date_of_birth             DATE          NULL,
    anniversary_date          DATE          NULL,
    spouse_date_of_birth      DATE          NULL,
    gst_number                VARCHAR(20)   NULL,
    pan_number                CHAR(10)      NULL,
    aadhaar_number            CHAR(12)      NULL,
    drug_license_no           VARCHAR(80)   NULL,
    medical_registration_no   VARCHAR(100)  NULL,
    photo_path                VARCHAR(255)  NULL,
    photo_thumbnail_path      VARCHAR(255)  NULL,
    remarks                   TEXT          NULL,
    extra_info_1              VARCHAR(255)  NULL,
    extra_info_2              VARCHAR(255)  NULL,
    extra_info_3              VARCHAR(255)  NULL,
    is_active                 TINYINT(1)    NOT NULL DEFAULT 1,
    is_delete_requested       TINYINT(1)    NOT NULL DEFAULT 0,
    delete_requested_by       VARCHAR(40)   NULL,
    delete_request_reason     VARCHAR(255)  NULL,
    is_approved               TINYINT(1)    NOT NULL DEFAULT 0,
    approved_by_emp_code      VARCHAR(40)   NULL,
    approved_on               DATETIME      NULL,
    has_fund_support          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by                VARCHAR(40)   NULL,
    updated_by                VARCHAR(40)   NULL,
    created_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_code (client_code),
    KEY idx_client_master_name (client_name),
    KEY idx_client_master_emp (mapped_emp_code),
    KEY idx_client_master_state (state_id),
    KEY idx_client_master_district (district_id),
    KEY idx_client_master_city (city_id),
    KEY idx_client_master_category (category_id),
    KEY idx_client_master_speciality (speciality_id),
    KEY idx_client_master_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- table created earlier from the plain DDL: add what is missing
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_requested_by');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_request_reason');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approved_on');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by_emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'degree_id');
SET @sql := IF(@t <> 'bigint unsigned', 'ALTER TABLE client_master MODIFY degree_id BIGINT UNSIGNED NULL', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- mapped_emp_code must match emp_detail.emp_id exactly for the FK
SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
SET @cur := (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'mapped_emp_code');
SET @sql := IF(@cur <> @co, CONCAT('ALTER TABLE client_master MODIFY mapped_emp_code VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_client_type');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_client_type FOREIGN KEY (client_type_id) REFERENCES client_type (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_category');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_category FOREIGN KEY (category_id) REFERENCES category_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_speciality');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_speciality FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_degree');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_degree FOREIGN KEY (degree_id) REFERENCES degree_master (id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_state');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_state FOREIGN KEY (state_id) REFERENCES state_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_district');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_district FOREIGN KEY (district_id) REFERENCES district (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_city');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_city FOREIGN KEY (city_id) REFERENCES city_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_emp');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_emp FOREIGN KEY (mapped_emp_code) REFERENCES emp_detail (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS client_master_child (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    client_id           BIGINT        NOT NULL,
    mapped_emp_code     VARCHAR(40)   NULL,
    instagram_handle    VARCHAR(150)  NULL,
    facebook_handle     VARCHAR(150)  NULL,
    twitter_handle      VARCHAR(150)  NULL,
    is_active           TINYINT(1)    NOT NULL DEFAULT 1,
    created_by          VARCHAR(40)   NULL,
    created_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_child_client (client_id),
    KEY idx_client_master_child_emp (mapped_emp_code),
    CONSTRAINT fk_cmc_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- one-time copy of the old `client` table (same id; masters only when they exist)
SET @has := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client');
SET @sql := IF(@has = 0, 'DO 0', 'INSERT INTO client_master (id, client_code, client_name, client_type_id, category_id, speciality_id, degree_id,
       city_id, state_id, address, mobile_number_1, email, date_of_birth, anniversary_date, drug_license_no, gst_number, remarks,
       property_name, is_active, is_approved, created_by, created_on)
SELECT c.oid, LEFT(c.client_code, 30), LEFT(TRIM(CONCAT(IFNULL(CONCAT(c.prefix, '' ''), ''''), c.name)), 150),
       (SELECT t.oid FROM client_type t WHERE t.oid = c.client_type_oid),
       (SELECT x.oid FROM category_master x WHERE x.oid = c.category_oid),
       (SELECT x.oid FROM speciality_master x WHERE x.oid = c.speciality_oid),
       (SELECT x.id FROM degree_master x WHERE x.id = c.degree_oid),
       (SELECT a.oid FROM city_master a WHERE a.oid = c.area_oid),
       (SELECT a.state_oid FROM city_master a JOIN state_master s ON s.oid = a.state_oid WHERE a.oid = c.area_oid),
       c.address, LEFT(c.mobile, 15), c.email, c.dob, c.anniversary, c.drug_license_no, LEFT(c.gst_no, 20), c.remarks,
       c.firm_name, IF(c.status = ''Y'', 1, 0), 1, ''import'', c.created_on
FROM client c
WHERE NOT EXISTS (SELECT 1 FROM client_master m WHERE m.id = c.oid OR m.client_code = LEFT(c.client_code, 30))');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('CLIENT_MASTER_SAVE', 'Client Master', 'Add / edit Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_STATUS', 'Client Master', 'Activate / deactivate Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_UPLOAD', 'Client Master', 'Bulk Excel upload of Client Master', 1, NOW(), 'seed');
-- approving clients / delete requests is for managers: default DENIED
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_MASTER_APPROVE', 'Client Master', 'Approve clients and their delete requests', 1, 0, NOW(), 'seed');

-- menu: next to Create Employee (else Master Entry, else Masters)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Client Master', 'Client Master', 'hospital',
       'clientmaster', 'master/client-master.html', '_SELF', 20, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'clientmaster');

-- ==================== initech_db ====================
USE initech_db;

CREATE TABLE IF NOT EXISTS client_master (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    client_code               VARCHAR(30)   NOT NULL,
    client_name               VARCHAR(150)  NOT NULL,
    client_type_id            BIGINT        NULL COMMENT '-> client_type.oid (Doctor / Chemist / Stockist ...)',
    category_id               BIGINT        NULL COMMENT '-> category_master.oid',
    sub_category              VARCHAR(100)  NULL,
    speciality_id             BIGINT        NULL COMMENT '-> speciality_master.oid',
    sub_speciality            VARCHAR(100)  NULL,
    degree_id                 BIGINT UNSIGNED NULL COMMENT '-> degree_master.id',
    qualification             VARCHAR(150)  NULL,
    designation               VARCHAR(100)  NULL,
    is_prescriber             TINYINT(1)    NOT NULL DEFAULT 0,
    experience_years          VARCHAR(100)  NULL,
    mapped_emp_code           VARCHAR(40)   NULL COMMENT '-> emp_detail.emp_id (field employee)',
    state_id                  BIGINT        NULL COMMENT '-> state_master.oid',
    district_id               BIGINT        NULL COMMENT '-> district.oid',
    city_id                   BIGINT        NULL COMMENT '-> city_master.oid (Area master)',
    locality_type             ENUM('Urban','Semi-Urban','Rural') NULL,
    pin_code                  VARCHAR(10)   NULL,
    address                   VARCHAR(500)  NULL,
    landmark                  VARCHAR(200)  NULL,
    latitude                  DECIMAL(10,7) NULL,
    longitude                 DECIMAL(10,7) NULL,
    has_geo_tag               TINYINT(1)    NOT NULL DEFAULT 0,
    sitting_place             VARCHAR(100)  NULL COMMENT 'Clinic/Hospital/etc',
    property_name             VARCHAR(150)  NULL,
    star_rating               TINYINT UNSIGNED NOT NULL DEFAULT 0,
    visit_frequency           VARCHAR(50)   NULL,
    visit_days                SET('MON','TUE','WED','THU','FRI','SAT','SUN') NULL,
    preferred_visit_time      VARCHAR(100)  NULL,
    patients_per_day          SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    no_of_machines            SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    is_multi_location         TINYINT(1)    NOT NULL DEFAULT 0,
    is_pool_client            TINYINT(1)    NOT NULL DEFAULT 0,
    mapped_campaign_code      VARCHAR(100)  NULL,
    mobile_number_1           VARCHAR(15)   NULL,
    mobile_number_2           VARCHAR(15)   NULL,
    email                     VARCHAR(120)  NULL,
    contact_person_1_name     VARCHAR(150)  NULL,
    contact_person_1_mobile   VARCHAR(15)   NULL,
    contact_person_2_name     VARCHAR(150)  NULL,
    contact_person_2_mobile   VARCHAR(15)   NULL,
    gender                    ENUM('Male','Female','Other') NULL,
    date_of_birth             DATE          NULL,
    anniversary_date          DATE          NULL,
    spouse_date_of_birth      DATE          NULL,
    gst_number                VARCHAR(20)   NULL,
    pan_number                CHAR(10)      NULL,
    aadhaar_number            CHAR(12)      NULL,
    drug_license_no           VARCHAR(80)   NULL,
    medical_registration_no   VARCHAR(100)  NULL,
    photo_path                VARCHAR(255)  NULL,
    photo_thumbnail_path      VARCHAR(255)  NULL,
    remarks                   TEXT          NULL,
    extra_info_1              VARCHAR(255)  NULL,
    extra_info_2              VARCHAR(255)  NULL,
    extra_info_3              VARCHAR(255)  NULL,
    is_active                 TINYINT(1)    NOT NULL DEFAULT 1,
    is_delete_requested       TINYINT(1)    NOT NULL DEFAULT 0,
    delete_requested_by       VARCHAR(40)   NULL,
    delete_request_reason     VARCHAR(255)  NULL,
    is_approved               TINYINT(1)    NOT NULL DEFAULT 0,
    approved_by_emp_code      VARCHAR(40)   NULL,
    approved_on               DATETIME      NULL,
    has_fund_support          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by                VARCHAR(40)   NULL,
    updated_by                VARCHAR(40)   NULL,
    created_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_code (client_code),
    KEY idx_client_master_name (client_name),
    KEY idx_client_master_emp (mapped_emp_code),
    KEY idx_client_master_state (state_id),
    KEY idx_client_master_district (district_id),
    KEY idx_client_master_city (city_id),
    KEY idx_client_master_category (category_id),
    KEY idx_client_master_speciality (speciality_id),
    KEY idx_client_master_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- table created earlier from the plain DDL: add what is missing
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_requested_by');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_request_reason');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approved_on');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by_emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'degree_id');
SET @sql := IF(@t <> 'bigint unsigned', 'ALTER TABLE client_master MODIFY degree_id BIGINT UNSIGNED NULL', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- mapped_emp_code must match emp_detail.emp_id exactly for the FK
SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
SET @cur := (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'mapped_emp_code');
SET @sql := IF(@cur <> @co, CONCAT('ALTER TABLE client_master MODIFY mapped_emp_code VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_client_type');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_client_type FOREIGN KEY (client_type_id) REFERENCES client_type (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_category');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_category FOREIGN KEY (category_id) REFERENCES category_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_speciality');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_speciality FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_degree');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_degree FOREIGN KEY (degree_id) REFERENCES degree_master (id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_state');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_state FOREIGN KEY (state_id) REFERENCES state_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_district');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_district FOREIGN KEY (district_id) REFERENCES district (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_city');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_city FOREIGN KEY (city_id) REFERENCES city_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_emp');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_emp FOREIGN KEY (mapped_emp_code) REFERENCES emp_detail (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS client_master_child (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    client_id           BIGINT        NOT NULL,
    mapped_emp_code     VARCHAR(40)   NULL,
    instagram_handle    VARCHAR(150)  NULL,
    facebook_handle     VARCHAR(150)  NULL,
    twitter_handle      VARCHAR(150)  NULL,
    is_active           TINYINT(1)    NOT NULL DEFAULT 1,
    created_by          VARCHAR(40)   NULL,
    created_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_child_client (client_id),
    KEY idx_client_master_child_emp (mapped_emp_code),
    CONSTRAINT fk_cmc_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- one-time copy of the old `client` table (same id; masters only when they exist)
SET @has := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client');
SET @sql := IF(@has = 0, 'DO 0', 'INSERT INTO client_master (id, client_code, client_name, client_type_id, category_id, speciality_id, degree_id,
       city_id, state_id, address, mobile_number_1, email, date_of_birth, anniversary_date, drug_license_no, gst_number, remarks,
       property_name, is_active, is_approved, created_by, created_on)
SELECT c.oid, LEFT(c.client_code, 30), LEFT(TRIM(CONCAT(IFNULL(CONCAT(c.prefix, '' ''), ''''), c.name)), 150),
       (SELECT t.oid FROM client_type t WHERE t.oid = c.client_type_oid),
       (SELECT x.oid FROM category_master x WHERE x.oid = c.category_oid),
       (SELECT x.oid FROM speciality_master x WHERE x.oid = c.speciality_oid),
       (SELECT x.id FROM degree_master x WHERE x.id = c.degree_oid),
       (SELECT a.oid FROM city_master a WHERE a.oid = c.area_oid),
       (SELECT a.state_oid FROM city_master a JOIN state_master s ON s.oid = a.state_oid WHERE a.oid = c.area_oid),
       c.address, LEFT(c.mobile, 15), c.email, c.dob, c.anniversary, c.drug_license_no, LEFT(c.gst_no, 20), c.remarks,
       c.firm_name, IF(c.status = ''Y'', 1, 0), 1, ''import'', c.created_on
FROM client c
WHERE NOT EXISTS (SELECT 1 FROM client_master m WHERE m.id = c.oid OR m.client_code = LEFT(c.client_code, 30))');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('CLIENT_MASTER_SAVE', 'Client Master', 'Add / edit Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_STATUS', 'Client Master', 'Activate / deactivate Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_UPLOAD', 'Client Master', 'Bulk Excel upload of Client Master', 1, NOW(), 'seed');
-- approving clients / delete requests is for managers: default DENIED
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_MASTER_APPROVE', 'Client Master', 'Approve clients and their delete requests', 1, 0, NOW(), 'seed');

-- menu: next to Create Employee (else Master Entry, else Masters)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Client Master', 'Client Master', 'hospital',
       'clientmaster', 'master/client-master.html', '_SELF', 20, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'clientmaster');

-- ==================== sfa_demo ====================
USE sfa_demo;

CREATE TABLE IF NOT EXISTS client_master (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    client_code               VARCHAR(30)   NOT NULL,
    client_name               VARCHAR(150)  NOT NULL,
    client_type_id            BIGINT        NULL COMMENT '-> client_type.oid (Doctor / Chemist / Stockist ...)',
    category_id               BIGINT        NULL COMMENT '-> category_master.oid',
    sub_category              VARCHAR(100)  NULL,
    speciality_id             BIGINT        NULL COMMENT '-> speciality_master.oid',
    sub_speciality            VARCHAR(100)  NULL,
    degree_id                 BIGINT UNSIGNED NULL COMMENT '-> degree_master.id',
    qualification             VARCHAR(150)  NULL,
    designation               VARCHAR(100)  NULL,
    is_prescriber             TINYINT(1)    NOT NULL DEFAULT 0,
    experience_years          VARCHAR(100)  NULL,
    mapped_emp_code           VARCHAR(40)   NULL COMMENT '-> emp_detail.emp_id (field employee)',
    state_id                  BIGINT        NULL COMMENT '-> state_master.oid',
    district_id               BIGINT        NULL COMMENT '-> district.oid',
    city_id                   BIGINT        NULL COMMENT '-> city_master.oid (Area master)',
    locality_type             ENUM('Urban','Semi-Urban','Rural') NULL,
    pin_code                  VARCHAR(10)   NULL,
    address                   VARCHAR(500)  NULL,
    landmark                  VARCHAR(200)  NULL,
    latitude                  DECIMAL(10,7) NULL,
    longitude                 DECIMAL(10,7) NULL,
    has_geo_tag               TINYINT(1)    NOT NULL DEFAULT 0,
    sitting_place             VARCHAR(100)  NULL COMMENT 'Clinic/Hospital/etc',
    property_name             VARCHAR(150)  NULL,
    star_rating               TINYINT UNSIGNED NOT NULL DEFAULT 0,
    visit_frequency           VARCHAR(50)   NULL,
    visit_days                SET('MON','TUE','WED','THU','FRI','SAT','SUN') NULL,
    preferred_visit_time      VARCHAR(100)  NULL,
    patients_per_day          SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    no_of_machines            SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    is_multi_location         TINYINT(1)    NOT NULL DEFAULT 0,
    is_pool_client            TINYINT(1)    NOT NULL DEFAULT 0,
    mapped_campaign_code      VARCHAR(100)  NULL,
    mobile_number_1           VARCHAR(15)   NULL,
    mobile_number_2           VARCHAR(15)   NULL,
    email                     VARCHAR(120)  NULL,
    contact_person_1_name     VARCHAR(150)  NULL,
    contact_person_1_mobile   VARCHAR(15)   NULL,
    contact_person_2_name     VARCHAR(150)  NULL,
    contact_person_2_mobile   VARCHAR(15)   NULL,
    gender                    ENUM('Male','Female','Other') NULL,
    date_of_birth             DATE          NULL,
    anniversary_date          DATE          NULL,
    spouse_date_of_birth      DATE          NULL,
    gst_number                VARCHAR(20)   NULL,
    pan_number                CHAR(10)      NULL,
    aadhaar_number            CHAR(12)      NULL,
    drug_license_no           VARCHAR(80)   NULL,
    medical_registration_no   VARCHAR(100)  NULL,
    photo_path                VARCHAR(255)  NULL,
    photo_thumbnail_path      VARCHAR(255)  NULL,
    remarks                   TEXT          NULL,
    extra_info_1              VARCHAR(255)  NULL,
    extra_info_2              VARCHAR(255)  NULL,
    extra_info_3              VARCHAR(255)  NULL,
    is_active                 TINYINT(1)    NOT NULL DEFAULT 1,
    is_delete_requested       TINYINT(1)    NOT NULL DEFAULT 0,
    delete_requested_by       VARCHAR(40)   NULL,
    delete_request_reason     VARCHAR(255)  NULL,
    is_approved               TINYINT(1)    NOT NULL DEFAULT 0,
    approved_by_emp_code      VARCHAR(40)   NULL,
    approved_on               DATETIME      NULL,
    has_fund_support          TINYINT(1)    NOT NULL DEFAULT 0,
    created_by                VARCHAR(40)   NULL,
    updated_by                VARCHAR(40)   NULL,
    created_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_code (client_code),
    KEY idx_client_master_name (client_name),
    KEY idx_client_master_emp (mapped_emp_code),
    KEY idx_client_master_state (state_id),
    KEY idx_client_master_district (district_id),
    KEY idx_client_master_city (city_id),
    KEY idx_client_master_category (category_id),
    KEY idx_client_master_speciality (speciality_id),
    KEY idx_client_master_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- table created earlier from the plain DDL: add what is missing
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'photo_thumbnail_path');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN photo_thumbnail_path VARCHAR(255) NULL AFTER photo_path', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_requested_by');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_requested_by VARCHAR(40) NULL AFTER is_delete_requested', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'delete_request_reason');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN delete_request_reason VARCHAR(255) NULL AFTER delete_requested_by', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'approved_on');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD COLUMN approved_on DATETIME NULL AFTER approved_by_emp_code', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @t := (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'degree_id');
SET @sql := IF(@t <> 'bigint unsigned', 'ALTER TABLE client_master MODIFY degree_id BIGINT UNSIGNED NULL', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- mapped_emp_code must match emp_detail.emp_id exactly for the FK
SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @cs, @co FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emp_detail' AND COLUMN_NAME = 'emp_id';
SET @cur := (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND COLUMN_NAME = 'mapped_emp_code');
SET @sql := IF(@cur <> @co, CONCAT('ALTER TABLE client_master MODIFY mapped_emp_code VARCHAR(40) CHARACTER SET ', @cs, ' COLLATE ', @co, ' NULL'), 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_client_type');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_client_type FOREIGN KEY (client_type_id) REFERENCES client_type (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_category');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_category FOREIGN KEY (category_id) REFERENCES category_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_speciality');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_speciality FOREIGN KEY (speciality_id) REFERENCES speciality_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_degree');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_degree FOREIGN KEY (degree_id) REFERENCES degree_master (id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_state');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_state FOREIGN KEY (state_id) REFERENCES state_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_district');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_district FOREIGN KEY (district_id) REFERENCES district (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_city');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_city FOREIGN KEY (city_id) REFERENCES city_master (oid)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client_master' AND CONSTRAINT_NAME = 'fk_cm_emp');
SET @sql := IF(@has = 0, 'ALTER TABLE client_master ADD CONSTRAINT fk_cm_emp FOREIGN KEY (mapped_emp_code) REFERENCES emp_detail (emp_id)', 'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS client_master_child (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    client_id           BIGINT        NOT NULL,
    mapped_emp_code     VARCHAR(40)   NULL,
    instagram_handle    VARCHAR(150)  NULL,
    facebook_handle     VARCHAR(150)  NULL,
    twitter_handle      VARCHAR(150)  NULL,
    is_active           TINYINT(1)    NOT NULL DEFAULT 1,
    created_by          VARCHAR(40)   NULL,
    created_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_client_master_child_client (client_id),
    KEY idx_client_master_child_emp (mapped_emp_code),
    CONSTRAINT fk_cmc_client FOREIGN KEY (client_id) REFERENCES client_master (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- one-time copy of the old `client` table (same id; masters only when they exist)
SET @has := (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client');
SET @sql := IF(@has = 0, 'DO 0', 'INSERT INTO client_master (id, client_code, client_name, client_type_id, category_id, speciality_id, degree_id,
       city_id, state_id, address, mobile_number_1, email, date_of_birth, anniversary_date, drug_license_no, gst_number, remarks,
       property_name, is_active, is_approved, created_by, created_on)
SELECT c.oid, LEFT(c.client_code, 30), LEFT(TRIM(CONCAT(IFNULL(CONCAT(c.prefix, '' ''), ''''), c.name)), 150),
       (SELECT t.oid FROM client_type t WHERE t.oid = c.client_type_oid),
       (SELECT x.oid FROM category_master x WHERE x.oid = c.category_oid),
       (SELECT x.oid FROM speciality_master x WHERE x.oid = c.speciality_oid),
       (SELECT x.id FROM degree_master x WHERE x.id = c.degree_oid),
       (SELECT a.oid FROM city_master a WHERE a.oid = c.area_oid),
       (SELECT a.state_oid FROM city_master a JOIN state_master s ON s.oid = a.state_oid WHERE a.oid = c.area_oid),
       c.address, LEFT(c.mobile, 15), c.email, c.dob, c.anniversary, c.drug_license_no, LEFT(c.gst_no, 20), c.remarks,
       c.firm_name, IF(c.status = ''Y'', 1, 0), 1, ''import'', c.created_on
FROM client c
WHERE NOT EXISTS (SELECT 1 FROM client_master m WHERE m.id = c.oid OR m.client_code = LEFT(c.client_code, 30))');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('CLIENT_MASTER_SAVE', 'Client Master', 'Add / edit Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_STATUS', 'Client Master', 'Activate / deactivate Client Master', 1, NOW(), 'seed'),
    ('CLIENT_MASTER_UPLOAD', 'Client Master', 'Bulk Excel upload of Client Master', 1, NOW(), 'seed');
-- approving clients / delete requests is for managers: default DENIED
INSERT IGNORE INTO permission_master (permission_code, module, description, status, default_allowed, created_at, created_by) VALUES
    ('CLIENT_MASTER_APPROVE', 'Client Master', 'Approve clients and their delete requests', 1, 0, NOW(), 'seed');

-- menu: next to Create Employee (else Master Entry, else Masters)
SET @parent := COALESCE(
    (SELECT parent_id FROM company_menu_self WHERE href = 'master/create-employee.html' AND parent_id IS NOT NULL LIMIT 1),
    (SELECT id FROM company_menu_self WHERE label = 'Master Entry' LIMIT 1),
    2);
INSERT INTO company_menu_self
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT nx.next_id, @parent, p.emp_id, 'WEB', 'Client Master', 'Client Master', 'hospital',
       'clientmaster', 'master/client-master.html', '_SELF', 20, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @parent LIMIT 1) p
CROSS JOIN (SELECT MAX(id) + 1 AS next_id FROM company_menu_self) nx
WHERE NOT EXISTS (SELECT 1 FROM company_menu_self WHERE page = 'clientmaster');
