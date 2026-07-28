-- ===========================================================================
-- Profiles & branding.
--   COMMON  : app_branding      -> app-wide DEFAULT logo (single row id=1)
--   TENANT  : company_profile   -> per-tenant company details + COMPANY logo
--             employee.photo     -> per-user profile photo
--             menu rows 211/212  -> Branding + My Profile (under Preferences)
-- Images are stored as base64 data URIs (MEDIUMTEXT). Re-runnable.
--
-- Run:  mysql -u myroot < src/main/resources/sql/13_profile_branding.sql
-- ===========================================================================

-- ---- app-wide branding (default logo) lives in the COMMON db ----
USE sfa_central;
CREATE TABLE IF NOT EXISTS app_branding (
    id           INT          NOT NULL PRIMARY KEY,      -- always 1 (single row)
    app_name     VARCHAR(128) NOT NULL DEFAULT 'StarSFA',
    default_logo MEDIUMTEXT   NULL,                      -- base64 data URI
    updated_at   DATETIME     NULL,
    updated_by   VARCHAR(128) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO app_branding (id, app_name) VALUES (1, 'StarSFA')
ON DUPLICATE KEY UPDATE app_name = app_name;

-- ==================== acme_db ====================
USE acme_db;

CREATE TABLE IF NOT EXISTS company_profile (
    id           INT          NOT NULL PRIMARY KEY,
    company_name VARCHAR(160) NULL,
    address      VARCHAR(255) NULL,
    city         VARCHAR(96)  NULL,
    phone        VARCHAR(40)  NULL,
    email        VARCHAR(128) NULL,
    website      VARCHAR(128) NULL,
    logo         MEDIUMTEXT   NULL,
    updated_at   DATETIME     NULL,
    updated_by   VARCHAR(128) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO company_profile (id, company_name) VALUES (1, 'Acme Pharma')
ON DUPLICATE KEY UPDATE company_name = company_name;

-- profile photo on the employee (idempotent add)
SET @has_photo := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'photo');
SET @ddl := IF(@has_photo = 0, 'ALTER TABLE employee ADD COLUMN photo MEDIUMTEXT NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- menu items under Administration > Preferences (id 21)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (211, 21, 'Branding',   'Branding & Logos', 'image',         'branding', 'settings/branding.html', 20, NULL, 1),
    (212, 21, 'My Profile', 'My Profile',       'person-circle', 'profile',  'settings/profile.html',  30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== globex_db ====================
USE globex_db;

CREATE TABLE IF NOT EXISTS company_profile (
    id           INT          NOT NULL PRIMARY KEY,
    company_name VARCHAR(160) NULL,
    address      VARCHAR(255) NULL,
    city         VARCHAR(96)  NULL,
    phone        VARCHAR(40)  NULL,
    email        VARCHAR(128) NULL,
    website      VARCHAR(128) NULL,
    logo         MEDIUMTEXT   NULL,
    updated_at   DATETIME     NULL,
    updated_by   VARCHAR(128) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO company_profile (id, company_name) VALUES (1, 'Globex Pharma')
ON DUPLICATE KEY UPDATE company_name = company_name;

-- profile photo on the employee (idempotent add)
SET @has_photo := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'photo');
SET @ddl := IF(@has_photo = 0, 'ALTER TABLE employee ADD COLUMN photo MEDIUMTEXT NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- menu items under Administration > Preferences (id 21)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (211, 21, 'Branding',   'Branding & Logos', 'image',         'branding', 'settings/branding.html', 20, NULL, 1),
    (212, 21, 'My Profile', 'My Profile',       'person-circle', 'profile',  'settings/profile.html',  30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== initech_db ====================
USE initech_db;

CREATE TABLE IF NOT EXISTS company_profile (
    id           INT          NOT NULL PRIMARY KEY,
    company_name VARCHAR(160) NULL,
    address      VARCHAR(255) NULL,
    city         VARCHAR(96)  NULL,
    phone        VARCHAR(40)  NULL,
    email        VARCHAR(128) NULL,
    website      VARCHAR(128) NULL,
    logo         MEDIUMTEXT   NULL,
    updated_at   DATETIME     NULL,
    updated_by   VARCHAR(128) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO company_profile (id, company_name) VALUES (1, 'Initech Pharma')
ON DUPLICATE KEY UPDATE company_name = company_name;

-- profile photo on the employee (idempotent add)
SET @has_photo := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'photo');
SET @ddl := IF(@has_photo = 0, 'ALTER TABLE employee ADD COLUMN photo MEDIUMTEXT NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- menu items under Administration > Preferences (id 21)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (211, 21, 'Branding',   'Branding & Logos', 'image',         'branding', 'settings/branding.html', 20, NULL, 1),
    (212, 21, 'My Profile', 'My Profile',       'person-circle', 'profile',  'settings/profile.html',  30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== sfa_demo ====================
USE sfa_demo;

CREATE TABLE IF NOT EXISTS company_profile (
    id           INT          NOT NULL PRIMARY KEY,
    company_name VARCHAR(160) NULL,
    address      VARCHAR(255) NULL,
    city         VARCHAR(96)  NULL,
    phone        VARCHAR(40)  NULL,
    email        VARCHAR(128) NULL,
    website      VARCHAR(128) NULL,
    logo         MEDIUMTEXT   NULL,
    updated_at   DATETIME     NULL,
    updated_by   VARCHAR(128) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO company_profile (id, company_name) VALUES (1, 'Demo Pharma')
ON DUPLICATE KEY UPDATE company_name = company_name;

-- profile photo on the employee (idempotent add)
SET @has_photo := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'employee' AND column_name = 'photo');
SET @ddl := IF(@has_photo = 0, 'ALTER TABLE employee ADD COLUMN photo MEDIUMTEXT NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- menu items under Administration > Preferences (id 21)
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (211, 21, 'Branding',   'Branding & Logos', 'image',         'branding', 'settings/branding.html', 20, NULL, 1),
    (212, 21, 'My Profile', 'My Profile',       'person-circle', 'profile',  'settings/profile.html',  30, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

