# Vegetable Shop

Website thương mại điện tử bán rau củ và thực phẩm sạch, xây dựng bằng Java,
Spring Boot, Thymeleaf và MySQL. Dự án mô phỏng quy trình từ tìm kiếm sản phẩm,
giỏ hàng, đặt hàng đến quản trị cửa hàng.

![Vegetable Shop](src/main/resources/static/img/hero-img.jpg)

> Project cá nhân phục vụ đồ án và hồ sơ ứng tuyển thực tập Java Backend.

## Điểm nổi bật

- Kiến trúc phân lớp Controller, Service, Repository và Entity.
- Sản phẩm động từ MySQL; tìm kiếm, lọc, sắp xếp và phân trang.
- Chi tiết sản phẩm có gallery ảnh, SKU, thương hiệu, xuất xứ, đơn vị bán,
  hàng cùng loại/thương hiệu và lịch sử đã xem.
- Wishlist AJAX, lịch sử xem lâu dài và gợi ý sản phẩm rule-based.
- Thẻ sản phẩm gọn, dùng chung giữa trang chủ, danh sách và các nhóm gợi ý:
  ảnh/tên mở chi tiết, trái tim yêu thích và nút thêm vào giỏ; giá/đơn vị lấy từ dữ liệu sản phẩm.
- Đăng ký, đăng nhập và phân quyền `USER`/`ADMIN` bằng Spring Security.
- Hồ sơ cá nhân, đổi/quên mật khẩu và Google Login bằng OpenID Connect.
- Email HTML cho kích hoạt tài khoản, đặt lại mật khẩu và cập nhật đơn hàng.
- Giỏ hàng AJAX, Checkout COD hoặc chuyển khoản VietQR theo số tiền/mã đơn;
  Admin đối chiếu giao dịch thực nhận và xác nhận thanh toán thủ công.
- Review 1–5 sao và tối đa 5 ảnh JPG/PNG, xác minh từng chi tiết đơn hoàn tất.
- Admin duyệt/ẩn/xóa mềm đánh giá; thống kê sao chỉ tính nội dung đã duyệt.
- Quản trị sản phẩm theo SKU, thương hiệu, đơn vị bán, xuất xứ và nhiều ảnh.
- Quản lý riêng thương hiệu và nhà cung cấp; không đồng nhất hai khái niệm.
- Quản lý kho bằng phiếu nhập, xuất, kiểm kê nhiều dòng và lịch sử bất biến.
- Cảnh báo tồn thấp theo ngưỡng riêng của từng sản phẩm.
- Báo cáo tồn kho, doanh số đa chiều, biểu đồ doanh thu và xuất CSV.
- BCrypt, CSRF, kiểm tra quyền sở hữu, tồn kho và tổng tiền tại backend.
- Giao diện Responsive, menu tiếng Việt dùng chung, SweetAlert2 và Summernote.
- CAPTCHA một lần lưu trong session; bản đồ khu vực phục vụ không cần API key.
- Mô tả sản phẩm có định dạng và được làm sạch HTML tại backend bằng jsoup.
- Chatbot hybrid: AI hiểu câu hỏi mở và chọn sản phẩm từ catalog MySQL; backend
  kiểm tra lại ID/giá/tồn kho và tự fallback về 21A khi API không sẵn sàng.
- Admin quản lý FAQ ưu tiên trước AI, xem thống kê nguồn trả lời/độ trễ/câu cần
  rà soát và dọn nhật ký đã quá hạn; dữ liệu thống kê được che thông tin nhạy cảm.
- Có profile chỉ hiển thị giao diện khi chưa cấu hình MySQL.

## Chức năng đã hoàn thành

| Khu vực | Chức năng |
| --- | --- |
| Trang chủ | 8 sản phẩm ngẫu nhiên xen kẽ danh mục khi tải trang; lọc nhanh tại chỗ theo danh mục |
| Sản phẩm | Tìm kiếm, lọc danh mục/giá, sắp xếp và phân trang |
| Chi tiết | Thông tin hàng hóa, đánh giá, hàng liên quan và đã xem |
| Gợi ý | Wishlist, lịch sử xem và gợi ý theo danh mục/thương hiệu/giá |
| Đánh giá | Một lần mỗi chi tiết đơn hoàn tất; ảnh, nhãn đã mua, kiểm duyệt và phân bố số sao |
| Thành viên | Đăng ký, đăng nhập, đăng xuất và BCrypt |
| Email | Kích hoạt tài khoản, đặt lại mật khẩu, xác nhận và trạng thái đơn |
| Giỏ hàng | Thêm, cập nhật, xóa bằng AJAX và kiểm tra tồn kho |
| Đặt hàng | COD hoặc QR chuyển khoản ngân hàng; trừ tồn kho một lần, lịch sử và chi tiết đơn |
| Thanh toán QR | VietQR động tạo tại backend, báo đã chuyển, Admin đối soát thủ công, polling trạng thái và biên nhận |
| Quản trị | Dashboard; sản phẩm, danh mục, thương hiệu, nhà cung cấp, đơn hàng và người dùng |
| Quản lý kho | Phiếu nhập/xuất/kiểm kê nhiều sản phẩm, cảnh báo tồn thấp và lịch sử kho |
| Thống kê | Tồn kho, doanh số, lọc ngày, Chart.js và xuất CSV |
| Khuyến mãi | Voucher theo phạm vi, giới hạn lượt dùng, chương trình giảm giá và Flash Sale |
| Tiện ích | SweetAlert2, Summernote, CAPTCHA session và Google Maps |
| Chatbot | FAQ quản trị ưu tiên, AI hiểu ngôn ngữ tự nhiên, gợi ý sản phẩm thật, fallback và thống kê ẩn danh |
| Bảo mật | Spring Security, CSRF, phân quyền và kiểm tra chủ sở hữu |

Quy tắc chọn mẫu trang chủ và cách kiểm tra: [Sản phẩm ngẫu nhiên xen kẽ danh mục](docs/FIX-HOMEPAGE-RANDOM.md).

Giao diện thẻ sản phẩm và tiêu chí kiểm tra: [Thẻ sản phẩm thu gọn](docs/COMPACT-PRODUCT-CARDS.md).

Trạng thái hiện tại: hoàn thành **Giai đoạn 1–15, 17, 18, 18A
(Admin), 18B (Website), 19, 20, 21A, 21B và 21C**. Giai đoạn 18A chuẩn hóa quản trị sản phẩm, thương
hiệu, nhà cung cấp và phiếu kho; giai đoạn 18B dùng dữ liệu đó để hoàn thiện
trang chi tiết sản phẩm, gallery ảnh và gợi ý phía khách hàng. Giai đoạn 21B đã
tích hợp OpenAI theo chế độ tùy chọn; 21C bổ sung FAQ do Admin quản lý và thống
kê hội thoại đã làm sạch. Không cần API key nếu chỉ dùng chatbot 21A/FAQ 21C.
Giai đoạn 13 sử dụng chuyển khoản ngân hàng VietQR và đối soát thủ công bởi Admin,
không phải cổng VNPay/MoMo. Đối soát tự động và cổng thanh toán được tạm hoãn.
Giai đoạn 20 đã bổ sung voucher, khuyến mãi và Flash Sale; mọi số tiền giảm được
backend tính lại và lưu snapshot trong đơn hàng.

Hướng dẫn cấu hình, giới hạn và review: [Thanh toán QR](docs/PHASE-13.md) và
[Voucher, khuyến mãi, Flash Sale](docs/PHASE-20.md). Phân quyền chi tiết vẫn
thuộc lộ trình tiếp theo.

## Công nghệ

- Java 23, Spring Boot 4.1.1 và Maven Wrapper
- Spring MVC, Thymeleaf, Spring Data JPA / Hibernate
- Spring Security, Bean Validation và Spring Mail
- MySQL 8
- Bootstrap 5, HTML, CSS, JavaScript, jQuery, SweetAlert2, Summernote và Chart.js
- jsoup để làm sạch nội dung HTML do quản trị viên nhập
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

Nếu nâng cấp database đã có từ giai đoạn 11, chạy thêm
[`database/phase-12-email-migration.sql`](database/phase-12-email-migration.sql)
một lần trước khi bật gửi email. Để dùng Wishlist và lịch sử xem lâu dài, chạy
thêm [`database/phase-17-wishlist-recommendation-migration.sql`](database/phase-17-wishlist-recommendation-migration.sql).

Để bật sổ kho, ngưỡng tồn thấp và dữ liệu tồn đầu kỳ, chạy thêm
[`database/phase-18-inventory-migration.sql`](database/phase-18-inventory-migration.sql).

Sau đó chạy
[`database/phase-18a-admin-catalog-inventory-documents.sql`](database/phase-18a-admin-catalog-inventory-documents.sql)
để bổ sung SKU, thương hiệu, đơn vị bán, nhiều ảnh, thông tin nhà cung cấp và
phiếu kho nhiều dòng. Script giữ lại dữ liệu cũ và có thể chạy lại an toàn.
Giai đoạn 18B không cần migration riêng; xem
[`docs/PHASE-18B.md`](docs/PHASE-18B.md) để biết quy tắc dữ liệu phía website.

**Giai đoạn 19:** dừng ứng dụng, sao lưu MySQL rồi chạy
[`database/phase-19-advanced-reviews.sql`](database/phase-19-advanced-reviews.sql)
trước khi khởi động lại. Hibernate `ddl-auto=update` không tự bỏ ràng buộc duy nhất cũ
`uk_reviews_user_product`, nên migration vẫn cần thiết. Đánh giá cũ được giữ lại ở
trạng thái chờ duyệt và chỉ liên kết với chi tiết đơn đã hoàn tất nếu xác minh được.
Xem [hướng dẫn giai đoạn 19](docs/PHASE-19.md).

**Giai đoạn 20:** dừng ứng dụng, sao lưu MySQL rồi chạy
[`database/phase-20-discounts.sql`](database/phase-20-discounts.sql) trước khi
khởi động lại. Script bổ sung voucher, phạm vi áp dụng, chương trình khuyến mãi,
lịch sử sử dụng và snapshot giảm giá của đơn hàng. Xem
[hướng dẫn giai đoạn 20](docs/PHASE-20.md).

Ảnh đánh giá mặc định lưu tại `uploads/reviews` tương đối với thư mục chạy ứng dụng;
có thể đặt biến `REVIEW_IMAGE_DIRECTORY` thành đường dẫn tuyệt đối bên ngoài project.
Thư mục ảnh được loại khỏi Git, cần sao lưu riêng cùng database.

Nếu nâng cấp project đang có lên giai đoạn 21C, chạy
[`database/phase-21c-chatbot-admin-analytics.sql`](database/phase-21c-chatbot-admin-analytics.sql).
Script tạo bảng FAQ/analytics, thêm 5 FAQ mẫu và có thể chạy lại an toàn. Với
`spring.jpa.hibernate.ddl-auto=update`, Hibernate cũng có thể tạo bảng; migration
vẫn nên được lưu và chạy có kiểm soát khi triển khai thật.

## Bật email SMTP

Email là profile tùy chọn. Ví dụ dùng SMTP Gmail:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql,mail"
$env:MAIL_USERNAME = "<EMAIL_GUI_THU>"
$env:MAIL_PASSWORD = "<APP_PASSWORD_16_KY_TU>"
$env:APP_BASE_URL = "http://localhost:8081"
.\mvnw.cmd spring-boot:run
```

Không sử dụng mật khẩu đăng nhập Gmail thông thường. Nếu dùng Gmail, bật xác
minh hai bước và tạo App Password. Có thể đổi `MAIL_HOST`, `MAIL_PORT`,
`MAIL_FROM` và `MAIL_FROM_NAME` khi dùng nhà cung cấp SMTP khác.

## Bật chatbot AI (tùy chọn)

Mặc định hệ thống dùng chatbot nội bộ 21A và không gọi dịch vụ ngoài. Để bật
21B, tạo OpenAI API key riêng rồi thêm biến môi trường ở backend:

```powershell
$env:CHATBOT_AI_ENABLED = "true"
$env:OPENAI_API_KEY = "<OPENAI_API_KEY_CUA_BAN>"
$env:OPENAI_MODEL = "gpt-5.4-nano"
.\mvnw.cmd spring-boot:run
```

Không ghi API key vào source code, JavaScript hay commit Git. Khi OpenAI timeout,
giới hạn yêu cầu hoặc trả dữ liệu sai cấu trúc, ứng dụng tự dùng lại 21A. Xem
[`docs/PHASE-21B.md`](docs/PHASE-21B.md) để cấu hình IntelliJ và quy tắc an toàn.

FAQ đang hoạt động được kiểm tra trước OpenAI. Admin quản lý FAQ và xem thống kê
tại `/admin/chatbot`. Có thể đổi thời hạn dọn nhật ký mặc định 30 ngày bằng:

```powershell
$env:CHATBOT_ANALYTICS_RETENTION_DAYS = "30"
```

Xem [`docs/PHASE-21C.md`](docs/PHASE-21C.md) để biết dữ liệu nào được lưu/không
được lưu và cách kiểm thử.

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
| `/api/chatbot/messages` | Chatbot AI hoặc 21A fallback, chỉ gợi ý hàng còn bán | Công khai, giới hạn theo session |
| `/news` | Trang tin tức dự phòng | Công khai |
| `/register`, `/login` | Thành viên | Công khai |
| `/forgot-password`, `/reset-password` | Đặt lại mật khẩu | Công khai |
| `/activate-account`, `/resend-activation` | Kích hoạt email | Công khai |
| `/account` | Hồ sơ cá nhân | Đã đăng nhập |
| `/wishlist` | Sản phẩm yêu thích | Đã đăng nhập |
| `/cart`, `/checkout` | Giỏ hàng và đặt hàng | Đã đăng nhập |
| `/my-orders` | Lịch sử đơn hàng | Đã đăng nhập |
| `/admin` | Dashboard | `ADMIN` |
| `/admin/products` | Quản lý sản phẩm và lọc theo thương hiệu | `ADMIN` |
| `/admin/brands` | Quản lý thương hiệu hiển thị với khách hàng | `ADMIN` |
| `/admin/suppliers` | Quản lý nhà cung cấp phục vụ nhập hàng | `ADMIN` |
| `/admin/inventory` | Tổng quan và cảnh báo tồn kho | `ADMIN` |
| `/admin/inventory/documents` | Lập, sửa nháp và xác nhận phiếu kho | `ADMIN` |
| `/admin/inventory/history` | Lịch sử biến động kho | `ADMIN` |
| `/admin/categories` | Quản lý danh mục | `ADMIN` |
| `/admin/orders` | Quản lý đơn hàng | `ADMIN` |
| `/admin/reports` | Thống kê kinh doanh và tồn kho | `ADMIN` |
| `/admin/reports/export.csv` | Xuất báo cáo theo khoảng ngày | `ADMIN` |
| `/admin/users` | Quản lý tài khoản | `ADMIN` |
| `/admin/reviews` | Tìm kiếm, lọc và kiểm duyệt đánh giá | `ADMIN` |
| `/admin/reviews/{id}` | Xem ảnh/nội dung và duyệt, ẩn, xóa mềm | `ADMIN` |
| `/review-images/{id}` | Ảnh đã duyệt; ảnh riêng kiểm tra chủ sở hữu/Admin | Theo trạng thái đánh giá |
| `/admin/chatbot` | Thống kê hội thoại đã làm sạch và dọn dữ liệu quá hạn | `ADMIN` |
| `/admin/chatbot/faqs` | Thêm, sửa, ẩn/kích hoạt FAQ chính thức | `ADMIN` |

## Kiểm thử

```powershell
.\mvnw.cmd clean test
```

Kết quả đã xác minh:

```text
Tests run: 241, Failures: 0, Errors: 0, Skipped: 0
JavaScript: 15 tests passed
BUILD SUCCESS
```

Test bao phủ Controller, Service, DTO Validation, Security, Thymeleaf, CAPTCHA,
làm sạch HTML, nghiệp vụ giỏ hàng/đơn hàng, danh mục quản trị, phiếu kho và sổ
giao dịch kho, chatbot rule-based, lớp AI có kiểm chứng dữ liệu, FAQ ưu tiên,
che thông tin nhạy cảm và giao diện thống kê Chatbot Admin.

Giai đoạn 19 bổ sung kiểm thử database H2 cô lập (không truy cập MySQL thật),
chống gửi đồng thời/trùng chi tiết đơn, kiểm duyệt, bảo vệ ảnh riêng, upload và
render Thymeleaf. Migration đã chạy thành công trên MySQL local ngày 04/09/2026;
đã đối chiếu cấu trúc và số lượng dữ liệu trước/sau. Máy cài đặt khác vẫn cần chạy script nâng cấp.
Kiểm thử JavaScript: `node --test src/test/js/*.test.cjs`.

## Bảo mật cấu hình

- Mật khẩu MySQL, Admin, SMTP, OAuth và OpenAI API key được đọc từ biến môi trường.
- `.idea`, `target`, `.env` và file private key không được đưa vào Git.
- Thông tin nhận chuyển khoản nằm trong biến môi trường hoặc
  `.bank-transfer.local.properties` (đã bỏ qua trong Git); không đưa backup/database thật lên GitHub.
- CSRF được giữ cho các thao tác thay đổi dữ liệu.
- Tổng tiền và tồn kho luôn được kiểm tra lại tại backend.
- Không dùng tài khoản hoặc mật khẩu ví dụ làm thông tin đăng nhập thật.

Nếu một secret từng được commit, cần thu hồi hoặc thay đổi secret đó; chỉ xóa
khỏi file là chưa đủ.

## Lộ trình 1–21

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
| 11 | Quản lý tài khoản và Google Login | Hoàn thành |
| 12 | Email giao dịch | Hoàn thành |
| 13 | VietQR động, khách báo đã chuyển, Admin đối soát thủ công và biên nhận | Hoàn thành; đối soát tự động để dành cho mở rộng sau |
| 14 | Tiện ích giao diện nâng cao | Hoàn thành |
| 15 | Thống kê kinh doanh | Hoàn thành (ưu tiên trước) |
| 16 | Phân quyền nâng cao | Dự kiến |
| 17 | Wishlist, lịch sử xem và gợi ý cơ bản | Hoàn thành |
| 18 | Quản lý kho và lịch sử biến động | Hoàn thành |
| 18A | Chuẩn hóa Admin: SKU, thương hiệu, nhà cung cấp và phiếu kho | Hoàn thành |
| 18B | Hoàn thiện chi tiết sản phẩm và dữ liệu phía website | Hoàn thành |
| 19 | Đánh giá sản phẩm nâng cao | Đã triển khai, kiểm thử và cập nhật MySQL local; chờ review trên web |
| 20 | Voucher, khuyến mãi và Flash Sale | Đã triển khai, kiểm thử và cập nhật MySQL local; chờ review trên web |
| 21A | Chatbot FAQ và tìm sản phẩm từ MySQL | Hoàn thành |
| 21B | Chatbot AI hiểu câu hỏi mở và gợi ý sản phẩm có kiểm chứng | Hoàn thành |
| 21C | Quản trị FAQ, thống kê hội thoại và chính sách lưu trữ riêng tư | Hoàn thành |

Tài liệu kỹ thuật nằm trong [`docs`](docs/). Bản README phát triển đầy đủ trước
khi tối ưu cho GitHub được giữ nguyên tại
[`docs/README-DEVELOPMENT-ARCHIVE.md`](docs/README-DEVELOPMENT-ARCHIVE.md).

## Định hướng tiếp theo

1. Review voucher, khuyến mãi và Flash Sale của giai đoạn 20 trên web.
2. Phân quyền chi tiết cho `MANAGER` và `STAFF` (Giai đoạn 16).
3. Hoàn thiện cổng thanh toán/đối soát tự động khi quyết định triển khai lại phần thanh toán.
4. Tối ưu tìm kiếm/gợi ý dựa trên dữ liệu sử dụng sau khi có đủ dữ liệu thử nghiệm.

## Bản quyền giao diện

Giao diện public được chuyển đổi và tùy chỉnh từ template HTML Codex; giao diện
quản trị được tùy chỉnh từ adminHMD. Tài nguyên bên thứ ba giữ nguyên giấy phép
tương ứng trong project.

## Tác giả

Dự án cá nhân phục vụ học tập, đồ án và ứng tuyển thực tập Java Backend.
