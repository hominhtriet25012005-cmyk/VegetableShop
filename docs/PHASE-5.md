# Giai đoạn 5 - Authentication và Authorization

## Mục tiêu

Giai đoạn này triển khai đăng ký, đăng nhập, đăng xuất và phân quyền bằng Spring
Security. Tài khoản được lưu trong bảng `users` của MySQL và mật khẩu chỉ được
lưu dưới dạng BCrypt.

## Kiến trúc

### Đăng ký

```text
Trình duyệt
    ↓ POST /register + CSRF
AuthController
    ↓ @Valid RegisterRequest
UserService
    ↓ kiểm tra email + BCrypt + ép role USER
UserRepository
    ↓
MySQL users
```

Controller chỉ nhận form và quyết định view. Việc chuẩn hóa email, kiểm tra trùng,
mã hóa mật khẩu và gán role nằm trong `UserService`.

### Đăng nhập

```text
Trình duyệt
    ↓ POST /login + email/password + CSRF
Spring Security Filter Chain
    ↓
DatabaseUserDetailsService
    ↓
UserRepository.findByEmailIgnoreCase(...)
    ↓
So sánh password bằng BCrypt
    ↓
Kiểm tra status và ROLE_USER/ROLE_ADMIN
    ↓
Tạo phiên đăng nhập
```

## Quy tắc bảo mật

- `GET /login` và `GET/POST /register` là public.
- `/shop`, `/product/**` và static files là public.
- `/cart`, `/checkout`, `/my-orders/**` yêu cầu đăng nhập.
- `/admin/**` chỉ dành cho `ROLE_ADMIN`.
- Đăng xuất dùng `POST /logout`; GET không được dùng để thay đổi trạng thái.
- CSRF giữ mặc định và Thymeleaf tự chèn token vào các form `th:action`.
- Người dùng đăng ký không được gửi role; backend luôn gán `Role.USER`.
- User có `status=false` không thể đăng nhập.
- Mật khẩu đăng ký dài 8-72 ký tự vì BCrypt chỉ xử lý an toàn tối đa 72 byte.

## File tạo mới

### Java

- `dto/RegisterRequest.java`: DTO và Bean Validation cho form đăng ký.
- `exception/DuplicateEmailException.java`: lỗi nghiệp vụ email trùng.
- `service/DatabaseUserDetailsService.java`: nạp tài khoản MySQL cho Security.
- `config/AdminAccountInitializer.java`: tạo Admin ban đầu từ biến môi trường.
- `controller/AuthController.java`: GET/POST Register và GET Login.
- `controller/AdminController.java`: Dashboard kiểm chứng quyền Admin và trang 403.

### Thymeleaf

- `templates/login.html`
- `templates/register.html`
- `templates/fragments/account.html`
- `templates/admin/dashboard.html`
- `templates/error/403.html`

### Test

- `service/UserServiceTests.java`
- `service/DatabaseUserDetailsServiceTests.java`
- `dto/RegisterRequestTests.java`
- `controller/SecurityIntegrationTests.java`

## File sửa đổi

- `config/SecurityConfig.java`
- `service/UserService.java`
- `controller/TemplateModeController.java`
- `application-mysql.properties`
- Các template public có biểu tượng tài khoản.
- `README.md`

## Tạo tài khoản Admin

Không đặt sẵn mật khẩu Admin trong Git hoặc file SQL. Thêm các biến môi trường
sau vào IntelliJ Run Configuration:

```text
APP_ADMIN_NAME=Quản trị viên
APP_ADMIN_EMAIL=admin@vegetableshop.local
APP_ADMIN_PASSWORD=<MAT_KHAU_ADMIN_MANH_CUA_BAN>
```

Khi chạy profile `mysql`, `AdminAccountInitializer` chỉ tạo tài khoản nếu email
chưa tồn tại. Nó không tự nâng quyền một USER đã tồn tại, tránh cấp quyền ngoài ý
muốn. Có thể xóa các biến sau lần tạo thành công.

## Chạy

Trong IntelliJ, giữ Active profiles là `mysql`, cấu hình `DB_USERNAME` và
`DB_PASSWORD`, sau đó Stop và Run lại `VegetableShopApplication`.

Hoặc PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "mat-khau-mysql"
$env:APP_ADMIN_EMAIL = "admin@vegetableshop.local"
$env:APP_ADMIN_PASSWORD = "<MAT_KHAU_ADMIN_MANH_CUA_BAN>"
.\mvnw.cmd spring-boot:run
```

Website dùng cổng `8081`.

## Kiểm tra thủ công

1. Mở `http://localhost:8081/register` và tạo một tài khoản mới.
2. Trong MySQL chạy `SELECT email, password, role, status FROM users;`.
3. Xác nhận password bắt đầu bằng `$2a$`, `$2b$` hoặc `$2y$`, không giống mật
   khẩu đã nhập, và tài khoản có role `USER`.
4. Đăng nhập tại `http://localhost:8081/login`.
5. Mở `/cart`: tài khoản đã đăng nhập truy cập được giao diện Cart.
6. Mở `/admin` bằng USER: kết quả đúng là trang 403.
7. Đăng xuất, đăng nhập bằng Admin được tạo từ biến môi trường.
8. Mở `/admin`: kết quả đúng là Dashboard Admin.
9. Bấm Đăng xuất; kết quả đúng là quay lại `/login?logout`.

## Kiểm thử tự động

```powershell
.\mvnw.cmd clean package
```

Kết quả:

```text
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Ngoài unit test Service/DTO, `SecurityIntegrationTests` kiểm tra URL public,
chuyển hướng người chưa đăng nhập, CSRF, logout và quyền USER/ADMIN.

## Phạm vi chưa làm

- Cart/CartItem thuộc Giai đoạn 6.
- Checkout/Order thuộc Giai đoạn 7.
- Dashboard quản trị đầy đủ và CRUD thuộc Giai đoạn 8.
