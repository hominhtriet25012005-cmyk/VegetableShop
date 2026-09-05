# Thẻ sản phẩm thu gọn

Cập nhật ngày 03/09/2026 theo thiết kế đã thống nhất.

## Phạm vi

- Trang chủ, danh sách sản phẩm, hàng cùng danh mục, cùng thương hiệu, gợi ý và đã xem.
- Dùng chung fragment `templates/fragments/product-card.html` để đồng bộ cách hiển thị.
- Không thay đổi database, Admin, giá, tồn kho hay dữ liệu mô tả.

## Hành vi

- Bỏ nút “Xem chi tiết”; nhấn ảnh hoặc tên vẫn mở đúng `/product/{id}`.
- Bỏ mô tả trên thẻ; giữ mô tả đầy đủ ở trang chi tiết sản phẩm.
- Icon trái tim ở góc trên phải, có nhãn hỗ trợ đọc màn hình và trạng thái đã lưu.
- Thêm/bỏ yêu thích bằng AJAX, đồng bộ những thẻ cùng sản phẩm và số lượng trên menu.
- Khách chưa đăng nhập được chuyển đến trang đăng nhập khi mua hoặc yêu thích.
- Nút “Thêm vào giỏ” giữ form có mã sản phẩm, số lượng và CSRF; vô hiệu khi hết hàng.
- Giá và đơn vị bán lấy từ sản phẩm; trạng thái ngắn gọn “Còn hàng”/“Hết hàng”.
- Ảnh giữ tỷ lệ, không cắt; tên giới hạn hai dòng; nút mua được căn cuối thẻ.
- Lý do gợi ý ở nhóm đề xuất vẫn được giữ, không phải phần mô tả sản phẩm.

## Kiểm thử

- Toàn bộ Java: 195 tests, 0 lỗi, 0 thất bại, 0 bỏ qua.
- JavaScript: 5 tests thành công, gồm lọc danh mục và thao tác trái tim.
- Render template kiểm tra người đăng nhập/khách, liên kết ảnh/tên, form giỏ hàng có
  `productId`/`quantity`/CSRF, trạng thái hết hàng, icon yêu thích và mô tả trang chi tiết.
- Wishlist JS kiểm tra POST/DELETE, CSRF, cập nhật mọi icon cùng sản phẩm, xử lý lỗi
  và chặn bấm lặp trong lúc đang gửi yêu cầu.
- Kiểm tra trình duyệt dùng HTML render từ dữ liệu giả, không ghi vào MySQL thật.

Chạy lại kiểm thử từ thư mục project (PowerShell):

```powershell
.\mvnw.cmd test
node --test src/test/js/*.test.cjs
```

## Kiểm tra trên website của bạn

1. Khởi động lại Spring Boot, mở trang chủ và nhấn Ctrl+F5 để tải CSS/JS mới.
2. Chuyển các danh mục tại trang chủ; kiểm tra thẻ trên trang Sản phẩm và các nhóm liên quan.
3. Nhấn ảnh/tên để mở đúng sản phẩm; kiểm tra mô tả vẫn còn ở trang chi tiết.
4. Sau khi đăng nhập, thêm/bỏ trái tim và kiểm tra trang Yêu thích cùng số lượng trên menu.
5. Thêm sản phẩm còn hàng vào giỏ; kiểm tra số lượng và không gửi thiếu mã sản phẩm.
6. Kiểm tra sản phẩm hết hàng, tên dài và đơn vị bán khác kg trên điện thoại/máy tính.

Không cần chạy SQL hoặc nhập lại dữ liệu. Bản sao mã nguồn trước/sau chỉnh sửa nằm
ngoài project trong `D:\JavaProjects\TrietHo\backups`; không phải bản sao dữ liệu MySQL.
