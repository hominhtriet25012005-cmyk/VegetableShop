-- Phase 13: real bank transfer QR, manually reconciled by Admin.
-- Stop the app and back up vegetable_shop first. Rerunnable on MySQL 8+/9.
USE vegetable_shop;
SET @p13sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='checkout_token'), 'SELECT 1', 'ALTER TABLE orders ADD COLUMN checkout_token VARCHAR(36) NULL');
PREPARE p13stmt FROM @p13sql;
EXECUTE p13stmt;
DEALLOCATE PREPARE p13stmt;

SET @p13sql = IF(EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='orders' AND index_name='uk_orders_checkout_token'), 'SELECT 1', 'CREATE UNIQUE INDEX uk_orders_checkout_token ON orders(checkout_token)');
PREPARE p13stmt FROM @p13sql;
EXECUTE p13stmt;
DEALLOCATE PREPARE p13stmt;

SET @p13sql = IF(EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='orders' AND constraint_name='chk_orders_payment_method'), 'ALTER TABLE orders DROP CHECK chk_orders_payment_method', 'SELECT 1');
PREPARE p13stmt FROM @p13sql;
EXECUTE p13stmt;
DEALLOCATE PREPARE p13stmt;

ALTER TABLE orders MODIFY COLUMN payment_method VARCHAR(20) NOT NULL;
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_method CHECK (payment_method IN ('COD','VNPAY','BANK_TRANSFER'));
SET @p13sql = IF(EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='orders' AND constraint_name='chk_orders_payment_status'), 'ALTER TABLE orders DROP CHECK chk_orders_payment_status', 'SELECT 1');
PREPARE p13stmt FROM @p13sql;
EXECUTE p13stmt;
DEALLOCATE PREPARE p13stmt;

ALTER TABLE orders MODIFY COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_status CHECK (payment_status IN ('UNPAID','REPORTED','PAID','FAILED','REFUNDED'));
CREATE TABLE IF NOT EXISTS bank_transfer_payments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT NOT NULL,
 bank_bin VARCHAR(6) NOT NULL,
 bank_name VARCHAR(100) NOT NULL,
 account_number VARCHAR(19) NOT NULL,
 account_name VARCHAR(100) NOT NULL,
 reference VARCHAR(25) NOT NULL,
 amount DECIMAL(15,2) NOT NULL,
 reported_at DATETIME NULL,
 confirmed_by VARCHAR(150) NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT uk_bank_transfer_order UNIQUE(order_id),
 CONSTRAINT uk_bank_transfer_reference UNIQUE(reference),
 CONSTRAINT fk_bank_transfer_order FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE RESTRICT,
 CONSTRAINT chk_bank_transfer_amount CHECK(amount > 0 AND amount < 500000000 AND amount = FLOOR(amount))
) ENGINE=InnoDB;
SELECT 'Phase 13 schema ready' AS migration_result;
