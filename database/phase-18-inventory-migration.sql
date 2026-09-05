-- Vegetable Shop - Phase 18 inventory management migration.
-- Safe to execute more than once on MySQL 8.x.
USE vegetable_shop;

SET @has_low_stock_threshold = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'products'
      AND column_name = 'low_stock_threshold'
);
SET @sql = IF(
    @has_low_stock_threshold = 0,
    'ALTER TABLE products ADD COLUMN low_stock_threshold INT NOT NULL DEFAULT 10 AFTER quantity',
    'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity_before INT NOT NULL,
    quantity_change INT NOT NULL,
    quantity_after INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    reference_type VARCHAR(30) NULL,
    reference_id VARCHAR(100) NULL,
    performed_by VARCHAR(150) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_stock_movements_type CHECK (
        movement_type IN ('INITIAL', 'INBOUND', 'OUTBOUND', 'ADJUSTMENT', 'SALE', 'RETURN')
    ),
    CONSTRAINT chk_stock_movements_before CHECK (quantity_before >= 0),
    CONSTRAINT chk_stock_movements_after CHECK (quantity_after >= 0),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_stock_movements_product_created (product_id, created_at),
    INDEX idx_stock_movements_type_created (movement_type, created_at),
    INDEX idx_stock_movements_reference (reference_type, reference_id),
    INDEX idx_stock_movements_created (created_at)
) ENGINE = InnoDB;

-- Create one baseline transaction for products that predate Phase 18.
INSERT INTO stock_movements
    (product_id, product_name, movement_type, quantity_before, quantity_change,
     quantity_after, reason, reference_type, reference_id, performed_by)
SELECT p.id, p.name, 'INITIAL', 0, p.quantity, p.quantity,
       'Ghi nhận tồn đầu kỳ khi nâng cấp Giai đoạn 18',
       'PRODUCT', CAST(p.id AS CHAR), 'SYSTEM-MIGRATION'
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM stock_movements sm WHERE sm.product_id = p.id
);

-- Verification:
-- SHOW CREATE TABLE stock_movements;
-- SELECT id, name, quantity, low_stock_threshold FROM products ORDER BY id;
-- SELECT * FROM stock_movements ORDER BY created_at DESC, id DESC;
