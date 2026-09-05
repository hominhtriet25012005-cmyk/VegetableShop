# Giai đoạn 3 - MySQL, Entity, Repository và Shop động

## 1. Mục tiêu đã hoàn thành

Giai đoạn này tạo ba miền dữ liệu đầu tiên: `User`, `Category`, `Product`; kết
nối chúng với MySQL bằng Spring Data JPA; thêm dữ liệu mẫu; và hiển thị dữ liệu
từ database trên `/shop`.

Luồng code được giữ đúng kiến trúc:

```text
HTTP request
  → Controller
  → Service
  → Repository
  → Entity
  → MySQL
```

Controller chỉ nhận request và chuẩn bị Model. Service là nơi đặt quy tắc nghiệp
vụ. Repository chịu trách nhiệm truy vấn. Entity ánh xạ object Java với bảng.

## 2. Entity và annotation quan trọng

### BaseEntity

`BaseEntity` chứa `createdAt` và `updatedAt` dùng chung:

- `@MappedSuperclass`: các field được kế thừa vào bảng của Entity con.
- `@PrePersist`: tự đặt thời gian khi insert.
- `@PreUpdate`: tự cập nhật thời gian trước khi update.

### User

`User` ánh xạ bảng `users`. Email có unique constraint, role được lưu dạng chữ
`USER`/`ADMIN`, password dài 255 ký tự để chuẩn bị lưu BCrypt ở Giai đoạn 5.

### Category và Product

Quan hệ được khai báo hai chiều:

```text
Category 1 ──────── n Product
```

- `Category.products` dùng `@OneToMany(mappedBy = "category")`.
- `Product.category` dùng `@ManyToOne` và `@JoinColumn(category_id)`.
- Quan hệ dùng lazy loading để không tải dữ liệu dư thừa.
- Repository Product dùng `@EntityGraph("category")` cho trang Shop, nhờ đó
  Category cần hiển thị được lấy cùng truy vấn và vẫn hoạt động khi
  `spring.jpa.open-in-view=false`.

Các field bắt buộc được kiểm tra bằng Bean Validation như `@NotBlank`,
`@NotNull`, `@Email`, `@Size`, `@Min` và `@DecimalMin`.

## 3. Repository

Ba Repository kế thừa `JpaRepository`, nên có sẵn các thao tác như `findById`,
`findAll`, `save` và `deleteById` mà không cần tự viết SQL thông thường.

Truy vấn chính của trang Shop:

```java
@EntityGraph(attributePaths = "category")
List<Product> findByStatusTrueOrderByCreatedAtDesc();
```

Tên method cho Spring Data biết cần lấy Product đang hoạt động và sắp xếp mới
nhất trước.

## 4. Service

`ProductService`, `CategoryService`, `UserService` dùng constructor injection và
`@Transactional(readOnly = true)` cho thao tác đọc. Chúng chỉ được khởi tạo khi
profile `mysql` hoạt động.

Ví dụ:

```java
@Transactional(readOnly = true)
public List<Product> findAllActiveProducts() {
    return productRepository.findByStatusTrueOrderByCreatedAtDesc();
}
```

Service tách Controller khỏi cách dữ liệu được truy vấn. Khi thêm search,
pagination hoặc quy tắc tồn kho, Controller không phải chứa các chi tiết đó.

## 5. Controller và Thymeleaf

Ở profile `mysql`, `ProductController` xử lý `/shop`:

```java
@GetMapping("/shop")
public String shop(Model model) {
    List<Product> products = productService.findAllActiveProducts();
    model.addAttribute("products", products);
    model.addAttribute("featuredProducts", products.stream().limit(3).toList());
    model.addAttribute("categories", categoryService.findAllActiveCategories());
    return "shop";
}
```

`shop.html` đọc dữ liệu bằng:

```html
<div th:each="product : ${products}">
    <h4 th:text="${product.name}">Tên sản phẩm</h4>
    <span th:text="${product.category.name}">Danh mục</span>
</div>
```

Ở profile mặc định `template`, `TemplateModeController` truyền ba danh sách
rỗng để có thể kiểm tra giao diện mà không cần biết mật khẩu MySQL. Đây không
phải dữ liệu giả của shop; giao diện sẽ báo chưa có dữ liệu.

## 6. Dữ liệu mẫu

Có hai cách tạo dữ liệu:

1. Chạy toàn bộ `database/vegetable_shop.sql` để tạo database, 8 bảng và dữ liệu.
2. Khi profile `mysql` khởi động, `data-mysql.sql` bổ sung 3 Category và 5 Product
   bằng `INSERT IGNORE`.

Không thêm User mẫu vì password phải là chuỗi BCrypt; chức năng này thuộc Giai
đoạn 5.

## 7. File đã tạo

```text
docs/PHASE-3.md
src/main/java/com/vegetableshop/entity/BaseEntity.java
src/main/java/com/vegetableshop/entity/Role.java
src/main/java/com/vegetableshop/entity/User.java
src/main/java/com/vegetableshop/entity/Category.java
src/main/java/com/vegetableshop/entity/Product.java
src/main/java/com/vegetableshop/repository/UserRepository.java
src/main/java/com/vegetableshop/repository/CategoryRepository.java
src/main/java/com/vegetableshop/repository/ProductRepository.java
src/main/java/com/vegetableshop/service/UserService.java
src/main/java/com/vegetableshop/service/CategoryService.java
src/main/java/com/vegetableshop/service/ProductService.java
src/main/java/com/vegetableshop/controller/ProductController.java
src/main/java/com/vegetableshop/controller/TemplateModeController.java
src/main/resources/data-mysql.sql
src/test/java/com/vegetableshop/controller/ProductControllerTests.java
src/test/java/com/vegetableshop/service/ProductServiceTests.java
```

## 8. File đã sửa

```text
README.md
src/main/java/com/vegetableshop/config/SecurityConfig.java
src/main/java/com/vegetableshop/controller/HomeController.java
src/main/resources/application.properties
src/main/resources/application-mysql.properties
src/main/resources/application-template.properties
src/main/resources/templates/shop.html
```

## 9. Cách kiểm tra

Build và test:

```powershell
cd D:\JavaProjects\TrietHo\VegetableShop
.\mvnw.cmd clean package
```

Kiểm tra MySQL:

```sql
USE vegetable_shop;
SELECT COUNT(*) FROM categories;
SELECT COUNT(*) FROM products;
SELECT p.name, p.price, p.quantity, c.name AS category
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE p.status = TRUE
ORDER BY p.created_at DESC;
```

Kỳ vọng ít nhất 3 Category và 5 Product mẫu. Sau đó chạy ứng dụng với profile
`mysql` và mở `http://localhost:8081/shop`.

## 10. Phạm vi chưa làm

- Search, filter, pagination và `/product/{id}`: Giai đoạn 4.
- Register/login/BCrypt/role thực tế: Giai đoạn 5.
- Add to Cart: Giai đoạn 6.
- Entity cho Cart/Order/Review: các giai đoạn tương ứng sau đó. Database đã chuẩn
  bị các bảng nhưng Java chưa ánh xạ chúng ở Giai đoạn 3.
