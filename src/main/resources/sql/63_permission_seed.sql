-- ===========================================================================
-- 63_permission_seed.sql — PER TENANT database (acme_db, globex_db, initech_db, sfa_demo)
--
-- 1. Seeds permission_master with every code the backend now checks via
--    @RequiresPermission on the master controllers:
--        <MODULE>_SAVE    add / edit / Add Multiple
--        <MODULE>_STATUS  activate / deactivate
--        <MODULE>_UPLOAD  bulk Excel upload
--    Seeding the catalog changes NOTHING about access by itself — with no
--    permission_assignment rows every code still resolves to "allowed".
-- 2. Adds the "Permission Master" page under Masters > Master Entry (id 518).
--
-- Requires 13_permission_master.sql. Idempotent (INSERT IGNORE / NOT EXISTS).
-- Run:  mysql -u myroot < src/main/resources/sql/63_permission_seed.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ACTIVITY_TYPE_SAVE', 'Activity Type', 'Add / edit Activity Type', 1, NOW(), 'seed'),
    ('ACTIVITY_TYPE_STATUS', 'Activity Type', 'Activate / deactivate Activity Type', 1, NOW(), 'seed'),
    ('AREA_SAVE', 'Area', 'Add / edit Area', 1, NOW(), 'seed'),
    ('AREA_STATUS', 'Area', 'Activate / deactivate Area', 1, NOW(), 'seed'),
    ('AREA_UPLOAD', 'Area', 'Bulk Excel upload of Area', 1, NOW(), 'seed'),
    ('BANK_SAVE', 'Bank', 'Add / edit Bank', 1, NOW(), 'seed'),
    ('BANK_STATUS', 'Bank', 'Activate / deactivate Bank', 1, NOW(), 'seed'),
    ('CATEGORY_SAVE', 'Category', 'Add / edit Category', 1, NOW(), 'seed'),
    ('CATEGORY_STATUS', 'Category', 'Activate / deactivate Category', 1, NOW(), 'seed'),
    ('CLIENT_SAVE', 'Client', 'Add / edit Client', 1, NOW(), 'seed'),
    ('CLIENT_STATUS', 'Client', 'Activate / deactivate Client', 1, NOW(), 'seed'),
    ('CLIENT_UPLOAD', 'Client', 'Bulk Excel upload of Client', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_SAVE', 'Client Type', 'Add / edit Client Type', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_STATUS', 'Client Type', 'Activate / deactivate Client Type', 1, NOW(), 'seed'),
    ('COUNTRY_SAVE', 'Country', 'Add / edit Country', 1, NOW(), 'seed'),
    ('COUNTRY_STATUS', 'Country', 'Activate / deactivate Country', 1, NOW(), 'seed'),
    ('DEGREE_SAVE', 'Degree', 'Add / edit Degree', 1, NOW(), 'seed'),
    ('DEGREE_STATUS', 'Degree', 'Activate / deactivate Degree', 1, NOW(), 'seed'),
    ('DIVISION_SAVE', 'Division', 'Add / edit Division', 1, NOW(), 'seed'),
    ('DIVISION_STATUS', 'Division', 'Activate / deactivate Division', 1, NOW(), 'seed'),
    ('DIVISION_UPLOAD', 'Division', 'Bulk Excel upload of Division', 1, NOW(), 'seed'),
    ('DOCUMENT_SAVE', 'Document', 'Add / edit Document', 1, NOW(), 'seed'),
    ('DOCUMENT_STATUS', 'Document', 'Activate / deactivate Document', 1, NOW(), 'seed'),
    ('EMP_DETAIL_SAVE', 'Create Employee', 'Add / edit Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_STATUS', 'Create Employee', 'Activate / deactivate Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_UPLOAD', 'Create Employee', 'Bulk Excel upload of Create Employee', 1, NOW(), 'seed'),
    ('HQ_SAVE', 'HQ', 'Add / edit HQ', 1, NOW(), 'seed'),
    ('HQ_STATUS', 'HQ', 'Activate / deactivate HQ', 1, NOW(), 'seed'),
    ('HQ_UPLOAD', 'HQ', 'Bulk Excel upload of HQ', 1, NOW(), 'seed'),
    ('HQ_GROUP_SAVE', 'HQ Group', 'Add / edit HQ Group', 1, NOW(), 'seed'),
    ('HQ_GROUP_STATUS', 'HQ Group', 'Activate / deactivate HQ Group', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_SAVE', 'Image Type', 'Add / edit Image Type', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_STATUS', 'Image Type', 'Activate / deactivate Image Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_SAVE', 'Item Type', 'Add / edit Item Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_STATUS', 'Item Type', 'Activate / deactivate Item Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_SAVE', 'Meeting Type', 'Add / edit Meeting Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_STATUS', 'Meeting Type', 'Activate / deactivate Meeting Type', 1, NOW(), 'seed'),
    ('PRODUCT_SAVE', 'Product', 'Add / edit Product', 1, NOW(), 'seed'),
    ('ROUTE_SAVE', 'Route', 'Add / edit Route', 1, NOW(), 'seed'),
    ('ROUTE_STATUS', 'Route', 'Activate / deactivate Route', 1, NOW(), 'seed'),
    ('ROUTE_UPLOAD', 'Route', 'Bulk Excel upload of Route', 1, NOW(), 'seed'),
    ('ROUTE_AREA_SAVE', 'Route Area', 'Add / edit Route Area', 1, NOW(), 'seed'),
    ('ROUTE_AREA_STATUS', 'Route Area', 'Activate / deactivate Route Area', 1, NOW(), 'seed'),
    ('SPECIALITY_SAVE', 'Speciality', 'Add / edit Speciality', 1, NOW(), 'seed'),
    ('SPECIALITY_STATUS', 'Speciality', 'Activate / deactivate Speciality', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_SAVE', 'Sponsorship Type', 'Add / edit Sponsorship Type', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_STATUS', 'Sponsorship Type', 'Activate / deactivate Sponsorship Type', 1, NOW(), 'seed'),
    ('STATE_SAVE', 'State', 'Add / edit State', 1, NOW(), 'seed'),
    ('STATE_STATUS', 'State', 'Activate / deactivate State', 1, NOW(), 'seed'),
    ('STATE_UPLOAD', 'State', 'Bulk Excel upload of State', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_SAVE', 'Travel Type', 'Add / edit Travel Type', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_STATUS', 'Travel Type', 'Activate / deactivate Travel Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_SAVE', 'Visit Type', 'Add / edit Visit Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_STATUS', 'Visit Type', 'Activate / deactivate Visit Type', 1, NOW(), 'seed'),
    ('ZONE_SAVE', 'Zone', 'Add / edit Zone', 1, NOW(), 'seed'),
    ('ZONE_STATUS', 'Zone', 'Activate / deactivate Zone', 1, NOW(), 'seed'),
    ('ZONE_UPLOAD', 'Zone', 'Bulk Excel upload of Zone', 1, NOW(), 'seed');

INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 518, 500, me.emp_id, 'WEB', 'Permission Master', NULL, 'shield-lock',
       'permissionmaster', 'utilities/permission-master.html', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 500 LIMIT 1) me
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'permissionmaster');

-- ==================== globex_db ====================
USE globex_db;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ACTIVITY_TYPE_SAVE', 'Activity Type', 'Add / edit Activity Type', 1, NOW(), 'seed'),
    ('ACTIVITY_TYPE_STATUS', 'Activity Type', 'Activate / deactivate Activity Type', 1, NOW(), 'seed'),
    ('AREA_SAVE', 'Area', 'Add / edit Area', 1, NOW(), 'seed'),
    ('AREA_STATUS', 'Area', 'Activate / deactivate Area', 1, NOW(), 'seed'),
    ('AREA_UPLOAD', 'Area', 'Bulk Excel upload of Area', 1, NOW(), 'seed'),
    ('BANK_SAVE', 'Bank', 'Add / edit Bank', 1, NOW(), 'seed'),
    ('BANK_STATUS', 'Bank', 'Activate / deactivate Bank', 1, NOW(), 'seed'),
    ('CATEGORY_SAVE', 'Category', 'Add / edit Category', 1, NOW(), 'seed'),
    ('CATEGORY_STATUS', 'Category', 'Activate / deactivate Category', 1, NOW(), 'seed'),
    ('CLIENT_SAVE', 'Client', 'Add / edit Client', 1, NOW(), 'seed'),
    ('CLIENT_STATUS', 'Client', 'Activate / deactivate Client', 1, NOW(), 'seed'),
    ('CLIENT_UPLOAD', 'Client', 'Bulk Excel upload of Client', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_SAVE', 'Client Type', 'Add / edit Client Type', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_STATUS', 'Client Type', 'Activate / deactivate Client Type', 1, NOW(), 'seed'),
    ('COUNTRY_SAVE', 'Country', 'Add / edit Country', 1, NOW(), 'seed'),
    ('COUNTRY_STATUS', 'Country', 'Activate / deactivate Country', 1, NOW(), 'seed'),
    ('DEGREE_SAVE', 'Degree', 'Add / edit Degree', 1, NOW(), 'seed'),
    ('DEGREE_STATUS', 'Degree', 'Activate / deactivate Degree', 1, NOW(), 'seed'),
    ('DIVISION_SAVE', 'Division', 'Add / edit Division', 1, NOW(), 'seed'),
    ('DIVISION_STATUS', 'Division', 'Activate / deactivate Division', 1, NOW(), 'seed'),
    ('DIVISION_UPLOAD', 'Division', 'Bulk Excel upload of Division', 1, NOW(), 'seed'),
    ('DOCUMENT_SAVE', 'Document', 'Add / edit Document', 1, NOW(), 'seed'),
    ('DOCUMENT_STATUS', 'Document', 'Activate / deactivate Document', 1, NOW(), 'seed'),
    ('EMP_DETAIL_SAVE', 'Create Employee', 'Add / edit Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_STATUS', 'Create Employee', 'Activate / deactivate Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_UPLOAD', 'Create Employee', 'Bulk Excel upload of Create Employee', 1, NOW(), 'seed'),
    ('HQ_SAVE', 'HQ', 'Add / edit HQ', 1, NOW(), 'seed'),
    ('HQ_STATUS', 'HQ', 'Activate / deactivate HQ', 1, NOW(), 'seed'),
    ('HQ_UPLOAD', 'HQ', 'Bulk Excel upload of HQ', 1, NOW(), 'seed'),
    ('HQ_GROUP_SAVE', 'HQ Group', 'Add / edit HQ Group', 1, NOW(), 'seed'),
    ('HQ_GROUP_STATUS', 'HQ Group', 'Activate / deactivate HQ Group', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_SAVE', 'Image Type', 'Add / edit Image Type', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_STATUS', 'Image Type', 'Activate / deactivate Image Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_SAVE', 'Item Type', 'Add / edit Item Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_STATUS', 'Item Type', 'Activate / deactivate Item Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_SAVE', 'Meeting Type', 'Add / edit Meeting Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_STATUS', 'Meeting Type', 'Activate / deactivate Meeting Type', 1, NOW(), 'seed'),
    ('PRODUCT_SAVE', 'Product', 'Add / edit Product', 1, NOW(), 'seed'),
    ('ROUTE_SAVE', 'Route', 'Add / edit Route', 1, NOW(), 'seed'),
    ('ROUTE_STATUS', 'Route', 'Activate / deactivate Route', 1, NOW(), 'seed'),
    ('ROUTE_UPLOAD', 'Route', 'Bulk Excel upload of Route', 1, NOW(), 'seed'),
    ('ROUTE_AREA_SAVE', 'Route Area', 'Add / edit Route Area', 1, NOW(), 'seed'),
    ('ROUTE_AREA_STATUS', 'Route Area', 'Activate / deactivate Route Area', 1, NOW(), 'seed'),
    ('SPECIALITY_SAVE', 'Speciality', 'Add / edit Speciality', 1, NOW(), 'seed'),
    ('SPECIALITY_STATUS', 'Speciality', 'Activate / deactivate Speciality', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_SAVE', 'Sponsorship Type', 'Add / edit Sponsorship Type', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_STATUS', 'Sponsorship Type', 'Activate / deactivate Sponsorship Type', 1, NOW(), 'seed'),
    ('STATE_SAVE', 'State', 'Add / edit State', 1, NOW(), 'seed'),
    ('STATE_STATUS', 'State', 'Activate / deactivate State', 1, NOW(), 'seed'),
    ('STATE_UPLOAD', 'State', 'Bulk Excel upload of State', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_SAVE', 'Travel Type', 'Add / edit Travel Type', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_STATUS', 'Travel Type', 'Activate / deactivate Travel Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_SAVE', 'Visit Type', 'Add / edit Visit Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_STATUS', 'Visit Type', 'Activate / deactivate Visit Type', 1, NOW(), 'seed'),
    ('ZONE_SAVE', 'Zone', 'Add / edit Zone', 1, NOW(), 'seed'),
    ('ZONE_STATUS', 'Zone', 'Activate / deactivate Zone', 1, NOW(), 'seed'),
    ('ZONE_UPLOAD', 'Zone', 'Bulk Excel upload of Zone', 1, NOW(), 'seed');

INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 518, 500, me.emp_id, 'WEB', 'Permission Master', NULL, 'shield-lock',
       'permissionmaster', 'utilities/permission-master.html', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 500 LIMIT 1) me
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'permissionmaster');

-- ==================== initech_db ====================
USE initech_db;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ACTIVITY_TYPE_SAVE', 'Activity Type', 'Add / edit Activity Type', 1, NOW(), 'seed'),
    ('ACTIVITY_TYPE_STATUS', 'Activity Type', 'Activate / deactivate Activity Type', 1, NOW(), 'seed'),
    ('AREA_SAVE', 'Area', 'Add / edit Area', 1, NOW(), 'seed'),
    ('AREA_STATUS', 'Area', 'Activate / deactivate Area', 1, NOW(), 'seed'),
    ('AREA_UPLOAD', 'Area', 'Bulk Excel upload of Area', 1, NOW(), 'seed'),
    ('BANK_SAVE', 'Bank', 'Add / edit Bank', 1, NOW(), 'seed'),
    ('BANK_STATUS', 'Bank', 'Activate / deactivate Bank', 1, NOW(), 'seed'),
    ('CATEGORY_SAVE', 'Category', 'Add / edit Category', 1, NOW(), 'seed'),
    ('CATEGORY_STATUS', 'Category', 'Activate / deactivate Category', 1, NOW(), 'seed'),
    ('CLIENT_SAVE', 'Client', 'Add / edit Client', 1, NOW(), 'seed'),
    ('CLIENT_STATUS', 'Client', 'Activate / deactivate Client', 1, NOW(), 'seed'),
    ('CLIENT_UPLOAD', 'Client', 'Bulk Excel upload of Client', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_SAVE', 'Client Type', 'Add / edit Client Type', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_STATUS', 'Client Type', 'Activate / deactivate Client Type', 1, NOW(), 'seed'),
    ('COUNTRY_SAVE', 'Country', 'Add / edit Country', 1, NOW(), 'seed'),
    ('COUNTRY_STATUS', 'Country', 'Activate / deactivate Country', 1, NOW(), 'seed'),
    ('DEGREE_SAVE', 'Degree', 'Add / edit Degree', 1, NOW(), 'seed'),
    ('DEGREE_STATUS', 'Degree', 'Activate / deactivate Degree', 1, NOW(), 'seed'),
    ('DIVISION_SAVE', 'Division', 'Add / edit Division', 1, NOW(), 'seed'),
    ('DIVISION_STATUS', 'Division', 'Activate / deactivate Division', 1, NOW(), 'seed'),
    ('DIVISION_UPLOAD', 'Division', 'Bulk Excel upload of Division', 1, NOW(), 'seed'),
    ('DOCUMENT_SAVE', 'Document', 'Add / edit Document', 1, NOW(), 'seed'),
    ('DOCUMENT_STATUS', 'Document', 'Activate / deactivate Document', 1, NOW(), 'seed'),
    ('EMP_DETAIL_SAVE', 'Create Employee', 'Add / edit Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_STATUS', 'Create Employee', 'Activate / deactivate Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_UPLOAD', 'Create Employee', 'Bulk Excel upload of Create Employee', 1, NOW(), 'seed'),
    ('HQ_SAVE', 'HQ', 'Add / edit HQ', 1, NOW(), 'seed'),
    ('HQ_STATUS', 'HQ', 'Activate / deactivate HQ', 1, NOW(), 'seed'),
    ('HQ_UPLOAD', 'HQ', 'Bulk Excel upload of HQ', 1, NOW(), 'seed'),
    ('HQ_GROUP_SAVE', 'HQ Group', 'Add / edit HQ Group', 1, NOW(), 'seed'),
    ('HQ_GROUP_STATUS', 'HQ Group', 'Activate / deactivate HQ Group', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_SAVE', 'Image Type', 'Add / edit Image Type', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_STATUS', 'Image Type', 'Activate / deactivate Image Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_SAVE', 'Item Type', 'Add / edit Item Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_STATUS', 'Item Type', 'Activate / deactivate Item Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_SAVE', 'Meeting Type', 'Add / edit Meeting Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_STATUS', 'Meeting Type', 'Activate / deactivate Meeting Type', 1, NOW(), 'seed'),
    ('PRODUCT_SAVE', 'Product', 'Add / edit Product', 1, NOW(), 'seed'),
    ('ROUTE_SAVE', 'Route', 'Add / edit Route', 1, NOW(), 'seed'),
    ('ROUTE_STATUS', 'Route', 'Activate / deactivate Route', 1, NOW(), 'seed'),
    ('ROUTE_UPLOAD', 'Route', 'Bulk Excel upload of Route', 1, NOW(), 'seed'),
    ('ROUTE_AREA_SAVE', 'Route Area', 'Add / edit Route Area', 1, NOW(), 'seed'),
    ('ROUTE_AREA_STATUS', 'Route Area', 'Activate / deactivate Route Area', 1, NOW(), 'seed'),
    ('SPECIALITY_SAVE', 'Speciality', 'Add / edit Speciality', 1, NOW(), 'seed'),
    ('SPECIALITY_STATUS', 'Speciality', 'Activate / deactivate Speciality', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_SAVE', 'Sponsorship Type', 'Add / edit Sponsorship Type', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_STATUS', 'Sponsorship Type', 'Activate / deactivate Sponsorship Type', 1, NOW(), 'seed'),
    ('STATE_SAVE', 'State', 'Add / edit State', 1, NOW(), 'seed'),
    ('STATE_STATUS', 'State', 'Activate / deactivate State', 1, NOW(), 'seed'),
    ('STATE_UPLOAD', 'State', 'Bulk Excel upload of State', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_SAVE', 'Travel Type', 'Add / edit Travel Type', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_STATUS', 'Travel Type', 'Activate / deactivate Travel Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_SAVE', 'Visit Type', 'Add / edit Visit Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_STATUS', 'Visit Type', 'Activate / deactivate Visit Type', 1, NOW(), 'seed'),
    ('ZONE_SAVE', 'Zone', 'Add / edit Zone', 1, NOW(), 'seed'),
    ('ZONE_STATUS', 'Zone', 'Activate / deactivate Zone', 1, NOW(), 'seed'),
    ('ZONE_UPLOAD', 'Zone', 'Bulk Excel upload of Zone', 1, NOW(), 'seed');

INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 518, 500, me.emp_id, 'WEB', 'Permission Master', NULL, 'shield-lock',
       'permissionmaster', 'utilities/permission-master.html', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 500 LIMIT 1) me
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'permissionmaster');

-- ==================== sfa_demo ====================
USE sfa_demo;
INSERT IGNORE INTO permission_master (permission_code, module, description, status, created_at, created_by) VALUES
    ('ACTIVITY_TYPE_SAVE', 'Activity Type', 'Add / edit Activity Type', 1, NOW(), 'seed'),
    ('ACTIVITY_TYPE_STATUS', 'Activity Type', 'Activate / deactivate Activity Type', 1, NOW(), 'seed'),
    ('AREA_SAVE', 'Area', 'Add / edit Area', 1, NOW(), 'seed'),
    ('AREA_STATUS', 'Area', 'Activate / deactivate Area', 1, NOW(), 'seed'),
    ('AREA_UPLOAD', 'Area', 'Bulk Excel upload of Area', 1, NOW(), 'seed'),
    ('BANK_SAVE', 'Bank', 'Add / edit Bank', 1, NOW(), 'seed'),
    ('BANK_STATUS', 'Bank', 'Activate / deactivate Bank', 1, NOW(), 'seed'),
    ('CATEGORY_SAVE', 'Category', 'Add / edit Category', 1, NOW(), 'seed'),
    ('CATEGORY_STATUS', 'Category', 'Activate / deactivate Category', 1, NOW(), 'seed'),
    ('CLIENT_SAVE', 'Client', 'Add / edit Client', 1, NOW(), 'seed'),
    ('CLIENT_STATUS', 'Client', 'Activate / deactivate Client', 1, NOW(), 'seed'),
    ('CLIENT_UPLOAD', 'Client', 'Bulk Excel upload of Client', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_SAVE', 'Client Type', 'Add / edit Client Type', 1, NOW(), 'seed'),
    ('CLIENT_TYPE_STATUS', 'Client Type', 'Activate / deactivate Client Type', 1, NOW(), 'seed'),
    ('COUNTRY_SAVE', 'Country', 'Add / edit Country', 1, NOW(), 'seed'),
    ('COUNTRY_STATUS', 'Country', 'Activate / deactivate Country', 1, NOW(), 'seed'),
    ('DEGREE_SAVE', 'Degree', 'Add / edit Degree', 1, NOW(), 'seed'),
    ('DEGREE_STATUS', 'Degree', 'Activate / deactivate Degree', 1, NOW(), 'seed'),
    ('DIVISION_SAVE', 'Division', 'Add / edit Division', 1, NOW(), 'seed'),
    ('DIVISION_STATUS', 'Division', 'Activate / deactivate Division', 1, NOW(), 'seed'),
    ('DIVISION_UPLOAD', 'Division', 'Bulk Excel upload of Division', 1, NOW(), 'seed'),
    ('DOCUMENT_SAVE', 'Document', 'Add / edit Document', 1, NOW(), 'seed'),
    ('DOCUMENT_STATUS', 'Document', 'Activate / deactivate Document', 1, NOW(), 'seed'),
    ('EMP_DETAIL_SAVE', 'Create Employee', 'Add / edit Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_STATUS', 'Create Employee', 'Activate / deactivate Create Employee', 1, NOW(), 'seed'),
    ('EMP_DETAIL_UPLOAD', 'Create Employee', 'Bulk Excel upload of Create Employee', 1, NOW(), 'seed'),
    ('HQ_SAVE', 'HQ', 'Add / edit HQ', 1, NOW(), 'seed'),
    ('HQ_STATUS', 'HQ', 'Activate / deactivate HQ', 1, NOW(), 'seed'),
    ('HQ_UPLOAD', 'HQ', 'Bulk Excel upload of HQ', 1, NOW(), 'seed'),
    ('HQ_GROUP_SAVE', 'HQ Group', 'Add / edit HQ Group', 1, NOW(), 'seed'),
    ('HQ_GROUP_STATUS', 'HQ Group', 'Activate / deactivate HQ Group', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_SAVE', 'Image Type', 'Add / edit Image Type', 1, NOW(), 'seed'),
    ('IMAGE_TYPE_STATUS', 'Image Type', 'Activate / deactivate Image Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_SAVE', 'Item Type', 'Add / edit Item Type', 1, NOW(), 'seed'),
    ('ITEM_TYPE_STATUS', 'Item Type', 'Activate / deactivate Item Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_SAVE', 'Meeting Type', 'Add / edit Meeting Type', 1, NOW(), 'seed'),
    ('MEETING_TYPE_STATUS', 'Meeting Type', 'Activate / deactivate Meeting Type', 1, NOW(), 'seed'),
    ('PRODUCT_SAVE', 'Product', 'Add / edit Product', 1, NOW(), 'seed'),
    ('ROUTE_SAVE', 'Route', 'Add / edit Route', 1, NOW(), 'seed'),
    ('ROUTE_STATUS', 'Route', 'Activate / deactivate Route', 1, NOW(), 'seed'),
    ('ROUTE_UPLOAD', 'Route', 'Bulk Excel upload of Route', 1, NOW(), 'seed'),
    ('ROUTE_AREA_SAVE', 'Route Area', 'Add / edit Route Area', 1, NOW(), 'seed'),
    ('ROUTE_AREA_STATUS', 'Route Area', 'Activate / deactivate Route Area', 1, NOW(), 'seed'),
    ('SPECIALITY_SAVE', 'Speciality', 'Add / edit Speciality', 1, NOW(), 'seed'),
    ('SPECIALITY_STATUS', 'Speciality', 'Activate / deactivate Speciality', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_SAVE', 'Sponsorship Type', 'Add / edit Sponsorship Type', 1, NOW(), 'seed'),
    ('SPONSORSHIP_TYPE_STATUS', 'Sponsorship Type', 'Activate / deactivate Sponsorship Type', 1, NOW(), 'seed'),
    ('STATE_SAVE', 'State', 'Add / edit State', 1, NOW(), 'seed'),
    ('STATE_STATUS', 'State', 'Activate / deactivate State', 1, NOW(), 'seed'),
    ('STATE_UPLOAD', 'State', 'Bulk Excel upload of State', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_SAVE', 'Travel Type', 'Add / edit Travel Type', 1, NOW(), 'seed'),
    ('TRAVEL_TYPE_STATUS', 'Travel Type', 'Activate / deactivate Travel Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_SAVE', 'Visit Type', 'Add / edit Visit Type', 1, NOW(), 'seed'),
    ('VISIT_TYPE_STATUS', 'Visit Type', 'Activate / deactivate Visit Type', 1, NOW(), 'seed'),
    ('ZONE_SAVE', 'Zone', 'Add / edit Zone', 1, NOW(), 'seed'),
    ('ZONE_STATUS', 'Zone', 'Activate / deactivate Zone', 1, NOW(), 'seed'),
    ('ZONE_UPLOAD', 'Zone', 'Bulk Excel upload of Zone', 1, NOW(), 'seed');

INSERT INTO `company_menu_self`
    (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT 518, 500, me.emp_id, 'WEB', 'Permission Master', NULL, 'shield-lock',
       'permissionmaster', 'utilities/permission-master.html', '_SELF', 8, 1, 1
FROM (SELECT emp_id FROM `company_menu_self` WHERE id = 500 LIMIT 1) me
WHERE NOT EXISTS (SELECT 1 FROM `company_menu_self` WHERE page = 'permissionmaster');
