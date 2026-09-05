# Giai đoạn 18 – Quản lý kho

## Kết quả

Giai đoạn 18 bổ sung sổ kho nền tảng dành riêng cho `ADMIN`. Giai đoạn 18A sau
đó đã thay thao tác kho trực tiếp bằng **phiếu kho nhiều dòng**; xem
[`PHASE-18A.md`](PHASE-18A.md) để biết luồng hiện hành.

Phần nền tảng của giai đoạn 18 gồm:

- `/admin/inventory`: tổng quan tồn kho, cảnh báo và thao tác kho.
- `/admin/inventory/documents`: nhập, xuất hoặc điều chỉnh kiểm kê bằng phiếu.
- `/admin/inventory/history`: lịch sử biến động, lọc theo sản phẩm, nghiệp vụ và ngày.

Từ bảng **Tồn kho sản phẩm** hoặc **Quản lý sản phẩm**, nút **Nhập kho** mở
phiếu nhập với đúng sản phẩm đã chọn sẵn. Admin chỉ cần nhập số lượng, giá vốn,
nhà cung cấp rồi lưu nháp và bấm **Xác nhận phiếu**. Khi xác nhận, tồn sản phẩm
và nhật ký kho được cập nhật tự động trong cùng transaction; không cần sửa trực
tiếp trong MySQL.

Tổng quan kho hiển thị 10 sản phẩm mỗi trang và giữ lại từ khóa/tình trạng kho
khi chuyển trang. Các chỉ số tổng tồn, sắp hết và hết hàng vẫn tính trên toàn bộ
danh mục, không chỉ trên trang đang xem.

Database hiện có phải chạy
`database/phase-18-inventory-migration.sql` một lần. Script có thể chạy lại an
toàn và tự tạo giao dịch `INITIAL` cho sản phẩm chưa có lịch sử.

## Nghiệp vụ kho

Các loại giao dịch:

| Loại | Ý nghĩa | Thay đổi tồn |
| --- | --- | --- |
| `INITIAL` | Tồn đầu kỳ hoặc tạo sản phẩm | Ghi nhận ban đầu |
| `INBOUND` | Nhập hàng | Tăng |
| `OUTBOUND` | Xuất thủ công, hỏng hoặc thất thoát | Giảm |
| `ADJUSTMENT` | Đặt lại theo số thực tế kiểm kê | Tăng hoặc giảm |
| `SALE` | Đặt hàng thành công | Giảm tự động |
| `RETURN` | Hủy đơn hợp lệ | Tăng tự động |

Mỗi dòng `stock_movements` lưu:

- Sản phẩm và tên sản phẩm dạng snapshot.
- Tồn trước, lượng thay đổi có dấu và tồn sau.
- Loại nghiệp vụ và lý do.
- Tham chiếu sản phẩm hoặc mã đơn hàng.
- Email người thực hiện và thời điểm tạo.

Lịch sử không có chức năng sửa/xóa từ giao diện. Sai lệch phải được sửa bằng
một giao dịch điều chỉnh mới để không mất dấu vết.

## Quy tắc an toàn

- Sản phẩm được khóa bằng `PESSIMISTIC_WRITE` trước khi cập nhật.
- Xuất kho không được làm số lượng âm.
- Nhập và xuất phải lớn hơn `0`.
- Điều chỉnh dùng số tồn thực tế sau kiểm đếm và phải làm thay đổi tồn kho.
- Từ 18A, biểu mẫu sản phẩm không được sửa số lượng; mọi thay đổi thủ công phải
  đi qua phiếu kho.
- Sản phẩm mới có tồn bằng `0`; phiếu nhập là nghiệp vụ đưa hàng vào kho.
- Trừ kho khi checkout và ghi `SALE` nằm trong cùng transaction.
- Hủy đơn và ghi `RETURN` nằm trong cùng transaction.
- Toàn bộ `/admin/inventory/**` yêu cầu vai trò `ADMIN` và giữ CSRF cho POST.

## Cảnh báo tồn thấp

Mỗi Product có `low_stock_threshold`, mặc định `10`. Sản phẩm được xem là:

- Hết hàng: `quantity = 0`.
- Sắp hết: `0 < quantity <= low_stock_threshold`.
- Ổn định: `quantity > low_stock_threshold`.

Ngưỡng có thể cập nhật trực tiếp trên trang tổng quan kho. Dashboard và báo cáo
kinh doanh dùng ngưỡng riêng này thay cho một ngưỡng cố định toàn hệ thống.

## Tệp chính

- `database/phase-18-inventory-migration.sql`
- `src/main/java/com/vegetableshop/entity/StockMovement.java`
- `src/main/java/com/vegetableshop/entity/StockMovementType.java`
- `src/main/java/com/vegetableshop/service/InventoryService.java`
- `src/main/java/com/vegetableshop/controller/AdminInventoryController.java`
- `src/main/resources/templates/admin/inventory.html`
- `src/main/resources/templates/admin/inventory-movement.html`
- `src/main/resources/templates/admin/inventory-history.html`
- `src/test/java/com/vegetableshop/service/InventoryServiceTests.java`
- `src/test/java/com/vegetableshop/controller/AdminInventoryControllerTests.java`

## Cách cập nhật database

Trong MySQL Workbench:

1. Mở `database/phase-18-inventory-migration.sql`.
2. Kiểm tra schema đang là `vegetable_shop`.
3. Chạy toàn bộ script.
4. Refresh phần Tables.
5. Kiểm tra bảng `stock_movements` và cột `products.low_stock_threshold`.

Không nhập trực tiếp số lượng bằng câu SQL sau khi đưa module vào sử dụng vì
thao tác đó sẽ không tạo lịch sử kho.

## Kiểm thử

Bộ test bao phủ nhập, xuất, điều chỉnh, chặn tồn âm, cảnh báo theo ngưỡng riêng,
hoàn kho khi hủy đơn, ghi `SALE` khi checkout, lọc lịch sử, render Thymeleaf,
CSRF và quyền `ADMIN`.

```text
Tests run: 156, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Backup

- Trước khi triển khai: `D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-18-20260830-1210.zip`
- Sau khi triển khai và kiểm thử: `D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-18-verified-20260830-1335.zip`

Hai bản backup đều loại trừ `.git`, `.idea`, `target` và `VegetableShop.iml`.
