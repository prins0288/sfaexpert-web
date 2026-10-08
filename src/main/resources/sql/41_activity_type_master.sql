-- ============================================================================
-- 41_activity_type_master.sql   (TENANT db: run in acme_db, globex_db, initech_db, sfa_demo)
--
-- A new "Activity Type" master — SAME SHAPE as client_type (type_code, type_name,
-- singular_label, plural_label, status CHAR(1) Y/N) — + its menu entry under
-- Masters > Parties, next to Client Type.
--
-- Idempotent: table created only if absent; menu row inserted only if a row
-- with page='activitytype' does not already exist. emp_id is copied from the
-- Parties group row so it matches this tenant's owner marker.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `activity_type_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `singular_label` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plural_label` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_activity_type_master_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 132, 11, par.emp_id, 'WEB', 'Activity Type', NULL, 'clipboard2-pulse',
       'activitytype', 'master/activity-type.html', '_SELF', 40, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 11 LIMIT 1) par
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'activitytype');
