# Giai đoạn 14 – Tiện ích giao diện nâng cao

Giai đoạn này hoàn thiện các tiện ích hỗ trợ trải nghiệm người dùng và quản trị,
không thay đổi schema MySQL.

## Chức năng đã triển khai

### SweetAlert2 trong Admin

- Hiển thị flash message thành công/lỗi dạng toast.
- Xác nhận trước các thao tác đổi trạng thái sản phẩm, danh mục, tài khoản và
  đơn hàng.
- Khi chuyển đơn sang `CANCELLED`, nội dung xác nhận cảnh báo việc hoàn tồn kho.
- Nếu CDN không tải được, hệ thống tự dùng `window.confirm` và Bootstrap alert.

### Summernote cho mô tả sản phẩm

- Form thêm/sửa sản phẩm có trình soạn thảo mô tả dài.
- Hỗ trợ tiêu đề, chữ đậm/nghiêng, danh sách và liên kết.
- Nếu CDN không tải được, textarea thường vẫn hoạt động.
- Backend làm sạch HTML bằng jsoup trước khi lưu; script, event handler, iframe,
  ảnh nhúng và giao thức URL nguy hiểm bị loại bỏ để chống stored XSS.
- Trang chủ, danh sách và chi tiết sản phẩm chỉ render nội dung đã làm sạch.

### CAPTCHA không cần API key

- Áp dụng cho đăng ký và quên mật khẩu trong profile `mysql`.
- Phép cộng ngẫu nhiên được lưu trong session, hết hạn sau 10 phút và chỉ dùng
  được một lần.
- Backend luôn kiểm tra kết quả; thử lại hoặc nhập sai sẽ nhận thử thách mới.

### Bản đồ và trang liên hệ

- Trang `/contact` đã được Việt hóa và loại dữ liệu mẫu New York.
- Google Maps dùng chế độ embed theo khu vực TP. Hồ Chí Minh, không cần API key.
- Vì chưa có địa chỉ cửa hàng thực tế, giao diện ghi rõ đây là khu vực phục vụ,
  không giả lập một địa chỉ cụ thể.

### Chart.js

- Biểu đồ doanh thu theo tháng và doanh thu theo danh mục của Giai đoạn 15 tiếp
  tục dùng Chart.js.
- Bảng số liệu và xuất CSV vẫn là fallback khi thư viện biểu đồ không tải được.

## Bảo mật và nguyên tắc vận hành

- CAPTCHA bổ sung chống bot, không thay thế CSRF hoặc giới hạn tần suất ở hạ tầng.
- Không tin HTML do trình duyệt gửi lên; dữ liệu được sanitize tại Service.
- Không dùng Google Maps API key nên không phát sinh secret mới trong repository.
- Các thư viện giao diện dùng CDN và đều có fallback chức năng cơ bản.

## Kiểm thử

- CAPTCHA đúng, sai, hết hạn và chống dùng lại.
- jsoup giữ định dạng hợp lệ và loại nội dung thực thi/URL nguy hiểm.
- Thymeleaf render Summernote, SweetAlert2, CAPTCHA và bản đồ tiếng Việt.
- Toàn bộ test dự án: `144` test, không lỗi.

Lệnh kiểm tra:

```powershell
.\mvnw.cmd clean test
```
