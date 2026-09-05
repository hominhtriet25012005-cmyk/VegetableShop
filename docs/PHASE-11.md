# Giai đoạn 11 – Tài khoản và Google Login

## Phạm vi

Đã triển khai:

- Xem và cập nhật họ tên, số điện thoại, địa chỉ tại `/account`.
- Đổi mật khẩu tại `/account/change-password`.
- Tài khoản tạo bằng Google có thể tạo thêm mật khẩu cục bộ.
- Quên mật khẩu và đặt lại mật khẩu bằng token có thời hạn.
- Google Login bằng OpenID Connect.
- Liên kết tài khoản cục bộ với Google khi Google trả về cùng email đã xác minh.

Không triển khai Facebook Login theo phạm vi hiện tại.

## Bảo mật

- Token đặt lại mật khẩu được tạo bằng `SecureRandom` 256 bit.
- Database chỉ lưu SHA-256 của token, không lưu token gốc.
- Token mặc định hết hạn sau 30 phút và chỉ dùng một lần.
- Yêu cầu quên mật khẩu luôn trả cùng một thông báo để tránh dò email.
- Chỉ chấp nhận email Google đã được xác minh.
- Quyền sau Google Login được lấy từ database, không lấy từ Google.
- Client ID và Client Secret chỉ được đọc từ biến môi trường.

## Migration database hiện có

Chạy một lần:

```text
database/phase-11-account-migration.sql
```

Migration cho phép `users.password` rỗng đối với tài khoản chỉ dùng Google, thêm
`auth_provider`, `oauth_subject` và bảng `password_reset_tokens`.

## Cấu hình Google Login

1. Tạo OAuth Client loại **Web application** trong Google Cloud Console.
2. Thêm Authorized redirect URI:

```text
http://localhost:8081/login/oauth2/code/google
```

3. Thiết lập biến môi trường:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql,google"
$env:GOOGLE_CLIENT_ID = "<GOOGLE_CLIENT_ID>"
$env:GOOGLE_CLIENT_SECRET = "<GOOGLE_CLIENT_SECRET>"
.\mvnw.cmd spring-boot:run
```

Nếu không bật profile `google`, website vẫn hoạt động bình thường và nút Google
không xuất hiện.

## Quên mật khẩu trước Giai đoạn 12

Giai đoạn 12 sẽ gửi liên kết qua email. Để kiểm thử cục bộ trước khi có email,
có thể bật tạm liên kết demo:

```powershell
$env:PASSWORD_RESET_DEMO_LINK_ENABLED = "true"
```

Không bật tùy chọn này trên môi trường public hoặc production.

## Route mới

| Route | Chức năng | Quyền |
| --- | --- | --- |
| `/account` | Xem/cập nhật hồ sơ | Đã đăng nhập |
| `/account/change-password` | Đổi hoặc tạo mật khẩu cục bộ | Đã đăng nhập |
| `/forgot-password` | Tạo yêu cầu đặt lại mật khẩu | Công khai |
| `/reset-password?token=...` | Đặt mật khẩu mới | Công khai |
| `/oauth2/authorization/google` | Bắt đầu Google Login | Công khai |
| `/login/oauth2/code/google` | Callback từ Google | Công khai |
