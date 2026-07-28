-- ===========================================================================
-- category_master — a production-style master, PER TENANT database.
--   soft-delete `status`, an `icon`, and full audit columns
--   (created_at / created_by / updated_at / updated_by).
-- The audit columns are filled by the JPA entity (@PrePersist / @PreUpdate);
-- these seeds set them to 'system' / NOW() directly.
-- Also registers the "Category" menu item under Masters > Catalog.
--
-- Run:  mysql -u myroot < src/main/resources/sql/10_category_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
CREATE TABLE IF NOT EXISTS category_master (
    oid           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(64)  NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    description   VARCHAR(255) NULL,
    icon          VARCHAR(128) NULL,                       -- bootstrap-icon name or image URL
    status        CHAR(1)      NOT NULL DEFAULT 'Y',        -- Y active / N inactive (soft delete)
    created_at    DATETIME     NULL,
    created_by    VARCHAR(128) NULL,
    updated_at    DATETIME     NULL,
    updated_by    VARCHAR(128) NULL,
    UNIQUE KEY uq_category_master_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO category_master
    (category_code, category_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('CAT-PREM', 'Premium',  'High-value / key accounts', 'star',   'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-STD',  'Standard', 'Regular accounts',          'circle', 'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-NEW',  'New',      'Recently onboarded',        'stars',  'Y', NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    category_name=VALUES(category_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Category" under Masters > Catalog (id 12); Products is 120
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (121, 12, 'Category', 'Category Master', 'bookmarks', 'categorymaster', 'master/category-master.html', 20, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== globex_db ====================
USE globex_db;
CREATE TABLE IF NOT EXISTS category_master (
    oid           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(64)  NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    description   VARCHAR(255) NULL,
    icon          VARCHAR(128) NULL,                       -- bootstrap-icon name or image URL
    status        CHAR(1)      NOT NULL DEFAULT 'Y',        -- Y active / N inactive (soft delete)
    created_at    DATETIME     NULL,
    created_by    VARCHAR(128) NULL,
    updated_at    DATETIME     NULL,
    updated_by    VARCHAR(128) NULL,
    UNIQUE KEY uq_category_master_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO category_master
    (category_code, category_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('CAT-PREM', 'Premium',  'High-value / key accounts', 'star',   'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-STD',  'Standard', 'Regular accounts',          'circle', 'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-NEW',  'New',      'Recently onboarded',        'stars',  'Y', NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    category_name=VALUES(category_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Category" under Masters > Catalog (id 12); Products is 120
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (121, 12, 'Category', 'Category Master', 'bookmarks', 'categorymaster', 'master/category-master.html', 20, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== initech_db ====================
USE initech_db;
CREATE TABLE IF NOT EXISTS category_master (
    oid           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(64)  NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    description   VARCHAR(255) NULL,
    icon          VARCHAR(128) NULL,                       -- bootstrap-icon name or image URL
    status        CHAR(1)      NOT NULL DEFAULT 'Y',        -- Y active / N inactive (soft delete)
    created_at    DATETIME     NULL,
    created_by    VARCHAR(128) NULL,
    updated_at    DATETIME     NULL,
    updated_by    VARCHAR(128) NULL,
    UNIQUE KEY uq_category_master_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO category_master
    (category_code, category_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('CAT-PREM', 'Premium',  'High-value / key accounts', 'star',   'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-STD',  'Standard', 'Regular accounts',          'circle', 'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-NEW',  'New',      'Recently onboarded',        'stars',  'Y', NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    category_name=VALUES(category_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Category" under Masters > Catalog (id 12); Products is 120
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (121, 12, 'Category', 'Category Master', 'bookmarks', 'categorymaster', 'master/category-master.html', 20, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

-- ==================== sfa_demo ====================
USE sfa_demo;
CREATE TABLE IF NOT EXISTS category_master (
    oid           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(64)  NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    description   VARCHAR(255) NULL,
    icon          VARCHAR(128) NULL,                       -- bootstrap-icon name or image URL
    status        CHAR(1)      NOT NULL DEFAULT 'Y',        -- Y active / N inactive (soft delete)
    created_at    DATETIME     NULL,
    created_by    VARCHAR(128) NULL,
    updated_at    DATETIME     NULL,
    updated_by    VARCHAR(128) NULL,
    UNIQUE KEY uq_category_master_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO category_master
    (category_code, category_name, description, icon, status, created_at, created_by, updated_at, updated_by) VALUES
    ('CAT-PREM', 'Premium',  'High-value / key accounts', 'star',   'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-STD',  'Standard', 'Regular accounts',          'circle', 'Y', NOW(), 'system', NOW(), 'system'),
    ('CAT-NEW',  'New',      'Recently onboarded',        'stars',  'Y', NOW(), 'system', NOW(), 'system')
ON DUPLICATE KEY UPDATE
    category_name=VALUES(category_name), description=VALUES(description),
    icon=VALUES(icon), updated_at=NOW(), updated_by='system';
-- add "Category" under Masters > Catalog (id 12); Products is 120
INSERT INTO menu_item (id, parent_id, label, title, icon, page, href, sort_order, roles, enabled) VALUES
    (121, 12, 'Category', 'Category Master', 'bookmarks', 'categorymaster', 'master/category-master.html', 20, NULL, 1)
ON DUPLICATE KEY UPDATE
    parent_id=VALUES(parent_id), label=VALUES(label), title=VALUES(title), icon=VALUES(icon),
    page=VALUES(page), href=VALUES(href), sort_order=VALUES(sort_order), roles=VALUES(roles), enabled=VALUES(enabled);

