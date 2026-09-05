-- Vegetable Shop - Phase 12 migration for an existing MySQL database.
-- Run once after selecting the vegetable_shop schema.

USE vegetable_shop;

SET @has_email_verified = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'email_verified'
);
SET @sql = IF(
    @has_email_verified = 0,
    "ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE AFTER status",
    "SELECT 1"
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- All accounts that existed before Phase 12 remain usable.
UPDATE users SET email_verified = TRUE WHERE email_verified IS NULL;

CREATE TABLE IF NOT EXISTS account_activation_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_activation_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_activation_token_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_activation_token_user (user_id),
    INDEX idx_activation_token_expiry (expires_at)
) ENGINE = InnoDB;
