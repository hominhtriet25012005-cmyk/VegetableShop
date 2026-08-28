# Vegetable Shop

Website thương mại điện tử bán rau củ và thực phẩm sạch, xây dựng bằng Java,
Spring Boot, Thymeleaf và MySQL. Dự án mô phỏng quy trình từ tìm kiếm sản phẩm,
giỏ hàng, đặt hàng đến quản trị cửa hàng.

![Vegetable Shop](src/main/resources/static/img/hero-img.jpg)

> Project cá nhân phục vụ đồ án và hồ sơ ứng tuyển thực tập Java Backend.

## Điểm nổi bật

- Kiến trúc phân lớp Controller, Service, Repository và Entity.
- Sản phẩm động từ MySQL; tìm kiếm, lọc, sắp xếp và phân trang.
- Chi tiết sản phẩm, hàng cùng loại/nhà cung cấp và lịch sử đã xem.
- Đăng ký, đăng nhập và phân quyền `USER`/`ADMIN` bằng Spring Security.
- Giỏ hàng AJAX, Checkout COD và lịch sử đơn hàng.
- Review chỉ dành cho khách đã mua sản phẩm trong đơn hoàn tất.
- Quản trị sản phẩm, danh mục, đơn hàng và tài khoản người dùng.
- BCrypt, CSRF, kiểm tra quyền sở hữu, tồn kho và tổng tiền tại backend.
- Giao diện Responsive, menu tiếng Việt dùng chung và SweetAlert2.
- Có profile chỉ hiển thị giao diện khi chưa cấu hình MySQL.

## Chức năng đã hoàn thành

| Khu vực | Chức năng |
| --- | --- |
| Trang chủ | Sản phẩm từ database và lọc nhanh theo danh mục |
| Sản phẩm | Tìm kiếm, lọc danh mục/giá, sắp xếp và phân trang |
| Chi tiết | Thông tin hàng hóa, đánh giá, hàng liên quan và đã xem |
| Thành viên | Đăng ký, đăng nhập, đăng xuất và BCrypt |
| Giỏ hàng | Thêm, cập nhật, xóa bằng AJAX và kiểm tra tồn kho |
| Đặt hàng | Checkout COD, trừ tồn kho, lịch sử và chi tiết đơn |
| Quản trị | Dashboard, sản phẩm, danh mục, đơn hàng và người dùng |
| Bảo mật | Spring Security, CSRF, phân quyền và kiểm tra chủ sở hữu |

Trạng thái hiện tại: hoàn thành **Giai đoạn 1–10**. Email, thanh toán trực tuyến,
thống kê nâng cao và phân quyền chi tiết thuộc lộ trình tiếp theo.

## Công nghệ

- Java 23, Spring Boot 4.1.1 và Maven Wrapper
- Spring MVC, Thymeleaf, Spring Data JPA / Hibernate
- Spring Security và Bean Validation
- MySQL 8
- Bootstrap 5, HTML, CSS, JavaScript, jQuery và SweetAlert2
- JUnit, Spring Boot Test và Mockito

## Kiến trúc

```text
Browser
   |
Controller  ->  Service  ->  Repository  ->  MySQL
   |              |
Thymeleaf      Nghiệp vụ
```

```text
VegetableShop/
├── database/                       # Script schema MySQL
├── docs/                           # Tài liệu từng giai đoạn
├── src/main/java/com/vegetableshop/
│   ├── config/                     # Security và khởi tạo Admin
│   ├── controller/                 # Public, Cart, Checkout, Admin
│   ├── dto/                        # Request/Response và Validation
│   ├── entity/                     # JPA Entity và Enum
│   ├── exception/                  # Xử lý lỗi tập trung
│   ├── repository/                 # Spring Data JPA
│   └── service/                    # Nghiệp vụ ứng dụng
├── src/main/resources/
│   ├── static/                     # CSS, JavaScript và hình ảnh
│   ├── templates/                  # Thymeleaf public và Admin
│   └── application-*.properties
├── src/test/                       # Unit và integration tests
└── pom.xml
```

## Chạy nhanh không cần database

Yêu cầu JDK 23. Project đã có Maven Wrapper nên không cần cài Maven.

```powershell
git clone <URL_REPOSITORY_CUA_BAN>
cd VegetableShop
.\mvnw.cmd spring-boot:run
```

Mở `http://localhost:8081`. Profile mặc định `template` chỉ dùng để xem giao
diện; chức năng dữ liệu thật cần profile `mysql`.

## Chạy với MySQL

### 1. Tạo database

Chạy [`database/vegetable_shop.sql`](database/vegetable_shop.sql). Script sử
dụng database `vegetable_shop`, có dữ liệu mẫu nhưng không có tài khoản thật.

### 2. Cấu hình biến môi trường

Không ghi mật khẩu trực tiếp vào file cấu hình:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/vegetable_shop?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<MAT_KHAU_MYSQL_CUA_BAN>"
```

Tùy chọn tạo Admin ban đầu:

```powershell
$env:APP_ADMIN_NAME = "Quản trị viên"
$env:APP_ADMIN_EMAIL = "<EMAIL_ADMIN_CUA_BAN>"
$env:APP_ADMIN_PASSWORD = "<MAT_KHAU_ADMIN_MANH_CUA_BAN>"
```

Admin chỉ được tạo khi email chưa tồn tại. Không commit giá trị thật lên GitHub.

### 3. Khởi động

```powershell
.\mvnw.cmd spring-boot:run
```

Đổi cổng nếu `8081` đang bận:

```powershell
$env:SERVER_PORT = "8082"
.\mvnw.cmd spring-boot:run
```

## Route chính

| Đường dẫn | Mô tả | Quyền |
| --- | --- | --- |
| `/` | Trang chủ | Công khai |
| `/shop` | Danh sách sản phẩm | Công khai |
| `/product/{id}` | Chi tiết sản phẩm | Công khai |
| `/news` | Trang tin tức dự phòng | Công khai |
| `/register`, `/login` | Thành viên | Công khai |
| `/cart`, `/checkout` | Giỏ hàng và đặt hàng | Đã đăng nhập |
| `/my-orders` | Lịch sử đơn hàng | Đã đăng nhập |
| `/admin` | Dashboard | `ADMIN` |
| `/admin/products` | Quản lý sản phẩm | `ADMIN` |
| `/admin/categories` | Quản lý danh mục | `ADMIN` |
| `/admin/orders` | Quản lý đơn hàng | `ADMIN` |
| `/admin/users` | Quản lý tài khoản | `ADMIN` |

## Kiểm thử

```powershell
.\mvnw.cmd clean test
```

Kết quả đã xác minh:

```text
Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test bao phủ Controller, Service, DTO Validation, Security, Thymeleaf và nghiệp
vụ giỏ hàng/đơn hàng.

## Bảo mật cấu hình

- Mật khẩu MySQL và Admin được đọc từ biến môi trường.
- `.idea`, `target`, `.env` và file private key không được đưa vào Git.
- CSRF được giữ cho các thao tác thay đổi dữ liệu.
- Tổng tiền và tồn kho luôn được kiểm tra lại tại backend.
- Không dùng tài khoản hoặc mật khẩu ví dụ làm thông tin đăng nhập thật.

Nếu một secret từng được commit, cần thu hồi hoặc thay đổi secret đó; chỉ xóa
khỏi file là chưa đủ.

## Lộ trình 1–16

| Giai đoạn | Nội dung | Trạng thái |
| ---: | --- | --- |
| 1 | Phân tích template và thiết kế dự án | Hoàn thành |
| 2 | Khởi tạo Spring Boot và chuyển giao diện | Hoàn thành |
| 3 | MySQL và sản phẩm động | Hoàn thành |
| 4 | Tìm kiếm, lọc, sắp xếp và phân trang | Hoàn thành |
| 5 | Thành viên và bảo mật cơ bản | Hoàn thành |
| 6 | Giỏ hàng | Hoàn thành |
| 7 | Checkout và quản lý đơn hàng | Hoàn thành |
| 8 | Khu vực quản trị cơ bản | Hoàn thành |
| 9 | Chi tiết, review và sản phẩm liên quan | Hoàn thành |
| 10 | AJAX Cart và hoàn thiện trải nghiệm | Hoàn thành |
| 11 | Quản lý tài khoản và Google Login | Dự kiến |
| 12 | Email giao dịch | Dự kiến |
| 13 | Thanh toán trực tuyến VNPay | Dự kiến |
| 14 | Tiện ích giao diện nâng cao | Dự kiến |
| 15 | Thống kê kinh doanh | Dự kiến |
| 16 | Phân quyền nâng cao | Dự kiến |

Tài liệu kỹ thuật nằm trong [`docs`](docs/). Bản README phát triển đầy đủ trước
khi tối ưu cho GitHub được giữ nguyên tại
[`docs/README-DEVELOPMENT-ARCHIVE.md`](docs/README-DEVELOPMENT-ARCHIVE.md).

## Định hướng tiếp theo

1. Quản lý hồ sơ và quên/đổi mật khẩu.
2. Email kích hoạt tài khoản và xác nhận đơn hàng.
3. Tích hợp một cổng thanh toán, ưu tiên VNPay.
4. Chart.js và báo cáo doanh thu.
5. Phân quyền chi tiết cho `MANAGER` và `STAFF`.

## Bản quyền giao diện

Giao diện public được chuyển đổi và tùy chỉnh từ template HTML Codex; giao diện
quản trị được tùy chỉnh từ adminHMD. Tài nguyên bên thứ ba giữ nguyên giấy phép
tương ứng trong project.

## Tác giả

Dự án cá nhân phục vụ học tập, đồ án và ứng tuyển thực tập Java Backend.
