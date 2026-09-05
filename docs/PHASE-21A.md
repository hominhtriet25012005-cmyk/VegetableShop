# Giai đoạn 21A – Chatbot FAQ và tìm sản phẩm

## Kết quả

Giai đoạn 21A bổ sung trợ lý mua hàng rule-based, chưa sử dụng API AI và không
phát sinh chi phí dịch vụ ngoài:

- Widget chatbot dùng chung ở các trang website có thanh tài khoản.
- Giao diện responsive, điều khiển bằng bàn phím và vùng trả lời `aria-live`.
- Trả lời FAQ về giao hàng, COD, đổi trả, tài khoản, liên hệ và đơn hàng.
- Tìm sản phẩm theo tên, danh mục, thương hiệu và mô tả.
- Hiểu mức giá `100k`, `100.000đ`, `1,5 triệu`, “dưới”, “trên” và khoảng
  “từ ... đến ...”.
- Trả tối đa 5 sản phẩm đang hoạt động, danh mục đang hoạt động và còn hàng.
- Mỗi kết quả có ảnh, giá, đơn vị, số tồn và liên kết trang chi tiết.
- Có chế độ trả lời an toàn khi chạy profile `template` không có MySQL.

## Kiến trúc

```text
chatbot.js
    -> GET /api/chatbot/messages?message=...
    -> ChatbotController
    -> ChatbotRateLimiter
    -> ChatbotService
    -> ProductRepository
    -> MySQL products/categories/brands
```

Đây là API chỉ đọc. Chatbot không thêm giỏ, đặt hàng hay thay đổi database.

## Quy tắc tìm kiếm

1. Chuẩn hóa chữ thường và bỏ dấu tiếng Việt để so khớp linh hoạt.
2. Tách điều kiện giá khỏi từ khóa sản phẩm.
3. Chấm điểm: tên sản phẩm, danh mục, thương hiệu rồi mô tả.
4. Nếu người dùng yêu cầu “giá rẻ”, ưu tiên giá tăng dần.
5. Lọc lại trạng thái và tồn kho tại truy vấn backend.
6. Không tin dữ liệu do JavaScript tự tạo; đường dẫn chi tiết được backend trả
   về theo ID sản phẩm.

Ví dụ câu hỏi:

- `Có nấm dưới 100.000đ không?`
- `Gợi ý rau củ giá rẻ`
- `Tìm sản phẩm từ 50k đến 150k`
- `Phí giao hàng tính thế nào?`
- `Tôi xem đơn hàng ở đâu?`

## Bảo mật và riêng tư

- Nội dung câu hỏi dài tối đa 300 ký tự.
- Giới hạn 30 câu hỏi/phút cho mỗi HTTP session; vượt giới hạn trả HTTP `429`.
- API dùng `GET` và không thay đổi trạng thái nên không cần tắt CSRF.
- Không lưu lịch sử hội thoại vào database.
- Không đọc hoặc trả dữ liệu đơn hàng, địa chỉ hay tài khoản cá nhân.
- JavaScript dựng nội dung động bằng `textContent`; URL sản phẩm phải khớp mẫu
  `/product/{id}` trước khi gắn vào liên kết.
- Giao diện nhắc khách không nhập mật khẩu hoặc thông tin thanh toán.

## API

```http
GET /api/chatbot/messages?message=Co%20nam%20duoi%20100k%20khong
Accept: application/json
```

Response gồm:

- `message`: câu trả lời văn bản.
- `products`: tối đa 5 thẻ sản phẩm.
- `suggestions`: các câu hỏi hoặc liên kết thao tác nhanh.

## Database và cấu hình

Giai đoạn 21A **không có migration mới**, không cần API key và không cần thêm
biến môi trường. Chỉ cần chạy project với profile `mysql` như hiện tại.

## Tệp chính

- `controller/ChatbotController.java`
- `controller/TemplateChatbotController.java`
- `service/ChatbotService.java`
- `service/ChatbotRateLimiter.java`
- `dto/ChatbotResponse.java`, `dto/ChatbotProductView.java`
- `repository/ProductRepository.java`
- `static/js/chatbot.js`, `static/css/chatbot.css`
- `templates/fragments/account.html`

## Kiểm thử

```text
Tests run: 175, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test mới bao phủ FAQ không truy vấn database, tìm nấm theo giá, lọc danh mục,
sắp xếp giá rẻ, không có kết quả, validation 300 ký tự, rate limit, API công
khai và việc widget được render trên giao diện.

## Backup

- Trước triển khai:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-21A-20260830-2157.zip`
- Sau triển khai và kiểm thử:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-21A-verified-20260830-2210.zip`

Hai file ZIP loại trừ `.git`, `.idea`, `target` và file module IDE.
