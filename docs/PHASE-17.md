# Giai đoạn 17 – Wishlist, lịch sử xem và gợi ý cơ bản

## Kết quả

Giai đoạn 17 bổ sung tính năng giữ sản phẩm quan tâm và gợi ý rule-based:

- Thêm hoặc bỏ yêu thích bằng AJAX tại trang chủ, danh sách và chi tiết sản phẩm.
- Trang `/wishlist` chỉ dành cho người đã đăng nhập.
- Chuyển sản phẩm yêu thích sang giỏ hàng bằng AJAX.
- Bộ đếm yêu thích và giỏ hàng được cập nhật không tải lại trang.
- Người đăng nhập có lịch sử xem lâu dài trong MySQL; khách chưa đăng nhập dùng session.
- Trang chi tiết hiển thị tối đa 8 gợi ý đang bán và còn hàng.

Chức năng chuyển sang giỏ áp dụng trực tiếp cho `Product`. Từ giai đoạn 18A–18B,
gợi ý sử dụng `Brand` đúng nghĩa thương hiệu; `Supplier` chỉ còn phục vụ nghiệp
vụ nhập kho trong Admin.

## Quy tắc dữ liệu và bảo mật

- Unique key `(user_id, product_id)` ngăn Wishlist trùng ở tầng database.
- API chỉ lấy email từ `Authentication`, không nhận `userId` từ trình duyệt.
- Mọi route `/wishlist/**` và `/api/wishlist/**` yêu cầu vai trò `USER` hoặc `ADMIN`.
- POST/DELETE giữ CSRF; JavaScript gửi token từ meta tag.
- Sản phẩm hoặc danh mục ngừng hoạt động không xuất hiện trong Wishlist/gợi ý.
- Sản phẩm hết hàng không được đưa vào danh sách gợi ý.

## Thuật toán gợi ý

Mỗi ứng viên được chấm điểm:

| Tiêu chí | Điểm |
| --- | ---: |
| Cùng danh mục | +4 |
| Cùng thương hiệu | +3 |
| Giá chênh không quá 20% | +2 |
| Khớp danh mục/thương hiệu trong lịch sử xem gần đây | +2 |

Kết quả được sắp theo điểm giảm dần, độ lệch giá tăng dần rồi theo ID để thứ tự
ổn định. Đây là thuật toán giải thích được, phù hợp dữ liệu hiện tại và là nền
tảng để nâng cấp Recommendation System sau này.

## Database

Hai bảng mới:

- `wishlists`: danh sách yêu thích, unique theo người dùng và sản phẩm.
- `product_view_histories`: lần xem gần nhất và tổng số lượt xem.

Với database đã tồn tại, chạy một lần:

```sql
SOURCE database/phase-17-wishlist-recommendation-migration.sql;
```

Hoặc mở file migration trong MySQL Workbench và thực thi toàn bộ. Schema đầy đủ
`database/vegetable_shop.sql` cũng đã được cập nhật.

## API và route

| Method | Route | Chức năng |
| --- | --- | --- |
| GET | `/wishlist` | Xem sản phẩm yêu thích |
| POST | `/api/wishlist/items?productId={id}` | Thêm yêu thích |
| DELETE | `/api/wishlist/items/{id}` | Bỏ yêu thích |
| POST | `/api/wishlist/items/{id}/move-to-cart` | Chuyển sang giỏ |

## Kiểm thử

Test mới bao phủ tính duy nhất, quyền sở hữu, lọc sản phẩm ngừng bán, tăng số lần
xem, xếp hạng gợi ý, API chuyển sang giỏ, CSRF và bảo vệ route.

```text
Tests run: 140, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Tệp chính

- `entity/Wishlist.java`, `entity/ProductViewHistory.java`
- `service/WishlistService.java`, `service/ProductViewHistoryService.java`
- `service/RecommendationService.java`
- `controller/WishlistController.java`, `controller/WishlistApiController.java`
- `templates/wishlist.html`, `static/js/wishlist.js`
- `database/phase-17-wishlist-recommendation-migration.sql`
