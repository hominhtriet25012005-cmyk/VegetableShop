# Giai đoạn 18B – Hoàn thiện chi tiết sản phẩm phía website

## Kết quả

Giai đoạn 18B đưa dữ liệu danh mục sản phẩm đã chuẩn hóa ở 18A ra website và
tách đúng vai trò thương hiệu với nhà cung cấp:

- Trang chi tiết hiển thị SKU, thương hiệu, xuất xứ và đơn vị bán động.
- Ảnh đại diện và ảnh phụ tạo thành gallery; ảnh đại diện luôn đứng đầu và URL
  trùng được loại bỏ.
- Nút ảnh thu nhỏ thay ảnh chính mà không tải lại trang.
- Khách hàng xem sản phẩm cùng danh mục và cùng thương hiệu.
- Nhà cung cấp không còn xuất hiện ở website; dữ liệu này chỉ dùng trong Admin
  và chứng từ nhập kho.
- Gợi ý rule-based dùng thương hiệu thay cho nhà cung cấp.
- Số lượng mua bị giới hạn theo tồn kho; sản phẩm hết hàng không có biểu mẫu
  thêm vào giỏ.
- Nút **Mua ngay** dùng chung nghiệp vụ thêm giỏ an toàn rồi chuyển đến checkout.

## Luồng dữ liệu

```text
Admin sản phẩm (18A)
    -> SKU / thương hiệu / đơn vị / xuất xứ / nhiều ảnh
    -> ProductService và ProductRepository
    -> /product/{id}
    -> Gallery + thông tin bán + sản phẩm liên quan
```

Trang công khai chỉ tải `Category`, `Brand` và `ProductImage` cần thiết. Quan hệ
`Supplier` không được fetch cho các truy vấn website.

## Quy tắc gallery

1. Ảnh đại diện của sản phẩm đứng đầu.
2. Ảnh phụ được sắp theo `displayOrder` từ Admin.
3. Bỏ URL rỗng và URL trùng.
4. Nếu chưa có ảnh hợp lệ, dùng `/img/hero-img.jpg` làm ảnh dự phòng.

## Sản phẩm liên quan và gợi ý

- **Hàng cùng loại:** tối đa 4 sản phẩm cùng danh mục, loại sản phẩm hiện tại.
- **Cùng thương hiệu:** tối đa 4 sản phẩm cùng thương hiệu, loại sản phẩm hiện
  tại.
- Chỉ lấy sản phẩm và danh mục đang hoạt động.
- Gợi ý cá nhân loại sản phẩm hết hàng và chấm điểm theo danh mục, thương hiệu,
  khoảng giá và lịch sử xem gần đây.

## Database

Giai đoạn 18B **không có migration mới**. Database đang dùng chỉ cần đã chạy:

1. `database/phase-18-inventory-migration.sql`.
2. `database/phase-18a-admin-catalog-inventory-documents.sql`.

Nếu sản phẩm cũ chưa có thương hiệu, ảnh phụ hoặc xuất xứ, trang vẫn hoạt động;
Admin có thể bổ sung tại `/admin/products/{id}/edit`.

## Tệp chính

- `controller/ProductController.java`
- `service/ProductService.java`
- `service/RecommendationService.java`
- `repository/ProductRepository.java`
- `templates/shop-detail.html`
- `static/js/product-detail.js`
- `static/js/cart.js`
- `static/css/style.css`

## Kiểm thử

Đã xác minh toàn bộ project:

```text
Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test 18B bao phủ model của trang chi tiết, gallery không trùng ảnh, truy vấn sản
phẩm cùng thương hiệu, lý do gợi ý theo thương hiệu và render SKU, xuất xứ, đơn
vị bán, gallery, nút Mua ngay cùng CSRF.

## Backup

- Trước khi triển khai:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-18B-20260830-1703.zip`
- Sau triển khai và kiểm thử:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-18B-verified-20260830-1714.zip`

Hai file ZIP loại trừ `.git`, `.idea`, `target` và file module IDE.
