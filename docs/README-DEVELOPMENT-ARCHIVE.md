# Vegetable Shop

Website thương mại điện tử bán rau củ và thực phẩm sạch, xây dựng bằng Java,
Spring Boot, Spring MVC, Spring Data JPA, Hibernate, Spring Security, Thymeleaf
và MySQL.

## Trạng thái hiện tại

Đến Giai đoạn 10 đã hoàn thành:

- Thiết kế database MySQL `vegetable_shop` với 8 bảng cho toàn bộ lộ trình.
- Tạo Entity `User`, `Category`, `Product` và enum `Role`.
- Tạo Repository và Service tương ứng, không đặt nghiệp vụ trong Controller.
- Tạo `ProductController` để lấy Category/Product thật từ MySQL.
- Chuyển danh sách sản phẩm và danh mục trong `shop.html` sang Thymeleaf động.
- Thêm dữ liệu mẫu idempotent: 3 danh mục và 5 sản phẩm.
- Giữ profile `template` để vẫn kiểm tra được giao diện khi chưa cấu hình mật
  khẩu MySQL.
- Thêm tìm kiếm Product theo tên.
- Lọc theo Category và khoảng giá.
- Sắp xếp theo mới nhất, giá tăng/giảm và tên A-Z.
- Phân trang, mặc định 9 sản phẩm mỗi trang; chỉ hiển thị tối đa 5 số trang gần
  trang hiện tại và tự xuống dòng trên màn hình nhỏ.
- Tạo trang chi tiết động `/product/{id}` và danh sách sản phẩm liên quan.
- Trả trang 404 thân thiện khi Product không tồn tại hoặc đã bị vô hiệu hóa.
- Tạo `/register`, kiểm tra Bean Validation và không cho phép email trùng.
- Mã hóa mọi mật khẩu mới bằng BCrypt; không lưu mật khẩu rõ trong MySQL.
- Dùng email làm tài khoản đăng nhập tại `/login` và đăng xuất bằng POST `/logout`.
- Nạp tài khoản từ bảng `users` qua `DatabaseUserDetailsService`.
- Mọi tài khoản tự đăng ký luôn có `ROLE_USER`; không nhận role từ trình duyệt.
- Chặn `/admin/**` bằng `ROLE_ADMIN` và tạo trang kiểm chứng `/admin`.
- Giữ CSRF, bảo vệ Cart/Checkout và hiển thị menu theo trạng thái đăng nhập.
- Cho phép tạo Admin ban đầu an toàn bằng biến môi trường.
- Tạo Entity/Repository/Service cho `Cart` và `CartItem`.
- Mỗi User có tối đa một Cart; mỗi Product chỉ có một CartItem trong Cart.
- Thêm Product, cộng dồn, cập nhật số lượng và xóa CartItem.
- Kiểm tra số lượng lớn hơn 0 và không vượt tồn kho tại Backend.
- Kiểm tra quyền sở hữu CartItem bằng email của tài khoản đăng nhập.
- Tính lại thành tiền và tổng Cart từ giá Product trong MySQL.
- Chuyển `cart.html` sang Thymeleaf động và thêm badge số lượng Cart.
- Giữ POST + CSRF cho tất cả thao tác thay đổi Cart.
- Tạo Checkout động, hiện hỗ trợ thanh toán khi nhận hàng (COD).
- Tạo `Order`, `OrderDetail` và các enum trạng thái tương ứng.
- Lưu snapshot tên/giá sản phẩm để hóa đơn cũ không đổi theo Product.
- Khóa Product, kiểm tra và trừ tồn kho trong một transaction.
- Tính tổng tại Backend; không nhận giá hoặc tổng tiền từ trình duyệt.
- Chỉ xóa CartItem khi toàn bộ đơn được tạo thành công.
- Thêm lịch sử `/my-orders` và chi tiết `/orders/{id}` theo đúng chủ sở hữu.
- Tích hợp giao diện adminHMD Bootstrap 5 cho toàn bộ `/admin/**`.
- Dashboard thống kê Product, Category, User, Order, doanh thu, đơn mới và tồn kho thấp.
- Quản lý Product: tìm kiếm, phân trang, thêm, sửa, ẩn/kích hoạt và validate.
- Lọc Product trong Admin theo danh mục và trạng thái; giữ bộ lọc khi phân trang.
- Quản lý Category: thêm, sửa, chống trùng tên và ẩn an toàn thay cho xóa vật lý.
- Quản lý Order: tìm/lọc, xem chi tiết và chuyển trạng thái theo workflow hợp lệ.
- Hoàn tồn kho trong transaction khi Admin hủy Order PENDING.
- Quản lý User: tìm kiếm, khóa/mở USER và bảo vệ toàn bộ tài khoản ADMIN.
- Hoàn thiện trang chi tiết: thông tin nhà cung cấp, hàng cùng loại và cùng nhà cung cấp.
- Lưu tối đa 6 sản phẩm đã xem gần nhất trong HTTP session, không lưu dữ liệu nhạy cảm.
- Hiển thị điểm trung bình, danh sách Review/Rating và nhận xét khách hàng.
- Chỉ tài khoản đã mua sản phẩm trong đơn COMPLETED mới được đánh giá.
- Mỗi User có một Review cho mỗi Product; gửi lại sẽ cập nhật nội dung cũ.
- Thêm Cart API cho thao tác thêm, cập nhật và xóa sản phẩm không tải lại trang.
- Sửa luồng AJAX thêm giỏ hàng để đọc `productId` và `quantity` trước khi khóa
  biểu mẫu; tránh lỗi `Required parameter 'productId' is not present`.
- Cập nhật badge, thành tiền và tổng Cart tức thời từ kết quả backend.
- Tích hợp SweetAlert2, hiệu ứng bay vào giỏ và hiệu ứng biến mất khi xóa.
- Giữ form truyền thống làm fallback và hỗ trợ `prefers-reduced-motion`.
- Bảo vệ Cart API bằng đăng nhập, ROLE_USER/ROLE_ADMIN và CSRF.
- Việt hóa toàn bộ trang chủ và thay dữ liệu sản phẩm mẫu bằng Product/Category
  đang hoạt động lấy trực tiếp từ MySQL.
- Hiển thị tối đa 8 sản phẩm trên trang chủ; ảnh, tên và nút chi tiết đều mở
  `/product/{id}`.
- Các nút danh mục lọc sản phẩm ngay trên trang chủ, không chuyển sang `/shop`;
  backend nạp tối đa 8 sản phẩm cho từng danh mục và loại bỏ sản phẩm trùng.
- Chuẩn hóa nhãn Admin, thêm nút quay về Website và loại bỏ tên giai đoạn/tác
  giả template khỏi footer hiển thị của khu vực quản trị.
- Đồng bộ menu tiếng Việt bằng một Thymeleaf fragment trên toàn bộ trang công
  khai: `Trang chủ`, `Sản phẩm`, `Tin tức`, `Tiện ích`, `Liên hệ`; bỏ mục menu
  `Sản phẩm mới` và đổi toàn bộ tên menu `Cửa hàng` thành `Sản phẩm`.
- Thêm trang tin tức dự phòng `/news` để phát triển nội dung ở giai đoạn sau.
- Build thành công và 94/94 bài test đã qua.

Email xác nhận tài khoản/đơn hàng thuộc giai đoạn tiếp theo.

## Công nghệ

- Java 23
- Spring Boot 4.1.1
- Maven Wrapper 3.9.16
- Spring MVC và Thymeleaf
- Spring Data JPA / Hibernate
- Spring Security
- Bean Validation
- MySQL 8
- Bootstrap, HTML, CSS, JavaScript và jQuery

## Cấu trúc chính

```text
VegetableShop/
├── database/vegetable_shop.sql
├── docs/PHASE-3.md
├── docs/PHASE-4.md
├── docs/PHASE-5.md
├── docs/PHASE-6.md
├── docs/PHASE-7.md
├── docs/PHASE-8.md
├── docs/PHASE-9.md
├── docs/PHASE-10.md
├── pom.xml
├── src/main/java/com/vegetableshop/
│   ├── config/
│   │   ├── AdminAccountInitializer.java
│   │   └── SecurityConfig.java
│   ├── dto/
│   │   ├── AdminCategoryRequest.java / AdminProductRequest.java
│   │   ├── AdminDashboardView.java
│   │   ├── CheckoutRequest.java
│   │   ├── CartApiError.java / CartMutationResponse.java
│   │   ├── ProductFilter.java
│   │   └── RegisterRequest.java
│   ├── exception/
│   │   ├── CartOperationException.java
│   │   ├── DuplicateEmailException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ProductNotFoundException.java
│   ├── controller/
│   │   ├── AdminController.java
│   │   ├── AuthController.java
│   │   ├── CartController.java
│   │   ├── CartApiController.java
│   │   ├── CartModelAdvice.java
│   │   ├── CheckoutController.java
│   │   ├── HomeController.java
│   │   ├── ProductController.java
│   │   └── TemplateModeController.java
│   ├── entity/
│   │   ├── BaseEntity.java
│   │   ├── Cart.java
│   │   ├── CartItem.java
│   │   ├── Category.java
│   │   ├── Order.java / OrderDetail.java
│   │   ├── OrderStatus.java / PaymentMethod.java / PaymentStatus.java
│   │   ├── Product.java
│   │   ├── Review.java / Supplier.java
│   │   ├── Role.java
│   │   └── User.java
│   ├── repository/
│   │   ├── CartItemRepository.java
│   │   ├── CartRepository.java
│   │   ├── CategoryRepository.java
│   │   ├── OrderRepository.java
│   │   ├── ProductRepository.java
│   │   ├── ProductSpecifications.java
│   │   ├── ReviewRepository.java / SupplierRepository.java
│   │   └── UserRepository.java
│   └── service/
│       ├── AdminService.java
│       ├── CartService.java
│       ├── CategoryService.java
│       ├── DatabaseUserDetailsService.java
│       ├── OrderService.java
│       ├── ProductService.java
│       ├── RecentlyViewedService.java / ReviewService.java
│       └── UserService.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-mysql.properties
│   ├── application-template.properties
│   ├── data-mysql.sql
│   ├── static/
│   │   ├── admin/ (CSS, JS và Bootstrap Icons của adminHMD)
│   │   └── js/cart.js (AJAX Cart, SweetAlert2 và hiệu ứng)
│   └── templates/
│       ├── admin/ (Dashboard, Product, Category, Order, User)
│       ├── fragments/account.html
│       ├── fragments/cart-link.html
│       ├── cart.html
│       ├── checkout.html
│       ├── my-orders.html
│       ├── order-detail.html
│       ├── login.html
│       ├── register.html
│       ├── shop.html
│       └── shop-detail.html
└── src/test/java/com/vegetableshop/
    ├── VegetableShopApplicationTests.java
    ├── controller/ProductControllerTests.java
    ├── controller/ProductTemplateRenderingTests.java
    ├── dto/ProductFilterTests.java
    └── service/ProductServiceTests.java
```

## Luồng dữ liệu trang Shop

```text
Trình duyệt GET /shop?keyword=...&categoryId=...&page=...
        ↓
ProductController
        ↓
ProductService + CategoryService
        ↓
ProductRepository + JpaSpecificationExecutor + CategoryRepository
        ↓
Entity Product + Category
        ↓
MySQL vegetable_shop
        ↓
Model: productPage, products, featuredProducts, categories, filter
        ↓
templates/shop.html (th:each, th:text, th:src)
        ↓
HTML trả về trình duyệt
```

Chi tiết Giai đoạn 3 và 4 nằm trong `docs/PHASE-3.md` và `docs/PHASE-4.md`.

## Luồng đăng ký và đăng nhập

```text
POST /register → AuthController → UserService → BCryptPasswordEncoder
               → UserRepository → users (role luôn là USER)

POST /login → Spring Security → DatabaseUserDetailsService
            → UserRepository → kiểm tra BCrypt + status + role
            → tạo phiên đăng nhập
```

Chi tiết Giai đoạn 5 nằm trong `docs/PHASE-5.md`.

## Luồng dữ liệu giỏ hàng

```text
POST /cart/items → CartController → CartService
                 → ProductRepository + CartRepository + CartItemRepository
                 → kiểm tra tồn kho và chủ sở hữu
                 → MySQL carts/cart_items

GET /cart → CartController → CartService → tính tổng phía Backend → cart.html
```

Chi tiết Giai đoạn 6 nằm trong `docs/PHASE-6.md`.

## Luồng Checkout và đơn hàng

```text
POST /checkout → CheckoutController → OrderService (@Transactional)
               → khóa Product + kiểm tra tồn kho + snapshot giá
               → lưu orders/order_details + trừ tồn kho + xóa CartItem

GET /my-orders, /orders/{id} → luôn lọc bằng email đăng nhập
```

Chi tiết Giai đoạn 7 nằm trong `docs/PHASE-7.md`.

## Luồng quản trị

```text
/admin/** + ROLE_ADMIN → AdminController → AdminService
                       → Repository → MySQL → Thymeleaf adminHMD
```

Product/Category được ẩn thay vì xóa vật lý. Order chỉ được chuyển theo workflow
hợp lệ; hủy Order PENDING sẽ hoàn tồn kho trong transaction. Chi tiết nằm trong
`docs/PHASE-8.md`.

## Luồng chi tiết sản phẩm và đánh giá

```text
GET /product/{id} → ProductController
                  → Product + Category + Supplier
                  → cùng loại + cùng nhà cung cấp + hàng đã xem trong session
                  → Review + điểm trung bình → shop-detail.html

POST /product/{id}/reviews → kiểm tra đăng nhập + CSRF
                            → kiểm tra đơn COMPLETED
                            → tạo/cập nhật Review
```

Chi tiết Giai đoạn 9 nằm trong `docs/PHASE-9.md`.

## Luồng AJAX giỏ hàng

```text
Form thêm/cập nhật/xóa → cart.js + CSRF
                         → /api/cart/**
                         → CartApiController → CartService
                         → kiểm tra User/Product/tồn kho → MySQL
                         → JSON tổng tiền backend → cập nhật DOM + SweetAlert2
```

Nếu JavaScript không chạy, form cũ vẫn POST về `CartController`. Chi tiết Giai
đoạn 10 nằm trong `docs/PHASE-10.md`.

## Chuẩn bị MySQL

Yêu cầu MySQL 8 đang chạy ở cổng `3306`. Mở MySQL Client và chạy:

```sql
SOURCE D:/JavaProjects/TrietHo/VegetableShop/database/vegetable_shop.sql;
```

Hoặc mở `database/vegetable_shop.sql` bằng MySQL Workbench và chạy toàn bộ file.
Script tạo 9 bảng, 3 danh mục, 3 nhà cung cấp và 5 sản phẩm mẫu.

Không ghi mật khẩu thật vào source code. Trong PowerShell, cấu hình bằng biến môi
trường:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_URL = "jdbc:mysql://localhost:3306/vegetable_shop"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "mat-khau-mysql-cua-ban"
```

Nếu tài khoản `root` không có mật khẩu, đặt `$env:DB_PASSWORD = ""`.

Để tạo tài khoản Admin ban đầu, thêm ba biến sau trước lần chạy đầu tiên:

```powershell
$env:APP_ADMIN_NAME = "Quản trị viên"
$env:APP_ADMIN_EMAIL = "admin@vegetableshop.local"
$env:APP_ADMIN_PASSWORD = "<MAT_KHAU_ADMIN_MANH_CUA_BAN>"
```

Admin chỉ được tạo nếu email chưa tồn tại. Sau khi tạo thành công, có thể xóa ba
biến trên khỏi Run Configuration; mật khẩu lưu trong database là chuỗi BCrypt.

## Chạy project

### Chạy giao diện không cần database

Profile mặc định là `template`:

```powershell
cd D:\JavaProjects\TrietHo\VegetableShop
.\mvnw.cmd spring-boot:run
```

Trang `/shop` sẽ hiển thị trạng thái chưa có dữ liệu. Cách này hữu ích để kiểm
tra HTML/CSS/JavaScript mà không lưu hoặc chia sẻ mật khẩu database.

### Chạy với Product thật từ MySQL

Sau khi import database và đặt các biến môi trường ở trên:

```powershell
cd D:\JavaProjects\TrietHo\VegetableShop
.\mvnw.cmd spring-boot:run
```

Mở `http://localhost:8081/shop`. Kết quả đúng là có 3 danh mục và 5 sản phẩm:
Bông cải xanh, Ớt chuông, Chuối, Cam và Táo. Do kích thước trang mặc định là 3,
hai sản phẩm còn lại nằm ở trang 2.

Ví dụ kiểm tra nhanh Giai đoạn 4:

```text
http://localhost:8081/shop?keyword=Cam
http://localhost:8081/shop?categoryId=1
http://localhost:8081/shop?minPrice=40000&maxPrice=80000
http://localhost:8081/shop?sort=priceAsc&size=3&page=1
http://localhost:8081/product/1
```

`data-mysql.sql` dùng `INSERT IGNORE`, vì vậy chạy ứng dụng nhiều lần không tạo
trùng dữ liệu mẫu.

### URL ảnh cho 80 sản phẩm mở rộng

Ngày 2026-08-28, database đang chạy đã được gắn URL ảnh 800×600 cho 80 sản phẩm
thuộc bốn danh mục `Rau củ`, `Trái cây`, `Thực phẩm sạch` và `Nấm các loại`.
Mỗi URL dùng từ khóa tiếng Anh phù hợp với tên sản phẩm và tham số `lock` để giữ
ảnh ổn định. Hai mươi sản phẩm thuộc `Đồ khô & Hạt dinh dưỡng` được giữ nguyên.

Để áp dụng lại bộ URL vào database có cùng dữ liệu và ID sản phẩm, chạy:

```sql
SOURCE D:/JavaProjects/TrietHo/VegetableShop/database/product_image_urls_20260828.sql;
```

Nếu cần quay về trạng thái trước đó (80 giá trị `image` đều là `NULL`), chạy:

```sql
SOURCE D:/JavaProjects/TrietHo/VegetableShop/database/product_image_urls_20260828_rollback.sql;
```

Đây là ảnh minh họa dành cho đồ án. Trước khi triển khai website thật, nên tải
ảnh đã chọn về máy chủ/CDN riêng và bổ sung thông tin giấy phép, tác giả theo
nguồn ảnh.

Kiểm tra Giai đoạn 5:

```text
http://localhost:8081/register
http://localhost:8081/login
http://localhost:8081/admin
```

Tài khoản đăng ký qua `/register` là `ROLE_USER` và sẽ nhận trang 403 nếu cố mở
`/admin`. Tài khoản tạo bằng `APP_ADMIN_*` có `ROLE_ADMIN` và mở được Dashboard.

Kiểm tra Giai đoạn 6 sau khi đăng nhập:

```text
http://localhost:8081/shop
http://localhost:8081/product/1
http://localhost:8081/cart
```

Nút thêm vào giỏ có trên Shop và Product Detail. Backend từ chối số lượng bằng
0, số lượng âm hoặc số lượng vượt tồn kho.

Kiểm tra Giai đoạn 7 sau khi giỏ đã có sản phẩm:

```text
http://localhost:8081/checkout
http://localhost:8081/my-orders
http://localhost:8081/orders/{id}
```

Checkout hiện hỗ trợ COD. Khi đặt thành công, ứng dụng tạo Order/OrderDetail,
trừ tồn kho và làm rỗng giỏ trong cùng một transaction.

Kiểm tra Giai đoạn 8 bằng tài khoản `ROLE_ADMIN`:

```text
http://localhost:8081/admin
http://localhost:8081/admin/products
http://localhost:8081/admin/categories
http://localhost:8081/admin/orders
http://localhost:8081/admin/users
```

## Route hiện có

| URL | Trạng thái ở Giai đoạn 10 |
| --- | --- |
| `/` | Trang chủ tiếng Việt, hiển thị Category/Product mới nhất từ MySQL |
| `/shop` | Search, filter, sort và pagination từ MySQL |
| `/product/{id}` | Chi tiết Product và sản phẩm liên quan |
| `/news` | Trang tin tức tiếng Việt dự phòng cho giai đoạn phát triển sau |
| `POST /product/{id}/reviews` | Tạo/cập nhật đánh giá đã xác minh mua hàng |
| `/register` | Đăng ký USER, validate và BCrypt password |
| `/login` | Đăng nhập bằng email/password từ MySQL |
| `POST /logout` | Đăng xuất an toàn với CSRF |
| `/admin` | Dashboard thống kê, đơn mới và cảnh báo tồn kho |
| `/admin/products/**` | Tìm, thêm, sửa và ẩn/kích hoạt Product |
| `/admin/categories/**` | Thêm, sửa và ẩn/kích hoạt Category |
| `/admin/orders/**` | Tìm/lọc, chi tiết và cập nhật trạng thái Order |
| `/admin/users/**` | Tìm, khóa/mở tài khoản USER |
| `/access-denied` | Trang lỗi quyền truy cập 403 |
| `/shop-detail` | Chuyển hướng về `/shop` vì URL chi tiết cần Product ID |
| `/cart` | Cart động của User, tổng tiền tính tại Backend |
| `POST /cart/items` | Thêm Product hoặc cộng dồn số lượng |
| `POST /cart/items/{id}/quantity` | Cập nhật số lượng có kiểm tra tồn kho/chủ sở hữu |
| `POST /cart/items/{id}/delete` | Xóa CartItem thuộc User hiện tại |
| `POST /api/cart/items` | AJAX thêm/cộng dồn Product, trả tổng Cart từ backend |
| `PATCH /api/cart/items/{id}` | AJAX cập nhật số lượng và thành tiền |
| `DELETE /api/cart/items/{id}` | AJAX xóa dòng hàng và trả trạng thái Cart |
| `GET /checkout` | Form nhận hàng và tóm tắt Cart động |
| `POST /checkout` | Tạo đơn COD, trừ kho và xóa Cart trong transaction |
| `/my-orders` | Lịch sử Order của User hiện tại |
| `/orders/{id}` | Chi tiết Order có kiểm tra chủ sở hữu |
| `/contact` | Giao diện tĩnh |
| `/testimonial` | Giao diện tĩnh |
| `/missing-page` | Kiểm tra trang lỗi 404 |

## Chạy kiểm thử

```powershell
.\mvnw.cmd clean package
```

Kết quả xác minh Giai đoạn 10:

```text
Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Các test bao phủ toàn bộ giai đoạn cũ, Admin, Review đã mua hàng, Cart API/AJAX,
DTO validation, workflow Order, tồn kho, Security/CSRF, Controller, trang chủ
động và render giao diện Thymeleaf.

Việc kết nối profile `mysql` cần đúng mật khẩu của MySQL trên máy. Nếu gặp
`Access denied for user 'root'`, hãy đặt lại `DB_USERNAME`/`DB_PASSWORD`; đây
không phải lỗi biên dịch Java.

## Mở bằng IntelliJ IDEA

1. Chọn **File → Open**.
2. Mở `D:\JavaProjects\TrietHo\VegetableShop`.
3. Chờ IntelliJ tải Maven dependencies.
4. Kiểm tra Project SDK là JDK 23.
5. Với MySQL, thêm bốn biến môi trường ở phần trên vào Run Configuration.
6. Chạy class `VegetableShopApplication`.

## Xử lý lỗi thường gặp

### Access denied for user root

Đặt đúng `DB_USERNAME` và `DB_PASSWORD` trong terminal hoặc IntelliJ Run
Configuration rồi chạy lại.

### Unknown database vegetable_shop

Import `database/vegetable_shop.sql`. URL mặc định cũng có
`createDatabaseIfNotExist=true`, nhưng tài khoản MySQL phải có quyền tạo database.

### Port 8081 already in use

Website mặc định chạy ở cổng `8081`; MySQL vẫn dùng cổng `3306`. Nếu `8081`
đang bị một ứng dụng khác sử dụng, chạy tạm bằng:

```powershell
$env:SERVER_PORT = "18080"
.\mvnw.cmd spring-boot:run
```

## Backup

Backup trước Giai đoạn 3:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-3-20260827-0840.zip
SHA-256: BBB21010CB1E567C73DF1BED02F38FF7B705AB5F93B2AC7E4C5078A9D9A7A3EB
```

Backup sau khi hoàn tất và xác minh Giai đoạn 3:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-3-verified-20260827-0900.zip
```

Backup sau khi đổi cổng website mặc định sang `8081`:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-3-port-8081-20260827-0917.zip
```

Backup an toàn trước Giai đoạn 4:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-4-20260827-0928.zip
SHA-256: 7BCFED990243E01B60F77C23C7AFEFD6036101B9A53677E8A116961CD4686094
```

Backup sau khi hoàn tất và xác minh Giai đoạn 4:

```text
D:\\JavaProjects\\TrietHo\\backups\\VegetableShop-after-phase-4-verified-20260827-1302.zip
```

Backup Giai đoạn 4 loại trừ cả `target` và `.idea`; nhờ đó không đóng gói mật
khẩu MySQL có thể nằm trong IntelliJ Run Configuration.

SHA-256 của backup sau được ghi trong báo cáo hoàn tất để có thể kiểm tra tính
toàn vẹn mà không tạo vòng lặp thay đổi chính file backup.

Backup an toàn trước Giai đoạn 5:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-5-20260827-1307.zip
SHA-256: E17722193C28A8D2DE331E4C682E08AA9FA4118D3257B51770BE344E0947E47E
```

Backup sau khi hoàn tất và xác minh Giai đoạn 5:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-5-verified-20260827-1320.zip
```

Backup loại trừ `target` và `.idea` để không lưu build output hoặc mật khẩu có
thể nằm trong IntelliJ Run Configuration.

Backup an toàn trước Giai đoạn 7:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-7-20260827-1617.zip
SHA-256: 733CE81E9EFCC1FD60E1BAF26A298645BAE0171EB88C7C61DD7C209DA46A32B6
```

Backup sau khi hoàn tất và xác minh Giai đoạn 7:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-7-verified-20260827-1632.zip
```

Hai backup Giai đoạn 7 đều loại trừ `target` và `.idea` để không lưu build
output hoặc mật khẩu có thể nằm trong IntelliJ Run Configuration.

Backup an toàn trước Giai đoạn 8:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-8-20260827-2108.zip
SHA-256: 750798F34CF26236E746C3DED58E7B2098CAAB67A5B87D129F05CC5406E12D2C
```

Backup sau khi hoàn tất và xác minh Giai đoạn 8:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-8-verified-20260827-2130.zip
```

Hai backup Giai đoạn 8 loại trừ `target` và `.idea` để không lưu build output
hoặc mật khẩu có thể nằm trong IntelliJ Run Configuration.

Backup an toàn trước Giai đoạn 9:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-9-20260827.zip
SHA-256: 592C39156A0C84A06AAAE745C82A27FAE109348075ADB4438A1A971A46FFE7B4
```

Backup sau khi hoàn tất và xác minh Giai đoạn 9:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-9-verified-20260827-2255.zip
```

Hai backup Giai đoạn 9 loại trừ `target`, `.idea` và file `.iml`; không đóng gói
build output hoặc cấu hình IntelliJ có thể chứa mật khẩu MySQL.

Backup an toàn trước Giai đoạn 10:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-10-20260827-2330.zip
SHA-256: 223ABCAF7D7CA8CFE9BF7328B6402D0F9B40E39C65FAB820F3B8A15C1B163D00
```

Backup sau khi hoàn tất và xác minh Giai đoạn 10:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-10-verified-20260827-2350.zip
```

Hai backup Giai đoạn 10 loại trừ `target`, `.idea` và `.iml` để không đóng gói
build output hoặc cấu hình IntelliJ có thể chứa mật khẩu MySQL.

Backup sau khi Việt hóa và xác minh trang chủ động:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-homepage-vietnamese-verified-20260828-0841.zip
```

Backup trang chủ loại trừ `target`, `.idea`, `.git` và `.iml`; gồm mã nguồn,
kiểm thử và README mới nhất nhưng không đóng gói cấu hình IntelliJ.

Backup sau khi thêm và xác minh bộ lọc danh mục ngay trên trang chủ:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-home-category-filter-verified-20260828-0855.zip
```

Đã kiểm tra trực tiếp nút `Nấm các loại`: URL vẫn là `/`, giao diện hiển thị
8 sản phẩm thuộc danh mục Nấm và các liên kết chi tiết vẫn trỏ tới
`/product/{id}`. Backup loại trừ `target`, `.idea`, `.git` và `.iml`.

Backup an toàn trước Giai đoạn 6:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-6-20260827-1425.zip
SHA-256: 52BFFE9733D9795FB05FD3DC1960311FD93626B71B73E383902A94CA71D4CAF3
```

Backup sau khi hoàn tất và xác minh Giai đoạn 6:

```text
D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-6-verified-20260827-1441.zip
```

Backup loại trừ `target` và `.idea` để không lưu build output hoặc mật khẩu có
thể nằm trong IntelliJ Run Configuration.

Các file backup không chứa thư mục build `target`; có thể tạo lại bằng
`.\mvnw.cmd clean package`.

## Bản quyền template

Giao diện gốc là **Fruitables - Vegetable Website Template** của HTML Codex,
phát hành theo CC BY 4.0. Credit của tác giả được giữ trong footer. Nội dung giấy
phép nằm trong `THIRD-PARTY-LICENSES/fruitables`.

Khu vực quản trị sử dụng **adminHMD** của Md. Hasan Mahmud, phân phối bởi
ThemeWagon theo MIT. Credit được giữ trong footer và giấy phép nằm tại
`THIRD-PARTY-LICENSES/adminhmd/LICENSE.txt`.

Thông báo giỏ hàng dùng **SweetAlert2** qua jsDelivr CDN. SweetAlert2 phát hành
theo MIT; giấy phép nằm tại `THIRD-PARTY-LICENSES/sweetalert2/LICENSE.txt`.

## Lộ trình thống nhất Giai đoạn 1–16

Từ thời điểm này, số **giai đoạn** được hiểu theo roadmap dưới đây, không lấy số
thứ tự của từng mục chức năng trong yêu cầu ban đầu. Ví dụ, quản lý đơn hàng
Admin đã thuộc Giai đoạn 8; **Giai đoạn 15 chính thức là Thống kê kinh doanh**.

| Giai đoạn | Phạm vi chính | Trạng thái |
| --- | --- | --- |
| 1 | Phân tích template và lập kế hoạch chuyển đổi | Hoàn thành |
| 2 | Tạo Spring Boot project, đưa template vào Thymeleaf/static | Hoàn thành |
| 3 | MySQL, Entity/Repository cho User–Category–Product, Shop động | Hoàn thành |
| 4 | Search, filter, sort, pagination và Product Detail | Hoàn thành |
| 5 | Register, Login, Logout, BCrypt và Spring Security | Hoàn thành |
| 6 | Cart và CartItem | Hoàn thành |
| 7 | Checkout COD, Order, OrderDetail và lịch sử đơn hàng | Hoàn thành |
| 8 | Admin Dashboard và quản lý Product, Category, Order, User | Hoàn thành |
| 9 | Chi tiết sản phẩm nâng cao, Supplier, Recently Viewed, Review/Rating | Hoàn thành |
| 10 | Cart AJAX, SweetAlert2, UX và rà soát giao diện | Hoàn thành |
| 11 | Quản lý tài khoản và đăng nhập ngoài | Chưa triển khai |
| 12 | Email giao dịch | Chưa triển khai |
| 13 | Thanh toán trực tuyến | Chưa triển khai |
| 14 | Tiện ích giao diện | Hoàn thành một phần |
| 15 | Thống kê kinh doanh | Hoàn thành phần Dashboard cơ bản |
| 16 | Phân quyền nâng cao | Mới có USER/ADMIN cơ bản |

### Giai đoạn 1 – Phân tích template

- Kiểm kê page, CSS, JavaScript, ảnh và thư viện của template Fruitables.
- Phân loại phần giao diện tĩnh, phần có JavaScript và phần cần backend.
- Xác định kiến trúc `Controller → Service → Repository → Entity → MySQL`.

### Giai đoạn 2 – Khởi tạo Spring Boot và chuyển template

- Tạo Maven/Spring Boot project với Thymeleaf, JPA, Security và MySQL Driver.
- Chuyển HTML vào `templates`, tài nguyên vào `static`.
- Chạy được trang chủ đầu tiên bằng Spring MVC.

### Giai đoạn 3 – MySQL và Shop động

- Thiết kế database `vegetable_shop`.
- Tạo `User`, `Category`, `Product`, Repository và Service.
- Đọc Product/Category thật từ MySQL và hiển thị tại `/shop`.

### Giai đoạn 4 – Hoàn thiện cửa hàng

- Tìm kiếm, lọc danh mục/khoảng giá, sắp xếp và phân trang.
- Trang `/product/{id}`, sản phẩm liên quan và xử lý Product không tồn tại.

### Giai đoạn 5 – Thành viên và bảo mật cơ bản

- Đăng ký, đăng nhập, đăng xuất bằng Spring Security.
- BCrypt, email duy nhất, `ROLE_USER`, `ROLE_ADMIN`, CSRF và chặn `/admin/**`.

### Giai đoạn 6 – Giỏ hàng

- Cart gắn với User, thêm/cộng dồn/cập nhật/xóa CartItem.
- Kiểm tra tồn kho và tính tổng tiền hoàn toàn ở backend.

### Giai đoạn 7 – Checkout và đơn hàng

- Checkout COD, tạo Order/OrderDetail trong transaction.
- Chụp giá tại thời điểm mua, trừ kho, xóa giỏ và xem lịch sử đơn hàng.

### Giai đoạn 8 – Khu vực quản trị cơ bản

- Dashboard và layout adminHMD.
- Quản lý Product, Category, Order và User.
- Workflow đơn hàng `PENDING → CONFIRMED → SHIPPING → COMPLETED`; chỉ
  `PENDING → CANCELLED`, có hoàn tồn kho và chống chuyển trạng thái vô lý.

### Giai đoạn 9 – Chi tiết sản phẩm nâng cao

- Nhà cung cấp, hàng cùng loại, hàng cùng nhà cung cấp và sản phẩm đã xem.
- Rating/Review; chỉ khách đã mua trong đơn `COMPLETED` được đánh giá.

### Giai đoạn 10 – AJAX và hoàn thiện trải nghiệm cơ bản

- Cart API/AJAX, badge tức thời, hiệu ứng bay vào giỏ và xóa sản phẩm.
- SweetAlert2, fallback form truyền thống và kiểm tra CSRF/Security.
- Việt hóa trang chủ, dùng dữ liệu MySQL và lọc sản phẩm theo danh mục tại chỗ.

### Giai đoạn 11 – Quản lý tài khoản và đăng nhập ngoài

- Xem/cập nhật hồ sơ: họ tên, điện thoại, địa chỉ và ảnh đại diện.
- Đổi mật khẩu khi đã đăng nhập; kiểm tra mật khẩu hiện tại.
- Chuẩn bị token quên/đặt lại mật khẩu; phần gửi link thực tế thuộc Giai đoạn 12.
- Đăng nhập Google OAuth2 là mục tiêu chính.
- Facebook Login chỉ là phần mở rộng vì cấu hình ứng dụng Facebook phức tạp và
  không làm tăng nhiều giá trị cho phần cốt lõi.

### Giai đoạn 12 – Email giao dịch

- Xác nhận đăng ký và kích hoạt tài khoản.
- Gửi liên kết đặt lại mật khẩu có token, thời hạn và chỉ dùng một lần.
- Gửi xác nhận đặt hàng.
- Thông báo thay đổi trạng thái đơn hàng.
- Không ghi mật khẩu SMTP vào source/README; dùng biến môi trường.
- Email quảng cáo chỉ làm khi còn thời gian và phải có đăng ký/hủy nhận thư.

### Giai đoạn 13 – Thanh toán trực tuyến

- Chỉ triển khai **một cổng thanh toán**; mục tiêu ưu tiên là **VNPay**.
- Tạo giao dịch thanh toán riêng với mã giao dịch của cổng và trạng thái
  `PENDING`, `PAID`, `FAILED`, `REFUNDED`.
- Callback/webhook phải kiểm tra chữ ký, số tiền và liên kết đúng Order.
- Tổng tiền luôn lấy lại từ backend; không tin dữ liệu từ trình duyệt.
- Callback phải idempotent để không tạo đơn hoặc ghi nhận thanh toán hai lần.
- MoMo, Stripe và PayPal không triển khai song song; chỉ cân nhắc thay thế VNPay
  nếu môi trường thử nghiệm không khả dụng và phải thống nhất trước.

### Giai đoạn 14 – Tiện ích giao diện

- SweetAlert2 cho xác nhận xóa, hủy đơn và thông báo thành công: **đã có một phần**.
- Summernote cho mô tả dài của Product trong Admin.
- Chart.js làm nền cho biểu đồ Giai đoạn 15.
- CAPTCHA cho đăng ký, quên mật khẩu hoặc liên hệ; backend vẫn phải validate.
- Google Maps chỉ làm khi có địa chỉ cửa hàng thực tế.

### Giai đoạn 15 – Thống kê kinh doanh

- Tồn kho và sản phẩm sắp hết hàng.
- Doanh số theo sản phẩm, danh mục, nhà cung cấp và khách hàng.
- Doanh thu theo tháng, quý và năm.
- Bộ lọc khoảng ngày và biểu đồ Chart.js.
- Xuất CSV; Excel chỉ làm thêm khi còn thời gian.
- Quy tắc chính thức: **chỉ Order có `OrderStatus.COMPLETED` được tính vào doanh
  thu**. Đơn `PAID` nhưng chưa `COMPLETED` được theo dõi là tiền đã thu/chờ hoàn
  tất, không cộng vào báo cáo doanh thu để tránh tính sai hoặc tính hai lần.
- Dashboard hiện đã có tổng doanh thu đơn `COMPLETED` và cảnh báo tồn kho thấp;
  các báo cáo phân nhóm và biểu đồ vẫn chưa triển khai.

### Giai đoạn 16 – Phân quyền nâng cao

Phiên bản hiện tại tiếp tục dùng `USER` và `ADMIN`. Khi triển khai sẽ chuyển sang:

```text
users
roles
permissions
user_roles
role_permissions
```

Quyền dự kiến:

```text
PRODUCT_VIEW, PRODUCT_CREATE, PRODUCT_UPDATE
ORDER_VIEW, ORDER_UPDATE
USER_MANAGE, REPORT_VIEW, ROLE_MANAGE
```

Vai trò dự kiến:

- `ADMIN`: toàn quyền.
- `MANAGER`: sản phẩm, đơn hàng và thống kê.
- `STAFF`: xem/xử lý đơn hàng theo quyền được cấp.
- `CUSTOMER`: mua hàng; thay thế ý nghĩa của `USER` khi migration.

Không cho quản trị viên tự xóa vai trò/quyền cuối cùng của chính mình; hệ thống
phải luôn giữ ít nhất một tài khoản có toàn quyền quản trị.

### Thứ tự triển khai chính thức tiếp theo

```text
Giai đoạn 11 → 12 → 13 → 14 → 15 → 16
```

Mỗi giai đoạn phải có backup trước/sau, tài liệu `docs/PHASE-N.md`, kiểm thử
backend/render/security tương ứng và chỉ chuyển tiếp khi build thành công.
