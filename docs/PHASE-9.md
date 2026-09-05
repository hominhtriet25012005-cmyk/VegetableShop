# Giai đoạn 9 - Hoàn thiện trang chi tiết sản phẩm

## Mục tiêu

Giai đoạn 9 biến trang `/product/{id}` thành trang chi tiết có dữ liệu nghiệp vụ:

- Hiển thị thông tin, tồn kho, danh mục và nhà cung cấp.
- Gợi ý hàng cùng loại và hàng cùng nhà cung cấp.
- Lưu tối đa 6 sản phẩm đã xem gần nhất trong HTTP session.
- Hiển thị điểm trung bình, số lượt đánh giá và nhận xét khách hàng.
- Chỉ cho phép tài khoản đã mua sản phẩm trong đơn `COMPLETED` đánh giá.
- Mỗi tài khoản có một đánh giá trên mỗi sản phẩm; gửi lại sẽ cập nhật đánh giá cũ.
- Tiếp tục kiểm tra số lượng giỏ hàng ở backend, không tin dữ liệu từ HTML.

## Mô hình dữ liệu mới

### suppliers

`Supplier` lưu tên, điện thoại, email, địa chỉ và trạng thái. `products.supplier_id`
được để nullable để những sản phẩm cũ chưa khai báo nhà cung cấp vẫn chạy được.
Khi nhà cung cấp bị xóa ở mức database, khóa ngoại đặt `ON DELETE SET NULL`.

### reviews

Bảng `reviews` đã có trong schema dự kiến và nay được kết nối với Entity/Repository/
Service. Ràng buộc duy nhất `(user_id, product_id)` ngăn đánh giá trùng. Rating chỉ
nhận giá trị từ 1 đến 5 và comment tối đa 1000 ký tự.

## Quy tắc đánh giá đã mua hàng

```text
POST /product/{id}/reviews
        ↓
Spring Security: USER hoặc ADMIN + CSRF
        ↓
Bean Validation: rating 1..5, comment <= 1000
        ↓
ReviewService
        ↓
OrderRepository kiểm tra OrderStatus.COMPLETED
        ↓
Tạo mới hoặc cập nhật Review duy nhất của User/Product
```

Việc chỉ kiểm tra “đã từng đặt hàng” là chưa đủ. Đơn phải ở trạng thái `COMPLETED`
để tránh đánh giá giả từ đơn chờ xử lý hoặc đã hủy.

## Hàng đã xem

Danh sách ID sản phẩm được lưu trong session, không cần tạo bảng và không thu thập
lịch sử lâu dài. Khi xem lại một sản phẩm, ID được đưa lên đầu và không bị lặp.
Controller chỉ tải những sản phẩm/danh mục còn hoạt động.

## Nâng cấp MySQL hiện có

Với profile `mysql`, `spring.jpa.hibernate.ddl-auto=update` sẽ tạo bảng `suppliers`
và thêm `products.supplier_id` khi ứng dụng khởi động. Sau đó `data-mysql.sql` tạo
3 nhà cung cấp mẫu và gắn 5 sản phẩm mẫu. Không cần xóa database cũ.

Nếu tạo database mới, chạy toàn bộ `database/vegetable_shop.sql` như trước.

Kiểm tra nhanh sau khi ứng dụng đã khởi động:

```sql
USE vegetable_shop;
SELECT id, name, status FROM suppliers;
SELECT id, name, supplier_id FROM products ORDER BY id;
SELECT user_id, product_id, rating, comment FROM reviews;
```

## Kiểm thử

```powershell
.\mvnw.cmd clean package
```

Kết quả tại thời điểm hoàn tất Giai đoạn 9:

```text
Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Kiểm thử bao phủ Service đánh giá, điều kiện đã mua hàng, lịch sử xem, render
Thymeleaf, Security/CSRF và toàn bộ chức năng của các giai đoạn trước.
