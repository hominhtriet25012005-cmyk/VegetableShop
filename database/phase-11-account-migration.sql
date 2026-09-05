-- Vegetable Shop - Phase 11 migration for an existing MySQL database.
-- Run once after selecting the vegetable_shop schema.

USE vegetable_shop;

-- Google-only accounts do not have a local password.
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;

SET @has_auth_provider = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'auth_provider'
);
SET @sql = IF(
    @has_auth_provider = 0,
    "ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' AFTER password",
    "SELECT 1"
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @has_oauth_subject = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'oauth_subject'
);
SET @sql = IF(
    @has_oauth_subject = 0,
    "ALTER TABLE users ADD COLUMN oauth_subject VARCHAR(255) NULL AFTER auth_provider",
    "SELECT 1"
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @has_oauth_unique = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'uk_users_oauth_subject'
);
SET @sql = IF(
    @has_oauth_unique = 0,
    "ALTER TABLE users ADD CONSTRAINT uk_users_oauth_subject UNIQUE (oauth_subject)",
    "SELECT 1"
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_password_reset_user (user_id),
    INDEX idx_password_reset_expiry (expires_at)
) ENGINE = InnoDB;
