# Giai đoạn 6 - Giỏ hàng

## Mục tiêu

Xây dựng giỏ hàng gắn với tài khoản đã đăng nhập, gồm thêm sản phẩm, cộng dồn số
lượng, cập nhật, xóa và tính tổng tiền. Mọi dữ liệu quan trọng được kiểm tra lại
ở backend.

## Kiến trúc

```text
Trình duyệt
    ↓ POST/GET /cart/** + CSRF + Authentication
CartController
    ↓ email của Principal
CartService (@Transactional)
    ↓ kiểm tra chủ sở hữu, Product, status và tồn kho
CartRepository + CartItemRepository + ProductRepository + UserRepository
    ↓
Entity Cart + CartItem + Product + User
    ↓
MySQL carts + cart_items + products + users
```

## Quan hệ dữ liệu

```text
User 1 ─── 0..1 Cart
Cart 1 ─── n CartItem
Product 1 ─── n CartItem
```

- `carts.user_id` là UNIQUE nên mỗi User chỉ có tối đa một Cart.
- `(cart_id, product_id)` là UNIQUE nên một Product chỉ xuất hiện một lần trong
  Cart; khi thêm lại, Service cộng số lượng vào item cũ.
- Xóa Cart sẽ xóa CartItem; xóa Product đang được tham chiếu vẫn bị database
  chặn bằng `ON DELETE RESTRICT`.

## Quy tắc nghiệp vụ

- Người chưa đăng nhập không truy cập được `/cart/**`.
- Controller lấy email từ `Authentication`, không nhận userId từ form.
- Cập nhật/xóa CartItem luôn truy vấn kèm email chủ sở hữu.
- Số lượng phải lớn hơn 0 và không được vượt tồn kho hiện tại.
- Product phải đang hoạt động và Category cũng phải hoạt động khi thêm/cập nhật.
- Thêm Product đã có trong Cart sẽ cộng dồn số lượng.
- Tổng tiền là `Product.price × CartItem.quantity`, tính lại trong `CartService`.
- Không nhận giá, thành tiền hoặc tổng tiền từ trình duyệt.
- Các form thêm/cập nhật/xóa dùng POST và CSRF.
- Tồn kho chưa bị trừ khi thêm vào giỏ; việc trừ tồn kho thuộc Checkout Giai đoạn 7.

## File tạo mới

### Java

- `entity/Cart.java`
- `entity/CartItem.java`
- `repository/CartRepository.java`
- `repository/CartItemRepository.java`
- `service/CartService.java`
- `controller/CartController.java`
- `controller/CartModelAdvice.java`
- `exception/CartOperationException.java`

### Thymeleaf

- `templates/fragments/cart-link.html`

### Test

- `service/CartServiceTests.java`
- `controller/CartControllerTests.java`
- `controller/CartTemplateRenderingTests.java`

## File sửa đổi

- `config/SecurityConfig.java`: bảo vệ toàn bộ `/cart/**`.
- `controller/HomeController.java`: bỏ mapping Cart tĩnh.
- `controller/TemplateModeController.java`: dữ liệu Cart rỗng cho profile template.
- `templates/cart.html`: thay dữ liệu hard-code bằng Thymeleaf động.
- `templates/shop.html`: nút thêm nhanh một sản phẩm.
- `templates/shop-detail.html`: chọn số lượng và thêm vào Cart.
- Các template chính: badge số lượng Cart động.
- `README.md` và comment trong database script.

## Endpoint

| Method | URL | Chức năng |
| --- | --- | --- |
| GET | `/cart` | Hiển thị Cart của tài khoản hiện tại |
| POST | `/cart/items` | Thêm Product hoặc cộng dồn số lượng |
| POST | `/cart/items/{id}/quantity` | Cập nhật số lượng của item thuộc User |
| POST | `/cart/items/{id}/delete` | Xóa item thuộc User |

## Kiểm tra thủ công

1. Chạy profile `mysql` và đăng nhập USER.
2. Mở `/shop` hoặc `/product/1` và thêm sản phẩm.
3. Mở `/cart`; kiểm tra đúng tên, giá, số lượng và tổng tiền.
4. Thêm cùng Product lần nữa; Cart phải cộng số lượng, không tạo dòng trùng.
5. Cập nhật số lượng hợp lệ.
6. Nhập số lượng lớn hơn tồn kho; backend phải từ chối và hiện thông báo.
7. Xóa item; badge và tổng tiền phải cập nhật.
8. Đăng xuất và mở `/cart`; kết quả đúng là chuyển đến `/login`.

Kiểm tra database:

```sql
SELECT c.id AS cart_id, u.email
FROM carts c
JOIN users u ON u.id = c.user_id;

SELECT ci.id, ci.cart_id, ci.product_id, ci.quantity
FROM cart_items ci
ORDER BY ci.cart_id, ci.id;
```

## Kiểm thử tự động

```powershell
.\mvnw.cmd clean package
```

Kết quả dự kiến sau khi đồng bộ project chính:

```text
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Phạm vi chưa làm

- Checkout, tạo Order, trừ tồn kho và xóa CartItem thuộc Giai đoạn 7.
- Badge Cart trên trang chủ vẫn chỉ phản ánh Cart thật; các Product hard-code còn
  lại của template trang chủ chưa được chuyển thành Product MySQL.
