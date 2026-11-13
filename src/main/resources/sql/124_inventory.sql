-- 124: Inventory module (Stock & Sales Submit) - inventory_field_master (what is submitted), inventory_setting (rate shown, file upload / mandatory), party_sales_parent + party_sales_child (one submission per party per month, unique), menu under Inventory. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS inventory_field_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  field_code VARCHAR(24) NOT NULL COMMENT 'built-in (OPENING, PRIMARY, SECONDARY, FREE, RETURN_RETAILER, RETURN_CONSIGNEE, EXPIRY_RETAILER, EXPIRY_CONSIGNEE, CLOSING, SPECIAL_RATE, STOCK_VALUE) or a field the company added',
  label VARCHAR(60) NOT NULL,
  short_code VARCHAR(10) NULL COMMENT 'column code in the stock report',
  is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = the user fills it (a hidden field counts as 0 in the closing)',
  is_mandatory TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = it must be filled in every item row that is submitted',
  sign_op TINYINT NOT NULL DEFAULT 1 COMMENT 'in the closing formula: 1 = added, -1 = subtracted, 0 = information only',
  field_role VARCHAR(8) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / SALE (the one solved from the closing) / CLOSING / RATE (special rate) / VALUE (stock value, calculated)',
  is_builtin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = has its own column in party_sales_child; 0 = added by the company, kept in extra_json',
  is_core TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = Opening, Primary, Secondary, Free, Closing, Stock value: always there, can be renamed but never deleted or hidden; the others can be added, hidden or deleted',
  sort_order SMALLINT NOT NULL DEFAULT 100,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_invfield_code (field_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what a user submits in the party inventory (stock & sales) - the company decides the fields';
INSERT IGNORE INTO inventory_field_master (field_code, label, short_code, sign_op, field_role, is_builtin, is_core, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate','SR',0,'RATE',1,0,10),('OPENING','Opening Quantity','O',1,'NORMAL',1,1,20),('PRIMARY','Purchase Quantity','P',1,'NORMAL',1,1,30),('SECONDARY','Sale','S',-1,'SALE',1,1,40),('FREE','Free Goods','FG',-1,'NORMAL',1,1,50),
 ('RETURN_RETAILER','Return from Retailer','RGFR',1,'NORMAL',1,0,60),('RETURN_CONSIGNEE','Return to Consignee','RGTCSA',-1,'NORMAL',1,0,70),('EXPIRY_RETAILER','Expiry from Retailer','EGFR',1,'NORMAL',1,0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee','EGRTCSA',-1,'NORMAL',1,0,90),
 ('CLOSING','Closing Quantity','C',0,'CLOSING',1,1,100),('STOCK_VALUE','Stock Value','SV',0,'VALUE',1,1,110);

CREATE TABLE IF NOT EXISTS inventory_setting (
  setting_key VARCHAR(30) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RATE_SHOWN = SPECIAL (special rate when entered, else item rate) | ITEM (always the item rate); FILE_UPLOAD = Y | N; FILE_MANDATORY = Y | N; RATE_BASIS = PTS | PTR | MRP | PURCHASE_RATE (the column of item_rate the stock is submitted on); SALE_CLOSING_CALC = Y | N (only Sale typed: closing is calculated; only Closing typed: sale is calculated; both typed: no formula)';
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS');

CREATE TABLE IF NOT EXISTS party_sales_parent (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the employee whose party it is / who submitted',
  party_id BIGINT NOT NULL COMMENT '-> client_master.id (party / stockist)',
  sale_month TINYINT UNSIGNED NOT NULL, sale_year SMALLINT UNSIGNED NOT NULL,
  rate_basis VARCHAR(14) NOT NULL DEFAULT 'PTS' COMMENT 'the item_rate column the item_rate of the rows was taken from',
  status VARCHAR(12) NOT NULL DEFAULT 'SUBMITTED',
  remarks VARCHAR(500) NULL,
  file_path VARCHAR(255) NULL, file_name VARCHAR(255) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_psp_party_month (party_id, sale_month, sale_year), KEY ix_psp_emp (emp_id, sale_year, sale_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one stock & sales submission per party / stockist per month (a second submit updates it)';

CREATE TABLE IF NOT EXISTS party_sales_child (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL, item_id BIGINT UNSIGNED NOT NULL,
  opening_qty DECIMAL(12,2) NULL, purchase_qty DECIMAL(12,2) NULL, sale_qty DECIMAL(12,2) NULL, free_qty DECIMAL(12,2) NULL,
  return_retailer_qty DECIMAL(12,2) NULL, return_consignee_qty DECIMAL(12,2) NULL, expiry_retailer_qty DECIMAL(12,2) NULL, expiry_consignee_qty DECIMAL(12,2) NULL,
  closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'typed, or (opening + purchase) - (sale + free) + (return retailer - return consignee) + (expiry retailer - expiry consignee) when only the sale was typed',
  calc_field CHAR(1) NULL COMMENT 'C = closing was calculated from the sale, S = sale was calculated from the closing, NULL = both typed / no formula',
  item_rate DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'the rate of the item (item_rate.pts / ptr / mrp per rate_basis) - always kept',
  extra_json TEXT NULL COMMENT 'values of the fields the company added (code -> number)',
  special_rate DECIMAL(12,2) NULL COMMENT 'the special rate when the user entered one (the item rate stays in item_rate too)',
  stock_value DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'closing x the rate in use (RATE_SHOWN)',
  PRIMARY KEY (id), UNIQUE KEY uq_psc_item (parent_id, item_id), KEY ix_psc_item (item_id),
  CONSTRAINT fk_psc_parent FOREIGN KEY (parent_id) REFERENCES party_sales_parent (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- menu: Inventory (root, created when the company has none) > Party Inventory, Inventory Master
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Inventory', 'Party / stockist stock and sales', 'boxes', NULL, NULL, '_SELF', 7, 1, 1
FROM DUAL WHERE @iroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Stock & Sales Submit', 'Submit and see the stock and sales of a party / stockist (month wise)', 'box-seam', 'partyinventory', 'inventory/party-inventory.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'partyinventory');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Inventory Master', 'What the user submits, the rate shown and the file setting', 'sliders', 'inventorymaster', 'inventory/inventory-master.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'inventorymaster');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('partyinventory', 'inventorymaster');

USE globex_db;

CREATE TABLE IF NOT EXISTS inventory_field_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  field_code VARCHAR(24) NOT NULL COMMENT 'OPENING / PRIMARY (Purchase) / SECONDARY (Sale) / FREE / RETURN_RETAILER / RETURN_CONSIGNEE / EXPIRY_RETAILER / EXPIRY_CONSIGNEE / SPECIAL_RATE; CLOSING (typed, or calculated from the sale) and STOCK_VALUE (calculated)',
  label VARCHAR(60) NOT NULL,
  is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = the user fills it (a hidden field counts as 0 in the closing)',
  is_mandatory TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = it must be filled in every item row that is submitted',
  is_auto TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = calculated, never typed',
  sort_order SMALLINT NOT NULL DEFAULT 100,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_invfield_code (field_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what a user submits in the party inventory (stock & sales)';
INSERT IGNORE INTO inventory_field_master (field_code, label, is_auto, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate',0,10),('OPENING','Opening Quantity',0,20),('PRIMARY','Purchase Quantity',0,30),('SECONDARY','Sale',0,40),('FREE','Free Goods',0,50),
 ('RETURN_RETAILER','Return from Retailer',0,60),('RETURN_CONSIGNEE','Return to Consignee',0,70),('EXPIRY_RETAILER','Expiry from Retailer',0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee',0,90),
 ('CLOSING','Closing Quantity',0,100),('STOCK_VALUE','Stock Value',1,110);

CREATE TABLE IF NOT EXISTS inventory_setting (
  setting_key VARCHAR(30) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RATE_SHOWN = SPECIAL (special rate when entered, else item rate) | ITEM (always the item rate); FILE_UPLOAD = Y | N; FILE_MANDATORY = Y | N; RATE_BASIS = PTS | PTR | MRP | PURCHASE_RATE (the column of item_rate the stock is submitted on); SALE_CLOSING_CALC = Y | N (only Sale typed: closing is calculated; only Closing typed: sale is calculated; both typed: no formula)';
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS');

CREATE TABLE IF NOT EXISTS party_sales_parent (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the employee whose party it is / who submitted',
  party_id BIGINT NOT NULL COMMENT '-> client_master.id (party / stockist)',
  sale_month TINYINT UNSIGNED NOT NULL, sale_year SMALLINT UNSIGNED NOT NULL,
  rate_basis VARCHAR(14) NOT NULL DEFAULT 'PTS' COMMENT 'the item_rate column the item_rate of the rows was taken from',
  status VARCHAR(12) NOT NULL DEFAULT 'SUBMITTED',
  remarks VARCHAR(500) NULL,
  file_path VARCHAR(255) NULL, file_name VARCHAR(255) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_psp_party_month (party_id, sale_month, sale_year), KEY ix_psp_emp (emp_id, sale_year, sale_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one stock & sales submission per party / stockist per month (a second submit updates it)';

CREATE TABLE IF NOT EXISTS party_sales_child (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL, item_id BIGINT UNSIGNED NOT NULL,
  opening_qty DECIMAL(12,2) NULL, purchase_qty DECIMAL(12,2) NULL, sale_qty DECIMAL(12,2) NULL, free_qty DECIMAL(12,2) NULL,
  return_retailer_qty DECIMAL(12,2) NULL, return_consignee_qty DECIMAL(12,2) NULL, expiry_retailer_qty DECIMAL(12,2) NULL, expiry_consignee_qty DECIMAL(12,2) NULL,
  closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'typed, or (opening + purchase) - (sale + free) + (return retailer - return consignee) + (expiry retailer - expiry consignee) when only the sale was typed',
  calc_field CHAR(1) NULL COMMENT 'C = closing was calculated from the sale, S = sale was calculated from the closing, NULL = both typed / no formula',
  item_rate DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'the rate of the item (item_rate.pts / ptr / mrp per rate_basis) - always kept',
  extra_json TEXT NULL COMMENT 'values of the fields the company added (code -> number)',
  special_rate DECIMAL(12,2) NULL COMMENT 'the special rate when the user entered one (the item rate stays in item_rate too)',
  stock_value DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'closing x the rate in use (RATE_SHOWN)',
  PRIMARY KEY (id), UNIQUE KEY uq_psc_item (parent_id, item_id), KEY ix_psc_item (item_id),
  CONSTRAINT fk_psc_parent FOREIGN KEY (parent_id) REFERENCES party_sales_parent (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- menu: Inventory (root, created when the company has none) > Party Inventory, Inventory Master
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Inventory', 'Party / stockist stock and sales', 'boxes', NULL, NULL, '_SELF', 7, 1, 1
FROM DUAL WHERE @iroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Stock & Sales Submit', 'Submit and see the stock and sales of a party / stockist (month wise)', 'box-seam', 'partyinventory', 'inventory/party-inventory.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'partyinventory');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Inventory Master', 'What the user submits, the rate shown and the file setting', 'sliders', 'inventorymaster', 'inventory/inventory-master.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'inventorymaster');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('partyinventory', 'inventorymaster');

USE initech_db;

CREATE TABLE IF NOT EXISTS inventory_field_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  field_code VARCHAR(24) NOT NULL COMMENT 'OPENING / PRIMARY (Purchase) / SECONDARY (Sale) / FREE / RETURN_RETAILER / RETURN_CONSIGNEE / EXPIRY_RETAILER / EXPIRY_CONSIGNEE / SPECIAL_RATE; CLOSING (typed, or calculated from the sale) and STOCK_VALUE (calculated)',
  label VARCHAR(60) NOT NULL,
  is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = the user fills it (a hidden field counts as 0 in the closing)',
  is_mandatory TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = it must be filled in every item row that is submitted',
  is_auto TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = calculated, never typed',
  sort_order SMALLINT NOT NULL DEFAULT 100,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_invfield_code (field_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what a user submits in the party inventory (stock & sales)';
INSERT IGNORE INTO inventory_field_master (field_code, label, is_auto, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate',0,10),('OPENING','Opening Quantity',0,20),('PRIMARY','Purchase Quantity',0,30),('SECONDARY','Sale',0,40),('FREE','Free Goods',0,50),
 ('RETURN_RETAILER','Return from Retailer',0,60),('RETURN_CONSIGNEE','Return to Consignee',0,70),('EXPIRY_RETAILER','Expiry from Retailer',0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee',0,90),
 ('CLOSING','Closing Quantity',0,100),('STOCK_VALUE','Stock Value',1,110);

CREATE TABLE IF NOT EXISTS inventory_setting (
  setting_key VARCHAR(30) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RATE_SHOWN = SPECIAL (special rate when entered, else item rate) | ITEM (always the item rate); FILE_UPLOAD = Y | N; FILE_MANDATORY = Y | N; RATE_BASIS = PTS | PTR | MRP | PURCHASE_RATE (the column of item_rate the stock is submitted on); SALE_CLOSING_CALC = Y | N (only Sale typed: closing is calculated; only Closing typed: sale is calculated; both typed: no formula)';
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS');

CREATE TABLE IF NOT EXISTS party_sales_parent (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the employee whose party it is / who submitted',
  party_id BIGINT NOT NULL COMMENT '-> client_master.id (party / stockist)',
  sale_month TINYINT UNSIGNED NOT NULL, sale_year SMALLINT UNSIGNED NOT NULL,
  rate_basis VARCHAR(14) NOT NULL DEFAULT 'PTS' COMMENT 'the item_rate column the item_rate of the rows was taken from',
  status VARCHAR(12) NOT NULL DEFAULT 'SUBMITTED',
  remarks VARCHAR(500) NULL,
  file_path VARCHAR(255) NULL, file_name VARCHAR(255) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_psp_party_month (party_id, sale_month, sale_year), KEY ix_psp_emp (emp_id, sale_year, sale_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one stock & sales submission per party / stockist per month (a second submit updates it)';

CREATE TABLE IF NOT EXISTS party_sales_child (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL, item_id BIGINT UNSIGNED NOT NULL,
  opening_qty DECIMAL(12,2) NULL, purchase_qty DECIMAL(12,2) NULL, sale_qty DECIMAL(12,2) NULL, free_qty DECIMAL(12,2) NULL,
  return_retailer_qty DECIMAL(12,2) NULL, return_consignee_qty DECIMAL(12,2) NULL, expiry_retailer_qty DECIMAL(12,2) NULL, expiry_consignee_qty DECIMAL(12,2) NULL,
  closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'typed, or (opening + purchase) - (sale + free) + (return retailer - return consignee) + (expiry retailer - expiry consignee) when only the sale was typed',
  calc_field CHAR(1) NULL COMMENT 'C = closing was calculated from the sale, S = sale was calculated from the closing, NULL = both typed / no formula',
  item_rate DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'the rate of the item (item_rate.pts / ptr / mrp per rate_basis) - always kept',
  extra_json TEXT NULL COMMENT 'values of the fields the company added (code -> number)',
  special_rate DECIMAL(12,2) NULL COMMENT 'the special rate when the user entered one (the item rate stays in item_rate too)',
  stock_value DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'closing x the rate in use (RATE_SHOWN)',
  PRIMARY KEY (id), UNIQUE KEY uq_psc_item (parent_id, item_id), KEY ix_psc_item (item_id),
  CONSTRAINT fk_psc_parent FOREIGN KEY (parent_id) REFERENCES party_sales_parent (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- menu: Inventory (root, created when the company has none) > Party Inventory, Inventory Master
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Inventory', 'Party / stockist stock and sales', 'boxes', NULL, NULL, '_SELF', 7, 1, 1
FROM DUAL WHERE @iroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Stock & Sales Submit', 'Submit and see the stock and sales of a party / stockist (month wise)', 'box-seam', 'partyinventory', 'inventory/party-inventory.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'partyinventory');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Inventory Master', 'What the user submits, the rate shown and the file setting', 'sliders', 'inventorymaster', 'inventory/inventory-master.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'inventorymaster');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('partyinventory', 'inventorymaster');

USE sfa_demo;

CREATE TABLE IF NOT EXISTS inventory_field_master (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  field_code VARCHAR(24) NOT NULL COMMENT 'OPENING / PRIMARY (Purchase) / SECONDARY (Sale) / FREE / RETURN_RETAILER / RETURN_CONSIGNEE / EXPIRY_RETAILER / EXPIRY_CONSIGNEE / SPECIAL_RATE; CLOSING (typed, or calculated from the sale) and STOCK_VALUE (calculated)',
  label VARCHAR(60) NOT NULL,
  is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = the user fills it (a hidden field counts as 0 in the closing)',
  is_mandatory TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = it must be filled in every item row that is submitted',
  is_auto TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = calculated, never typed',
  sort_order SMALLINT NOT NULL DEFAULT 100,
  updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_invfield_code (field_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='what a user submits in the party inventory (stock & sales)';
INSERT IGNORE INTO inventory_field_master (field_code, label, is_auto, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate',0,10),('OPENING','Opening Quantity',0,20),('PRIMARY','Purchase Quantity',0,30),('SECONDARY','Sale',0,40),('FREE','Free Goods',0,50),
 ('RETURN_RETAILER','Return from Retailer',0,60),('RETURN_CONSIGNEE','Return to Consignee',0,70),('EXPIRY_RETAILER','Expiry from Retailer',0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee',0,90),
 ('CLOSING','Closing Quantity',0,100),('STOCK_VALUE','Stock Value',1,110);

CREATE TABLE IF NOT EXISTS inventory_setting (
  setting_key VARCHAR(30) NOT NULL, setting_value VARCHAR(60) NOT NULL, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (setting_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RATE_SHOWN = SPECIAL (special rate when entered, else item rate) | ITEM (always the item rate); FILE_UPLOAD = Y | N; FILE_MANDATORY = Y | N; RATE_BASIS = PTS | PTR | MRP | PURCHASE_RATE (the column of item_rate the stock is submitted on); SALE_CLOSING_CALC = Y | N (only Sale typed: closing is calculated; only Closing typed: sale is calculated; both typed: no formula)';
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS');

CREATE TABLE IF NOT EXISTS party_sales_parent (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  emp_id VARCHAR(40) NOT NULL COMMENT 'the employee whose party it is / who submitted',
  party_id BIGINT NOT NULL COMMENT '-> client_master.id (party / stockist)',
  sale_month TINYINT UNSIGNED NOT NULL, sale_year SMALLINT UNSIGNED NOT NULL,
  rate_basis VARCHAR(14) NOT NULL DEFAULT 'PTS' COMMENT 'the item_rate column the item_rate of the rows was taken from',
  status VARCHAR(12) NOT NULL DEFAULT 'SUBMITTED',
  remarks VARCHAR(500) NULL,
  file_path VARCHAR(255) NULL, file_name VARCHAR(255) NULL,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by VARCHAR(40) NULL, updated_at DATETIME NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_psp_party_month (party_id, sale_month, sale_year), KEY ix_psp_emp (emp_id, sale_year, sale_month)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='one stock & sales submission per party / stockist per month (a second submit updates it)';

CREATE TABLE IF NOT EXISTS party_sales_child (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL, item_id BIGINT UNSIGNED NOT NULL,
  opening_qty DECIMAL(12,2) NULL, purchase_qty DECIMAL(12,2) NULL, sale_qty DECIMAL(12,2) NULL, free_qty DECIMAL(12,2) NULL,
  return_retailer_qty DECIMAL(12,2) NULL, return_consignee_qty DECIMAL(12,2) NULL, expiry_retailer_qty DECIMAL(12,2) NULL, expiry_consignee_qty DECIMAL(12,2) NULL,
  closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'typed, or (opening + purchase) - (sale + free) + (return retailer - return consignee) + (expiry retailer - expiry consignee) when only the sale was typed',
  calc_field CHAR(1) NULL COMMENT 'C = closing was calculated from the sale, S = sale was calculated from the closing, NULL = both typed / no formula',
  item_rate DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'the rate of the item (item_rate.pts / ptr / mrp per rate_basis) - always kept',
  extra_json TEXT NULL COMMENT 'values of the fields the company added (code -> number)',
  special_rate DECIMAL(12,2) NULL COMMENT 'the special rate when the user entered one (the item rate stays in item_rate too)',
  stock_value DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'closing x the rate in use (RATE_SHOWN)',
  PRIMARY KEY (id), UNIQUE KEY uq_psc_item (parent_id, item_id), KEY ix_psc_item (item_id),
  CONSTRAINT fk_psc_parent FOREIGN KEY (parent_id) REFERENCES party_sales_parent (id) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- menu: Inventory (root, created when the company has none) > Party Inventory, Inventory Master
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, NULL, (SELECT emp_id FROM company_menu_self WHERE parent_id IS NULL ORDER BY id LIMIT 1), 'WEB', 'Inventory', 'Party / stockist stock and sales', 'boxes', NULL, NULL, '_SELF', 7, 1, 1
FROM DUAL WHERE @iroot IS NULL AND EXISTS (SELECT 1 FROM company_menu_self);
SET @iroot := (SELECT id FROM company_menu_self WHERE label = 'Inventory' AND (parent_id IS NULL OR parent_id = 0) LIMIT 1);
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Stock & Sales Submit', 'Submit and see the stock and sales of a party / stockist (month wise)', 'box-seam', 'partyinventory', 'inventory/party-inventory.html', '_SELF', 1, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'partyinventory');
INSERT INTO company_menu_self (id, parent_id, emp_id, menu_type, label, title, icon, page, href, target, sort_order, is_visible, is_active)
SELECT (SELECT COALESCE(MAX(id),0) FROM company_menu_self) + 1, @iroot, p.emp_id, 'WEB', 'Inventory Master', 'What the user submits, the rate shown and the file setting', 'sliders', 'inventorymaster', 'inventory/inventory-master.html', '_SELF', 2, 1, 1
FROM (SELECT emp_id FROM company_menu_self WHERE id = @iroot LIMIT 1) p WHERE @iroot IS NOT NULL AND NOT EXISTS (SELECT 1 FROM company_menu_self x WHERE x.page = 'inventorymaster');
UPDATE company_menu_self SET is_visible = 1, is_active = 1 WHERE page IN ('partyinventory', 'inventorymaster');
