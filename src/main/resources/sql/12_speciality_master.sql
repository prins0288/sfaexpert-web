-- ===========================================================================
-- speciality_master — production-style master (PER TENANT db), same shape as
-- category_master: TINYINT status (1/0), icon, and audit columns; dates
-- serialize to the client as dd-MM-yyyy. Also registers the "Speciality"
-- menu item under Masters > Catalog.
--
-- Run:  mysql -u myroot < src/main/resources/sql/12_speciality_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
CREATE TABLE IF NOT EXISTS speciality_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    speciality_code VARCHAR(64)  NOT NULL,
    speciality_name VARCHAR(128) NOT NULL,
    description     VARCHAR(255) NULL,
    icon            VARCHAR(128) NULL,                     -- bootstrap-icon name or image URL
    status          TINYINT(1)   NOT NULL DEFAULT 1,       -- 1 = active (true), 0 = inactive (false)
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_speciality_master_code (speciality_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO speciality_master
    (speciality_code, speciality_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('SPEC-CARD',  'Cardiology',       'Heart specialists',      'heart-pulse',       1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-ORTHO', 'Orthopedics',      'Bone & joint',           'bandaid',           1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-NEURO', 'Neurology',        'Nervous system',         'activity',          1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-GEN',   'General Medicine', 'General practice',       'clipboard2-pulse',  1, NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    speciality_name=VALUES(speciality_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Speciality" under Masters > Catalog (id 12), after Category (121)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (122, 12, 'Speciality', 'Speciality Master', 'clipboard2-pulse', 'specialitymaster', 'master/speciality-master.html', 30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== globex_db ====================
USE globex_db;
CREATE TABLE IF NOT EXISTS speciality_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    speciality_code VARCHAR(64)  NOT NULL,
    speciality_name VARCHAR(128) NOT NULL,
    description     VARCHAR(255) NULL,
    icon            VARCHAR(128) NULL,                     -- bootstrap-icon name or image URL
    status          TINYINT(1)   NOT NULL DEFAULT 1,       -- 1 = active (true), 0 = inactive (false)
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_speciality_master_code (speciality_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO speciality_master
    (speciality_code, speciality_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('SPEC-CARD',  'Cardiology',       'Heart specialists',      'heart-pulse',       1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-ORTHO', 'Orthopedics',      'Bone & joint',           'bandaid',           1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-NEURO', 'Neurology',        'Nervous system',         'activity',          1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-GEN',   'General Medicine', 'General practice',       'clipboard2-pulse',  1, NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    speciality_name=VALUES(speciality_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Speciality" under Masters > Catalog (id 12), after Category (121)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (122, 12, 'Speciality', 'Speciality Master', 'clipboard2-pulse', 'specialitymaster', 'master/speciality-master.html', 30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== initech_db ====================
USE initech_db;
CREATE TABLE IF NOT EXISTS speciality_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    speciality_code VARCHAR(64)  NOT NULL,
    speciality_name VARCHAR(128) NOT NULL,
    description     VARCHAR(255) NULL,
    icon            VARCHAR(128) NULL,                     -- bootstrap-icon name or image URL
    status          TINYINT(1)   NOT NULL DEFAULT 1,       -- 1 = active (true), 0 = inactive (false)
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_speciality_master_code (speciality_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO speciality_master
    (speciality_code, speciality_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('SPEC-CARD',  'Cardiology',       'Heart specialists',      'heart-pulse',       1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-ORTHO', 'Orthopedics',      'Bone & joint',           'bandaid',           1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-NEURO', 'Neurology',        'Nervous system',         'activity',          1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-GEN',   'General Medicine', 'General practice',       'clipboard2-pulse',  1, NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    speciality_name=VALUES(speciality_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Speciality" under Masters > Catalog (id 12), after Category (121)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (122, 12, 'Speciality', 'Speciality Master', 'clipboard2-pulse', 'specialitymaster', 'master/speciality-master.html', 30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== sfa_demo ====================
USE sfa_demo;
CREATE TABLE IF NOT EXISTS speciality_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    speciality_code VARCHAR(64)  NOT NULL,
    speciality_name VARCHAR(128) NOT NULL,
    description     VARCHAR(255) NULL,
    icon            VARCHAR(128) NULL,                     -- bootstrap-icon name or image URL
    status          TINYINT(1)   NOT NULL DEFAULT 1,       -- 1 = active (true), 0 = inactive (false)
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_speciality_master_code (speciality_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO speciality_master
    (speciality_code, speciality_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('SPEC-CARD',  'Cardiology',       'Heart specialists',      'heart-pulse',       1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-ORTHO', 'Orthopedics',      'Bone & joint',           'bandaid',           1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-NEURO', 'Neurology',        'Nervous system',         'activity',          1, NOW(), 'system', NOW(), 'system'),
    ('SPEC-GEN',   'General Medicine', 'General practice',       'clipboard2-pulse',  1, NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    speciality_name=VALUES(speciality_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Speciality" under Masters > Catalog (id 12), after Category (121)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (122, 12, 'Speciality', 'Speciality Master', 'clipboard2-pulse', 'specialitymaster', 'master/speciality-master.html', 30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

