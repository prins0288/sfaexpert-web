-- ===========================================================================
-- Per-employee menu hide/show.
--
-- The menu_item table already carries the BASE menu + a GLOBAL is_visible flag
-- (the "All employees" switch). This adds the sparse per-employee OVERRIDE
-- layer: one row per (emp_id, menu_item_id) that differs from the global flag.
--   effective visible = override for the user's emp_id if present,
--                       else menu_item.is_visible.
-- Used for both admin "employee-wise" and "self" hide/show.
--
-- Also seeds the "Menu Visibility" management page under the Utilities menu
-- (parent id 413, from 24_top_level_menu_expansion.sql).
--
-- TENANT db, acme_db.
-- Run:  mysql -u myroot acme_db < src/main/resources/sql/29_menu_visibility.sql
-- ===========================================================================

USE acme_db;

CREATE TABLE IF NOT EXISTS menu_visibility (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    emp_id       VARCHAR(100) NOT NULL,
    menu_item_id BIGINT       NOT NULL,
    is_visible   TINYINT(1)   NOT NULL DEFAULT 1,
    updated_at   DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_menu_visibility (emp_id, menu_item_id),
    KEY ix_menu_visibility_emp (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Utilities > Menu Visibility (report/management page). emp_id is the tenant's
-- own owner marker, matched to the rest of this tenant's menu rows.
INSERT INTO menu_item (id, parent_id, emp_id, label, title, description, icon, page, href, sort_order, is_visible, is_active)
VALUES (700, 413,
        (SELECT e FROM (SELECT emp_id e FROM menu_item WHERE id <> 700 ORDER BY id LIMIT 1) t),
        'Menu Visibility', 'Menu Visibility',
        'Show or hide menu items per employee (or for everyone), and hide items for yourself.',
        'eye', 'menu-visibility', 'utilities/menu-visibility.html', 1, 1, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), emp_id=VALUES(emp_id), label=VALUES(label), title=VALUES(title),
    description=VALUES(description), icon=VALUES(icon), page=VALUES(page), href=VALUES(href),
    sort_order=VALUES(sort_order), is_visible=VALUES(is_visible), is_active=VALUES(is_active);
