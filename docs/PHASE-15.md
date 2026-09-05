# Giai đoạn 15 – Thống kê kinh doanh

## Kết quả

Giai đoạn 15 bổ sung khu vực báo cáo dành riêng cho `ADMIN` tại:

- `/admin/reports`: xem và lọc báo cáo.
- `/admin/reports/export.csv`: tải dữ liệu CSV theo cùng khoảng ngày.

Không có thay đổi cấu trúc database nên không cần chạy migration mới.

## Quy tắc doanh thu

Chỉ đơn hàng có trạng thái `COMPLETED` được đưa vào doanh thu. Đơn `PENDING`,
`CONFIRMED`, `SHIPPING` hoặc `CANCELLED` không được tính. Khoảng ngày dùng mốc
`created_at`, lấy trọn ngày bắt đầu và ngày kết thúc.

Mặc định báo cáo hiển thị từ ngày đầu năm hiện tại đến hôm nay. Giá trị đơn
trung bình bằng tổng doanh thu chia số đơn hoàn tất, làm tròn đến đơn vị đồng.

## Chức năng đã triển khai

- Tổng doanh thu, số đơn hoàn tất, số lượng sản phẩm bán và giá trị đơn trung bình.
- Tổng tồn kho, sản phẩm sắp hết theo ngưỡng riêng (Giai đoạn 18) và sản phẩm hết hàng (`0`).
- Bảng tồn kho theo sản phẩm, danh mục và nhà cung cấp.
- Doanh số theo sản phẩm.
- Doanh số theo danh mục.
- Doanh số theo nhà cung cấp, có nhóm `Chưa có nhà cung cấp`.
- Doanh số theo khách hàng.
- Doanh thu theo tháng, quý và năm.
- Bộ lọc `Từ ngày` – `Đến ngày`, có kiểm tra thứ tự ngày.
- Biểu đồ cột theo tháng và biểu đồ tròn theo danh mục bằng Chart.js.
- Xuất CSV UTF-8 có BOM để Microsoft Excel đọc đúng tiếng Việt.

Các bảng vẫn hiển thị đầy đủ nếu CDN Chart.js không truy cập được; chỉ phần biểu
đồ cần kết nối mạng để tải thư viện từ jsDelivr.

> Từ Giai đoạn 18, cảnh báo tồn thấp dùng `products.low_stock_threshold` thay
> cho ngưỡng cố định `10`. Báo cáo vẫn đọc số tồn hiện tại từ Product, còn mọi
> biến động được lưu tại `stock_movements`.

## Luồng xử lý

```text
GET /admin/reports?from=YYYY-MM-DD&to=YYYY-MM-DD
        |
        v
AdminController
        |
        v
AdminService.businessReport(...)
        |
        +--> OrderRepository: chỉ lấy đơn COMPLETED trong khoảng ngày
        +--> ProductRepository: lấy tồn kho hiện tại
        |
        v
AdminReportView --> Thymeleaf + Chart.js
```

Repository tải trước User, OrderDetail, Product, Category và Supplier bằng
`EntityGraph` để tránh truy vấn N+1 trong lúc tổng hợp báo cáo.

## Lưu ý dữ liệu lịch sử

`order_details` đã lưu ảnh chụp tên, giá và thành tiền của sản phẩm tại lúc đặt
hàng. Danh mục và nhà cung cấp hiện vẫn đọc từ Product hiện tại; nếu sau này cần
báo cáo lịch sử tuyệt đối khi sản phẩm đổi danh mục/nhà cung cấp, nên bổ sung
`category_name` và `supplier_name` dạng snapshot vào `order_details`.

## Kiểm thử

Các test mới bao phủ:

- Tổng hợp doanh thu, số lượng bán và tồn kho.
- Nhóm theo sản phẩm, tháng, quý và năm.
- Từ chối khoảng ngày không hợp lệ.
- CSV UTF-8 và các phần dữ liệu bắt buộc.
- Controller trả đúng trang và phản hồi tải CSV.
- Thymeleaf render bộ lọc, biểu đồ, tồn kho và doanh số.

Kết quả toàn bộ project:

```text
Tests run: 130, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Tệp chính

- `src/main/java/com/vegetableshop/dto/AdminReportView.java`
- `src/main/java/com/vegetableshop/service/AdminService.java`
- `src/main/java/com/vegetableshop/repository/OrderRepository.java`
- `src/main/java/com/vegetableshop/repository/ProductRepository.java`
- `src/main/java/com/vegetableshop/controller/AdminController.java`
- `src/main/resources/templates/admin/reports.html`
- `src/main/resources/templates/admin/report-fragments.html`
- `src/test/java/com/vegetableshop/service/AdminBusinessReportTests.java`
