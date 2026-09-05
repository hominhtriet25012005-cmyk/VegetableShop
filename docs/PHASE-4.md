# Giai đoạn 4 - Hoàn thiện Shop và Product Detail

## 1. Chức năng đã triển khai

Trang `/shop` hỗ trợ đồng thời các query parameter:

| Tham số | Ý nghĩa | Ví dụ |
| --- | --- | --- |
| `keyword` | Tìm gần đúng theo tên | `Cam` |
| `categoryId` | Lọc theo Category | `1` |
| `minPrice` | Giá thấp nhất | `30000` |
| `maxPrice` | Giá cao nhất | `80000` |
| `sort` | Cách sắp xếp | `priceAsc` |
| `page` | Trang, bắt đầu từ 0 | `1` |
| `size` | Số phần tử: 3/6/9/12 | `3` |

Các giá trị `sort` hợp lệ:

```text
newest
priceAsc
priceDesc
nameAsc
```

Trang `/product/{id}` hiển thị dữ liệu Product thật, Category, giá, tồn kho, mô
tả, ba sản phẩm mới và tối đa bốn sản phẩm liên quan cùng Category.

## 2. Luồng tìm kiếm và phân trang

```text
GET /shop + query parameters
        ↓
ProductController nhận ProductFilter
        ↓
ProductService chuẩn hóa điều kiện và tạo PageRequest
        ↓
ProductSpecifications tạo Predicate động
        ↓
ProductRepository.findAll(Specification, Pageable)
        ↓
MySQL lọc + sắp xếp + phân trang
        ↓
Page<Product>
        ↓
shop.html hiển thị dữ liệu và nút chuyển trang
```

Việc lọc diễn ra ở MySQL, không tải toàn bộ Product về Java rồi mới lọc. Điều
này quan trọng khi dữ liệu lớn hơn.

## 3. Vai trò các lớp mới

### ProductFilter

DTO nhận dữ liệu từ query string. Method `normalize()`:

- bỏ khoảng trắng thừa;
- giới hạn keyword tối đa 100 ký tự;
- loại bỏ ID và giá âm;
- hoán đổi min/max nếu người dùng nhập ngược;
- chỉ chấp nhận page size 3, 6, 9 hoặc 12;
- đưa sort không hợp lệ về `newest`.

### ProductSpecifications

Tạo câu điều kiện JPA Criteria tùy theo field nào được nhập. Product và Category
phải cùng đang hoạt động. Các điều kiện không nhập sẽ không xuất hiện trong
query.

### ProductService

Service tạo `PageRequest`, quyết định Sort và gọi Repository. Sort luôn thêm
`id` làm tiêu chí phụ để kết quả phân trang ổn định khi nhiều sản phẩm có cùng
giá hoặc thời gian tạo.

### ProductController

Controller không tự lọc dữ liệu. Nó chỉ nhận request, gọi Service và đưa các
object sau vào Model:

```text
filter
productPage
products
featuredProducts
categories
```

Với trang chi tiết:

```text
product
relatedProducts
featuredProducts
categories
```

### GlobalExceptionHandler

Nếu `/product/{id}` không tồn tại hoặc Product/Category đã bị vô hiệu hóa,
`ProductNotFoundException` được đổi thành HTTP 404 và giao diện
`templates/error/404.html`; người dùng không thấy stack trace thô.

## 4. Thymeleaf

`shop.html` dùng form GET nên bộ lọc có thể bookmark và gửi cho người khác. Các
link phân trang giữ lại keyword, Category, khoảng giá, sort và size.

Mỗi Product dẫn tới:

```html
<a th:href="@{/product/{id}(id=${product.id})}">Xem chi tiết</a>
```

Trang `shop-detail.html` chỉ hiển thị dữ liệu do Controller cấp. Nút thêm giỏ
hàng vẫn bị khóa vì nghiệp vụ Cart thuộc Giai đoạn 6.

## 5. File đã tạo

```text
docs/PHASE-4.md
src/main/java/com/vegetableshop/dto/ProductFilter.java
src/main/java/com/vegetableshop/exception/GlobalExceptionHandler.java
src/main/java/com/vegetableshop/exception/ProductNotFoundException.java
src/main/java/com/vegetableshop/repository/ProductSpecifications.java
src/main/resources/templates/shop-detail.html
src/test/java/com/vegetableshop/controller/ProductTemplateRenderingTests.java
src/test/java/com/vegetableshop/dto/ProductFilterTests.java
```

## 6. File đã sửa

```text
README.md
src/main/java/com/vegetableshop/controller/HomeController.java
src/main/java/com/vegetableshop/controller/ProductController.java
src/main/java/com/vegetableshop/controller/TemplateModeController.java
src/main/java/com/vegetableshop/repository/ProductRepository.java
src/main/java/com/vegetableshop/service/ProductService.java
src/main/resources/templates/error/404.html
src/main/resources/templates/shop.html
src/test/java/com/vegetableshop/controller/ProductControllerTests.java
src/test/java/com/vegetableshop/service/ProductServiceTests.java
```

## 7. Cách kiểm tra

Build:

```powershell
cd D:\JavaProjects\TrietHo\VegetableShop
.\mvnw.cmd clean package
```

Chạy IntelliJ với Active profiles là `mysql`, sau đó thử:

```text
http://localhost:8081/shop
http://localhost:8081/shop?keyword=Cam
http://localhost:8081/shop?categoryId=1
http://localhost:8081/shop?minPrice=40000&maxPrice=80000
http://localhost:8081/shop?sort=priceDesc
http://localhost:8081/shop?size=3&page=1
http://localhost:8081/product/1
http://localhost:8081/product/999999
```

Kỳ vọng URL cuối trả HTTP 404 với trang lỗi thân thiện. Các URL Shop còn lại
phải giữ đúng bộ lọc khi chuyển trang.

## 8. Phạm vi chưa làm

- Đăng ký, đăng nhập, BCrypt và role: Giai đoạn 5.
- Thêm vào giỏ và kiểm tra số lượng mua: Giai đoạn 6.
- Review thật: Giai đoạn 9.
- CRUD Product/Category cho Admin: Giai đoạn 8.
