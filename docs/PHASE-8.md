# Giai đoạn 8 - Khu vực quản trị

## Mục tiêu

Hoàn thiện khu vực `/admin/**` dành riêng cho `ROLE_ADMIN`, sử dụng giao diện
adminHMD Bootstrap 5 và dữ liệu thật từ MySQL.

Layout đã được chuẩn hóa với nhãn `Admin`, nút `Website` để quay lại cửa hàng và
footer chỉ hiển thị thông tin hệ thống, không hiển thị tên giai đoạn/tác giả.

## Chức năng

### Dashboard

- Tổng Product, Category, User và Order.
- Doanh thu chỉ tính các Order `COMPLETED`.
- Năm đơn mới nhất.
- Năm sản phẩm đang bán có tồn kho thấp nhất (tối đa 10 sản phẩm).

### Product Management

- Danh sách, tìm theo tên và phân trang.
- Lọc kết hợp theo Category và trạng thái đang bán/đã ẩn; giữ bộ lọc khi phân trang.
- Thêm, sửa, thay đổi giá, tồn kho, Category và đường dẫn ảnh.
- Bean Validation từ chối `price < 0`, `quantity < 0` và thiếu Category.
- Ẩn/kích hoạt thay vì xóa vật lý để bảo toàn Cart và lịch sử Order.

### Category Management

- Danh sách, thêm, sửa và ẩn/kích hoạt.
- Không cho trùng tên không phân biệt hoa thường.
- Không xóa vật lý Category đang được Product tham chiếu.

### Order Management

- Tìm theo mã đơn, tên hoặc email khách hàng.
- Lọc theo trạng thái, phân trang và xem chi tiết.
- Chỉ cho phép workflow:

```text
PENDING → CONFIRMED → SHIPPING → COMPLETED
    └──────────────────────────→ CANCELLED (chỉ từ PENDING)
```

- Hủy Order PENDING sẽ khóa từng Product và hoàn tồn kho trong transaction.
- Hoàn tất Order COD tự chuyển `payment_status = PAID` và ghi `paid_at`.
- Không cho nhảy cóc, quay ngược hoặc sửa Order đã kết thúc.

### User Management

- Danh sách, tìm theo họ tên/email và phân trang.
- Khóa/mở khóa tài khoản USER.
- Không hiển thị password.
- Không cho khóa tài khoản có `ROLE_ADMIN`, kể cả Admin đang thao tác.

## Kiến trúc

```text
Browser /admin/** + ROLE_ADMIN + CSRF
        ↓
AdminController
        ↓
AdminService (@Transactional)
        ↓
ProductRepository / CategoryRepository / OrderRepository / UserRepository
        ↓
Entity
        ↓
MySQL vegetable_shop
        ↓
Thymeleaf adminHMD
```

Controller chỉ nhận request, BindingResult và điều hướng. Kiểm tra workflow,
hoàn kho, khóa User, mapping DTO và thống kê nằm trong `AdminService`.

## Endpoint

| Method | URL | Chức năng |
| --- | --- | --- |
| GET | `/admin` | Dashboard |
| GET | `/admin/products` | Danh sách/tìm Product |
| GET/POST | `/admin/products/new`, `/admin/products` | Form/tạo Product |
| GET/POST | `/admin/products/{id}/edit`, `/admin/products/{id}` | Form/cập nhật Product |
| POST | `/admin/products/{id}/toggle` | Ẩn/kích hoạt Product |
| GET | `/admin/categories` | Danh sách Category |
| GET/POST | `/admin/categories/new`, `/admin/categories` | Form/tạo Category |
| GET/POST | `/admin/categories/{id}/edit`, `/admin/categories/{id}` | Form/cập nhật Category |
| POST | `/admin/categories/{id}/toggle` | Ẩn/kích hoạt Category |
| GET | `/admin/orders` | Tìm/lọc Order |
| GET | `/admin/orders/{id}` | Chi tiết Order |
| POST | `/admin/orders/{id}/status` | Chuyển trạng thái hợp lệ |
| GET | `/admin/users` | Tìm User |
| POST | `/admin/users/{id}/toggle` | Khóa/mở khóa USER |

## Giao diện và giấy phép

Nguồn giao diện: `adminhmd-1.0.0.zip`, tác giả Md. Hasan Mahmud, phân phối bởi
ThemeWagon theo giấy phép MIT. CSS, JavaScript và Bootstrap Icons được đặt tại
`static/admin`; credit được giữ trong footer và
`THIRD-PARTY-LICENSES/adminhmd/LICENSE.txt`.

## Kiểm tra thủ công

1. Chạy profile `mysql`, đăng nhập tài khoản `ROLE_ADMIN`.
2. Mở `http://localhost:8081/admin` và kiểm tra số liệu.
3. Thêm/sửa/ẩn một Product; kiểm tra `/shop` phản ánh trạng thái.
4. Thêm/sửa/ẩn Category; Product thuộc Category ẩn không xuất hiện ở Shop.
5. Mở một Order PENDING và chuyển lần lượt trạng thái hợp lệ.
6. Tạo Order PENDING khác, ghi nhớ tồn kho rồi hủy; số lượng phải được hoàn lại.
7. Khóa USER; tài khoản đó không đăng nhập được. Admin không có nút tự khóa.
8. Đăng nhập USER và mở `/admin`; kết quả đúng là HTTP 403.

## Kiểm thử tự động

```powershell
.\mvnw.cmd clean package
```

Kết quả dự kiến sau khi hoàn tất đồng bộ:

```text
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Phạm vi chưa làm

- Upload file ảnh thật; Giai đoạn 8 dùng đường dẫn ảnh để giữ project đơn giản.
- Review/Rating và email xác nhận thuộc Giai đoạn 9.
- Báo cáo biểu đồ doanh thu theo tháng có thể bổ sung sau khi dữ liệu đủ lớn.
