# Vegetable Shop

Website thương mại điện tử bán rau củ và thực phẩm sạch. Dự án hỗ trợ tìm kiếm
sản phẩm, giỏ hàng, đặt hàng, quản lý tài khoản và khu vực quản trị cửa hàng.

## Công nghệ sử dụng

- Java 23, Spring Boot 4.1.1 và Maven Wrapper
- Spring MVC, Spring Security, Spring Data JPA và Hibernate
- Thymeleaf, HTML, CSS, JavaScript, Bootstrap 5 và AJAX
- MySQL 8
- Spring Mail, Google OAuth2 và OpenAI API (tùy chọn)
- JUnit, Spring Boot Test và Mockito

## Cấu trúc thư mục dự án

```text
VegetableShop/
├── database/                         # Script khởi tạo và nâng cấp MySQL
├── docs/                             # Tài liệu kỹ thuật của dự án
├── src/
│   ├── main/
│   │   ├── java/com/vegetableshop/
│   │   │   ├── config/               # Cấu hình bảo mật và ứng dụng
│   │   │   ├── controller/           # Tiếp nhận và xử lý HTTP request
│   │   │   ├── dto/                  # Dữ liệu request/response
│   │   │   ├── entity/               # Các JPA Entity và Enum
│   │   │   ├── exception/            # Xử lý ngoại lệ
│   │   │   ├── repository/           # Truy cập dữ liệu bằng Spring Data JPA
│   │   │   └── service/              # Nghiệp vụ của hệ thống
│   │   └── resources/
│   │       ├── static/               # CSS, JavaScript và hình ảnh
│   │       ├── templates/            # Giao diện Thymeleaf và Admin
│   │       └── application-*.properties
│   └── test/                         # Unit test và integration test
├── pom.xml                           # Dependency và cấu hình Maven
├── mvnw                              # Maven Wrapper cho Linux/macOS
└── mvnw.cmd                          # Maven Wrapper cho Windows
```

## Hướng dẫn khởi chạy dự án

Yêu cầu: JDK 23 và MySQL 8. Project đã có Maven Wrapper nên không cần cài Maven.

### 1. Tải project

```powershell
git clone https://github.com/hominhtriet25012005-cmyk/VegetableShop.git
Set-Location VegetableShop
```

### 2. Khởi tạo database

Mở MySQL Workbench và chạy file:

```text
database/vegetable_shop.sql
```

Script sẽ tạo database `vegetable_shop` cùng cấu trúc và dữ liệu mẫu cần thiết.

### 3. Cấu hình biến môi trường

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/vegetable_shop?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<MAT_KHAU_MYSQL>"

$env:APP_ADMIN_NAME = "Quản trị viên"
$env:APP_ADMIN_EMAIL = "<EMAIL_ADMIN>"
$env:APP_ADMIN_PASSWORD = "<MAT_KHAU_ADMIN>"
```

Không ghi mật khẩu hoặc API key thật vào source code và không commit chúng lên
GitHub.

### 4. Chạy ứng dụng

```powershell
.\mvnw.cmd spring-boot:run
```

Truy cập:

- Website: `http://localhost:8081`
- Trang quản trị: `http://localhost:8081/admin`

Nếu chỉ muốn xem giao diện mà chưa cấu hình MySQL:

```powershell
$env:SPRING_PROFILES_ACTIVE = "template"
.\mvnw.cmd spring-boot:run
```
