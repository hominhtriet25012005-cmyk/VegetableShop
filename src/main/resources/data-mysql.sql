-- Idempotent sample data for Phase 3.
-- USER accounts are created through /register in Phase 5. An initial ADMIN can
-- be created from APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD environment variables.

INSERT IGNORE INTO categories
    (id, name, description, image, status, created_at, updated_at)
VALUES
    (1, 'Rau củ', 'Các loại rau củ tươi sạch mỗi ngày', '/img/vegetable-item-1.jpg', TRUE, NOW(), NOW()),
    (2, 'Trái cây', 'Trái cây tươi trong nước và nhập khẩu', '/img/fruite-item-1.jpg', TRUE, NOW(), NOW()),
    (3, 'Thực phẩm sạch', 'Nhóm thực phẩm sạch được chọn lọc', '/img/best-product-1.jpg', TRUE, NOW(), NOW());

INSERT IGNORE INTO suppliers
    (id, name, phone, email, address, status, created_at, updated_at)
VALUES
    (1, 'Nông trại Xanh Việt', '0901000001', 'xanhviet@example.com', 'Đà Lạt, Lâm Đồng', TRUE, NOW(), NOW()),
    (2, 'Hợp tác xã Miền Tây', '0901000002', 'mientay@example.com', 'Cần Thơ', TRUE, NOW(), NOW()),
    (3, 'Fresh Food Việt Nam', '0901000003', 'freshfood@example.com', 'TP. Hồ Chí Minh', TRUE, NOW(), NOW());

INSERT IGNORE INTO products
    (id, name, description, price, quantity, image, category_id, supplier_id, status, created_at, updated_at)
VALUES
    (1, 'Bông cải xanh', 'Bông cải xanh tươi, phù hợp cho bữa ăn gia đình.', 35000.00, 50, '/img/vegetable-item-1.jpg', 1, 1, TRUE, NOW(), NOW()),
    (2, 'Ớt chuông', 'Ớt chuông giòn ngọt, giàu vitamin.', 45000.00, 40, '/img/vegetable-item-2.jpg', 1, 1, TRUE, NOW(), NOW()),
    (3, 'Chuối', 'Chuối chín tự nhiên, không sử dụng chất bảo quản.', 30000.00, 60, '/img/fruite-item-3.jpg', 2, 2, TRUE, NOW(), NOW()),
    (4, 'Cam', 'Cam tươi mọng nước, vị chua ngọt tự nhiên.', 55000.00, 45, '/img/fruite-item-4.jpg', 2, 2, TRUE, NOW(), NOW()),
    (5, 'Táo', 'Táo giòn, phù hợp dùng trực tiếp hoặc làm nước ép.', 75000.00, 35, '/img/fruite-item-6.jpg', 2, 3, TRUE, NOW(), NOW());

-- Existing Phase 3 rows are not changed by INSERT IGNORE, so attach suppliers once.
UPDATE products
SET supplier_id = CASE
    WHEN id IN (1, 2) THEN 1
    WHEN id IN (3, 4) THEN 2
    WHEN id = 5 THEN 3
    ELSE supplier_id
END
WHERE supplier_id IS NULL;
