-- Phase 19: advanced reviews. Stop the application and back up MySQL first.
-- MySQL 8.x; rerunnable. Existing reviews are retained and start pending moderation.
USE vegetable_shop;
SET @phase19_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'order_detail_id'),
  'SELECT 1', 'ALTER TABLE reviews ADD COLUMN order_detail_id BIGINT NULL');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'status'),
  'SELECT 1', 'ALTER TABLE reviews ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING''');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'moderation_note'),
  'SELECT 1', 'ALTER TABLE reviews ADD COLUMN moderation_note VARCHAR(500) NULL');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'moderated_at'),
  'SELECT 1', 'ALTER TABLE reviews ADD COLUMN moderated_at DATETIME NULL');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reviews' AND column_name = 'moderated_by'),
  'SELECT 1', 'ALTER TABLE reviews ADD COLUMN moderated_by BIGINT NULL');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'idx_reviews_user_id'),
  'SELECT 1', 'CREATE INDEX idx_reviews_user_id ON reviews(user_id)');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'uk_reviews_order_detail'),
  'SELECT 1', 'CREATE UNIQUE INDEX uk_reviews_order_detail ON reviews(order_detail_id)');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'idx_reviews_status_created'),
  'SELECT 1', 'CREATE INDEX idx_reviews_status_created ON reviews(status, created_at)');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

-- The explicit user index retains support for fk_reviews_user before dropping the old unique index.
SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'reviews' AND index_name = 'uk_reviews_user_product'),
  'ALTER TABLE reviews DROP INDEX uk_reviews_user_product', 'SELECT 1');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

-- Only map unlinked legacy reviews to a real completed purchase owned by the same customer.
-- Do not overwrite links or moderation states on reruns.
-- A temporary table avoids MySQL's target-table/subquery restriction.
DROP TEMPORARY TABLE IF EXISTS phase19_legacy_candidates;
CREATE TEMPORARY TABLE phase19_legacy_candidates AS
SELECT r.id AS review_id, MIN(d.id) AS detail_id
FROM reviews r
JOIN order_details d ON d.product_id = r.product_id
JOIN orders o ON o.id = d.order_id AND o.user_id = r.user_id AND o.status = 'COMPLETED'
LEFT JOIN reviews used ON used.order_detail_id = d.id
WHERE r.order_detail_id IS NULL AND used.id IS NULL AND r.status = 'PENDING'
GROUP BY r.id;
-- Only unambiguous mappings; unexpected duplicate legacy reviews remain unverified.
DROP TEMPORARY TABLE IF EXISTS phase19_unique_candidates;
CREATE TEMPORARY TABLE phase19_unique_candidates AS
SELECT MIN(review_id) AS review_id, detail_id FROM phase19_legacy_candidates
GROUP BY detail_id HAVING COUNT(*) = 1;
UPDATE reviews r JOIN phase19_unique_candidates c ON r.id = c.review_id
SET r.order_detail_id = c.detail_id WHERE r.id > 0 AND r.order_detail_id IS NULL;
DROP TEMPORARY TABLE phase19_legacy_candidates;
DROP TEMPORARY TABLE phase19_unique_candidates;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'reviews' AND constraint_name = 'fk_reviews_order_detail'),
  'SELECT 1', 'ALTER TABLE reviews ADD CONSTRAINT fk_reviews_order_detail FOREIGN KEY (order_detail_id) REFERENCES order_details(id) ON DELETE RESTRICT');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

SET @phase19_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'reviews' AND constraint_name = 'fk_reviews_moderated_by'),
  'SELECT 1', 'ALTER TABLE reviews ADD CONSTRAINT fk_reviews_moderated_by FOREIGN KEY (moderated_by) REFERENCES users(id) ON DELETE SET NULL');
PREPARE phase19_stmt FROM @phase19_sql;
EXECUTE phase19_stmt;
DEALLOCATE PREPARE phase19_stmt;

CREATE TABLE IF NOT EXISTS product_review_images (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  review_id BIGINT NOT NULL,
  storage_key VARCHAR(50) NOT NULL,
  CONSTRAINT uk_review_images_storage_key UNIQUE(storage_key),
  CONSTRAINT fk_review_images_review FOREIGN KEY(review_id) REFERENCES reviews(id) ON DELETE RESTRICT,
  INDEX idx_review_images_review(review_id)
) ENGINE=InnoDB;

SET SQL_SAFE_UPDATES = @phase19_safe_updates;
SELECT status, COUNT(*) AS review_count, SUM(order_detail_id IS NULL) AS unverified
FROM reviews GROUP BY status;
