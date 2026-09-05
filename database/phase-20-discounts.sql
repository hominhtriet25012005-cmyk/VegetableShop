-- Vegetable Shop - Phase 20: vouchers, promotions and Flash Sale.
-- Stop the application and back up MySQL before running. Safe to run repeatedly.
USE vegetable_shop;

CREATE TABLE IF NOT EXISTS vouchers (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30) NOT NULL, name VARCHAR(150) NOT NULL,
 discount_type VARCHAR(20) NOT NULL, discount_value DECIMAL(15,2) NOT NULL,
 minimum_order_amount DECIMAL(15,2) NOT NULL DEFAULT 0, maximum_discount_amount DECIMAL(15,2) NULL,
 starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL, total_usage_limit INT NULL,
 per_user_usage_limit INT NOT NULL DEFAULT 1, status BOOLEAN NOT NULL DEFAULT TRUE,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT uk_vouchers_code UNIQUE(code),
 CONSTRAINT chk_vouchers_type CHECK(discount_type IN('PERCENTAGE','FIXED_AMOUNT')),
 CONSTRAINT chk_vouchers_value CHECK(discount_value>0), CONSTRAINT chk_vouchers_dates CHECK(ends_at>starts_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS voucher_scopes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, voucher_id BIGINT NOT NULL,
 scope_type VARCHAR(20) NOT NULL, target_id BIGINT NULL,
 CONSTRAINT fk_voucher_scopes_voucher FOREIGN KEY(voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
 CONSTRAINT chk_voucher_scopes_type CHECK(scope_type IN('ORDER','CATEGORY','BRAND','PRODUCT')),
 INDEX idx_voucher_scopes_lookup(scope_type,target_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS promotions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(150) NOT NULL,
 discount_type VARCHAR(20) NOT NULL, discount_value DECIMAL(15,2) NOT NULL,
 starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL, flash_sale BOOLEAN NOT NULL DEFAULT FALSE,
 status BOOLEAN NOT NULL DEFAULT TRUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT chk_promotions_type CHECK(discount_type IN('PERCENTAGE','FIXED_AMOUNT')),
 CONSTRAINT chk_promotions_value CHECK(discount_value>0), CONSTRAINT chk_promotions_dates CHECK(ends_at>starts_at)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS promotion_products (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, promotion_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
 CONSTRAINT uk_promotion_product UNIQUE(promotion_id,product_id),
 CONSTRAINT fk_promotion_products_promotion FOREIGN KEY(promotion_id) REFERENCES promotions(id) ON DELETE CASCADE,
 CONSTRAINT fk_promotion_products_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE RESTRICT,
 INDEX idx_promotion_products_product(product_id)
) ENGINE=InnoDB;

SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='subtotal_amount'),'SELECT 1','ALTER TABLE orders ADD COLUMN subtotal_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER note'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='promotion_discount_amount'),'SELECT 1','ALTER TABLE orders ADD COLUMN promotion_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER subtotal_amount'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='voucher_discount_amount'),'SELECT 1','ALTER TABLE orders ADD COLUMN voucher_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER promotion_discount_amount'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='voucher_id'),'SELECT 1','ALTER TABLE orders ADD COLUMN voucher_id BIGINT NULL AFTER voucher_discount_amount'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='voucher_code'),'SELECT 1','ALTER TABLE orders ADD COLUMN voucher_code VARCHAR(30) NULL AFTER voucher_id'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='orders' AND constraint_name='fk_orders_voucher'),'SELECT 1','ALTER TABLE orders ADD CONSTRAINT fk_orders_voucher FOREIGN KEY(voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
UPDATE orders SET subtotal_amount=total_amount WHERE subtotal_amount=0;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='order_details' AND column_name='discount_amount'),'SELECT 1','ALTER TABLE order_details ADD COLUMN discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER quantity'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS voucher_usages (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, voucher_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 order_id BIGINT NOT NULL, discount_amount DECIMAL(15,2) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 released_at DATETIME NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT uk_voucher_usage_order UNIQUE(order_id),
 CONSTRAINT fk_voucher_usages_voucher FOREIGN KEY(voucher_id) REFERENCES vouchers(id) ON DELETE RESTRICT,
 CONSTRAINT fk_voucher_usages_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT fk_voucher_usages_order FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE RESTRICT,
 INDEX idx_voucher_usage_limits(voucher_id,user_id,active)
) ENGINE=InnoDB;

SELECT 'Phase 20 discount schema ready' AS migration_result;
