-- Vegetable Shop - Phase 18A Admin catalog and inventory documents.
-- MySQL 8.x. Safe to execute more than once.
-- IMPORTANT: run phase-18-inventory-migration.sql first when upgrading an old database.
USE vegetable_shop;

-- 1. Brands are customer-facing labels. They are not suppliers.
CREATE TABLE IF NOT EXISTS brands (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    logo VARCHAR(500) NULL,
    description TEXT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_brands_name UNIQUE (name)
) ENGINE = InnoDB;

-- 2. Enrich suppliers without losing existing rows.
SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'suppliers' AND column_name = 'code');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE suppliers ADD COLUMN code VARCHAR(30) NULL AFTER id', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'suppliers' AND column_name = 'contact_person');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE suppliers ADD COLUMN contact_person VARCHAR(100) NULL AFTER name', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'suppliers' AND column_name = 'tax_code');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE suppliers ADD COLUMN tax_code VARCHAR(30) NULL AFTER address', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

-- Include the primary key in WHERE so this also works with Workbench Safe Update Mode.
UPDATE suppliers
SET code = CONCAT('NCC-', LPAD(id, 6, '0'))
WHERE id > 0 AND (code IS NULL OR TRIM(code) = '');
ALTER TABLE suppliers MODIFY COLUMN code VARCHAR(30) NOT NULL;
SET @has_index = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'suppliers' AND index_name = 'uk_suppliers_code');
SET @sql = IF(@has_index = 0,
    'ALTER TABLE suppliers ADD CONSTRAINT uk_suppliers_code UNIQUE (code)', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

-- 3. Product catalog fields and audit actor snapshots.
SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'sku');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE products ADD COLUMN sku VARCHAR(40) NULL AFTER id', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'brand_id');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE products ADD COLUMN brand_id BIGINT NULL AFTER category_id', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'unit');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE products ADD COLUMN unit VARCHAR(30) NOT NULL DEFAULT ''KILOGRAM'' AFTER brand_id', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'origin');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE products ADD COLUMN origin VARCHAR(150) NULL AFTER unit', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'created_by');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE products ADD COLUMN created_by VARCHAR(150) NOT NULL DEFAULT ''SYSTEM-MIGRATION'' AFTER status', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'products' AND column_name = 'updated_by');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE products ADD COLUMN updated_by VARCHAR(150) NOT NULL DEFAULT ''SYSTEM-MIGRATION'' AFTER created_by', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

-- Include the primary key in WHERE so this also works with Workbench Safe Update Mode.
UPDATE products
SET sku = CONCAT('SP-', LPAD(id, 6, '0'))
WHERE id > 0 AND (sku IS NULL OR TRIM(sku) = '');
ALTER TABLE products MODIFY COLUMN sku VARCHAR(40) NOT NULL;
SET @has_index = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'products' AND index_name = 'uk_products_sku');
SET @sql = IF(@has_index = 0,
    'ALTER TABLE products ADD CONSTRAINT uk_products_sku UNIQUE (sku)', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_fk = (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'products'
      AND constraint_name = 'fk_products_brand' AND constraint_type = 'FOREIGN KEY');
SET @sql = IF(@has_fk = 0,
    'ALTER TABLE products ADD CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON UPDATE CASCADE ON DELETE SET NULL',
    'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS product_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON UPDATE CASCADE ON DELETE CASCADE,
    INDEX idx_product_images_product_order (product_id, display_order)
) ENGINE = InnoDB;

-- 4. Document header and immutable posted document lines.
CREATE TABLE IF NOT EXISTS inventory_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    supplier_id BIGINT NULL,
    invoice_reference VARCHAR(100) NULL,
    note VARCHAR(1000) NULL,
    created_by VARCHAR(150) NOT NULL,
    posted_by VARCHAR(150) NULL,
    posted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_inventory_documents_code UNIQUE (code),
    CONSTRAINT chk_inventory_documents_type CHECK (type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT')),
    CONSTRAINT chk_inventory_documents_status CHECK (status IN ('DRAFT', 'POSTED')),
    CONSTRAINT fk_inventory_documents_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_inventory_documents_created (created_at),
    INDEX idx_inventory_documents_type_status (type, status),
    INDEX idx_inventory_documents_supplier (supplier_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS inventory_document_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    product_sku VARCHAR(40) NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(15,2) NULL,
    quantity_before INT NULL,
    quantity_change INT NULL,
    quantity_after INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inventory_document_items_document FOREIGN KEY (document_id)
        REFERENCES inventory_documents(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_inventory_document_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_inventory_document_items_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_document_items_cost CHECK (unit_cost IS NULL OR unit_cost >= 0),
    CONSTRAINT uk_inventory_document_product UNIQUE (document_id, product_id),
    INDEX idx_inventory_document_items_product (product_id)
) ENGINE = InnoDB;

-- 5. Link the immutable stock ledger to its source document.
SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'stock_movements'
      AND column_name = 'inventory_document_id');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE stock_movements ADD COLUMN inventory_document_id BIGINT NULL AFTER product_id', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'stock_movements'
      AND column_name = 'inventory_document_item_id');
SET @sql = IF(@has_column = 0,
    'ALTER TABLE stock_movements ADD COLUMN inventory_document_item_id BIGINT NULL AFTER inventory_document_id', 'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_fk = (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'stock_movements'
      AND constraint_name = 'fk_stock_movements_document' AND constraint_type = 'FOREIGN KEY');
SET @sql = IF(@has_fk = 0,
    'ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_document FOREIGN KEY (inventory_document_id) REFERENCES inventory_documents(id) ON UPDATE CASCADE ON DELETE RESTRICT',
    'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

SET @has_fk = (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE() AND table_name = 'stock_movements'
      AND constraint_name = 'fk_stock_movements_document_item' AND constraint_type = 'FOREIGN KEY');
SET @sql = IF(@has_fk = 0,
    'ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_document_item FOREIGN KEY (inventory_document_item_id) REFERENCES inventory_document_items(id) ON UPDATE CASCADE ON DELETE RESTRICT',
    'SELECT 1');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

-- Legacy products.supplier_id is intentionally retained until Phase 18B so the
-- current public website keeps working. New inventory sourcing is recorded on
-- inventory_documents.supplier_id and does not use products.supplier_id.

-- Verification:
-- SELECT id, sku, name, unit, brand_id, quantity, created_by FROM products ORDER BY id;
-- SELECT id, code, type, status, supplier_id FROM inventory_documents ORDER BY id DESC;
-- SELECT * FROM inventory_document_items ORDER BY document_id DESC, id;
