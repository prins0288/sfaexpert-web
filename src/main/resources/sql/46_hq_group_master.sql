-- ============================================================================
-- 46_hq_group_master.sql   (TENANT db: run in acme_db, globex_db, initech_db, sfa_demo)
--
-- A new "HQ Group" master (wider region/cluster grouping several HQs, e.g.
-- Ulhasnagar / Thane / Byculla / Panvel / Delhi) + its menu entry under
-- Masters > Geography, next to HQ. Also:
--   - adds an OPTIONAL hq_group_id FK column on hq_master, and
--   - converts hq_master.status from CHAR(1) 'Y'/'N' to a TINYINT(1) is_active
--     column, matching the newer-masters convention.
--
-- Idempotent throughout.
-- ============================================================================

-- 1) hq_group_master table -----------------------------------------------
CREATE TABLE IF NOT EXISTS `hq_group_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `group_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `group_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hq_group_code` (`group_code`),
  UNIQUE KEY `uk_hq_group_name` (`group_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) menu entry: Masters(2) > Geography(10) > HQ Group -----------------------
INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 133, 10, geo.emp_id, 'WEB', 'HQ Group', NULL, 'collection',
       'hqgroupmaster', 'master/hq-group-master.html', '_SELF', 6, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 10 LIMIT 1) geo
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'hqgroupmaster');

-- 3) hq_master: optional hq_group_id FK ---------------------------------------
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'hq_master' AND column_name = 'hq_group_id');
SET @sql := IF(@has_col = 0,
    'ALTER TABLE hq_master ADD COLUMN hq_group_id BIGINT UNSIGNED NULL AFTER state_oid, ADD KEY idx_hq_hq_group (hq_group_id), ADD CONSTRAINT fk_hq_hq_group FOREIGN KEY (hq_group_id) REFERENCES hq_group_master(id) ON DELETE SET NULL',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) hq_master.status -> is_active TINYINT(1) ---------------------------------
SET @has_col2 := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'hq_master' AND column_name = 'is_active');
SET @sql2 := IF(@has_col2 = 0,
    'ALTER TABLE hq_master ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER hq_group_id',
    'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @sql2 := IF(@has_col2 = 0,
    'UPDATE hq_master SET is_active = IF(status IN (''Y'',''y'',''1'',''true''), 1, 0)',
    'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @has_status2 := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'hq_master' AND column_name = 'status');
SET @sql2 := IF(@has_status2 > 0, 'ALTER TABLE hq_master DROP COLUMN status', 'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
