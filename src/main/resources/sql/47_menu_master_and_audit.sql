-- ============================================================================
-- 47_menu_master_and_audit.sql   (TENANT db: run in acme_db, globex_db, initech_db, sfa_demo)
--
-- Two new admin pages:
--   - Menu Master (Masters > Master Entry > Menu Master) — create/edit/
--     activate/deactivate/delete any menu item, WEB or APP, at any depth.
--   - Menu Audit Report (Reports > Menu Audit Report) — who did which of
--     those actions, and when (company_menu_self has no created_by/updated_by
--     of its own, so this is a separate log table written by Menu Master).
--
-- Idempotent throughout.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `menu_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_item_id` bigint DEFAULT NULL,
  `label` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `menu_type` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `changed_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `changed_at` datetime NOT NULL,
  `details` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_menu_audit_item` (`menu_item_id`),
  KEY `idx_menu_audit_changed_at` (`changed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Menu Master: Masters(2) > Master Entry(500) > Menu Master ------------------
INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 516, 500, me.emp_id, 'WEB', 'Menu Master', NULL, 'diagram-3-fill',
       'menumaster', 'utilities/menu-master.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 500 LIMIT 1) me
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'menumaster');

-- Menu Audit Report: Reports(4) > Menu Audit Report ---------------------------
INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 134, 4, rep.emp_id, 'WEB', 'Menu Audit Report', NULL, 'clock-history',
       'menuauditreport', 'report/menu-audit-report.html', '_SELF', 10, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 4 LIMIT 1) rep
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'menuauditreport');
