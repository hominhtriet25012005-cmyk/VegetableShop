-- Phase 17 - Wishlist, persistent view history and rule-based recommendations
-- Safe to execute more than once on MySQL 8.x.
USE vegetable_shop;

CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_wishlists_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_wishlists_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    INDEX idx_wishlists_user_created_at (user_id, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS product_view_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    view_count INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_view_histories_user_product UNIQUE (user_id, product_id),
    CONSTRAINT chk_product_view_histories_count CHECK (view_count > 0),
    CONSTRAINT fk_product_view_histories_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_product_view_histories_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    INDEX idx_product_view_histories_user_updated_at (user_id, updated_at)
) ENGINE = InnoDB;

-- Verification:
-- SHOW CREATE TABLE wishlists;
-- SHOW CREATE TABLE product_view_histories;
