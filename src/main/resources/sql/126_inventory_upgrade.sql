-- 126: inventory upgrade - adds the columns / rows an earlier copy of 124 did not have (fixes 'A database error occurred' on Inventory Master). Run after 124. Safe to re-run.

USE acme_db;

-- brings an inventory set-up made by an EARLIER copy of 124 up to date (the tables are only created when missing). Safe to re-run.
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'short_code') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN short_code VARCHAR(10) NULL COMMENT ''column code in the stock report''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'sign_op') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN sign_op TINYINT NOT NULL DEFAULT 1 COMMENT ''in the closing formula: 1 = added, -1 = subtracted, 0 = information only''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'field_role') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN field_role VARCHAR(8) NOT NULL DEFAULT ''NORMAL'' COMMENT ''NORMAL / SALE / CLOSING / RATE / VALUE''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_builtin') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_builtin TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = has its own column in party_sales_child''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_core') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_core TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = always there, can be renamed, never deleted or hidden''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_auto') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_auto TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'primary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN primary_qty purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'secondary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN secondary_qty sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN return_qty return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'opening_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN opening_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'free_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN free_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'closing_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'calc_field') = 0, 'ALTER TABLE party_sales_child ADD COLUMN calc_field CHAR(1) NULL COMMENT ''C = closing calculated, S = sale calculated''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'extra_json') = 0, 'ALTER TABLE party_sales_child ADD COLUMN extra_json TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'item_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN item_rate DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'special_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN special_rate DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'stock_value') = 0, 'ALTER TABLE party_sales_child ADD COLUMN stock_value DECIMAL(14,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_parent' AND column_name = 'rate_basis') = 0, 'ALTER TABLE party_sales_parent ADD COLUMN rate_basis VARCHAR(14) NOT NULL DEFAULT ''PTS''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- the old single "Return" field is replaced by Return from Retailer / Return to Consignee
DELETE FROM inventory_field_master WHERE field_code = 'RETURN';
-- rows of an earlier copy get the new attributes (only the ones never upgraded)
UPDATE inventory_field_master SET short_code = CASE field_code WHEN 'SPECIAL_RATE' THEN 'SR' WHEN 'OPENING' THEN 'O' WHEN 'PRIMARY' THEN 'P' WHEN 'SECONDARY' THEN 'S' WHEN 'FREE' THEN 'FG' WHEN 'RETURN_RETAILER' THEN 'RGFR' WHEN 'RETURN_CONSIGNEE' THEN 'RGTCSA' WHEN 'EXPIRY_RETAILER' THEN 'EGFR' WHEN 'EXPIRY_CONSIGNEE' THEN 'EGRTCSA' WHEN 'CLOSING' THEN 'C' WHEN 'STOCK_VALUE' THEN 'SV' ELSE field_code END,
  sign_op = CASE field_code WHEN 'OPENING' THEN 1 WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN -1 WHEN 'FREE' THEN -1 WHEN 'RETURN_RETAILER' THEN 1 WHEN 'RETURN_CONSIGNEE' THEN -1 WHEN 'EXPIRY_RETAILER' THEN 1 WHEN 'EXPIRY_CONSIGNEE' THEN -1 ELSE 0 END,
  field_role = CASE field_code WHEN 'SECONDARY' THEN 'SALE' WHEN 'CLOSING' THEN 'CLOSING' WHEN 'SPECIAL_RATE' THEN 'RATE' WHEN 'STOCK_VALUE' THEN 'VALUE' ELSE 'NORMAL' END,
  is_builtin = 1, is_core = IF(field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE'), 1, 0)
WHERE short_code IS NULL AND field_code IN ('SPECIAL_RATE','OPENING','PRIMARY','SECONDARY','FREE','RETURN_RETAILER','RETURN_CONSIGNEE','EXPIRY_RETAILER','EXPIRY_CONSIGNEE','CLOSING','STOCK_VALUE');
UPDATE inventory_field_master SET short_code = field_code WHERE short_code IS NULL;
UPDATE inventory_field_master SET is_core = 1 WHERE field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE');
INSERT IGNORE INTO inventory_field_master (field_code, label, short_code, sign_op, field_role, is_builtin, is_core, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate','SR',0,'RATE',1,0,10),('OPENING','Opening Quantity','O',1,'NORMAL',1,1,20),('PRIMARY','Purchase Quantity','P',1,'NORMAL',1,1,30),('SECONDARY','Sale','S',-1,'SALE',1,1,40),('FREE','Free Goods','FG',-1,'NORMAL',1,1,50),
 ('RETURN_RETAILER','Return from Retailer','RGFR',1,'NORMAL',1,0,60),('RETURN_CONSIGNEE','Return to Consignee','RGTCSA',-1,'NORMAL',1,0,70),('EXPIRY_RETAILER','Expiry from Retailer','EGFR',1,'NORMAL',1,0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee','EGRTCSA',-1,'NORMAL',1,0,90),
 ('CLOSING','Closing Quantity','C',0,'CLOSING',1,1,100),('STOCK_VALUE','Stock Value','SV',0,'VALUE',1,1,110);
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS'),('OPENING_EDITABLE','N'),('SALE_CLOSING_LOCK','Y');

USE globex_db;

-- brings an inventory set-up made by an EARLIER copy of 124 up to date (the tables are only created when missing). Safe to re-run.
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'short_code') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN short_code VARCHAR(10) NULL COMMENT ''column code in the stock report''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'sign_op') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN sign_op TINYINT NOT NULL DEFAULT 1 COMMENT ''in the closing formula: 1 = added, -1 = subtracted, 0 = information only''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'field_role') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN field_role VARCHAR(8) NOT NULL DEFAULT ''NORMAL'' COMMENT ''NORMAL / SALE / CLOSING / RATE / VALUE''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_builtin') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_builtin TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = has its own column in party_sales_child''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_core') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_core TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = always there, can be renamed, never deleted or hidden''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_auto') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_auto TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'primary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN primary_qty purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'secondary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN secondary_qty sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN return_qty return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'opening_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN opening_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'free_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN free_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'closing_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'calc_field') = 0, 'ALTER TABLE party_sales_child ADD COLUMN calc_field CHAR(1) NULL COMMENT ''C = closing calculated, S = sale calculated''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'extra_json') = 0, 'ALTER TABLE party_sales_child ADD COLUMN extra_json TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'item_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN item_rate DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'special_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN special_rate DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'stock_value') = 0, 'ALTER TABLE party_sales_child ADD COLUMN stock_value DECIMAL(14,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_parent' AND column_name = 'rate_basis') = 0, 'ALTER TABLE party_sales_parent ADD COLUMN rate_basis VARCHAR(14) NOT NULL DEFAULT ''PTS''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- the old single "Return" field is replaced by Return from Retailer / Return to Consignee
DELETE FROM inventory_field_master WHERE field_code = 'RETURN';
-- rows of an earlier copy get the new attributes (only the ones never upgraded)
UPDATE inventory_field_master SET short_code = CASE field_code WHEN 'SPECIAL_RATE' THEN 'SR' WHEN 'OPENING' THEN 'O' WHEN 'PRIMARY' THEN 'P' WHEN 'SECONDARY' THEN 'S' WHEN 'FREE' THEN 'FG' WHEN 'RETURN_RETAILER' THEN 'RGFR' WHEN 'RETURN_CONSIGNEE' THEN 'RGTCSA' WHEN 'EXPIRY_RETAILER' THEN 'EGFR' WHEN 'EXPIRY_CONSIGNEE' THEN 'EGRTCSA' WHEN 'CLOSING' THEN 'C' WHEN 'STOCK_VALUE' THEN 'SV' ELSE field_code END,
  sign_op = CASE field_code WHEN 'OPENING' THEN 1 WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN -1 WHEN 'FREE' THEN -1 WHEN 'RETURN_RETAILER' THEN 1 WHEN 'RETURN_CONSIGNEE' THEN -1 WHEN 'EXPIRY_RETAILER' THEN 1 WHEN 'EXPIRY_CONSIGNEE' THEN -1 ELSE 0 END,
  field_role = CASE field_code WHEN 'SECONDARY' THEN 'SALE' WHEN 'CLOSING' THEN 'CLOSING' WHEN 'SPECIAL_RATE' THEN 'RATE' WHEN 'STOCK_VALUE' THEN 'VALUE' ELSE 'NORMAL' END,
  is_builtin = 1, is_core = IF(field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE'), 1, 0)
WHERE short_code IS NULL AND field_code IN ('SPECIAL_RATE','OPENING','PRIMARY','SECONDARY','FREE','RETURN_RETAILER','RETURN_CONSIGNEE','EXPIRY_RETAILER','EXPIRY_CONSIGNEE','CLOSING','STOCK_VALUE');
UPDATE inventory_field_master SET short_code = field_code WHERE short_code IS NULL;
UPDATE inventory_field_master SET is_core = 1 WHERE field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE');
INSERT IGNORE INTO inventory_field_master (field_code, label, short_code, sign_op, field_role, is_builtin, is_core, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate','SR',0,'RATE',1,0,10),('OPENING','Opening Quantity','O',1,'NORMAL',1,1,20),('PRIMARY','Purchase Quantity','P',1,'NORMAL',1,1,30),('SECONDARY','Sale','S',-1,'SALE',1,1,40),('FREE','Free Goods','FG',-1,'NORMAL',1,1,50),
 ('RETURN_RETAILER','Return from Retailer','RGFR',1,'NORMAL',1,0,60),('RETURN_CONSIGNEE','Return to Consignee','RGTCSA',-1,'NORMAL',1,0,70),('EXPIRY_RETAILER','Expiry from Retailer','EGFR',1,'NORMAL',1,0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee','EGRTCSA',-1,'NORMAL',1,0,90),
 ('CLOSING','Closing Quantity','C',0,'CLOSING',1,1,100),('STOCK_VALUE','Stock Value','SV',0,'VALUE',1,1,110);
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS'),('OPENING_EDITABLE','N'),('SALE_CLOSING_LOCK','Y');

USE initech_db;

-- brings an inventory set-up made by an EARLIER copy of 124 up to date (the tables are only created when missing). Safe to re-run.
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'short_code') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN short_code VARCHAR(10) NULL COMMENT ''column code in the stock report''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'sign_op') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN sign_op TINYINT NOT NULL DEFAULT 1 COMMENT ''in the closing formula: 1 = added, -1 = subtracted, 0 = information only''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'field_role') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN field_role VARCHAR(8) NOT NULL DEFAULT ''NORMAL'' COMMENT ''NORMAL / SALE / CLOSING / RATE / VALUE''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_builtin') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_builtin TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = has its own column in party_sales_child''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_core') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_core TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = always there, can be renamed, never deleted or hidden''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_auto') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_auto TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'primary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN primary_qty purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'secondary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN secondary_qty sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN return_qty return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'opening_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN opening_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'free_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN free_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'closing_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'calc_field') = 0, 'ALTER TABLE party_sales_child ADD COLUMN calc_field CHAR(1) NULL COMMENT ''C = closing calculated, S = sale calculated''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'extra_json') = 0, 'ALTER TABLE party_sales_child ADD COLUMN extra_json TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'item_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN item_rate DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'special_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN special_rate DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'stock_value') = 0, 'ALTER TABLE party_sales_child ADD COLUMN stock_value DECIMAL(14,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_parent' AND column_name = 'rate_basis') = 0, 'ALTER TABLE party_sales_parent ADD COLUMN rate_basis VARCHAR(14) NOT NULL DEFAULT ''PTS''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- the old single "Return" field is replaced by Return from Retailer / Return to Consignee
DELETE FROM inventory_field_master WHERE field_code = 'RETURN';
-- rows of an earlier copy get the new attributes (only the ones never upgraded)
UPDATE inventory_field_master SET short_code = CASE field_code WHEN 'SPECIAL_RATE' THEN 'SR' WHEN 'OPENING' THEN 'O' WHEN 'PRIMARY' THEN 'P' WHEN 'SECONDARY' THEN 'S' WHEN 'FREE' THEN 'FG' WHEN 'RETURN_RETAILER' THEN 'RGFR' WHEN 'RETURN_CONSIGNEE' THEN 'RGTCSA' WHEN 'EXPIRY_RETAILER' THEN 'EGFR' WHEN 'EXPIRY_CONSIGNEE' THEN 'EGRTCSA' WHEN 'CLOSING' THEN 'C' WHEN 'STOCK_VALUE' THEN 'SV' ELSE field_code END,
  sign_op = CASE field_code WHEN 'OPENING' THEN 1 WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN -1 WHEN 'FREE' THEN -1 WHEN 'RETURN_RETAILER' THEN 1 WHEN 'RETURN_CONSIGNEE' THEN -1 WHEN 'EXPIRY_RETAILER' THEN 1 WHEN 'EXPIRY_CONSIGNEE' THEN -1 ELSE 0 END,
  field_role = CASE field_code WHEN 'SECONDARY' THEN 'SALE' WHEN 'CLOSING' THEN 'CLOSING' WHEN 'SPECIAL_RATE' THEN 'RATE' WHEN 'STOCK_VALUE' THEN 'VALUE' ELSE 'NORMAL' END,
  is_builtin = 1, is_core = IF(field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE'), 1, 0)
WHERE short_code IS NULL AND field_code IN ('SPECIAL_RATE','OPENING','PRIMARY','SECONDARY','FREE','RETURN_RETAILER','RETURN_CONSIGNEE','EXPIRY_RETAILER','EXPIRY_CONSIGNEE','CLOSING','STOCK_VALUE');
UPDATE inventory_field_master SET short_code = field_code WHERE short_code IS NULL;
UPDATE inventory_field_master SET is_core = 1 WHERE field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE');
INSERT IGNORE INTO inventory_field_master (field_code, label, short_code, sign_op, field_role, is_builtin, is_core, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate','SR',0,'RATE',1,0,10),('OPENING','Opening Quantity','O',1,'NORMAL',1,1,20),('PRIMARY','Purchase Quantity','P',1,'NORMAL',1,1,30),('SECONDARY','Sale','S',-1,'SALE',1,1,40),('FREE','Free Goods','FG',-1,'NORMAL',1,1,50),
 ('RETURN_RETAILER','Return from Retailer','RGFR',1,'NORMAL',1,0,60),('RETURN_CONSIGNEE','Return to Consignee','RGTCSA',-1,'NORMAL',1,0,70),('EXPIRY_RETAILER','Expiry from Retailer','EGFR',1,'NORMAL',1,0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee','EGRTCSA',-1,'NORMAL',1,0,90),
 ('CLOSING','Closing Quantity','C',0,'CLOSING',1,1,100),('STOCK_VALUE','Stock Value','SV',0,'VALUE',1,1,110);
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS'),('OPENING_EDITABLE','N'),('SALE_CLOSING_LOCK','Y');

USE sfa_demo;

-- brings an inventory set-up made by an EARLIER copy of 124 up to date (the tables are only created when missing). Safe to re-run.
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'short_code') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN short_code VARCHAR(10) NULL COMMENT ''column code in the stock report''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'sign_op') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN sign_op TINYINT NOT NULL DEFAULT 1 COMMENT ''in the closing formula: 1 = added, -1 = subtracted, 0 = information only''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'field_role') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN field_role VARCHAR(8) NOT NULL DEFAULT ''NORMAL'' COMMENT ''NORMAL / SALE / CLOSING / RATE / VALUE''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_builtin') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_builtin TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = has its own column in party_sales_child''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_core') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_core TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = always there, can be renamed, never deleted or hidden''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory_field_master' AND column_name = 'is_auto') = 0, 'ALTER TABLE inventory_field_master ADD COLUMN is_auto TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'primary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN primary_qty purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'secondary_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN secondary_qty sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_qty') > 0 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child CHANGE COLUMN return_qty return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'opening_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN opening_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'purchase_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN purchase_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'sale_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN sale_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'free_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN free_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'return_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN return_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_retailer_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_retailer_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'expiry_consignee_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN expiry_consignee_qty DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'closing_qty') = 0, 'ALTER TABLE party_sales_child ADD COLUMN closing_qty DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'calc_field') = 0, 'ALTER TABLE party_sales_child ADD COLUMN calc_field CHAR(1) NULL COMMENT ''C = closing calculated, S = sale calculated''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'extra_json') = 0, 'ALTER TABLE party_sales_child ADD COLUMN extra_json TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'item_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN item_rate DECIMAL(12,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'special_rate') = 0, 'ALTER TABLE party_sales_child ADD COLUMN special_rate DECIMAL(12,2) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_child' AND column_name = 'stock_value') = 0, 'ALTER TABLE party_sales_child ADD COLUMN stock_value DECIMAL(14,2) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s := IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'party_sales_parent' AND column_name = 'rate_basis') = 0, 'ALTER TABLE party_sales_parent ADD COLUMN rate_basis VARCHAR(14) NOT NULL DEFAULT ''PTS''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- the old single "Return" field is replaced by Return from Retailer / Return to Consignee
DELETE FROM inventory_field_master WHERE field_code = 'RETURN';
-- rows of an earlier copy get the new attributes (only the ones never upgraded)
UPDATE inventory_field_master SET short_code = CASE field_code WHEN 'SPECIAL_RATE' THEN 'SR' WHEN 'OPENING' THEN 'O' WHEN 'PRIMARY' THEN 'P' WHEN 'SECONDARY' THEN 'S' WHEN 'FREE' THEN 'FG' WHEN 'RETURN_RETAILER' THEN 'RGFR' WHEN 'RETURN_CONSIGNEE' THEN 'RGTCSA' WHEN 'EXPIRY_RETAILER' THEN 'EGFR' WHEN 'EXPIRY_CONSIGNEE' THEN 'EGRTCSA' WHEN 'CLOSING' THEN 'C' WHEN 'STOCK_VALUE' THEN 'SV' ELSE field_code END,
  sign_op = CASE field_code WHEN 'OPENING' THEN 1 WHEN 'PRIMARY' THEN 1 WHEN 'SECONDARY' THEN -1 WHEN 'FREE' THEN -1 WHEN 'RETURN_RETAILER' THEN 1 WHEN 'RETURN_CONSIGNEE' THEN -1 WHEN 'EXPIRY_RETAILER' THEN 1 WHEN 'EXPIRY_CONSIGNEE' THEN -1 ELSE 0 END,
  field_role = CASE field_code WHEN 'SECONDARY' THEN 'SALE' WHEN 'CLOSING' THEN 'CLOSING' WHEN 'SPECIAL_RATE' THEN 'RATE' WHEN 'STOCK_VALUE' THEN 'VALUE' ELSE 'NORMAL' END,
  is_builtin = 1, is_core = IF(field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE'), 1, 0)
WHERE short_code IS NULL AND field_code IN ('SPECIAL_RATE','OPENING','PRIMARY','SECONDARY','FREE','RETURN_RETAILER','RETURN_CONSIGNEE','EXPIRY_RETAILER','EXPIRY_CONSIGNEE','CLOSING','STOCK_VALUE');
UPDATE inventory_field_master SET short_code = field_code WHERE short_code IS NULL;
UPDATE inventory_field_master SET is_core = 1 WHERE field_code IN ('OPENING','PRIMARY','SECONDARY','FREE','CLOSING','STOCK_VALUE');
INSERT IGNORE INTO inventory_field_master (field_code, label, short_code, sign_op, field_role, is_builtin, is_core, sort_order) VALUES
 ('SPECIAL_RATE','Spl. Rate','SR',0,'RATE',1,0,10),('OPENING','Opening Quantity','O',1,'NORMAL',1,1,20),('PRIMARY','Purchase Quantity','P',1,'NORMAL',1,1,30),('SECONDARY','Sale','S',-1,'SALE',1,1,40),('FREE','Free Goods','FG',-1,'NORMAL',1,1,50),
 ('RETURN_RETAILER','Return from Retailer','RGFR',1,'NORMAL',1,0,60),('RETURN_CONSIGNEE','Return to Consignee','RGTCSA',-1,'NORMAL',1,0,70),('EXPIRY_RETAILER','Expiry from Retailer','EGFR',1,'NORMAL',1,0,80),('EXPIRY_CONSIGNEE','Expiry to Consignee','EGRTCSA',-1,'NORMAL',1,0,90),
 ('CLOSING','Closing Quantity','C',0,'CLOSING',1,1,100),('STOCK_VALUE','Stock Value','SV',0,'VALUE',1,1,110);
INSERT IGNORE INTO inventory_setting (setting_key, setting_value) VALUES ('RATE_SHOWN','SPECIAL'),('FILE_UPLOAD','N'),('FILE_MANDATORY','N'),('SALE_CLOSING_CALC','Y'),('RATE_BASIS','PTS'),('OPENING_EDITABLE','N'),('SALE_CLOSING_LOCK','Y');
