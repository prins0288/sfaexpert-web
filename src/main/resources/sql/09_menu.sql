-- ===========================================================================
-- DB-driven, role-based navigation — stored PER TENANT (each tenant DB owns its
-- own menu). Tables:
--   menu_item          -> the 3-level menu as data. `label` = nav text,
--                         `title` = the report/page title shown in the header and
--                         breadcrumb (edit it here and the title updates live).
--   user_menu_favorite -> per-user starred items ("Favorites" group).
-- `roles` is a CSV allow-list; NULL/blank = all roles.
-- Run:  mysql -u myroot < src/main/resources/sql/09_menu.sql
-- ===========================================================================

-- These tables used to live in the common DB; drop those copies.
USE sfa_central;
DROP TABLE IF EXISTS user_menu_favorite;
DROP TABLE IF EXISTS menu_item;

-- ==================== acme_db ====================
USE acme_db;
CREATE TABLE IF NOT EXISTS menu_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NULL,
    label      VARCHAR(128) NOT NULL,          -- nav text (short)
    title      VARCHAR(160) NULL,              -- report/page title (falls back to label)
    icon       VARCHAR(64)  NULL,              -- bootstrap-icon name, no "bi-" prefix
    logo       VARCHAR(256) NULL,              -- optional image URL instead of/with icon
    page       VARCHAR(64)  NULL,              -- active-highlight key (matches data-page)
    href       VARCHAR(256) NULL,              -- link; NULL/blank => branch node
    sort_order INT          NOT NULL DEFAULT 0,
    roles      VARCHAR(256) NULL,              -- CSV allow-list; NULL = all roles
    enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- add `title` to tables created before this column existed (idempotent)
SET @has_title := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'menu_item' AND column_name = 'title');
SET @ddl := IF(@has_title = 0,
    'ALTER TABLE menu_item ADD COLUMN title VARCHAR(160) NULL AFTER label', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS user_menu_favorite (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(128) NOT NULL,
    menu_item_id BIGINT       NOT NULL,
    UNIQUE KEY uq_user_menu_favorite (username, menu_item_id),
    KEY idx_umf_user (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
  (1, NULL, 'Dashboard', 'Dashboard', 'speedometer2', 'dashboard', 'dashboard.html', 10, NULL, 1),
  (2, NULL, 'Masters', NULL, 'collection', NULL, NULL, 20, NULL, 1),
  (3, NULL, 'Administration', NULL, 'shield-lock', NULL, NULL, 30, NULL, 1),
  (10, 2, 'Geography', NULL, 'geo', NULL, NULL, 10, NULL, 1),
  (11, 2, 'Parties', NULL, 'people', NULL, NULL, 20, NULL, 1),
  (12, 2, 'Catalog', NULL, 'box-seam', NULL, NULL, 30, NULL, 1),
  (20, 3, 'People', NULL, 'person-badge', NULL, NULL, 10, 'ADMIN,MANAGER', 1),
  (21, 3, 'Preferences', NULL, 'sliders', NULL, NULL, 20, NULL, 1),
  (100, 10, 'Area', 'Area Master', 'geo-alt', 'area', 'master/area.html', 10, NULL, 1),
  (101, 10, 'Route', 'Route Master', 'signpost-2', 'route', 'master/route.html', 20, NULL, 1),
  (102, 10, 'Route-Area Map', 'Route-Area Mapping', 'diagram-3', 'routearea', 'master/route-area.html', 30, NULL, 1),
  (110, 11, 'Client Type', 'Client Type Master', 'tags', 'clienttype', 'master/client-type.html', 10, NULL, 1),
  (111, 11, 'Client', 'Client Master', 'person-vcard', 'client', 'master/client.html', 20, NULL, 1),
  (120, 12, 'Products', 'Products Master', 'boxes', 'products', 'master/products.html', 10, NULL, 1),
  (200, 20, 'Employee', 'Employee Master', 'person-workspace', 'employee', 'master/employee.html', 10, 'ADMIN,MANAGER', 1),
  (210, 21, 'Appearance', 'Appearance', 'palette', 'appearance', 'settings/appearance.html', 10, NULL, 1)
ON DUPLICATE KEY UPDATE
  parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
  page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== globex_db ====================
USE globex_db;
CREATE TABLE IF NOT EXISTS menu_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NULL,
    label      VARCHAR(128) NOT NULL,          -- nav text (short)
    title      VARCHAR(160) NULL,              -- report/page title (falls back to label)
    icon       VARCHAR(64)  NULL,              -- bootstrap-icon name, no "bi-" prefix
    logo       VARCHAR(256) NULL,              -- optional image URL instead of/with icon
    page       VARCHAR(64)  NULL,              -- active-highlight key (matches data-page)
    href       VARCHAR(256) NULL,              -- link; NULL/blank => branch node
    sort_order INT          NOT NULL DEFAULT 0,
    roles      VARCHAR(256) NULL,              -- CSV allow-list; NULL = all roles
    enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- add `title` to tables created before this column existed (idempotent)
SET @has_title := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'menu_item' AND column_name = 'title');
SET @ddl := IF(@has_title = 0,
    'ALTER TABLE menu_item ADD COLUMN title VARCHAR(160) NULL AFTER label', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS user_menu_favorite (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(128) NOT NULL,
    menu_item_id BIGINT       NOT NULL,
    UNIQUE KEY uq_user_menu_favorite (username, menu_item_id),
    KEY idx_umf_user (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
  (1, NULL, 'Dashboard', 'Dashboard', 'speedometer2', 'dashboard', 'dashboard.html', 10, NULL, 1),
  (2, NULL, 'Masters', NULL, 'collection', NULL, NULL, 20, NULL, 1),
  (3, NULL, 'Administration', NULL, 'shield-lock', NULL, NULL, 30, NULL, 1),
  (10, 2, 'Geography', NULL, 'geo', NULL, NULL, 10, NULL, 1),
  (11, 2, 'Parties', NULL, 'people', NULL, NULL, 20, NULL, 1),
  (12, 2, 'Catalog', NULL, 'box-seam', NULL, NULL, 30, NULL, 1),
  (20, 3, 'People', NULL, 'person-badge', NULL, NULL, 10, 'ADMIN,MANAGER', 1),
  (21, 3, 'Preferences', NULL, 'sliders', NULL, NULL, 20, NULL, 1),
  (100, 10, 'Area', 'Area Master', 'geo-alt', 'area', 'master/area.html', 10, NULL, 1),
  (101, 10, 'Route', 'Route Master', 'signpost-2', 'route', 'master/route.html', 20, NULL, 1),
  (102, 10, 'Route-Area Map', 'Route-Area Mapping', 'diagram-3', 'routearea', 'master/route-area.html', 30, NULL, 1),
  (110, 11, 'Client Type', 'Client Type Master', 'tags', 'clienttype', 'master/client-type.html', 10, NULL, 1),
  (111, 11, 'Client', 'Client Master', 'person-vcard', 'client', 'master/client.html', 20, NULL, 1),
  (120, 12, 'Products', 'Products Master', 'boxes', 'products', 'master/products.html', 10, NULL, 1),
  (200, 20, 'Employee', 'Employee Master', 'person-workspace', 'employee', 'master/employee.html', 10, 'ADMIN,MANAGER', 1),
  (210, 21, 'Appearance', 'Appearance', 'palette', 'appearance', 'settings/appearance.html', 10, NULL, 1)
ON DUPLICATE KEY UPDATE
  parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
  page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== initech_db ====================
USE initech_db;
CREATE TABLE IF NOT EXISTS menu_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NULL,
    label      VARCHAR(128) NOT NULL,          -- nav text (short)
    title      VARCHAR(160) NULL,              -- report/page title (falls back to label)
    icon       VARCHAR(64)  NULL,              -- bootstrap-icon name, no "bi-" prefix
    logo       VARCHAR(256) NULL,              -- optional image URL instead of/with icon
    page       VARCHAR(64)  NULL,              -- active-highlight key (matches data-page)
    href       VARCHAR(256) NULL,              -- link; NULL/blank => branch node
    sort_order INT          NOT NULL DEFAULT 0,
    roles      VARCHAR(256) NULL,              -- CSV allow-list; NULL = all roles
    enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- add `title` to tables created before this column existed (idempotent)
SET @has_title := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'menu_item' AND column_name = 'title');
SET @ddl := IF(@has_title = 0,
    'ALTER TABLE menu_item ADD COLUMN title VARCHAR(160) NULL AFTER label', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS user_menu_favorite (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(128) NOT NULL,
    menu_item_id BIGINT       NOT NULL,
    UNIQUE KEY uq_user_menu_favorite (username, menu_item_id),
    KEY idx_umf_user (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
  (1, NULL, 'Dashboard', 'Dashboard', 'speedometer2', 'dashboard', 'dashboard.html', 10, NULL, 1),
  (2, NULL, 'Masters', NULL, 'collection', NULL, NULL, 20, NULL, 1),
  (3, NULL, 'Administration', NULL, 'shield-lock', NULL, NULL, 30, NULL, 1),
  (10, 2, 'Geography', NULL, 'geo', NULL, NULL, 10, NULL, 1),
  (11, 2, 'Parties', NULL, 'people', NULL, NULL, 20, NULL, 1),
  (12, 2, 'Catalog', NULL, 'box-seam', NULL, NULL, 30, NULL, 1),
  (20, 3, 'People', NULL, 'person-badge', NULL, NULL, 10, 'ADMIN,MANAGER', 1),
  (21, 3, 'Preferences', NULL, 'sliders', NULL, NULL, 20, NULL, 1),
  (100, 10, 'Area', 'Area Master', 'geo-alt', 'area', 'master/area.html', 10, NULL, 1),
  (101, 10, 'Route', 'Route Master', 'signpost-2', 'route', 'master/route.html', 20, NULL, 1),
  (102, 10, 'Route-Area Map', 'Route-Area Mapping', 'diagram-3', 'routearea', 'master/route-area.html', 30, NULL, 1),
  (110, 11, 'Client Type', 'Client Type Master', 'tags', 'clienttype', 'master/client-type.html', 10, NULL, 1),
  (111, 11, 'Client', 'Client Master', 'person-vcard', 'client', 'master/client.html', 20, NULL, 1),
  (120, 12, 'Products', 'Products Master', 'boxes', 'products', 'master/products.html', 10, NULL, 1),
  (200, 20, 'Employee', 'Employee Master', 'person-workspace', 'employee', 'master/employee.html', 10, 'ADMIN,MANAGER', 1),
  (210, 21, 'Appearance', 'Appearance', 'palette', 'appearance', 'settings/appearance.html', 10, NULL, 1)
ON DUPLICATE KEY UPDATE
  parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
  page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== sfa_demo ====================
USE sfa_demo;
CREATE TABLE IF NOT EXISTS menu_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       NULL,
    label      VARCHAR(128) NOT NULL,          -- nav text (short)
    title      VARCHAR(160) NULL,              -- report/page title (falls back to label)
    icon       VARCHAR(64)  NULL,              -- bootstrap-icon name, no "bi-" prefix
    logo       VARCHAR(256) NULL,              -- optional image URL instead of/with icon
    page       VARCHAR(64)  NULL,              -- active-highlight key (matches data-page)
    href       VARCHAR(256) NULL,              -- link; NULL/blank => branch node
    sort_order INT          NOT NULL DEFAULT 0,
    roles      VARCHAR(256) NULL,              -- CSV allow-list; NULL = all roles
    enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    KEY idx_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- add `title` to tables created before this column existed (idempotent)
SET @has_title := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'menu_item' AND column_name = 'title');
SET @ddl := IF(@has_title = 0,
    'ALTER TABLE menu_item ADD COLUMN title VARCHAR(160) NULL AFTER label', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS user_menu_favorite (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(128) NOT NULL,
    menu_item_id BIGINT       NOT NULL,
    UNIQUE KEY uq_user_menu_favorite (username, menu_item_id),
    KEY idx_umf_user (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
  (1, NULL, 'Dashboard', 'Dashboard', 'speedometer2', 'dashboard', 'dashboard.html', 10, NULL, 1),
  (2, NULL, 'Masters', NULL, 'collection', NULL, NULL, 20, NULL, 1),
  (3, NULL, 'Administration', NULL, 'shield-lock', NULL, NULL, 30, NULL, 1),
  (10, 2, 'Geography', NULL, 'geo', NULL, NULL, 10, NULL, 1),
  (11, 2, 'Parties', NULL, 'people', NULL, NULL, 20, NULL, 1),
  (12, 2, 'Catalog', NULL, 'box-seam', NULL, NULL, 30, NULL, 1),
  (20, 3, 'People', NULL, 'person-badge', NULL, NULL, 10, 'ADMIN,MANAGER', 1),
  (21, 3, 'Preferences', NULL, 'sliders', NULL, NULL, 20, NULL, 1),
  (100, 10, 'Area', 'Area Master', 'geo-alt', 'area', 'master/area.html', 10, NULL, 1),
  (101, 10, 'Route', 'Route Master', 'signpost-2', 'route', 'master/route.html', 20, NULL, 1),
  (102, 10, 'Route-Area Map', 'Route-Area Mapping', 'diagram-3', 'routearea', 'master/route-area.html', 30, NULL, 1),
  (110, 11, 'Client Type', 'Client Type Master', 'tags', 'clienttype', 'master/client-type.html', 10, NULL, 1),
  (111, 11, 'Client', 'Client Master', 'person-vcard', 'client', 'master/client.html', 20, NULL, 1),
  (120, 12, 'Products', 'Products Master', 'boxes', 'products', 'master/products.html', 10, NULL, 1),
  (200, 20, 'Employee', 'Employee Master', 'person-workspace', 'employee', 'master/employee.html', 10, 'ADMIN,MANAGER', 1),
  (210, 21, 'Appearance', 'Appearance', 'palette', 'appearance', 'settings/appearance.html', 10, NULL, 1)
ON DUPLICATE KEY UPDATE
  parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
  page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

