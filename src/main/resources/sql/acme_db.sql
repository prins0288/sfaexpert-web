/*
SQLyog Ultimate v12.09 (32 bit)
MySQL - 8.0.46 : Database - acme_db
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`acme_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `acme_db`;

/*Table structure for table `activity_type_master` */

DROP TABLE IF EXISTS `activity_type_master`;

CREATE TABLE `activity_type_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_type_id` bigint DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_activity_type_master_code` (`type_code`),
  KEY `idx_activity_type_client_type` (`client_type_id`),
  CONSTRAINT `fk_activity_type_client_type` FOREIGN KEY (`client_type_id`) REFERENCES `client_type` (`oid`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `app_validations` */

DROP TABLE IF EXISTS `app_validations`;

CREATE TABLE `app_validations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'NULL = Default Configuration, Value = Employee Specific',
  `config_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `module_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `screen_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '',
  `platform` enum('WEB','APP','BOTH') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'BOTH',
  `config_type` enum('VALIDATION','VISIBILITY','FEATURE','SETTING','LIMIT','PERMISSION') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `data_type` enum('BOOLEAN','INTEGER','DECIMAL','STRING','TIME','DATE','JSON') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `default_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_enabled` tinyint(1) DEFAULT '1',
  `is_active` tinyint(1) DEFAULT '1',
  `display_order` int DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`config_code`),
  UNIQUE KEY `uk_emp_config` (`emp_id`,`config_code`),
  KEY `idx_module` (`module_name`),
  KEY `idx_platform` (`platform`),
  KEY `idx_type` (`config_type`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `bank_master` */

DROP TABLE IF EXISTS `bank_master`;

CREATE TABLE `bank_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `bank_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_code` (`bank_code`),
  UNIQUE KEY `uk_bank_name` (`bank_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `category_master` */

DROP TABLE IF EXISTS `category_master`;

CREATE TABLE `category_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT NULL,
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_category_master_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `city_master` */

DROP TABLE IF EXISTS `city_master`;

CREATE TABLE `city_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `area_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `pincode` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_area_code` (`area_code`),
  KEY `ix_area_status` (`status`),
  KEY `ix_area_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `client` */

DROP TABLE IF EXISTS `client`;

CREATE TABLE `client` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `client_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_type_oid` bigint NOT NULL,
  `prefix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(180) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `firm_name` varchar(180) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `degree_oid` bigint DEFAULT NULL,
  `speciality_oid` bigint DEFAULT NULL,
  `category_oid` bigint DEFAULT NULL,
  `route_oid` bigint DEFAULT NULL,
  `area_oid` bigint DEFAULT NULL,
  `address` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `anniversary` date DEFAULT NULL,
  `drug_license_no` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gst_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_client_code` (`client_code`),
  KEY `ix_client_type` (`client_type_oid`),
  KEY `ix_client_area` (`area_oid`),
  KEY `ix_client_route` (`route_oid`),
  KEY `ix_client_status` (`status`),
  CONSTRAINT `fk_client_type` FOREIGN KEY (`client_type_oid`) REFERENCES `client_type` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `client_type` */

DROP TABLE IF EXISTS `client_type`;

CREATE TABLE `client_type` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `singular_label` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `plural_label` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_ctype_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `code_sequence` */

DROP TABLE IF EXISTS `code_sequence`;

CREATE TABLE `code_sequence` (
  `seq_name` varchar(50) NOT NULL,
  `next_val` bigint unsigned NOT NULL DEFAULT '0' COMMENT 'last handed-out value',
  PRIMARY KEY (`seq_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `company_menu_self` */

DROP TABLE IF EXISTS `company_menu_self`;

CREATE TABLE `company_menu_self` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `emp_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `menu_type` enum('WEB','APP','BOTH') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'WEB',
  `label` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `page` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `href` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target` enum('_SELF','_BLANK') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '_SELF',
  `badge` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'New',
  `badge_color` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_visible` tinyint DEFAULT '1',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_menu` (`emp_id`,`parent_id`,`is_active`,`sort_order`),
  KEY `idx_parent_sort` (`parent_id`,`sort_order`),
  CONSTRAINT `fk_menu_parent` FOREIGN KEY (`parent_id`) REFERENCES `company_menu_self` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=704 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `company_profile` */

DROP TABLE IF EXISTS `company_profile`;

CREATE TABLE `company_profile` (
  `id` int NOT NULL,
  `company_name` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `website` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `company_setting_master` */

DROP TABLE IF EXISTS `company_setting_master`;

CREATE TABLE `company_setting_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `setting_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_app_setting_key` (`setting_key`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `country_master` */

DROP TABLE IF EXISTS `country_master`;

CREATE TABLE `country_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `country_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `country_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_country_code` (`country_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `dcr` */

DROP TABLE IF EXISTS `dcr`;

CREATE TABLE `dcr` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `employee_oid` bigint NOT NULL,
  `client_oid` bigint NOT NULL,
  `dcr_date` date DEFAULT NULL,
  `dcr_date_np` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `work_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `time_in` time DEFAULT NULL,
  `time_out` time DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT NULL,
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`oid`),
  KEY `idx_dcr_employee` (`employee_oid`),
  KEY `idx_dcr_client` (`client_oid`),
  KEY `idx_dcr_date` (`dcr_date`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `degree_master` */

DROP TABLE IF EXISTS `degree_master`;

CREATE TABLE `degree_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `degree_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `degree_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_degree_code` (`degree_code`),
  UNIQUE KEY `uk_degree_name` (`degree_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `designation_master` */

DROP TABLE IF EXISTS `designation_master`;

CREATE TABLE `designation_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `designation_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `designation_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `emp_level` int DEFAULT NULL COMMENT 'Hierarchy / employee level: 1,2,3,...',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `designation_code` (`designation_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `district` */

DROP TABLE IF EXISTS `district`;

CREATE TABLE `district` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `district_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `district_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `district_code` (`district_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `division_master` */

DROP TABLE IF EXISTS `division_master`;

CREATE TABLE `division_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `division_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_division_code` (`division_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `document_master` */

DROP TABLE IF EXISTS `document_master`;

CREATE TABLE `document_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `document_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `document_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_code` (`document_code`),
  UNIQUE KEY `uk_document_name` (`document_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `emp_address` */

DROP TABLE IF EXISTS `emp_address`;

CREATE TABLE `emp_address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `address_type` varchar(10) NOT NULL,
  `address_line` varchar(500) NOT NULL COMMENT 'Full address (free text, as in Excel)',
  `city` varchar(100) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `state_id` bigint DEFAULT NULL COMMENT '-> state_master.oid',
  `pincode` char(6) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_address_emp_type` (`emp_id`,`address_type`),
  KEY `idx_address_state` (`state_id`),
  CONSTRAINT `fk_address_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `fk_address_state` FOREIGN KEY (`state_id`) REFERENCES `state_master` (`oid`),
  CONSTRAINT `chk_address_pin` CHECK (((`pincode` is null) or regexp_like(`pincode`,_utf8mb4'^[1-9][0-9]{5}$'))),
  CONSTRAINT `chk_address_type` CHECK ((`address_type` in (_utf8mb4'PRESENT',_utf8mb4'PERMANENT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_app_setting` */

DROP TABLE IF EXISTS `emp_app_setting`;

CREATE TABLE `emp_app_setting` (
  `emp_id` varchar(40) NOT NULL,
  `expense_group_id` int DEFAULT NULL,
  `app_access_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Reporting App Access Enable/Disable',
  `stop_reporting` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Start/Stop reporting: TRUE = cannot submit DCR',
  `geo_fencing_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `lock_period_days` smallint unsigned NOT NULL DEFAULT '20',
  `lock_web_dcr` tinyint(1) NOT NULL DEFAULT '0',
  `all_day_working` tinyint(1) NOT NULL DEFAULT '0',
  `auto_mail` tinyint(1) NOT NULL DEFAULT '0',
  `is_starsfa` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by` varchar(30) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`emp_id`),
  KEY `idx_app_expense` (`expense_group_id`),
  CONSTRAINT `fk_app_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_assignment_history` */

DROP TABLE IF EXISTS `emp_assignment_history`;

CREATE TABLE `emp_assignment_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `division_id` bigint NOT NULL,
  `state_id` bigint DEFAULT NULL,
  `hq_id` bigint DEFAULT NULL,
  `designation_id` bigint NOT NULL,
  `manager_id` varchar(40) DEFAULT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL COMMENT 'NULL = current',
  `change_reason` varchar(30) DEFAULT NULL COMMENT 'JOINING / TRANSFER / PROMOTION / MANAGER_CHANGE / MIGRATION',
  `created_by` varchar(30) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_hist_emp_from` (`emp_id`,`effective_from`),
  KEY `idx_hist_mgr` (`manager_id`,`effective_from`),
  KEY `idx_hist_hq` (`hq_id`,`effective_from`),
  KEY `fk_hist_div` (`division_id`),
  KEY `fk_hist_st` (`state_id`),
  KEY `fk_hist_des` (`designation_id`),
  CONSTRAINT `fk_hist_des` FOREIGN KEY (`designation_id`) REFERENCES `designation_master` (`oid`),
  CONSTRAINT `fk_hist_div` FOREIGN KEY (`division_id`) REFERENCES `division_master` (`oid`),
  CONSTRAINT `fk_hist_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `fk_hist_hq` FOREIGN KEY (`hq_id`) REFERENCES `hq_master` (`oid`),
  CONSTRAINT `fk_hist_mgr` FOREIGN KEY (`manager_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `fk_hist_st` FOREIGN KEY (`state_id`) REFERENCES `state_master` (`oid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_bank_account` */

DROP TABLE IF EXISTS `emp_bank_account`;

CREATE TABLE `emp_bank_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `bank_id` bigint unsigned DEFAULT NULL COMMENT '-> bank_master.id',
  `ifsc_code` char(11) DEFAULT NULL,
  `account_no` varchar(20) NOT NULL,
  `branch_name` varchar(150) DEFAULT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `primary_marker` tinyint GENERATED ALWAYS AS (if(((0 <> `is_primary`) and (0 <> `is_active`)),1,NULL)) STORED COMMENT 'not mapped in JPA',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_one_primary` (`emp_id`,`primary_marker`),
  UNIQUE KEY `uk_bank_emp_acc` (`emp_id`,`account_no`,`ifsc_code`),
  KEY `idx_bank_bank` (`bank_id`),
  CONSTRAINT `fk_bankacc_bank` FOREIGN KEY (`bank_id`) REFERENCES `bank_master` (`id`),
  CONSTRAINT `fk_bankacc_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `chk_bank_acc` CHECK (regexp_like(`account_no`,_utf8mb4'^[0-9]{6,20}$')),
  CONSTRAINT `chk_bank_ifsc` CHECK (((`ifsc_code` is null) or regexp_like(`ifsc_code`,_utf8mb4'^[A-Z]{4}0[A-Z0-9]{6}$',_utf8mb4'c')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_child` */

DROP TABLE IF EXISTS `emp_child`;

CREATE TABLE `emp_child` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `child_name` varchar(150) NOT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `sort_order` tinyint unsigned NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_child_emp` (`emp_id`,`sort_order`),
  CONSTRAINT `fk_child_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `chk_child_gender` CHECK (((`gender` is null) or (`gender` in (_utf8mb4'MALE',_utf8mb4'FEMALE',_utf8mb4'OTHER'))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_credential_dispatch` */

DROP TABLE IF EXISTS `emp_credential_dispatch`;

CREATE TABLE `emp_credential_dispatch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `channel` varchar(10) NOT NULL,
  `sent_to` varchar(150) NOT NULL,
  `dispatch_status` varchar(10) NOT NULL DEFAULT 'PENDING',
  `provider_msg_id` varchar(100) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `retry_count` tinyint unsigned NOT NULL DEFAULT '0',
  `requested_by` varchar(30) DEFAULT NULL,
  `requested_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `sent_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_disp_emp` (`emp_id`,`requested_at`),
  KEY `idx_disp_status` (`dispatch_status`,`requested_at`) COMMENT 'retry worker picks PENDING/FAILED',
  CONSTRAINT `fk_disp_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `chk_disp_channel` CHECK ((`channel` in (_utf8mb4'WHATSAPP',_utf8mb4'EMAIL',_utf8mb4'SMS'))),
  CONSTRAINT `chk_disp_status` CHECK ((`dispatch_status` in (_utf8mb4'PENDING',_utf8mb4'SENT',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_detail` */

DROP TABLE IF EXISTS `emp_detail`;

CREATE TABLE `emp_detail` (
  `emp_id` varchar(40) NOT NULL COMMENT 'Business key e.g. EMP-001 (matches AppUser.emp_id / JWT emp_id claim)',
  `emp_code` varchar(30) NOT NULL COMMENT 'Employee Code e.g. 005418 (legacy/display code, distinct from emp_id)',
  `emp_name` varchar(150) NOT NULL COMMENT 'As per Aadhaar',
  `division_id` bigint DEFAULT NULL COMMENT '-> division_master.oid (NULL for a company-wide role like Super Admin)',
  `state_id` bigint DEFAULT NULL COMMENT '-> state_master.oid',
  `hq_id` bigint DEFAULT NULL COMMENT '-> hq_master.oid',
  `designation` bigint NOT NULL COMMENT '-> designation_master.oid',
  `emp_level` tinyint unsigned NOT NULL DEFAULT '1' COMMENT 'copied from designation, for fast hierarchy filters',
  `manager_id` varchar(40) DEFAULT NULL COMMENT 'Immediate Manager -> emp_detail.emp_id',
  `gender` varchar(10) DEFAULT NULL,
  `date_of_joining` date DEFAULT NULL,
  `reporting_date` date DEFAULT NULL,
  `mobile` char(10) DEFAULT NULL,
  `official_email` varchar(150) DEFAULT NULL,
  `department` varchar(100) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `is_office_staff` tinyint(1) NOT NULL DEFAULT '0',
  `is_confirmed` tinyint(1) NOT NULL DEFAULT '0',
  `confirmation_date` date DEFAULT NULL,
  `resignation_date` date DEFAULT NULL,
  `last_working_date` date DEFAULT NULL,
  `photo_path` varchar(255) DEFAULT NULL,
  `profile_complete` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'app sets TRUE when all mandatory sections are filled',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by` varchar(30) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by` varchar(30) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0' COMMENT 'JPA @Version optimistic lock',
  PRIMARY KEY (`emp_id`),
  UNIQUE KEY `uk_emp_code` (`emp_code`),
  UNIQUE KEY `uk_emp_official_mail` (`official_email`),
  KEY `idx_emp_designation` (`designation`),
  KEY `idx_emp_hq` (`hq_id`),
  KEY `idx_emp_state` (`state_id`),
  KEY `idx_emp_mobile` (`mobile`),
  KEY `idx_emp_name` (`emp_name`),
  KEY `idx_emp_manager` (`manager_id`,`is_active`),
  KEY `idx_emp_div_state_hq` (`division_id`,`state_id`,`hq_id`,`is_active`),
  KEY `idx_emp_active_level` (`is_active`,`emp_level`),
  CONSTRAINT `fk_emp_designation` FOREIGN KEY (`designation`) REFERENCES `designation_master` (`oid`),
  CONSTRAINT `fk_emp_division` FOREIGN KEY (`division_id`) REFERENCES `division_master` (`oid`),
  CONSTRAINT `fk_emp_hq` FOREIGN KEY (`hq_id`) REFERENCES `hq_master` (`oid`),
  CONSTRAINT `fk_emp_manager` FOREIGN KEY (`manager_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `fk_emp_state` FOREIGN KEY (`state_id`) REFERENCES `state_master` (`oid`),
  CONSTRAINT `chk_emp_gender` CHECK (((`gender` is null) or (`gender` in (_utf8mb4'MALE',_utf8mb4'FEMALE',_utf8mb4'OTHER')))),
  CONSTRAINT `chk_emp_mobile` CHECK (((`mobile` is null) or regexp_like(`mobile`,_utf8mb4'^[6-9][0-9]{9}$')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

/*Table structure for table `emp_detail_legacy_backup` */

DROP TABLE IF EXISTS `emp_detail_legacy_backup`;

CREATE TABLE `emp_detail_legacy_backup` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `emp_code` varchar(30) NOT NULL,
  `emp_name` varchar(150) NOT NULL,
  `father_name` varchar(150) DEFAULT NULL,
  `division_id` varchar(10) DEFAULT NULL,
  `division_name` varchar(150) DEFAULT NULL,
  `designation` varchar(100) DEFAULT NULL,
  `emp_level` tinyint unsigned DEFAULT NULL,
  `reporting_manager_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `department` varchar(120) DEFAULT NULL,
  `state_id` int unsigned DEFAULT NULL,
  `district_id` int unsigned DEFAULT NULL,
  `gender` enum('Male','Female','Other') DEFAULT NULL,
  `marital_status` enum('SINGLE','MARRIED','DIVORCED','WIDOWED','SEPARATED','NOT_SPECIFIED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'SINGLE',
  `date_of_birth` date DEFAULT NULL,
  `anniversary_date` date DEFAULT NULL,
  `qualification` varchar(150) DEFAULT NULL,
  `city` varchar(120) DEFAULT NULL,
  `correspondence_address` varchar(500) DEFAULT NULL,
  `permanent_address` varchar(500) DEFAULT NULL,
  `mobile_number` varchar(15) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `email` varchar(120) DEFAULT NULL,
  `emergency_contact_number` varchar(15) DEFAULT NULL,
  `nominee_name` varchar(120) DEFAULT NULL,
  `nominee_relationship` varchar(30) DEFAULT NULL,
  `pan_number` char(10) DEFAULT NULL,
  `pf_number` varchar(30) DEFAULT NULL,
  `esi_number` varchar(30) DEFAULT NULL,
  `uan_number` varchar(20) DEFAULT NULL,
  `other_id_card_no` varchar(50) DEFAULT NULL,
  `bank_account_number` varchar(20) DEFAULT NULL,
  `bank_name` varchar(120) DEFAULT NULL,
  `bank_branch` varchar(120) DEFAULT NULL,
  `ifsc_code` char(11) DEFAULT NULL,
  `date_of_joining` date DEFAULT NULL,
  `experience` varchar(100) DEFAULT NULL,
  `previous_experience` varchar(500) DEFAULT NULL,
  `salary` decimal(12,2) DEFAULT NULL,
  `confirmation_status` tinyint NOT NULL DEFAULT '0',
  `reporting_date` date DEFAULT NULL,
  `photo_path` varchar(255) DEFAULT NULL,
  `shirt_size` tinyint unsigned DEFAULT NULL,
  `dcr_lock_period_days` smallint unsigned DEFAULT NULL,
  `status` enum('Active','Inactive') NOT NULL DEFAULT 'Active',
  `is_office_staff` tinyint(1) NOT NULL DEFAULT '0',
  `is_resigned` tinyint(1) NOT NULL DEFAULT '0',
  `all_day_working` tinyint(1) NOT NULL DEFAULT '0',
  `stop_reporting` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1 = user cannot submit DCR',
  `auto_mail_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `is_logged_out` tinyint(1) NOT NULL DEFAULT '0',
  `app_version` varbinary(100) DEFAULT NULL,
  `remarks` text,
  `created_by` varchar(30) DEFAULT NULL,
  `updated_by` varchar(30) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_emp_detail_code` (`emp_code`),
  UNIQUE KEY `uq_emp_detail_emp_id` (`emp_id`),
  KEY `idx_emp_detail_manager` (`reporting_manager_id`),
  KEY `idx_emp_detail_designation` (`designation`),
  KEY `idx_emp_detail_state` (`state_id`),
  KEY `idx_emp_detail_district` (`district_id`),
  KEY `idx_emp_detail_status_level` (`status`,`emp_level`),
  KEY `idx_emp_detail_mobile` (`mobile_number`),
  KEY `idx_emp_detail_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_document` */

DROP TABLE IF EXISTS `emp_document`;

CREATE TABLE `emp_document` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `document_type_id` bigint unsigned NOT NULL COMMENT '-> document_master.id',
  `original_name` varchar(255) NOT NULL,
  `storage_key` varchar(500) NOT NULL COMMENT 'object key / relative path',
  `mime_type` varchar(100) DEFAULT NULL,
  `file_size_bytes` int unsigned DEFAULT NULL,
  `sha256` char(64) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `uploaded_by` varchar(30) DEFAULT NULL,
  `uploaded_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_doc_emp_type` (`emp_id`,`document_type_id`,`is_active`),
  KEY `idx_doc_type` (`document_type_id`),
  CONSTRAINT `fk_doc_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `fk_doc_type` FOREIGN KEY (`document_type_id`) REFERENCES `document_master` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_emergency_contact` */

DROP TABLE IF EXISTS `emp_emergency_contact`;

CREATE TABLE `emp_emergency_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `contact_name` varchar(150) DEFAULT NULL COMMENT 'NULL only for legacy rows',
  `relationship_id` int DEFAULT NULL,
  `contact_no_1` varchar(15) NOT NULL,
  `contact_no_2` varchar(15) DEFAULT NULL,
  `priority` tinyint unsigned NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_emerg_emp` (`emp_id`,`priority`),
  KEY `idx_emerg_rel` (`relationship_id`),
  CONSTRAINT `fk_emerg_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_hierarchy` */

DROP TABLE IF EXISTS `emp_hierarchy`;

CREATE TABLE `emp_hierarchy` (
  `ancestor_id` varchar(40) NOT NULL,
  `descendant_id` varchar(40) NOT NULL,
  `depth` tinyint unsigned NOT NULL,
  PRIMARY KEY (`ancestor_id`,`descendant_id`),
  KEY `idx_hier_desc` (`descendant_id`,`depth`),
  CONSTRAINT `fk_hier_anc` FOREIGN KEY (`ancestor_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `fk_hier_desc` FOREIGN KEY (`descendant_id`) REFERENCES `emp_detail` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_login` */

DROP TABLE IF EXISTS `emp_login`;

CREATE TABLE `emp_login` (
  `emp_id` varchar(40) NOT NULL,
  `login_id` varchar(30) NOT NULL COMMENT 'Auto generated',
  `password_hash` varchar(100) DEFAULT NULL COMMENT 'BCrypt/Argon2 hash. NEVER plain text. NULL = must set password',
  `must_change_password` tinyint(1) NOT NULL DEFAULT '1',
  `password_changed_at` datetime(3) DEFAULT NULL,
  `failed_attempts` tinyint unsigned NOT NULL DEFAULT '0',
  `locked_until` datetime(3) DEFAULT NULL,
  `force_logout` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'legacy isLogout',
  `last_login_at` datetime(3) DEFAULT NULL,
  `app_version` int NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`emp_id`),
  UNIQUE KEY `uk_login_id` (`login_id`),
  CONSTRAINT `fk_login_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_nominee` */

DROP TABLE IF EXISTS `emp_nominee`;

CREATE TABLE `emp_nominee` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `nominee_type` varchar(12) NOT NULL,
  `nominee_name` varchar(150) NOT NULL,
  `relationship_id` int DEFAULT NULL,
  `nominee_dob` date DEFAULT NULL,
  `contact_no` char(10) DEFAULT NULL,
  `share_pct` decimal(5,2) NOT NULL DEFAULT '100.00',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_nominee_emp_type` (`emp_id`,`nominee_type`),
  KEY `idx_nominee_rel` (`relationship_id`),
  CONSTRAINT `fk_nominee_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `chk_nominee_mob` CHECK (((`contact_no` is null) or regexp_like(`contact_no`,_utf8mb4'^[0-9]{10}$'))),
  CONSTRAINT `chk_nominee_share` CHECK (((`share_pct` > 0) and (`share_pct` <= 100))),
  CONSTRAINT `chk_nominee_type` CHECK ((`nominee_type` in (_utf8mb4'ESI',_utf8mb4'PF',_utf8mb4'GRATUITY',_utf8mb4'MEDICLAIM')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_personal` */

DROP TABLE IF EXISTS `emp_personal`;

CREATE TABLE `emp_personal` (
  `emp_id` varchar(40) NOT NULL,
  `dob` date DEFAULT NULL COMMENT 'As per Aadhaar',
  `father_name` varchar(150) DEFAULT NULL,
  `mother_name` varchar(150) DEFAULT NULL,
  `marital_status` varchar(10) DEFAULT NULL,
  `anniversary_date` date DEFAULT NULL,
  `spouse_name` varchar(150) DEFAULT NULL,
  `qualification_id` bigint unsigned DEFAULT NULL COMMENT '-> degree_master.id',
  `qualification_detail` varchar(150) DEFAULT NULL COMMENT 'e.g. B.Pharm / free text when Others',
  `blood_group` varchar(3) DEFAULT NULL,
  `personal_email` varchar(150) DEFAULT NULL,
  `landline_no` varchar(20) DEFAULT NULL,
  `total_experience_yrs` decimal(4,1) DEFAULT NULL COMMENT 'Excel: Experience (years)',
  `shirt_size` tinyint unsigned DEFAULT NULL,
  `remarks` varchar(1000) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`emp_id`),
  UNIQUE KEY `uk_personal_email` (`personal_email`),
  KEY `idx_personal_dob` (`dob`),
  KEY `idx_personal_anniv` (`anniversary_date`),
  KEY `idx_personal_qual` (`qualification_id`),
  CONSTRAINT `fk_personal_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `fk_personal_qual` FOREIGN KEY (`qualification_id`) REFERENCES `degree_master` (`id`),
  CONSTRAINT `chk_personal_blood` CHECK (((`blood_group` is null) or (`blood_group` in (_utf8mb4'A+',_utf8mb4'A-',_utf8mb4'B+',_utf8mb4'B-',_utf8mb4'AB+',_utf8mb4'AB-',_utf8mb4'O+',_utf8mb4'O-')))),
  CONSTRAINT `chk_personal_marital` CHECK (((`marital_status` is null) or (`marital_status` in (_utf8mb4'SINGLE',_utf8mb4'MARRIED'))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_prev_employment` */

DROP TABLE IF EXISTS `emp_prev_employment`;

CREATE TABLE `emp_prev_employment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) NOT NULL,
  `company_name` varchar(200) NOT NULL,
  `designation` varchar(100) DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `experience_yrs` decimal(4,1) DEFAULT NULL,
  `last_ctc` decimal(12,2) DEFAULT NULL COMMENT 'Annual CTC, numeric only',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_prev_emp` (`emp_id`),
  CONSTRAINT `fk_prev_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `chk_prev_ctc` CHECK (((`last_ctc` is null) or (`last_ctc` >= 0))),
  CONSTRAINT `chk_prev_dates` CHECK (((`to_date` is null) or (`from_date` is null) or (`to_date` >= `from_date`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `emp_statutory` */

DROP TABLE IF EXISTS `emp_statutory`;

CREATE TABLE `emp_statutory` (
  `emp_id` varchar(40) NOT NULL,
  `pan_no` char(10) DEFAULT NULL,
  `pf_no` varchar(30) DEFAULT NULL,
  `uan_no` char(12) DEFAULT NULL,
  `esi_no` varchar(20) DEFAULT NULL,
  `aadhaar_enc` varbinary(128) DEFAULT NULL COMMENT 'AES-256-GCM ciphertext (iv+data+tag)',
  `aadhaar_hash` binary(32) DEFAULT NULL COMMENT 'HMAC-SHA256(aadhaar) for uniqueness / search',
  `aadhaar_last4` char(4) DEFAULT NULL COMMENT 'for masked display XXXX-XXXX-1234',
  `mediclaim_policy_no` varchar(50) DEFAULT NULL COMMENT 'GP Mediclaim Insurance Number',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`emp_id`),
  UNIQUE KEY `uk_stat_pan` (`pan_no`),
  UNIQUE KEY `uk_stat_uan` (`uan_no`),
  UNIQUE KEY `uk_stat_pf` (`pf_no`),
  UNIQUE KEY `uk_stat_esi` (`esi_no`),
  UNIQUE KEY `uk_stat_aadhaar` (`aadhaar_hash`),
  CONSTRAINT `fk_stat_emp` FOREIGN KEY (`emp_id`) REFERENCES `emp_detail` (`emp_id`),
  CONSTRAINT `chk_stat_pan` CHECK (((`pan_no` is null) or regexp_like(`pan_no`,_utf8mb4'^[A-Z]{5}[0-9]{4}[A-Z]$',_utf8mb4'c'))),
  CONSTRAINT `chk_stat_uan` CHECK (((`uan_no` is null) or regexp_like(`uan_no`,_utf8mb4'^[0-9]{12}$')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `employee_legacy_backup` */

DROP TABLE IF EXISTS `employee_legacy_backup`;

CREATE TABLE `employee_legacy_backup` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `emp_name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `designation_oid` bigint DEFAULT NULL,
  `emp_level` int DEFAULT '1',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `district_oid` bigint DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `photo` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_emp_id` (`emp_id`),
  UNIQUE KEY `uq_emp_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `hq_group_master` */

DROP TABLE IF EXISTS `hq_group_master`;

CREATE TABLE `hq_group_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `group_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hq_group_code` (`group_code`),
  UNIQUE KEY `uk_hq_group_name` (`group_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `hq_master` */

DROP TABLE IF EXISTS `hq_master`;

CREATE TABLE `hq_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `hq_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hq_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_group_id` bigint unsigned DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_hq_code` (`hq_code`),
  KEY `ix_hq_state` (`state_oid`),
  KEY `idx_hq_hq_group` (`hq_group_id`),
  CONSTRAINT `fk_hq_hq_group` FOREIGN KEY (`hq_group_id`) REFERENCES `hq_group_master` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `image_type_master` */

DROP TABLE IF EXISTS `image_type_master`;

CREATE TABLE `image_type_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `image_type_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_image_type_code` (`image_type_code`),
  UNIQUE KEY `uk_image_type_name` (`image_type_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `item_type_master` */

DROP TABLE IF EXISTS `item_type_master`;

CREATE TABLE `item_type_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `item_type_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_type_code` (`item_type_code`),
  UNIQUE KEY `uk_item_type_name` (`item_type_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `label_master` */

DROP TABLE IF EXISTS `label_master`;

CREATE TABLE `label_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `label_key` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `label_value` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `lang` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_label` (`label_key`,`lang`),
  KEY `ix_label_status` (`lang`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `meeting_type_master` */

DROP TABLE IF EXISTS `meeting_type_master`;

CREATE TABLE `meeting_type_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `meeting_type_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `meeting_type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meeting_type_code` (`meeting_type_code`),
  UNIQUE KEY `uk_meeting_type_name` (`meeting_type_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `menu_audit_log` */

DROP TABLE IF EXISTS `menu_audit_log`;

CREATE TABLE `menu_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_item_id` bigint DEFAULT NULL,
  `label` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `menu_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `changed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `changed_at` datetime NOT NULL,
  `details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_menu_audit_item` (`menu_item_id`),
  KEY `idx_menu_audit_changed_at` (`changed_at`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `menu_visibility` */

DROP TABLE IF EXISTS `menu_visibility`;

CREATE TABLE `menu_visibility` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `menu_item_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `sort_order` int DEFAULT NULL,
  `is_visible` tinyint(1) NOT NULL DEFAULT '1',
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_menu_visibility` (`emp_id`,`menu_item_id`),
  KEY `ix_menu_visibility_emp` (`emp_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `pin_master` */

DROP TABLE IF EXISTS `pin_master`;

CREATE TABLE `pin_master` (
  `id` int NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(50) NOT NULL,
  `pin` varchar(20) NOT NULL,
  `app_version` varchar(200) DEFAULT NULL,
  `app_version_code` varchar(200) DEFAULT NULL,
  `android_version` varchar(200) DEFAULT NULL,
  `imei` varchar(200) DEFAULT NULL,
  `device_name` varchar(200) DEFAULT NULL,
  `model_no` varchar(200) DEFAULT NULL,
  `width` varchar(50) DEFAULT NULL,
  `height` varchar(50) DEFAULT NULL,
  `battery` varchar(10) DEFAULT NULL,
  `lat` varchar(50) DEFAULT NULL,
  `lon` varchar(50) DEFAULT NULL,
  `device_os` varchar(50) DEFAULT NULL,
  `creation_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_modified_date_time` timestamp NULL DEFAULT NULL,
  `fcm_token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_Date` datetime DEFAULT NULL,
  `is_valid` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `product` */

DROP TABLE IF EXISTS `product`;

CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `report_activity` */

DROP TABLE IF EXISTS `report_activity`;

CREATE TABLE `report_activity` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_label` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `filters_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`oid`),
  KEY `idx_report_activity_user_report` (`username`,`report_key`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `route` */

DROP TABLE IF EXISTS `route`;

CREATE TABLE `route` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `zone_oid` bigint DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `description` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_code` (`route_code`),
  KEY `ix_route_status` (`status`),
  KEY `ix_route_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `route_area_map` */

DROP TABLE IF EXISTS `route_area_map`;

CREATE TABLE `route_area_map` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_oid` bigint NOT NULL,
  `area_oid` bigint NOT NULL,
  `visit_sequence` int DEFAULT '0',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_area` (`route_oid`,`area_oid`),
  KEY `ix_ram_route` (`route_oid`),
  KEY `ix_ram_area` (`area_oid`),
  CONSTRAINT `fk_ram_area` FOREIGN KEY (`area_oid`) REFERENCES `city_master` (`oid`),
  CONSTRAINT `fk_ram_route` FOREIGN KEY (`route_oid`) REFERENCES `route` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `speciality_master` */

DROP TABLE IF EXISTS `speciality_master`;

CREATE TABLE `speciality_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `speciality_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `speciality_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT NULL,
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_speciality_master_code` (`speciality_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `sponsorship_type_master` */

DROP TABLE IF EXISTS `sponsorship_type_master`;

CREATE TABLE `sponsorship_type_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sponsorship_type_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sponsorship_type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sponsorship_type_code` (`sponsorship_type_code`),
  UNIQUE KEY `uk_sponsorship_type_name` (`sponsorship_type_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `state_master` */

DROP TABLE IF EXISTS `state_master`;

CREATE TABLE `state_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `state_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `country_oid` bigint DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_state_code` (`state_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `travel_type_master` */

DROP TABLE IF EXISTS `travel_type_master`;

CREATE TABLE `travel_type_master` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `travel_type_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `travel_type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `short_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_travel_type_code` (`travel_type_code`),
  UNIQUE KEY `uk_travel_type_name` (`travel_type_name`),
  KEY `idx_active` (`is_active`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `user_menu_favorite` */

DROP TABLE IF EXISTS `user_menu_favorite`;

CREATE TABLE `user_menu_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `menu_item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_menu_favorite` (`username`,`menu_item_id`),
  KEY `idx_umf_user` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `user_preference` */

DROP TABLE IF EXISTS `user_preference`;

CREATE TABLE `user_preference` (
  `username` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nav_layout` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'vertical',
  `theme_mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'light',
  `preset` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'default',
  `font_scale` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1.0',
  `density` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'comfortable',
  `sidebar_collapsed` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `user_theme_token` */

DROP TABLE IF EXISTS `user_theme_token`;

CREATE TABLE `user_theme_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mode` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_theme_token` (`username`,`mode`,`token_key`),
  KEY `idx_user_theme_token_user` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `visit_type_master` */

DROP TABLE IF EXISTS `visit_type_master`;

CREATE TABLE `visit_type_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_type_id` bigint DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_visit_type_master_code` (`type_code`),
  KEY `idx_visit_type_client_type` (`client_type_id`),
  CONSTRAINT `fk_visit_type_client_type` FOREIGN KEY (`client_type_id`) REFERENCES `client_type` (`oid`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Table structure for table `zone_master` */

DROP TABLE IF EXISTS `zone_master`;

CREATE TABLE `zone_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `zone_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `zone_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_zone_code` (`zone_code`),
  KEY `ix_zone_div` (`division_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
