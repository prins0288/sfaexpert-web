-- ===========================================================================
-- TENANT DATABASES  (one per tenant, each with its own product table)
-- ===========================================================================

-- ---- Tenant A ----
CREATE DATABASE IF NOT EXISTS tenant_a_db;
USE tenant_a_db;
CREATE TABLE IF NOT EXISTS product (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    price DECIMAL(12,2)
);
INSERT INTO product (name, price) VALUES ('A - Laptop', 55000.00), ('A - Mouse', 700.00);

-- ---- Tenant B ----
CREATE DATABASE IF NOT EXISTS tenant_b_db;
USE tenant_b_db;
CREATE TABLE IF NOT EXISTS product (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    price DECIMAL(12,2)
);
INSERT INTO product (name, price) VALUES ('B - Chair', 3200.00), ('B - Desk', 8000.00);
