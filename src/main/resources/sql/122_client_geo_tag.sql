-- 122: client_geo_tag - a client can sit at several places (several lat / lon); the primary one stays in client_master. Safe to re-run.

USE acme_db;

CREATE TABLE IF NOT EXISTS client_geo_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL COMMENT '-> client_master.id; the PRIMARY place stays in client_master.latitude / longitude, this table holds the other places the client sits at',
  label VARCHAR(80) NULL COMMENT 'e.g. Clinic, Hospital, Residence',
  latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL, address VARCHAR(500) NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_cgt_client (client_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='more geo tags of a client who sits at several places; a check-in is valid within the fence of ANY tag';

USE globex_db;

CREATE TABLE IF NOT EXISTS client_geo_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL COMMENT '-> client_master.id; the PRIMARY place stays in client_master.latitude / longitude, this table holds the other places the client sits at',
  label VARCHAR(80) NULL COMMENT 'e.g. Clinic, Hospital, Residence',
  latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL, address VARCHAR(500) NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_cgt_client (client_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='more geo tags of a client who sits at several places; a check-in is valid within the fence of ANY tag';

USE initech_db;

CREATE TABLE IF NOT EXISTS client_geo_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL COMMENT '-> client_master.id; the PRIMARY place stays in client_master.latitude / longitude, this table holds the other places the client sits at',
  label VARCHAR(80) NULL COMMENT 'e.g. Clinic, Hospital, Residence',
  latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL, address VARCHAR(500) NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_cgt_client (client_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='more geo tags of a client who sits at several places; a check-in is valid within the fence of ANY tag';

USE sfa_demo;

CREATE TABLE IF NOT EXISTS client_geo_tag (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_id BIGINT NOT NULL COMMENT '-> client_master.id; the PRIMARY place stays in client_master.latitude / longitude, this table holds the other places the client sits at',
  label VARCHAR(80) NULL COMMENT 'e.g. Clinic, Hospital, Residence',
  latitude DECIMAL(10,7) NOT NULL, longitude DECIMAL(10,7) NOT NULL, address VARCHAR(500) NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_by VARCHAR(40) NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY ix_cgt_client (client_id, is_active)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='more geo tags of a client who sits at several places; a check-in is valid within the fence of ANY tag';
