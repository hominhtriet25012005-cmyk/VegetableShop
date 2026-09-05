# Giai đoạn 10 - Giỏ hàng AJAX và trải nghiệm người dùng

## Mục tiêu

- Thêm sản phẩm từ `/shop` và `/product/{id}` mà không tải lại trang.
- Cập nhật badge giỏ hàng ngay sau mỗi thao tác.
- Tăng, giảm, nhập số lượng và cập nhật thành tiền tại `/cart` bằng AJAX.
- Xóa sản phẩm với SweetAlert2 và hiệu ứng biến mất.
- Hiệu ứng ảnh sản phẩm bay về biểu tượng giỏ hàng.
- Vẫn dùng form POST cũ làm fallback khi JavaScript không hoạt động.
- Giữ kiểm tra quyền sở hữu, trạng thái Product và tồn kho ở backend.
- Giữ CSRF cho POST, PATCH và DELETE.

## API

| Method | URL | Chức năng |
| --- | --- | --- |
| `POST` | `/api/cart/items` | Thêm/cộng dồn sản phẩm |
| `PATCH` | `/api/cart/items/{itemId}` | Cập nhật số lượng |
| `DELETE` | `/api/cart/items/{itemId}` | Xóa dòng hàng |

Mọi API yêu cầu `ROLE_USER` hoặc `ROLE_ADMIN`. Lỗi nghiệp vụ trả HTTP 400 với
JSON `{ "message": "..." }`; chưa đăng nhập bị chuyển tới `/login` và thiếu
CSRF nhận HTTP 403.

## Dữ liệu phản hồi

`CartMutationResponse` gồm ID dòng hàng, số lượng, tồn kho, thành tiền của dòng,
tổng số lượng, tổng tiền, cờ giỏ trống và thông báo. Tổng tiền được truy vấn và
tính lại phía server; JavaScript không gửi đơn giá hoặc tổng tiền lên API.

## Progressive enhancement

Các form HTML cũ vẫn trỏ tới `/cart/items/**`. `cart.js` chỉ chặn submit khi chạy
được và gọi `/api/cart/**`. Nếu JavaScript hoặc CDN SweetAlert2 không tải được,
người dùng vẫn thao tác bằng form truyền thống; thông báo xác nhận có fallback
sang `window.confirm`/`window.alert`.

Hiệu ứng tôn trọng `prefers-reduced-motion`, giúp người dùng đã tắt chuyển động
trong hệ điều hành không bị ép xem animation.

## File chính

```text
src/main/java/com/vegetableshop/controller/CartApiController.java
src/main/java/com/vegetableshop/dto/CartMutationResponse.java
src/main/java/com/vegetableshop/dto/CartApiError.java
src/main/resources/static/js/cart.js
src/main/resources/templates/cart.html
src/main/resources/templates/shop.html
src/main/resources/templates/shop-detail.html
```

## Kiểm thử

```powershell
.\mvnw.cmd clean package
```

Kết quả xác minh:

```text
Tests run: 85, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Ngoài test các giai đoạn trước, Giai đoạn 10 kiểm tra Cart API, lỗi tồn kho,
tổng tiền backend, đăng nhập, CSRF và marker AJAX trong template. `node --check`
cũng xác nhận `cart.js` không có lỗi cú pháp.
