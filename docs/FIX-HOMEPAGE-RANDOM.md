# Trang chủ: sản phẩm ngẫu nhiên xen kẽ danh mục

Ngày cập nhật: 03/09/2026.

## Nguyên nhân

Danh sách cũ đặt tám sản phẩm mới nhất trước các nhóm danh mục. JavaScript chỉ
hiện tám phần tử đầu khi chọn “Tất cả sản phẩm”, nên một đợt nhập Đồ khô khiến
mục này trông giống hệt tab Đồ khô.

## Quy tắc mới

- Backend lấy sản phẩm và danh mục đang hoạt động bằng một truy vấn catalog.
- Trộn ngẫu nhiên sản phẩm trong mỗi danh mục, lấy tối đa tám sản phẩm mỗi nhóm.
  Sản phẩm cũ cũng có thể được chọn, không chỉ tám sản phẩm mới nhất.
- Trộn thứ tự các nhóm rồi xen kẽ mỗi nhóm một sản phẩm. Với bốn danh mục có
  đủ hàng, tám ô đầu gồm hai sản phẩm mỗi danh mục; nhóm thiếu sản phẩm sẽ
  nhường vị trí cho nhóm còn lại. Không tạo bản sao sản phẩm để lấp đủ tám ô.
- Tải lại trang sẽ chọn mẫu mới. Chuyển tab trong cùng lượt tải giữ nguyên mẫu
  để không làm người dùng mất vị trí; tab riêng vẫn lọc tại chỗ, tối đa tám ô.
- Sản phẩm/danh mục ngừng hoạt động bị loại; trạng thái hết hàng giữ nguyên
  cách xử lý của cửa hàng. Không sửa giá, tồn kho hoặc dữ liệu MySQL.
- Không cần migration SQL. Khởi động lại Spring Boot rồi tải lại trang chủ.

Hiện tại truy vấn lấy toàn bộ catalog đang hoạt động trong bộ nhớ, phù hợp
catalog đồ án nhỏ; HTML chỉ gửi tối đa tám sản phẩm mỗi danh mục. Khi catalog
lớn cần chuyển sang lấy mẫu ID hoặc cache để tránh tải toàn bộ catalog.

## Kiểm tra

- `mvnw.cmd test`: kiểm tra phân bố danh mục, không trùng ID, thay đổi mẫu với
  seed cố định, sản phẩm cũ, nhóm thưa/trống, trạng thái ẩn và render Thymeleaf.
- `node --test src/test/js/home-products.test.cjs`: kiểm tra logic JavaScript
  hiển thị tám ô, đổi tab danh mục, quay lại Tất cả và thông báo nhóm trống.
- Kiểm tra thủ công: mở trang chủ, xem nhãn danh mục của tám ô đầu; bấm từng
  danh mục rồi quay về Tất cả; tải lại vài lần để xem các mẫu khác nhau.
