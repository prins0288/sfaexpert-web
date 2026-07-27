-- ============================================================================
-- Clone sfa_demo's full table structure into every tenant DB, each with
-- DIFFERENT regional data. Generated from sfa_demo schema.
--   acme_db    -> Gujarat/Rajasthan
--   globex_db  -> South India
--   initech_db -> Delhi NCR
-- Run:  mysql -u myroot < src/main/resources/sql/06_clone_sfa_demo_to_tenants.sql
-- ============================================================================

-- ==================== acme_db ====================
DROP DATABASE IF EXISTS `acme_db`;
CREATE DATABASE `acme_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `acme_db`;
SET FOREIGN_KEY_CHECKS=0;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `area` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `area_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `pincode` varchar(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_area_code` (`area_code`),
  KEY `ix_area_status` (`status`),
  KEY `ix_area_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_cat_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `client_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_type_oid` bigint NOT NULL,
  `prefix` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `firm_name` varchar(180) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `degree_oid` bigint DEFAULT NULL,
  `speciality_oid` bigint DEFAULT NULL,
  `category_oid` bigint DEFAULT NULL,
  `route_oid` bigint DEFAULT NULL,
  `area_oid` bigint DEFAULT NULL,
  `address` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `anniversary` date DEFAULT NULL,
  `drug_license_no` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gst_no` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_client_code` (`client_code`),
  KEY `ix_client_type` (`client_type_oid`),
  KEY `ix_client_area` (`area_oid`),
  KEY `ix_client_route` (`route_oid`),
  KEY `ix_client_status` (`status`),
  CONSTRAINT `fk_client_type` FOREIGN KEY (`client_type_oid`) REFERENCES `client_type` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_type` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `singular_label` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plural_label` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_ctype_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `degree` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `degree_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `degree_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_degree_code` (`degree_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `division` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `division_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_division_code` (`division_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emp_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `designation_oid` bigint DEFAULT NULL,
  `emp_level` int DEFAULT '1',
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_emp_id` (`emp_id`),
  UNIQUE KEY `uq_emp_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hq` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `hq_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hq_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_hq_code` (`hq_code`),
  KEY `ix_hq_state` (`state_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `label_key` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `label_value` varchar(400) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lang` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_label` (`label_key`,`lang`),
  KEY `ix_label_status` (`lang`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `zone_oid` bigint DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `description` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_code` (`route_code`),
  KEY `ix_route_status` (`status`),
  KEY `ix_route_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_area_map` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_oid` bigint NOT NULL,
  `area_oid` bigint NOT NULL,
  `visit_sequence` int DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_area` (`route_oid`,`area_oid`),
  KEY `ix_ram_route` (`route_oid`),
  KEY `ix_ram_area` (`area_oid`),
  CONSTRAINT `fk_ram_area` FOREIGN KEY (`area_oid`) REFERENCES `area` (`oid`),
  CONSTRAINT `fk_ram_route` FOREIGN KEY (`route_oid`) REFERENCES `route` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `speciality` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `speciality_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `speciality_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_spec_code` (`speciality_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `state` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `state_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_state_code` (`state_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zone` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `zone_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `zone_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_zone_code` (`zone_code`),
  KEY `ix_zone_div` (`division_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
-- ACME — Gujarat / Rajasthan region
INSERT INTO division (oid,division_code,division_name,status) VALUES
 (1,'DIV-GEN','General Division','Y'),(2,'DIV-ONCO','Onco Division','Y');
INSERT INTO zone (oid,zone_code,zone_name,division_oid,status) VALUES
 (1,'ZN-W','West Zone',1,'Y'),(2,'ZN-C','Central Zone',1,'Y');
INSERT INTO state (oid,state_code,state_name,status) VALUES
 (1,'GJ','Gujarat','Y'),(2,'RJ','Rajasthan','Y'),(3,'MP','Madhya Pradesh','Y');
INSERT INTO hq (oid,hq_code,hq_name,state_oid,status) VALUES
 (1,'HQ-AHM','Ahmedabad HQ',1,'Y'),(2,'HQ-SUR','Surat HQ',1,'Y'),(3,'HQ-JAI','Jaipur HQ',2,'Y');
INSERT INTO degree (oid,degree_code,degree_name,status) VALUES
 (1,'MBBS','MBBS','Y'),(2,'MD','MD','Y'),(3,'BHMS','BHMS','Y');
INSERT INTO speciality (oid,speciality_code,speciality_name,status) VALUES
 (1,'ONCO','Oncologist','Y'),(2,'GP','General Physician','Y'),(3,'PED','Pediatrician','Y');
INSERT INTO category (oid,category_code,category_name,status) VALUES
 (1,'A','Category A','Y'),(2,'B','Category B','Y'),(3,'C','Category C','Y');
INSERT INTO client_type (oid,type_code,type_name,singular_label,plural_label,status) VALUES
 (1,'DOC','Doctor','Doctor','Doctors','Y'),
 (2,'CHM','Chemist','Chemist','Chemists','Y'),
 (3,'STK','Stockist','Stockist','Stockists','Y');
INSERT INTO employee (oid,emp_id,emp_name,username,password_hash,emp_level,mobile,email,state_oid,status) VALUES
 (1,'EMP001','Acme Admin','admin','$2b$10$TMaeMErz4ptkuIJRjZMCtOc8kaQ0WFDkci2zvIe5n9/o0rWyISxqq',9,'9000000001','admin@acme.com',1,'Y');
INSERT INTO route (oid,route_code,route_name,division_oid,zone_oid,state_oid,hq_oid,distance_km,description,status) VALUES
 (1,'RT-AHM-01','Ahmedabad City Beat',1,1,1,1,14.00,'Ahmedabad core beat','Y'),
 (2,'RT-SUR-01','Surat Beat',1,1,1,2,20.00,'Surat textile belt','Y'),
 (3,'RT-JAI-01','Jaipur Beat',2,2,2,3,25.00,'Jaipur pink city beat','Y');
INSERT INTO area (oid,area_code,area_name,city,state_oid,hq_oid,pincode,area_type,distance_km,status) VALUES
 (1,'AR-NAV','Navrangpura','Ahmedabad',1,1,'380009','Core',3.00,'Y'),
 (2,'AR-SAT','Satellite','Ahmedabad',1,1,'380015','Core',5.50,'Y'),
 (3,'AR-ADAJ','Adajan','Surat',1,2,'395009','Core',4.00,'Y'),
 (4,'AR-MALV','Malviya Nagar','Jaipur',2,3,'302017','Metro',6.00,'Y');
INSERT INTO route_area_map (oid,route_oid,area_oid,visit_sequence,status) VALUES
 (1,1,1,1,'Y'),(2,1,2,2,'Y'),(3,2,3,1,'Y'),(4,3,4,1,'Y'),(5,3,1,2,'Y');
INSERT INTO client (oid,client_code,client_type_oid,prefix,name,firm_name,degree_oid,speciality_oid,category_oid,route_oid,area_oid,address,city,mobile,email,status) VALUES
 (1,'CL-D001',1,'Dr.','Kiran Patel',NULL,1,1,1,1,1,'Navrangpura','Ahmedabad','9820011111','kpatel@acme.com','Y'),
 (2,'CL-D002',1,'Dr.','Meena Desai',NULL,2,2,2,1,2,'Satellite','Ahmedabad','9820022222','mdesai@acme.com','Y'),
 (3,'CL-C001',2,NULL,'Surat Medical Store','Surat Medical & General',NULL,NULL,1,2,3,'Adajan','Surat','9820033333',NULL,'Y'),
 (4,'CL-S001',3,NULL,'Jaipur Distributors','Jaipur Pharma Distributors',NULL,NULL,1,3,4,'Malviya Nagar','Jaipur','9810044444',NULL,'Y');
INSERT INTO product (id,name,price) VALUES
 (1,'ACME Anvil',1200.00),(2,'ACME Rocket',9500.00),(3,'ACME Dynamite',300.00),(4,'ACME Jetpack',15000.00);
SET FOREIGN_KEY_CHECKS=1;

-- ==================== globex_db ====================
DROP DATABASE IF EXISTS `globex_db`;
CREATE DATABASE `globex_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `globex_db`;
SET FOREIGN_KEY_CHECKS=0;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `area` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `area_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `pincode` varchar(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_area_code` (`area_code`),
  KEY `ix_area_status` (`status`),
  KEY `ix_area_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_cat_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `client_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_type_oid` bigint NOT NULL,
  `prefix` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `firm_name` varchar(180) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `degree_oid` bigint DEFAULT NULL,
  `speciality_oid` bigint DEFAULT NULL,
  `category_oid` bigint DEFAULT NULL,
  `route_oid` bigint DEFAULT NULL,
  `area_oid` bigint DEFAULT NULL,
  `address` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `anniversary` date DEFAULT NULL,
  `drug_license_no` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gst_no` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_client_code` (`client_code`),
  KEY `ix_client_type` (`client_type_oid`),
  KEY `ix_client_area` (`area_oid`),
  KEY `ix_client_route` (`route_oid`),
  KEY `ix_client_status` (`status`),
  CONSTRAINT `fk_client_type` FOREIGN KEY (`client_type_oid`) REFERENCES `client_type` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_type` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `singular_label` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plural_label` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_ctype_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `degree` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `degree_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `degree_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_degree_code` (`degree_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `division` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `division_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_division_code` (`division_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emp_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `designation_oid` bigint DEFAULT NULL,
  `emp_level` int DEFAULT '1',
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_emp_id` (`emp_id`),
  UNIQUE KEY `uq_emp_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hq` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `hq_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hq_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_hq_code` (`hq_code`),
  KEY `ix_hq_state` (`state_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `label_key` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `label_value` varchar(400) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lang` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_label` (`label_key`,`lang`),
  KEY `ix_label_status` (`lang`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `zone_oid` bigint DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `description` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_code` (`route_code`),
  KEY `ix_route_status` (`status`),
  KEY `ix_route_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_area_map` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_oid` bigint NOT NULL,
  `area_oid` bigint NOT NULL,
  `visit_sequence` int DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_area` (`route_oid`,`area_oid`),
  KEY `ix_ram_route` (`route_oid`),
  KEY `ix_ram_area` (`area_oid`),
  CONSTRAINT `fk_ram_area` FOREIGN KEY (`area_oid`) REFERENCES `area` (`oid`),
  CONSTRAINT `fk_ram_route` FOREIGN KEY (`route_oid`) REFERENCES `route` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `speciality` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `speciality_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `speciality_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_spec_code` (`speciality_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `state` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `state_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_state_code` (`state_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zone` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `zone_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `zone_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_zone_code` (`zone_code`),
  KEY `ix_zone_div` (`division_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
-- GLOBEX — South India region
INSERT INTO division (oid,division_code,division_name,status) VALUES
 (1,'DIV-CARD','Cardio Division','Y'),(2,'DIV-NEURO','Neuro Division','Y');
INSERT INTO zone (oid,zone_code,zone_name,division_oid,status) VALUES
 (1,'ZN-S','South Zone',1,'Y'),(2,'ZN-SE','South-East Zone',1,'Y');
INSERT INTO state (oid,state_code,state_name,status) VALUES
 (1,'KA','Karnataka','Y'),(2,'TN','Tamil Nadu','Y'),(3,'KL','Kerala','Y');
INSERT INTO hq (oid,hq_code,hq_name,state_oid,status) VALUES
 (1,'HQ-BLR','Bangalore HQ',1,'Y'),(2,'HQ-CHN','Chennai HQ',2,'Y'),(3,'HQ-KOC','Kochi HQ',3,'Y');
INSERT INTO degree (oid,degree_code,degree_name,status) VALUES
 (1,'MBBS','MBBS','Y'),(2,'MD','MD','Y'),(3,'DM','DM','Y');
INSERT INTO speciality (oid,speciality_code,speciality_name,status) VALUES
 (1,'CARD','Cardiologist','Y'),(2,'NEURO','Neurologist','Y'),(3,'GP','General Physician','Y');
INSERT INTO category (oid,category_code,category_name,status) VALUES
 (1,'A','Category A','Y'),(2,'B','Category B','Y'),(3,'C','Category C','Y');
INSERT INTO client_type (oid,type_code,type_name,singular_label,plural_label,status) VALUES
 (1,'DOC','Doctor','Doctor','Doctors','Y'),
 (2,'CHM','Chemist','Chemist','Chemists','Y'),
 (3,'STK','Stockist','Stockist','Stockists','Y');
INSERT INTO employee (oid,emp_id,emp_name,username,password_hash,emp_level,mobile,email,state_oid,status) VALUES
 (1,'EMP001','Globex Admin','admin','$2b$10$TMaeMErz4ptkuIJRjZMCtOc8kaQ0WFDkci2zvIe5n9/o0rWyISxqq',9,'9000000002','admin@globex.com',1,'Y');
INSERT INTO route (oid,route_code,route_name,division_oid,zone_oid,state_oid,hq_oid,distance_km,description,status) VALUES
 (1,'RT-BLR-01','Bangalore City Beat',1,1,1,1,16.00,'Bangalore core beat','Y'),
 (2,'RT-CHN-01','Chennai Beat',1,1,2,2,22.00,'Chennai metro beat','Y'),
 (3,'RT-KOC-01','Kochi Beat',2,2,3,3,19.00,'Kochi backwater beat','Y');
INSERT INTO area (oid,area_code,area_name,city,state_oid,hq_oid,pincode,area_type,distance_km,status) VALUES
 (1,'AR-INDI','Indiranagar','Bangalore',1,1,'560038','Core',3.20,'Y'),
 (2,'AR-JAYA','Jayanagar','Bangalore',1,1,'560011','Core',5.00,'Y'),
 (3,'AR-TNAG','T Nagar','Chennai',2,2,'600017','Metro',4.80,'Y'),
 (4,'AR-MGRD','MG Road','Kochi',3,3,'682035','Core',2.50,'Y');
INSERT INTO route_area_map (oid,route_oid,area_oid,visit_sequence,status) VALUES
 (1,1,1,1,'Y'),(2,1,2,2,'Y'),(3,2,3,1,'Y'),(4,3,4,1,'Y'),(5,2,1,3,'Y');
INSERT INTO client (oid,client_code,client_type_oid,prefix,name,firm_name,degree_oid,speciality_oid,category_oid,route_oid,area_oid,address,city,mobile,email,status) VALUES
 (1,'CL-D001',1,'Dr.','Suresh Rao',NULL,1,1,1,1,1,'Indiranagar','Bangalore','9880011111','srao@globex.com','Y'),
 (2,'CL-D002',1,'Dr.','Latha Iyer',NULL,2,2,2,2,3,'T Nagar','Chennai','9880022222','liyer@globex.com','Y'),
 (3,'CL-C001',2,NULL,'Jayanagar Pharmacy','Jayanagar Health Pharmacy',NULL,NULL,1,1,2,'Jayanagar','Bangalore','9880033333',NULL,'Y'),
 (4,'CL-S001',3,NULL,'Kochi Distributors','Kochi Medical Distributors',NULL,NULL,1,3,4,'MG Road','Kochi','9840044444',NULL,'Y');
INSERT INTO product (id,name,price) VALUES
 (1,'Globex Reactor',250000.00),(2,'Globex Turbine',88000.00),(3,'Globex Generator',42000.00);
SET FOREIGN_KEY_CHECKS=1;

-- ==================== initech_db ====================
DROP DATABASE IF EXISTS `initech_db`;
CREATE DATABASE `initech_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `initech_db`;
SET FOREIGN_KEY_CHECKS=0;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `area` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `area_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `pincode` varchar(12) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_area_code` (`area_code`),
  KEY `ix_area_status` (`status`),
  KEY `ix_area_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `category_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_cat_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `client_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_type_oid` bigint NOT NULL,
  `prefix` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `firm_name` varchar(180) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `degree_oid` bigint DEFAULT NULL,
  `speciality_oid` bigint DEFAULT NULL,
  `category_oid` bigint DEFAULT NULL,
  `route_oid` bigint DEFAULT NULL,
  `area_oid` bigint DEFAULT NULL,
  `address` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `anniversary` date DEFAULT NULL,
  `drug_license_no` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gst_no` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remarks` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_client_code` (`client_code`),
  KEY `ix_client_type` (`client_type_oid`),
  KEY `ix_client_area` (`area_oid`),
  KEY `ix_client_route` (`route_oid`),
  KEY `ix_client_status` (`status`),
  CONSTRAINT `fk_client_type` FOREIGN KEY (`client_type_oid`) REFERENCES `client_type` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_type` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `singular_label` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plural_label` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_ctype_code` (`type_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `degree` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `degree_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `degree_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_degree_code` (`degree_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `division` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `division_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_division_code` (`division_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `emp_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `designation_oid` bigint DEFAULT NULL,
  `emp_level` int DEFAULT '1',
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_emp_id` (`emp_id`),
  UNIQUE KEY `uq_emp_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hq` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `hq_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hq_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_hq_code` (`hq_code`),
  KEY `ix_hq_state` (`state_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label_master` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `label_key` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `label_value` varchar(400) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lang` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_label` (`label_key`,`lang`),
  KEY `ix_label_status` (`lang`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `route_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `zone_oid` bigint DEFAULT NULL,
  `state_oid` bigint DEFAULT NULL,
  `hq_oid` bigint DEFAULT NULL,
  `distance_km` decimal(8,2) DEFAULT NULL,
  `description` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_code` (`route_code`),
  KEY `ix_route_status` (`status`),
  KEY `ix_route_hq` (`hq_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_area_map` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `route_oid` bigint NOT NULL,
  `area_oid` bigint NOT NULL,
  `visit_sequence` int DEFAULT '0',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_route_area` (`route_oid`,`area_oid`),
  KEY `ix_ram_route` (`route_oid`),
  KEY `ix_ram_area` (`area_oid`),
  CONSTRAINT `fk_ram_area` FOREIGN KEY (`area_oid`) REFERENCES `area` (`oid`),
  CONSTRAINT `fk_ram_route` FOREIGN KEY (`route_oid`) REFERENCES `route` (`oid`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `speciality` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `speciality_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `speciality_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_spec_code` (`speciality_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `state` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `state_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_state_code` (`state_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zone` (
  `oid` bigint NOT NULL AUTO_INCREMENT,
  `zone_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `zone_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `division_oid` bigint DEFAULT NULL,
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`),
  UNIQUE KEY `uq_zone_code` (`zone_code`),
  KEY `ix_zone_div` (`division_oid`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
-- INITECH — Delhi NCR region
INSERT INTO division (oid,division_code,division_name,status) VALUES
 (1,'DIV-DERM','Derma Division','Y'),(2,'DIV-GAS','Gastro Division','Y');
INSERT INTO zone (oid,zone_code,zone_name,division_oid,status) VALUES
 (1,'ZN-N','North Zone',1,'Y'),(2,'ZN-NW','North-West Zone',1,'Y');
INSERT INTO state (oid,state_code,state_name,status) VALUES
 (1,'DL','Delhi','Y'),(2,'UP','Uttar Pradesh','Y'),(3,'HR','Haryana','Y');
INSERT INTO hq (oid,hq_code,hq_name,state_oid,status) VALUES
 (1,'HQ-DEL','Delhi HQ',1,'Y'),(2,'HQ-NOI','Noida HQ',2,'Y'),(3,'HQ-GUR','Gurgaon HQ',3,'Y');
INSERT INTO degree (oid,degree_code,degree_name,status) VALUES
 (1,'MBBS','MBBS','Y'),(2,'MD','MD','Y'),(3,'DNB','DNB','Y');
INSERT INTO speciality (oid,speciality_code,speciality_name,status) VALUES
 (1,'DERM','Dermatologist','Y'),(2,'GASTRO','Gastroenterologist','Y'),(3,'GP','General Physician','Y');
INSERT INTO category (oid,category_code,category_name,status) VALUES
 (1,'A','Category A','Y'),(2,'B','Category B','Y'),(3,'C','Category C','Y');
INSERT INTO client_type (oid,type_code,type_name,singular_label,plural_label,status) VALUES
 (1,'DOC','Doctor','Doctor','Doctors','Y'),
 (2,'CHM','Chemist','Chemist','Chemists','Y'),
 (3,'STK','Stockist','Stockist','Stockists','Y');
INSERT INTO employee (oid,emp_id,emp_name,username,password_hash,emp_level,mobile,email,state_oid,status) VALUES
 (1,'EMP001','Initech Admin','admin','$2b$10$TMaeMErz4ptkuIJRjZMCtOc8kaQ0WFDkci2zvIe5n9/o0rWyISxqq',9,'9000000003','admin@initech.com',1,'Y');
INSERT INTO route (oid,route_code,route_name,division_oid,zone_oid,state_oid,hq_oid,distance_km,description,status) VALUES
 (1,'RT-DEL-01','Delhi Central Beat',1,1,1,1,11.00,'Delhi core beat','Y'),
 (2,'RT-NOI-01','Noida Beat',1,1,2,2,17.50,'Noida sectors beat','Y'),
 (3,'RT-GUR-01','Gurgaon Beat',2,2,3,3,21.00,'Gurgaon cyber beat','Y');
INSERT INTO area (oid,area_code,area_name,city,state_oid,hq_oid,pincode,area_type,distance_km,status) VALUES
 (1,'AR-CP','Connaught Place','Delhi',1,1,'110001','Metro',2.00,'Y'),
 (2,'AR-SAKET','Saket','Delhi',1,1,'110017','Core',7.00,'Y'),
 (3,'AR-SEC18','Sector 18','Noida',2,2,'201301','Metro',5.50,'Y'),
 (4,'AR-CYBER','Cyber City','Gurgaon',3,3,'122002','Metro',6.50,'Y');
INSERT INTO route_area_map (oid,route_oid,area_oid,visit_sequence,status) VALUES
 (1,1,1,1,'Y'),(2,1,2,2,'Y'),(3,2,3,1,'Y'),(4,3,4,1,'Y'),(5,1,3,3,'Y');
INSERT INTO client (oid,client_code,client_type_oid,prefix,name,firm_name,degree_oid,speciality_oid,category_oid,route_oid,area_oid,address,city,mobile,email,status) VALUES
 (1,'CL-D001',1,'Dr.','Amit Khanna',NULL,1,1,1,1,1,'Connaught Place','Delhi','9810011111','akhanna@initech.com','Y'),
 (2,'CL-D002',1,'Dr.','Ritu Malhotra',NULL,2,2,2,3,3,'Sector 18','Noida','9810022222','rmalhotra@initech.com','Y'),
 (3,'CL-C001',2,NULL,'Saket Chemist','Saket Health Chemist',NULL,NULL,1,1,2,'Saket','Delhi','9810033333',NULL,'Y'),
 (4,'CL-S001',3,NULL,'Cyber Distributors','Gurgaon Cyber Distributors',NULL,NULL,1,3,4,'Cyber City','Gurgaon','9810044444',NULL,'Y');
INSERT INTO product (id,name,price) VALUES
 (1,'TPS Report Cover',5.00),(2,'Red Stapler',45.00),(3,'Laser Printer',1200.00),(4,'Flair (37 pieces)',90.00),(5,'Coffee Mug',12.00);
SET FOREIGN_KEY_CHECKS=1;
