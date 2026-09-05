# Giai đoạn 20 — Voucher, khuyến mãi và Flash Sale

## Phạm vi đã triển khai

Giai đoạn 20 bổ sung khu vực **Bán hàng → Voucher & khuyến mãi** tại
`/admin/discounts`.

- Voucher giảm theo phần trăm hoặc số tiền cố định.
- Giá trị đơn tối thiểu và mức giảm tối đa.
- Thời gian hiệu lực, tổng lượt dùng và lượt dùng tối đa của từng khách hàng.
- Phạm vi toàn đơn, danh mục, thương hiệu hoặc sản phẩm.
- Khuyến mãi sản phẩm theo thời gian và nhãn Flash Sale.
- Một sản phẩm nằm trong nhiều chương trình sẽ nhận mức giá thấp nhất.
- Giá khuyến mãi xuất hiện đồng bộ ở danh sách, chi tiết, wishlist, giỏ hàng và checkout.
- Mỗi đơn dùng tối đa một voucher.
- Trang chi tiết đơn lưu và hiển thị giá gốc, giảm khuyến mãi, giảm voucher và số tiền cuối.

Phần thanh toán trực tuyến/đối soát tự động không thuộc thay đổi này và tiếp tục
được tạm hoãn. COD và VietQR đối soát thủ công hiện có không bị mở rộng.

## Thứ tự tính tiền

```text
Tạm tính theo giá gốc
  - Khuyến mãi sản phẩm
  = Tiền hàng sau khuyến mãi
  - Voucher hợp lệ
  = Tổng thanh toán
```

Voucher được kiểm tra trên số tiền **sau khuyến mãi**. Giảm cố định của chương
trình sản phẩm được hiểu là số tiền giảm trên **mỗi đơn vị sản phẩm**. Voucher
giảm cố định được áp dụng một lần và không vượt quá tổng tiền đủ điều kiện.

## Quy tắc an toàn

- Trình duyệt chỉ gửi mã voucher; không gửi số tiền giảm đáng tin cậy.
- Backend khóa voucher khi checkout, kiểm tra lại thời gian, trạng thái, số lượt
  và phạm vi trong transaction đặt hàng.
- Hạn mức tổng và hạn mức theo khách chỉ tính lượt còn hiệu lực.
- Mỗi đơn chỉ có một bản ghi sử dụng voucher; gửi checkout lặp không tạo thêm lượt.
- Hủy đơn hợp lệ giải phóng lượt voucher và hoàn tồn kho theo nghiệp vụ hiện có.
- Đơn hàng và chi tiết đơn giữ snapshot số tiền giảm, nên thay đổi chương trình
  sau này không làm thay đổi hóa đơn cũ.
- Khi nhiều khuyến mãi cùng áp dụng, hệ thống chọn giá thấp nhất, không cộng dồn.

## Database

Database mới đã có đầy đủ bảng trong `database/vegetable_shop.sql`.

Với database đang dùng:

1. Dừng ứng dụng.
2. Sao lưu MySQL.
3. Chạy toàn bộ `database/phase-20-discounts.sql` trong MySQL Workbench.
4. Khởi động lại ứng dụng với profile `mysql`.

Migration tạo:

- `vouchers`
- `voucher_scopes`
- `voucher_usages`
- `promotions`
- `promotion_products`
- snapshot giảm giá trong `orders` và `order_details`

Script có thể chạy lại. Hibernate `ddl-auto=update` có thể tạo phần lớn cấu trúc
khi ứng dụng khởi động, nhưng vẫn nên chạy migration để có index, ràng buộc và
backfill đơn hàng cũ đầy đủ.

Ngày 06/09/2026 migration đã được áp dụng vào MySQL local và ứng dụng đã khởi
động thành công với profile `mysql`. Bản sao lưu trước migration nằm ngoài
project tại `D:\JavaProjects\TrietHo\backups\vegetable_shop-before-phase20-20260906-145244.sql`;
không đưa file database thật này lên GitHub.

## Checklist review

1. Vào `/admin/discounts`, tạo một khuyến mãi 10% cho hai sản phẩm và bật trạng thái.
2. Kiểm tra giá gốc gạch ngang và giá giảm ở trang sản phẩm, chi tiết và giỏ hàng.
3. Tạo voucher toàn đơn có đơn tối thiểu, mức giảm tối đa và giới hạn một lượt/khách.
4. Nhập voucher ở checkout; kiểm tra đủ bốn dòng tạm tính/khuyến mãi/voucher/tổng tiền.
5. Đặt đơn và kiểm tra snapshot giảm giá tại trang chi tiết đơn.
6. Thử dùng lại voucher vượt giới hạn; backend phải từ chối.
7. Hủy đơn khi trạng thái cho phép rồi dùng lại voucher; lượt phải được giải phóng.
8. Thử voucher hết hạn, tắt, sai phạm vi và đơn chưa đủ tối thiểu.

Kiểm thử tự động bao phủ tính giá tốt nhất, mức giảm không âm, phạm vi voucher,
giới hạn theo khách, thứ tự tính tiền và tổng các dòng đơn.

Kết quả xác minh: **247 kiểm thử Java** và **15 kiểm thử JavaScript** đạt; trang
chủ và trang sản phẩm trả HTTP 200 trên MySQL local. Phiên ứng dụng kiểm tra đã
được dừng sau khi xác minh.
