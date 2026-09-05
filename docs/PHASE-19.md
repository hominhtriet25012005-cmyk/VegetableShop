# Giai đoạn 19 — Đánh giá sản phẩm nâng cao

Triển khai ngày 04/09/2026 theo lộ trình README: đánh giá có ảnh, xác minh chi tiết
đơn, kiểm duyệt và thống kê sao. Không phải giai đoạn voucher.

## Chức năng và quy tắc

- Khách đăng nhập chọn chi tiết đơn `COMPLETED` thuộc tài khoản mình.
- Gửi 1–5 sao, nhận xét tối đa 1000 ký tự và tối đa 5 ảnh JPG/PNG.
- Mỗi chi tiết đơn chỉ gửi một lần; lần mua khác của cùng sản phẩm được đánh giá riêng.
- Không tự sửa sau khi gửi. Đánh giá mới là `PENDING`; khách xem trạng thái riêng.
- Admin vào **Bán hàng → Đánh giá sản phẩm** (`/admin/reviews`), lọc trạng thái,
  tìm sản phẩm/khách hàng, xem ảnh và đơn mua, duyệt/ẩn/xóa mềm.
- `APPROVED` và đã xác minh: công khai, có nhãn **Đã mua hàng**, tính điểm/sao.
- `PENDING`, `HIDDEN`, `DELETED`: không công khai và không tính điểm.
- Ẩn/xóa cần lý do; lưu người duyệt, thời điểm, lý do kiểm duyệt gần nhất.
- Xóa mềm giữ nội dung trong Admin và unique chi tiết đơn; không có nút khôi phục.
- Thống kê 5 mức sao có số lượng, tỷ lệ và điểm trung bình của các đánh giá đã duyệt.

## Nâng cấp MySQL hiện có — hướng dẫn cho database chưa nâng cấp

**Máy phát triển đã cập nhật ngày 04/09/2026 lúc 12:46 (Asia/Saigon).**
Migration chạy thành công trên `vegetable_shop`, MySQL 9.2.0, cổng 3306,
khi ứng dụng cổng 8081 không chạy. Không cần chạy lại trên database này.

- Backup SQL trước migration: `D:\JavaProjects\TrietHo\backups\vegetable_shop-before-phase19-db-20260904-124646.sql`.
- Backup bằng mysqldump single-transaction, gồm schema, dữ liệu, trigger, routine và event;
  đã kiểm tra exit code, kích thước và dấu kết thúc dump, chưa thử phục hồi backup.
- Đã xác nhận các cột kiểm duyệt, unique theo chi tiết đơn, khóa ngoại và bảng ảnh mới.
- Đã bỏ unique cũ `uk_reviews_user_product`; không có liên kết đánh giá sai chủ đơn/sản phẩm.
- Số lượng trước/sau không đổi: 106 sản phẩm, 1 đơn hàng, 3 người dùng, 0 đánh giá.
- Chưa khởi động lại ứng dụng hoặc tạo đánh giá thật; cần review theo checklist bên dưới.
- Backup SQL có dữ liệu riêng tư, giữ ngoài Git và không chia sẻ công khai.

Các bước dưới đây dành cho máy/database khác chưa nâng cấp:

1. Dừng Spring Boot trong IntelliJ.
2. Sao lưu database `vegetable_shop` qua MySQL Workbench **Server → Data Export**.
   Backup mã nguồn ZIP không thay thế backup database.
3. Mở và chạy **toàn bộ** `database/phase-19-advanced-reviews.sql` trong Workbench.
4. Kiểm tra không có dòng đỏ, Refresh bảng rồi khởi động lại với profile `mysql`.
5. Ctrl+F5 trên website, mở `/admin/reviews` để kiểm duyệt dữ liệu cũ.

Không cần tạo lại database. Script có thể chạy lại, không xóa đánh giá cũ.
SQL_SAFE_UPDATES được lưu/khôi phục; nếu script dừng giữa chừng, chạy
`SET SQL_SAFE_UPDATES = @phase19_safe_updates;` trong cùng kết nối.

**Không chỉ dựa vào Hibernate update:** Hibernate không tự bỏ unique index cũ
`uk_reviews_user_product`. Migration thêm index hỗ trợ khóa ngoại user trước khi
bỏ unique cũ và thay bằng `uk_reviews_order_detail`.

Đánh giá cũ bắt đầu ở trạng thái chờ duyệt. Script liên kết với một chi tiết đơn
đã hoàn tất của đúng khách/sản phẩm. Nếu chưa xác minh hoặc dữ liệu trùng bất
thường, giữ nguyên không liên kết; Admin không thể duyệt công khai. Chạy lại
không thay đổi quyết định kiểm duyệt đã có.

Database mới dùng `database/vegetable_shop.sql`, đã có cấu trúc giai đoạn 19.

## Ảnh và bảo mật

- Mỗi ảnh tối đa 2 MB, 12 megapixel, 6000 pixel mỗi chiều.
- Backend kiểm tra định dạng thực JPEG/PNG, xuất lại JPEG cạnh dài tối đa 1600
  pixel để loại bỏ metadata/nội dung đính kèm; không chỉ tin MIME khách gửi.
- Tên tệp UUID do server tạo; không dùng tên hoặc đường dẫn khách gửi.
- Mặc định ảnh ở `uploads/reviews`, tương đối với thư mục chạy ứng dụng.
  Nên đặt biến môi trường đường dẫn tuyệt đối:

```text
REVIEW_IMAGE_DIRECTORY=D:\JavaProjects\TrietHo\uploads\reviews
```

- Không trỏ thư mục ảnh vào `static` hoặc public trực tiếp bằng web server.
- `/review-images/{id}` kiểm tra quyền: ảnh chờ duyệt/ẩn chỉ chủ đánh giá và Admin
  đọc; ảnh đã xóa chỉ Admin. Dùng `no-store`, không cache sau kiểm duyệt.
- Rollback transaction dọn ảnh vừa tạo. Nếu tiến trình bị tắt đột ngột lúc ghi ảnh
  trước commit, có thể còn tệp mồ côi; chưa có tác vụ tự dọn trường hợp này.
- Sao lưu thư mục ảnh cùng database, không commit Git. Xóa mềm không xóa tệp;
  cần chính sách lưu giữ riêng trước khi triển khai thật. Chưa tích hợp antivirus.
- Khóa ghi chi tiết đơn trong transaction và unique index chặn gửi đồng thời.
- Kiểm tra chủ đơn, sản phẩm, COMPLETED và trạng thái tài khoản ở backend.
- Mọi POST giữ CSRF; Admin mới được kiểm duyệt. Nhận xét dùng `th:text`, không chạy HTML.

Biện pháp upload tham chiếu [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html).

## Cấu trúc bổ sung

- Entity: `Review`, `ReviewStatus`, `ReviewImage`.
- Repository: `ReviewRepository`, `OrderDetailRepository`, `ReviewImageRepository`.
- Service: `ReviewService`, `ReviewImageStorage`.
- Controller: `ProductController`, `AdminReviewController`, `ReviewImageController`, `ReviewUploadAdvice`.
- Website: `shop-detail.html`, `fragments/review-images.html`, `js/reviews.js`.
- Admin: `admin/reviews.html`, `admin/review-detail.html`, sidebar chung.

## Kiểm thử

- 221 Java tests, 0 lỗi/thất bại/bỏ qua; 8 JavaScript tests đạt.
- H2 cô lập kiểm tra JPQL/JPA, unique index, hai lần mua, hai request đồng thời,
  phân bố sao và vòng đời duyệt/ẩn/xóa; không dùng MySQL thật.
- Unit/MVC kiểm tra giả mạo chủ đơn/mã sản phẩm, rating, CSRF, phân quyền,
  multipart, MIME giả/SVG/ảnh quá lớn, rollback ảnh, ảnh riêng và XSS escaping.
- Kiểm tra trực quan với HTML render từ dữ liệu giả.
- Script migration đã chạy trên MySQL local 9.2.0; kiểm tra cấu trúc, index, khóa ngoại
  và số lượng dữ liệu đạt. Kiểm thử nghiệp vụ tự động vẫn chạy trên H2, không phải MySQL thật.

Chạy từ thư mục project:

```powershell
.\mvnw.cmd test
node --test src/test/js/*.test.cjs
```

## Checklist review thủ công

1. Khách có đơn hoàn tất: mở sản phẩm → đánh giá → chọn đơn → 5 sao, nhận xét, 2 ảnh.
2. Thấy **Chờ duyệt** trong **Đánh giá của bạn**; khách khác chưa thấy ảnh/nội dung.
3. Admin duyệt: website xuất hiện nhãn **Đã mua hàng**, ảnh, điểm và tỷ lệ sao.
4. Admin ẩn/xóa có lý do: ảnh/nội dung không còn công khai, điểm được tính lại.
5. Gửi lại cùng chi tiết đơn bị từ chối; mua ở đơn hoàn tất khác có thêm lượt.
6. Tài khoản khác không gửi đánh giá hoặc xem ảnh riêng không thuộc mình.
7. Thử 6 ảnh, SVG, ảnh quá 2 MB: không được ghi đánh giá/ảnh không hợp lệ.

Chưa bao gồm trả lời đánh giá, video, báo cáo vi phạm, sửa đánh giá hoặc review có thưởng.
