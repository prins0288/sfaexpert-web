-- ===========================================================================
-- Bring the OTHER tenant DBs' menu_item up to the same schema acme_db already
-- has (emp_id / is_visible / is_active / menu_type / target / badge…), so the
-- shared MenuItem entity works for every tenant, and add the per-tenant
-- menu_visibility table + the "Menu Visibility" management page.
--
-- These tenants never received the top-level menu expansion (Utilities etc.),
-- so their Menu Visibility entry is placed under Administration (id 3), which
-- exists in every tenant.
--
-- Idempotency: this assumes the OLD schema (roles/enabled/logo present). Run
-- once per listed tenant. acme_db is already migrated — do NOT run it there.
--
-- Run:  mysql -u myroot globex_db  < src/main/resources/sql/30_menu_schema_align_other_tenants.sql
--   (repeat per tenant; the USE lines below cover globex_db + initech_db)
-- ===========================================================================

-- ---------- globex_db --------------------------------------------------------
USE globex_db;

ALTER TABLE menu_item
  ADD COLUMN emp_id      VARCHAR(100) NULL AFTER parent_id,
  ADD COLUMN base_url    VARCHAR(255) NULL AFTER emp_id,
  ADD COLUMN menu_type   ENUM('WEB','APP','BOTH') DEFAULT 'WEB' AFTER base_url,
  ADD COLUMN target      ENUM('_SELF','_BLANK') DEFAULT '_SELF' AFTER href,
  ADD COLUMN badge       VARCHAR(30) DEFAULT 'New' AFTER target,
  ADD COLUMN badge_color VARCHAR(100) NULL AFTER badge,
  ADD COLUMN is_visible  TINYINT DEFAULT 1 AFTER sort_order,
  ADD COLUMN is_active   TINYINT(1) NOT NULL DEFAULT 1 AFTER is_visible,
  ADD COLUMN created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE menu_item SET is_active = enabled, emp_id = 'GLOBEX001';
ALTER TABLE menu_item MODIFY COLUMN emp_id VARCHAR(100) NOT NULL;
ALTER TABLE menu_item DROP COLUMN roles, DROP COLUMN enabled, DROP COLUMN logo;

CREATE TABLE IF NOT EXISTS menu_visibility (
    id BIGINT NOT NULL AUTO_INCREMENT, emp_id VARCHAR(100) NOT NULL,
    menu_item_id BIGINT NOT NULL, is_visible TINYINT(1) NOT NULL DEFAULT 1, updated_at DATETIME NULL,
    PRIMARY KEY (id), UNIQUE KEY uq_menu_visibility (emp_id, menu_item_id), KEY ix_menu_visibility_emp (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO menu_item (id, parent_id, emp_id, label, title, description, icon, page, href, sort_order, is_visible, is_active)
VALUES (700, 3, 'GLOBEX001', 'Menu Visibility', 'Menu Visibility',
        'Show or hide menu items per employee (or for everyone), and hide items for yourself.',
        'eye', 'menu-visibility', 'utilities/menu-visibility.html', 90, 1, 1)
ON DUPLICATE KEY UPDATE label=VALUES(label), href=VALUES(href);

-- ---------- initech_db -------------------------------------------------------
USE initech_db;

ALTER TABLE menu_item
  ADD COLUMN emp_id      VARCHAR(100) NULL AFTER parent_id,
  ADD COLUMN base_url    VARCHAR(255) NULL AFTER emp_id,
  ADD COLUMN menu_type   ENUM('WEB','APP','BOTH') DEFAULT 'WEB' AFTER base_url,
  ADD COLUMN target      ENUM('_SELF','_BLANK') DEFAULT '_SELF' AFTER href,
  ADD COLUMN badge       VARCHAR(30) DEFAULT 'New' AFTER target,
  ADD COLUMN badge_color VARCHAR(100) NULL AFTER badge,
  ADD COLUMN is_visible  TINYINT DEFAULT 1 AFTER sort_order,
  ADD COLUMN is_active   TINYINT(1) NOT NULL DEFAULT 1 AFTER is_visible,
  ADD COLUMN created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE menu_item SET is_active = enabled, emp_id = 'INITECH001';
ALTER TABLE menu_item MODIFY COLUMN emp_id VARCHAR(100) NOT NULL;
ALTER TABLE menu_item DROP COLUMN roles, DROP COLUMN enabled, DROP COLUMN logo;

CREATE TABLE IF NOT EXISTS menu_visibility (
    id BIGINT NOT NULL AUTO_INCREMENT, emp_id VARCHAR(100) NOT NULL,
    menu_item_id BIGINT NOT NULL, is_visible TINYINT(1) NOT NULL DEFAULT 1, updated_at DATETIME NULL,
    PRIMARY KEY (id), UNIQUE KEY uq_menu_visibility (emp_id, menu_item_id), KEY ix_menu_visibility_emp (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO menu_item (id, parent_id, emp_id, label, title, description, icon, page, href, sort_order, is_visible, is_active)
VALUES (700, 3, 'INITECH001', 'Menu Visibility', 'Menu Visibility',
        'Show or hide menu items per employee (or for everyone), and hide items for yourself.',
        'eye', 'menu-visibility', 'utilities/menu-visibility.html', 90, 1, 1)
ON DUPLICATE KEY UPDATE label=VALUES(label), href=VALUES(href);

-- ---------- sfa_demo (tenant "demo") ----------------------------------------
USE sfa_demo;

ALTER TABLE menu_item
  ADD COLUMN emp_id      VARCHAR(100) NULL AFTER parent_id,
  ADD COLUMN base_url    VARCHAR(255) NULL AFTER emp_id,
  ADD COLUMN menu_type   ENUM('WEB','APP','BOTH') DEFAULT 'WEB' AFTER base_url,
  ADD COLUMN target      ENUM('_SELF','_BLANK') DEFAULT '_SELF' AFTER href,
  ADD COLUMN badge       VARCHAR(30) DEFAULT 'New' AFTER target,
  ADD COLUMN badge_color VARCHAR(100) NULL AFTER badge,
  ADD COLUMN is_visible  TINYINT DEFAULT 1 AFTER sort_order,
  ADD COLUMN is_active   TINYINT(1) NOT NULL DEFAULT 1 AFTER is_visible,
  ADD COLUMN created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE menu_item SET is_active = enabled, emp_id = 'DEMO001';
ALTER TABLE menu_item MODIFY COLUMN emp_id VARCHAR(100) NOT NULL;
ALTER TABLE menu_item DROP COLUMN roles, DROP COLUMN enabled, DROP COLUMN logo;

CREATE TABLE IF NOT EXISTS menu_visibility (
    id BIGINT NOT NULL AUTO_INCREMENT, emp_id VARCHAR(100) NOT NULL,
    menu_item_id BIGINT NOT NULL, is_visible TINYINT(1) NOT NULL DEFAULT 1, updated_at DATETIME NULL,
    PRIMARY KEY (id), UNIQUE KEY uq_menu_visibility (emp_id, menu_item_id), KEY ix_menu_visibility_emp (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO menu_item (id, parent_id, emp_id, label, title, description, icon, page, href, sort_order, is_visible, is_active)
VALUES (700, 3, 'DEMO001', 'Menu Visibility', 'Menu Visibility',
        'Show or hide menu items per employee (or for everyone), and hide items for yourself.',
        'eye', 'menu-visibility', 'utilities/menu-visibility.html', 90, 1, 1)
ON DUPLICATE KEY UPDATE label=VALUES(label), href=VALUES(href);
