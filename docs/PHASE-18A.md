# Giai đoạn 18A – Chuẩn hóa Admin sản phẩm và kho

## Kết quả

Giai đoạn 18A tách rõ dữ liệu bán hàng và dữ liệu nhập hàng:

- **Thương hiệu** là nhãn hiển thị cho khách hàng trên sản phẩm.
- **Nhà cung cấp** là đối tác cung ứng, chỉ được gắn vào phiếu nhập kho.
- **Sản phẩm** giữ thông tin hàng hóa; tồn kho không còn sửa trực tiếp từ biểu
  mẫu sản phẩm.
- **Phiếu kho** là chứng từ làm thay đổi tồn kho và có thể chứa nhiều sản phẩm.

## Menu Admin

Sidebar được chia thành các nhóm:

- Tổng quan.
- Sản phẩm: danh mục, thương hiệu, sản phẩm.
- Kho & nhập hàng: nhà cung cấp, tồn kho, phiếu kho, lịch sử kho.
- Bán hàng: đơn hàng, khách hàng, đánh giá và thống kê.

Các route mới:

| Route | Chức năng |
| --- | --- |
| `/admin/brands` | Danh sách và tìm thương hiệu |
| `/admin/brands/new` | Tạo thương hiệu |
| `/admin/suppliers` | Danh sách và tìm nhà cung cấp |
| `/admin/suppliers/new` | Tạo nhà cung cấp |
| `/admin/inventory/documents` | Danh sách, lọc phiếu kho |
| `/admin/inventory/documents/new` | Tạo phiếu nhập, xuất hoặc kiểm kê |
| `/admin/inventory/documents/{id}` | Chi tiết và xác nhận phiếu |

Tất cả route trên yêu cầu vai trò `ADMIN`; các thao tác POST giữ CSRF.

## Sản phẩm

Thông tin quản trị được bổ sung:

- SKU duy nhất, tự chuẩn hóa chữ hoa.
- Danh mục và thương hiệu độc lập.
- Đơn vị bán (`kg`, `g`, gói, hộp, chai, cái/quả hoặc bó).
- Xuất xứ, ảnh đại diện và tối đa 8 ảnh phụ.
- Người tạo và người cập nhật gần nhất.
- Ngưỡng cảnh báo tồn thấp.

Sản phẩm mới luôn có tồn kho bằng `0`. Muốn có hàng bán phải lập phiếu nhập
kho; biểu mẫu sản phẩm không được phép thay đổi số tồn. Nút **Nhập kho** trên
danh sách sản phẩm và tổng quan tồn kho mở sẵn phiếu cho đúng sản phẩm, giúp
nhập số lượng/giá vốn/nhà cung cấp nhanh hơn mà vẫn giữ đầy đủ chứng từ.

## Phiếu kho

Ba loại chứng từ:

| Loại | Ý nghĩa | Giá vốn |
| --- | --- | --- |
| `INBOUND` | Nhập hàng, tăng tồn | Bắt buộc theo từng dòng |
| `OUTBOUND` | Xuất hỏng, thất thoát hoặc mục đích nội bộ | Không áp dụng |
| `ADJUSTMENT` | Đặt tồn về số kiểm kê thực tế | Không áp dụng |

Vòng đời phiếu:

1. `DRAFT`: chưa đổi tồn, có thể sửa hoặc xóa.
2. `POSTED`: khóa sản phẩm, kiểm tra toàn bộ dòng và cập nhật tồn trong cùng
   transaction.
3. Phiếu đã xác nhận không được sửa hay xóa. Nếu sai phải tạo phiếu điều chỉnh.

Quy tắc nghiệp vụ:

- Phiếu nhập bắt buộc có nhà cung cấp đang hoạt động.
- Một sản phẩm chỉ xuất hiện một lần trong một phiếu.
- Xuất kho không được làm tồn âm.
- Kiểm kê phải thực sự làm thay đổi số tồn.
- Các sản phẩm được khóa theo thứ tự ID để giảm nguy cơ deadlock.
- Mỗi dòng đã xác nhận tạo một `stock_movements` liên kết ngược về phiếu và
  dòng phiếu nguồn.
- Mã phiếu có dạng `PN-yyyyMMdd-000001`, `PX-...` hoặc `KK-...`.

## Báo cáo

- Doanh số sản phẩm được tổng hợp theo **thương hiệu**.
- Giá trị nhập hàng được tổng hợp theo **nhà cung cấp** từ các phiếu nhập đã
  xác nhận.
- Số tiền của phiếu nhập được tính từ số lượng nhân giá vốn trên từng dòng.

## Cập nhật database

Với database đang dùng, chạy lần lượt:

1. `database/phase-18-inventory-migration.sql` nếu chưa từng chạy.
2. `database/phase-18a-admin-catalog-inventory-documents.sql`.
3. Refresh schema trong MySQL Workbench.

Migration 18A vẫn giữ `products.supplier_id` để tương thích dữ liệu cũ. Từ 18B,
website không đọc hay hiển thị trường legacy này; dữ liệu nhà cung cấp nguồn
nhập chỉ lấy từ `inventory_documents.supplier_id`.

Các bảng/cột chính được thêm:

- `brands`.
- `product_images`.
- `inventory_documents`.
- `inventory_document_items`.
- `products.sku`, `brand_id`, `unit`, `origin`, `created_by`, `updated_by`.
- Thông tin mở rộng của `suppliers`.
- Liên kết chứng từ trong `stock_movements`.

## Kiểm thử

Đã xác minh toàn bộ project:

```text
Tests run: 165, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Bộ test 18A bao phủ chuẩn hóa thương hiệu/nhà cung cấp, phiếu nháp không đổi
tồn, xác nhận phiếu nhập, chặn xuất âm, chặn xác nhận hai lần, chuyển hướng các
route cập nhật kho cũ và render giao diện có CSRF.

## Backup

- Trước khi triển khai:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-18A-20260830-1409.zip`
- Sau triển khai và kiểm thử:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-18A-verified-20260830-1648.zip`

Hai file ZIP loại trừ `.git`, `.idea`, `target` và file module IDE để bản sao
gọn, không chứa kết quả build tạm.
