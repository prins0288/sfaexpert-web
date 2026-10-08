-- ============================================================================
-- 38_sponsorship_type_master.sql   (TENANT db: run in acme_db, globex_db, initech_db, sfa_demo)
--
-- A new "Sponsorship Type" master + its menu entry under Masters > Catalog.
-- Idempotent: table created only if absent; menu row inserted only if a row
-- with page='sponsorshiptypemaster' does not already exist. emp_id is copied from the
-- Catalog group row so it matches this tenant's owner marker.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `sponsorship_type_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sponsorship_type_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sponsorship_type_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sponsorship_type_code` (`sponsorship_type_code`),
  UNIQUE KEY `uk_sponsorship_type_name` (`sponsorship_type_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 129, 12, cat.emp_id, 'WEB', 'Sponsorship Type', NULL, 'award',
       'sponsorshiptypemaster', 'master/sponsorship-type-master.html', '_SELF', 100, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 12 LIMIT 1) cat
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'sponsorshiptypemaster');
