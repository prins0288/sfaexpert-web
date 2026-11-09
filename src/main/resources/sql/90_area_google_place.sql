-- ===========================================================================
-- 90: Area master - Google place auto-complete.
--   area_master: google_place_name (full name), google_place_id, latitude, longitude
--   sfa_central.company_details: google_api_key (the company's Google Maps/Places key, plain text)
--   Enable "Places API (New)" for the key; a browser key restricted to your site's URL works.
-- Safe to re-run. Run: mysql -u myroot < 90_area_google_place.sql
-- ===========================================================================
USE sfa_central;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'company_details' AND column_name = 'google_api_key') = 0,
 "ALTER TABLE company_details ADD google_api_key VARCHAR(200) NULL COMMENT 'Google Maps / Places API key of this company (area name auto-complete)'", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
-- UPDATE company_details SET google_api_key = 'YOUR_KEY' WHERE company_code = 'acme';

USE acme_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'google_place_name') = 0,
 "ALTER TABLE area_master
    ADD google_place_name VARCHAR(300) NULL COMMENT 'full place name picked from Google, e.g. Adajan, Surat, Gujarat, India (area_name = its first comma part)' AFTER pincode,
    ADD google_place_id   VARCHAR(200) NULL COMMENT 'Google place id of the picked place' AFTER google_place_name,
    ADD latitude          DECIMAL(10,7) NULL COMMENT 'latitude of the place (Google)' AFTER google_place_id,
    ADD longitude         DECIMAL(10,7) NULL COMMENT 'longitude of the place (Google)' AFTER latitude", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

USE globex_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'google_place_name') = 0,
 "ALTER TABLE area_master
    ADD google_place_name VARCHAR(300) NULL COMMENT 'full place name picked from Google, e.g. Adajan, Surat, Gujarat, India (area_name = its first comma part)' AFTER pincode,
    ADD google_place_id   VARCHAR(200) NULL COMMENT 'Google place id of the picked place' AFTER google_place_name,
    ADD latitude          DECIMAL(10,7) NULL COMMENT 'latitude of the place (Google)' AFTER google_place_id,
    ADD longitude         DECIMAL(10,7) NULL COMMENT 'longitude of the place (Google)' AFTER latitude", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

USE initech_db;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'google_place_name') = 0,
 "ALTER TABLE area_master
    ADD google_place_name VARCHAR(300) NULL COMMENT 'full place name picked from Google, e.g. Adajan, Surat, Gujarat, India (area_name = its first comma part)' AFTER pincode,
    ADD google_place_id   VARCHAR(200) NULL COMMENT 'Google place id of the picked place' AFTER google_place_name,
    ADD latitude          DECIMAL(10,7) NULL COMMENT 'latitude of the place (Google)' AFTER google_place_id,
    ADD longitude         DECIMAL(10,7) NULL COMMENT 'longitude of the place (Google)' AFTER latitude", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;

USE sfa_demo;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'area_master' AND column_name = 'google_place_name') = 0,
 "ALTER TABLE area_master
    ADD google_place_name VARCHAR(300) NULL COMMENT 'full place name picked from Google, e.g. Adajan, Surat, Gujarat, India (area_name = its first comma part)' AFTER pincode,
    ADD google_place_id   VARCHAR(200) NULL COMMENT 'Google place id of the picked place' AFTER google_place_name,
    ADD latitude          DECIMAL(10,7) NULL COMMENT 'latitude of the place (Google)' AFTER google_place_id,
    ADD longitude         DECIMAL(10,7) NULL COMMENT 'longitude of the place (Google)' AFTER latitude", 'SELECT 1');
PREPARE p FROM @s; EXECUTE p; DEALLOCATE PREPARE p;
