# Giai đoạn 13 — Chuyển khoản ngân hàng bằng VietQR

> Phiên bản hiện tại sử dụng đối soát thủ công bởi Admin. Tích hợp tự động với
> dịch vụ thông báo giao dịch đã được gỡ và để dành cho phần mở rộng sau.

## Phạm vi đã triển khai

Giữ COD; bổ sung lựa chọn **Chuyển khoản ngân hàng (QR)** tại checkout.
QR được tạo tại backend bằng ZXing, không gửi thông tin đơn/tài khoản tới dịch vụ tạo ảnh bên ngoài.
QR có ngân hàng, số tài khoản, số tiền VND và nội dung duy nhất theo mã đơn.
Ví dụ đơn 185.000 đ tạo QR có số tiền 185000, không phải một ảnh QR cố định cho mọi đơn.

Đây là chuyển khoản thật vào tài khoản người bán, **không phải tích hợp VNPay/MoMo**.
Tạo/hiển thị QR không chứng minh đã nhận tiền. Không yêu cầu mật khẩu, OTP hay quyền truy cập ngân hàng.

## Luồng khách hàng và Admin

1. Khách đăng nhập, checkout và chọn QR. Backend tính lại giá/tổng tiền và kiểm tra tồn kho.
2. Đơn được lưu, tồn kho trừ theo nghiệp vụ hiện có, giao dịch ở `UNPAID`.
3. Trang `/orders/{id}/payment` hiện QR, tài khoản, số tiền và nội dung; hỗ trợ tải ảnh, sao chép.
4. Khách kiểm tra thông tin trên ứng dụng ngân hàng trước khi tự xác nhận chuyển tiền.
5. Nút **Tôi đã chuyển khoản** chỉ chuyển sang `REPORTED` (chờ đối soát).
6. Admin mở **Bán hàng → Đối soát chuyển khoản QR**, kiểm tra tiền thực nhận trong ngân hàng,
   nhập số tiền và mã giao dịch thật rồi xác nhận `PAID`.
7. Hệ thống lưu người xác nhận, thời gian và mã giao dịch. Xác nhận thanh toán không trừ kho lần nữa.

Thanh toán và trạng thái xử lý đơn tách riêng. Đơn QR phải `PAID` trước khi chuyển sang giao hàng/hoàn tất.
Mã giao dịch không được dùng lại cho đơn khác; xác nhận lặp cùng giao dịch không ghi đè kết quả.
Khóa giỏ hàng/đơn hàng và checkout token chống tạo đơn hoặc thay đổi tồn kho trùng khi gửi đồng thời.
Checkout và xác nhận dùng `READ_COMMITTED` để đọc được kết quả giao dịch vừa hoàn tất sau khi chờ khóa.

## Cấu hình

Chạy với profile `mysql` (thêm `mail` nếu cần email). Working directory là thư mục gốc project.
Các biến môi trường hỗ trợ:

```text
BANK_TRANSFER_ENABLED=true
BANK_TRANSFER_BIN=<BIN ngân hàng 6 chữ số>
BANK_TRANSFER_BANK_NAME=<tên ngân hàng>
BANK_TRANSFER_ACCOUNT_NUMBER=<số tài khoản nhận>
BANK_TRANSFER_ACCOUNT_NAME=<tên chủ tài khoản>
```

Hoặc tạo `.bank-transfer.local.properties` ở gốc project:

```properties
app.bank-transfer.enabled=true
app.bank-transfer.bin=YOUR_BANK_BIN
app.bank-transfer.bank-name=YOUR_BANK_NAME
app.bank-transfer.account-number=YOUR_ACCOUNT_NUMBER
app.bank-transfer.account-name=YOUR_ACCOUNT_NAME
```

File này được import tùy chọn và đã có trong `.gitignore`. Bản local hiện đã cấu hình tài khoản MB
do chủ project cung cấp. README không chứa số tài khoản thật. Nếu đổi cấu hình, khởi động lại ứng dụng.
Đơn đã tạo giữ snapshot tài khoản/số tiền cũ, không đổi người nhận âm thầm theo cấu hình mới.
Khi cấu hình chưa đầy đủ hoặc tắt, checkout không hiển thị lựa chọn QR; backend cũng từ chối tạo QR.

## Database và backup

- Database mới: `database/vegetable_shop.sql` đã chứa schema thanh toán.
- Nâng cấp database cũ: dừng ứng dụng, backup rồi chạy `database/phase-13-bank-transfer.sql`.
- Bổ sung bảng `bank_transfer_payments`, checkout token duy nhất, phương thức `BANK_TRANSFER`
  và trạng thái `REPORTED`. Giữ các giá trị thanh toán cũ để tương thích.
- Ngày 04/09/2026 đã backup và chạy migration trên MySQL local, chạy lại để kiểm tra tính lặp an toàn.
  Số dòng của 21 bảng có sẵn không đổi; bảng giao dịch mới chưa có giao dịch thật.
- Backup nằm ngoài project tại `../backups/`: bản source trước thay đổi và
  `vegetable_shop-before-phase13-db-20260904-142643.sql`.
  Dump đã kiểm tra hoàn tất, chưa thử phục hồi lên server riêng. Backup chứa dữ liệu riêng tư, không upload GitHub.

## Kiểm thử và checklist review

Đã kiểm thử QR encode/decode, CRC, đối chiếu BIN/tài khoản từ QR nguồn, snapshot,
quyền sở hữu, CSRF, phân quyền Admin, số tiền sai, giao dịch trùng, report lặp,
checkout/xác nhận đồng thời, không trừ kho hai lần và render trang khách/Admin.
Kiểm thử tự động dùng H2/dữ liệu giả, không tạo giao dịch ngân hàng hoặc đơn thật trong MySQL.
Kết quả sau khi chuyển về đối soát thủ công: 241 kiểm thử Java và 15 kiểm thử JavaScript đạt;
không có lỗi kiểm thử.

```powershell
.\mvnw.cmd test
node --test src/test/js/*.test.cjs
```

Review trên web:

1. Tạo đơn QR, quét bằng ứng dụng ngân hàng; kiểm tra đúng tên người nhận, tài khoản, số tiền và nội dung.
   Có thể dừng ở màn hình xác nhận, chưa cần chuyển tiền.
2. Chỉ khi muốn thử tiền thật, tự xác nhận trong ứng dụng ngân hàng; khoản tiền thực chuyển không tự hoàn lại.
3. Bấm báo chuyển khoản; kiểm tra vẫn chờ đối soát, chưa được giao hàng.
4. Admin đối chiếu sao kê thật, xác nhận đúng số tiền/mã giao dịch; kiểm tra đơn hiển thị đã thanh toán.
5. Thử gửi lặp và kiểm tra tồn kho không giảm thêm. Tài khoản khác không được xem QR/đơn của khách.

**Chưa thực hiện chuyển tiền thật hoặc quét bằng ứng dụng ngân hàng trong quá trình triển khai.**

## Giới hạn có chủ đích

- Nhận số tiền nguyên VND, lớn hơn 0 và dưới 500 triệu; từ chối số tiền không hợp lệ trước khi hoàn tất checkout.
- Không kết nối API ngân hàng/webhook: Admin phải đối soát thật, không chỉ tin ảnh chụp hoặc lời báo của khách.
- Không tự xử lý tiền thiếu/thừa, hoàn tiền hay tự động hết hạn QR/giải phóng tồn kho.
- Đơn QR chưa báo chuyển khoản có thể hủy theo quy tắc đơn hàng hiện có và hoàn tồn kho.
  Đơn đã báo chuyển hoặc đã thanh toán bị chặn hủy trực tiếp để tránh bỏ sót tiền.
  Quy trình bác báo cáo sai/đối soát ngoại lệ/hoàn tiền cần phát triển riêng; không sửa trạng thái tùy tiện trong DB.
- QR tải về vẫn có thể được ngân hàng đọc sau khi đơn đã hủy; website chỉ ẩn/ngừng cấp QR,
  không thể vô hiệu hóa ảnh chuyển khoản đã lưu. Khoản đến muộn phải được đối soát thủ công.
- Phải hoàn thiện quy trình ngoại lệ trên trước khi vận hành thương mại không có người giám sát.

Thư viện tạo/đọc QR: [ZXing](https://github.com/zxing/zxing), giấy phép Apache-2.0.
