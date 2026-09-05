# Giai đoạn 12 – Email giao dịch

## Phạm vi đã triển khai

- Gửi email xác minh và kích hoạt tài khoản sau đăng ký.
- Cho phép gửi lại email kích hoạt nhưng không tiết lộ email có tồn tại hay không.
- Gửi liên kết đặt lại mật khẩu.
- Gửi email HTML xác nhận đơn hàng, gồm sản phẩm, tổng tiền và địa chỉ nhận.
- Gửi thông báo khi Admin thay đổi trạng thái đơn hàng.
- Email chỉ được gửi sau khi transaction database commit thành công.
- SMTP lỗi được ghi log nhưng không rollback tài khoản, đơn hàng hoặc trạng thái đã lưu.

Email quảng cáo chưa triển khai. Chức năng này cần thêm cơ chế đồng ý nhận thư,
hủy đăng ký và quản lý danh sách nhận trước khi sử dụng.

## Thay đổi database

Chạy một lần với database hiện có:

```text
database/phase-12-email-migration.sql
```

Migration thêm:

- `users.email_verified`: tách trạng thái xác minh email khỏi trạng thái khóa tài khoản.
- `account_activation_tokens`: lưu token kích hoạt đã băm SHA-256.

Tài khoản tồn tại trước giai đoạn 12 được giữ `email_verified = TRUE`, vì vậy
không bị mất quyền đăng nhập sau migration.

## Bật SMTP Gmail

1. Bật xác minh hai bước trên tài khoản Google dùng để gửi thư.
2. Tạo App Password riêng cho ứng dụng. Không dùng mật khẩu Gmail thông thường.
3. Thiết lập biến môi trường trong PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql,mail"
$env:MAIL_USERNAME = "your-email@gmail.com"
$env:MAIL_PASSWORD = "<APP_PASSWORD_16_KY_TU>"
$env:APP_BASE_URL = "http://localhost:8081"
.\mvnw.cmd spring-boot:run
```

Thông số mặc định:

```text
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
STARTTLS=true
```

Google yêu cầu bật xác minh hai bước trước khi tạo App Password:
[Google Account Help – App Passwords](https://support.google.com/accounts/answer/185833).

Spring Boot tự cấu hình `JavaMailSender` khi có starter mail và thông tin SMTP:
[Spring Boot – Sending Email](https://docs.spring.io/spring-boot/reference/io/email.html).

## Biến môi trường tùy chọn

| Biến | Mặc định | Mục đích |
| --- | --- | --- |
| `MAIL_HOST` | `smtp.gmail.com` | Máy chủ SMTP |
| `MAIL_PORT` | `587` | Cổng STARTTLS |
| `MAIL_FROM` | `MAIL_USERNAME` | Địa chỉ From |
| `MAIL_FROM_NAME` | `Vegetable Shop` | Tên người gửi |
| `APP_BASE_URL` | `http://localhost:8081` | Gốc URL trong email |
| `ACCOUNT_ACTIVATION_TOKEN_HOURS` | `24` | Thời hạn link kích hoạt |
| `PASSWORD_RESET_TOKEN_MINUTES` | `30` | Thời hạn link đặt lại mật khẩu |

Không commit `MAIL_PASSWORD`, App Password hoặc file `.env` lên GitHub.

## Luồng hoạt động

### Đăng ký và kích hoạt

1. Người dùng đăng ký khi profile `mail` đang bật.
2. Tài khoản được tạo với `status = TRUE`, `email_verified = FALSE`.
3. Token ngẫu nhiên 256 bit được tạo; database chỉ lưu SHA-256 của token.
4. Email được gửi sau khi transaction commit.
5. Người dùng mở `/activate-account?token=...` để xác minh email.
6. Token được đánh dấu đã sử dụng và tài khoản có thể đăng nhập.

`status` vẫn dành cho Admin khóa/mở tài khoản. Email kích hoạt không thể mở lại
tài khoản đã bị Admin khóa.

### Đơn hàng

- Khi đặt đơn thành công, sự kiện email chứa snapshot dữ liệu đơn được phát.
- Khi Admin đổi trạng thái, khách nhận email trạng thái tiếng Việt.
- Listener bắt lỗi SMTP để tránh tình trạng đơn đã lưu nhưng người dùng thấy lỗi
  và đặt lại đơn lần nữa.

## Route mới

| Route | Chức năng | Quyền |
| --- | --- | --- |
| `/activate-account?token=...` | Kích hoạt tài khoản | Công khai |
| `/resend-activation` | Gửi lại link kích hoạt | Công khai |

## Kiểm thử

- Token kích hoạt: tạo, băm, hết hạn, dùng một lần.
- Tài khoản chưa xác minh không thể đăng nhập hoặc đặt lại mật khẩu.
- Workflow chỉ yêu cầu kích hoạt khi email được bật.
- Email đơn hàng được phát khi tạo đơn và đổi trạng thái.
- Bốn template HTML được render bằng Spring Thymeleaf.
- Lỗi SMTP không làm lỗi nghiệp vụ đã commit.

Kết quả sau giai đoạn 12:

```text
Tests run: 124, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
