-- Hoàn tác đúng snapshot trước khi gắn ảnh ngày 2026-08-28.
-- Trước lần cập nhật này, cột image của toàn bộ sản phẩm id 7-86 đều NULL.

USE vegetable_shop;

UPDATE products
SET image = NULL,
    updated_at = NOW()
WHERE id BETWEEN 7 AND 86
  AND category_id IN (1, 2, 3, 4)
  AND image LIKE 'https://loremflickr.com/%';

SELECT category_id, COUNT(*) AS products_without_image
FROM products
WHERE category_id IN (1, 2, 3, 4)
  AND image IS NULL
GROUP BY category_id
ORDER BY category_id;
