-- ============================================================================
-- 33_item_type_master.sql   (TENANT db: run in acme_db, globex_db, initech_db, sfa_demo)
--
-- A new "Item Type" master (product dosage forms like Tablet / Capsule / Syrup /
-- Injection) + its menu entry under Masters > Catalog, next to Category /
-- Speciality / Degree.
--
-- Idempotent: the table is created only if absent, and the menu row is inserted
-- only if a row with page='itemtypemaster' does not already exist. emp_id is
-- copied from the Catalog group row so it matches this tenant's owner marker.
-- ============================================================================

-- 1) table -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `item_type_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `item_type_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_type_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_type_code` (`item_type_code`),
  UNIQUE KEY `uk_item_type_name` (`item_type_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) menu entry: Masters(2) > Catalog(12) > Item Type ------------------------
INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 124, 12, cat.emp_id, 'WEB', 'Item Type', NULL, 'capsule',
       'itemtypemaster', 'master/item-type-master.html', '_SELF', 50, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 12 LIMIT 1) cat
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'itemtypemaster');
