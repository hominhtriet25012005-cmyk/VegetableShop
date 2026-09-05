# Giai đoạn 7 - Checkout và đơn hàng

## Mục tiêu

Hoàn thiện luồng từ giỏ hàng đến đơn hàng: nhập thông tin nhận hàng, thanh toán
COD, tạo `Order`/`OrderDetail`, trừ tồn kho, xóa giỏ và xem lại lịch sử đơn.

## Luồng xử lý

```text
POST /checkout + CSRF + email đăng nhập
        ↓
CheckoutController + Bean Validation
        ↓
OrderService.placeOrder() @Transactional
        ├─ nạp User và Cart thuộc tài khoản
        ├─ khóa từng Product theo id (PESSIMISTIC_WRITE)
        ├─ kiểm tra status và tồn kho lần cuối
        ├─ chụp tên, giá, số lượng vào OrderDetail
        ├─ tính tổng hoàn toàn ở backend
        ├─ trừ Product.quantity
        ├─ lưu Order + OrderDetail
        └─ xóa CartItem
```

Nếu bất kỳ bước nào lỗi, transaction rollback toàn bộ: không tạo đơn dở dang,
không trừ một phần tồn kho và không làm mất giỏ hàng.

## Quy tắc nghiệp vụ và bảo mật

- Chỉ USER/ADMIN đã đăng nhập được mở Checkout và đơn hàng.
- Email chủ tài khoản lấy từ `Authentication`; form không nhận `userId`.
- Tổng tiền, đơn giá và tồn kho không được tin từ trình duyệt.
- `OrderDetail.productName` và `price` là snapshot tại lúc đặt hàng.
- Khóa bi quan Product trong transaction giúp các lượt mua đồng thời không bán
  vượt tồn kho; Product được khóa theo thứ tự id để giảm nguy cơ deadlock.
- Giai đoạn 7 chỉ chấp nhận `COD`; giá trị `VNPAY` giả mạo bị từ chối.
- Trạng thái ban đầu là `PENDING` và `UNPAID`.
- Chi tiết đơn luôn truy vấn bằng cả `orderId` và email người đăng nhập.
- Các POST dùng CSRF của Spring Security.

## Endpoint

| Method | URL | Chức năng |
| --- | --- | --- |
| GET | `/checkout` | Form nhận hàng và tóm tắt giỏ |
| POST | `/checkout` | Tạo đơn trong một transaction |
| GET | `/my-orders` | Lịch sử đơn của tài khoản hiện tại |
| GET | `/orders/{id}` | Chi tiết đơn thuộc tài khoản hiện tại |

## File chính

- `dto/CheckoutRequest.java`
- `entity/Order.java`, `OrderDetail.java` và các enum trạng thái
- `repository/OrderRepository.java`
- `service/OrderService.java`
- `controller/CheckoutController.java`
- `templates/checkout.html`, `my-orders.html`, `order-detail.html`
- Các test Checkout, Order, Security và Thymeleaf

## Kiểm tra thủ công với MySQL

1. Chạy profile `mysql` rồi đăng nhập tài khoản USER.
2. Thêm ít nhất một sản phẩm vào `/cart`.
3. Chọn **Tiến hành thanh toán**, nhập thông tin và đặt COD.
4. Kết quả đúng: chuyển đến `/orders/{id}?success`, Cart về 0, tồn kho giảm.
5. Mở `/my-orders` để xem lại đơn và chi tiết.
6. Thử đặt số lượng vượt tồn kho: không tạo đơn và giỏ vẫn còn nguyên.

```sql
SELECT id, order_code, user_id, total_amount, payment_method,
       payment_status, status, created_at
FROM orders ORDER BY id DESC;

SELECT order_id, product_id, product_name, price, quantity, subtotal
FROM order_details ORDER BY order_id DESC, id;

SELECT id, name, quantity FROM products ORDER BY id;
SELECT * FROM cart_items ORDER BY cart_id, id;
```

## Kiểm thử tự động

```powershell
.\mvnw.cmd clean package
```

```text
Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Phạm vi chưa làm

- Quản trị trạng thái đơn hàng thuộc giai đoạn 8.
- Thanh toán online/VNPay, webhook, email và đơn vị vận chuyển chưa triển khai.
