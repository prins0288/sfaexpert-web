-- ===========================================================================
-- permission_master + permission_assignment — PER TENANT database.
--
-- permission_master is the catalog of permission codes the app checks via
-- @RequiresPermission(...) on the backend and GET /api/permissions/my on the
-- front-end (to hide/show buttons and menu items).
--
-- permission_assignment grants/denies one permission_code to ONE target:
--   target_type = EMP_ID       -> target_value = emp_id        (most specific)
--   target_type = DESIGNATION  -> target_value = designation_code
--   target_type = EMP_LEVEL    -> target_value = emp_level (as text)
-- Resolution order (most specific wins): EMP_ID > DESIGNATION > EMP_LEVEL.
-- If NO row exists anywhere for a (target, permission_code), the permission
-- is ALLOWED by default — a tenant that configures nothing keeps full access.
--
-- Run:  mysql -u myroot < src/main/resources/sql/13_permission_master.sql
-- ===========================================================================

-- ==================== acme_db ====================
USE acme_db;
CREATE TABLE IF NOT EXISTS permission_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    module          VARCHAR(100) NULL,
    description     VARCHAR(255) NULL,
    status          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_master_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permission_assignment (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type     VARCHAR(20)  NOT NULL,   -- EMP_ID | DESIGNATION | EMP_LEVEL
    target_value    VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    allowed         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_assignment_target (target_type, target_value, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== globex_db ====================
USE globex_db;
CREATE TABLE IF NOT EXISTS permission_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    module          VARCHAR(100) NULL,
    description     VARCHAR(255) NULL,
    status          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_master_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permission_assignment (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type     VARCHAR(20)  NOT NULL,
    target_value    VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    allowed         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_assignment_target (target_type, target_value, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== initech_db ====================
USE initech_db;
CREATE TABLE IF NOT EXISTS permission_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    module          VARCHAR(100) NULL,
    description     VARCHAR(255) NULL,
    status          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_master_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permission_assignment (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type     VARCHAR(20)  NOT NULL,
    target_value    VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    allowed         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_assignment_target (target_type, target_value, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== sfa_demo ====================
USE sfa_demo;
CREATE TABLE IF NOT EXISTS permission_master (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    module          VARCHAR(100) NULL,
    description     VARCHAR(255) NULL,
    status          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_master_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permission_assignment (
    oid             BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type     VARCHAR(20)  NOT NULL,
    target_value    VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    allowed         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NULL,
    created_by      VARCHAR(128) NULL,
    updated_at      DATETIME     NULL,
    updated_by      VARCHAR(128) NULL,
    UNIQUE KEY uq_permission_assignment_target (target_type, target_value, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
