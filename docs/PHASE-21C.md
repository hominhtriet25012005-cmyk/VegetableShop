# Giai đoạn 21C – Quản trị FAQ và thống kê hội thoại

## Kết quả

Giai đoạn 21C hoàn thiện lớp vận hành cho chatbot 21A/21B:

- Admin thêm, sửa, tìm kiếm, ẩn hoặc kích hoạt FAQ chính thức.
- FAQ đang hoạt động được so khớp trước khi gọi OpenAI và trước chatbot 21A.
- So khớp không phụ thuộc chữ hoa/thường hoặc dấu tiếng Việt; hỗ trợ nhiều cụm
  từ khóa phân cách bằng dấu phẩy.
- Dashboard thống kê tổng lượt hỏi, nguồn trả lời, câu cần xem lại, số FAQ hoạt
  động, thời gian phản hồi trung bình và lượt hỏi theo ngày.
- Danh sách hội thoại lọc theo ngày, từ khóa, nguồn và kết quả xử lý.
- Admin có nút xóa các dòng analytics đã quá thời hạn lưu trữ.

## Luồng ưu tiên

```text
Câu hỏi
  -> FAQ quản trị khớp? -> trả lời chính thức
  -> AI đang bật?       -> OpenAI + kiểm tra sản phẩm bằng MySQL
  -> chatbot 21A        -> rule-based fallback
  -> lưu metadata đã làm sạch để Admin đánh giá chất lượng
```

FAQ phù hợp giúp chính sách giao hàng, thanh toán và tài khoản nhất quán, đồng
thời tránh gọi API cho những câu hỏi lặp lại.

## Database

Chạy migration sau khi đã có database `vegetable_shop`:

```text
database/phase-21c-chatbot-admin-analytics.sql
```

Migration có thể chạy lặp lại và tạo:

- `chatbot_faqs`: câu hỏi chuẩn, câu trả lời, từ khóa, thứ tự, trạng thái và audit.
- `chatbot_interactions`: câu hỏi đã che dữ liệu nhạy cảm, nguồn trả lời, kết quả,
  số sản phẩm, thời gian phản hồi, FAQ đã khớp và thời điểm.
- 5 FAQ mẫu về giao hàng, thanh toán, đơn hàng, quên mật khẩu và liên hệ.

Khi dùng cấu hình phát triển `spring.jpa.hibernate.ddl-auto=update`, Hibernate có
thể tự tạo hai bảng. File migration vẫn là nguồn tham chiếu để triển khai có kiểm
soát và tạo dữ liệu mẫu.

## Route Admin

| Route | Chức năng |
| --- | --- |
| `/admin/chatbot` | Dashboard, bộ lọc và lịch sử đã làm sạch |
| `/admin/chatbot/faqs` | Danh sách FAQ |
| `/admin/chatbot/faqs/new` | Tạo FAQ |
| `/admin/chatbot/faqs/{id}/edit` | Sửa FAQ |

Các route thay đổi dữ liệu dùng `POST`, giữ CSRF và yêu cầu quyền `ADMIN` theo
cấu hình bảo mật hiện tại.

## Riêng tư và lưu trữ

Hệ thống chỉ lưu dữ liệu tối thiểu phục vụ đánh giá chatbot:

- Email được thay bằng `[email]`.
- Số điện thoại được thay bằng `[số điện thoại]`.
- Dãy 12–19 chữ số được thay bằng `[dãy số nhạy cảm]`.
- Câu hỏi được giới hạn 300 ký tự.
- Không lưu user ID, session ID, IP, API key hoặc toàn bộ câu trả lời AI.
- Request OpenAI của 21B tiếp tục đặt `store=false`.

Thời hạn mặc định là 30 ngày. Có thể cấu hình trước khi chạy ứng dụng:

```text
CHATBOT_ANALYTICS_RETENTION_DAYS=30
```

Giá trị tối thiểu mà backend chấp nhận là 7 ngày. Nút **Dọn dữ liệu quá hạn** ở
dashboard xóa các dòng cũ hơn thời hạn trên.

## Sử dụng

1. Đăng nhập bằng tài khoản `ADMIN`.
2. Mở **Hệ thống → Chatbot & FAQ**.
3. Chọn **Quản lý FAQ → Thêm FAQ**.
4. Nhập một câu hỏi chuẩn, câu trả lời ngắn gọn và các cụm từ khóa rõ nghĩa.
5. Mở website và hỏi chatbot bằng một trong các cụm từ khóa.
6. Quay lại dashboard để kiểm tra nguồn `FAQ quản trị` và thời gian phản hồi.

Không nên dùng từ khóa một từ quá chung như `đơn`, `giá` hoặc `hàng`, vì có thể
khớp nhầm câu hỏi mua sản phẩm.

## Tệp chính

- `controller/AdminChatbotController.java`
- `service/AdminChatbotService.java`
- `service/ChatbotFaqService.java`
- `service/ChatbotAnalyticsService.java`
- `service/ChatbotFacade.java`
- `entity/ChatbotFaq.java`
- `entity/ChatbotInteraction.java`
- `templates/admin/chatbot-*.html`
- `database/phase-21c-chatbot-admin-analytics.sql`

## Kiểm thử

Test bao phủ ưu tiên FAQ trước AI, so khớp tiếng Việt, che email/điện thoại/dãy
số, tổng hợp dashboard, thời hạn dọn dữ liệu và render ba màn hình Admin có CSRF.

```text
Tests run: 188, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Backup

- Trước triển khai:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-21C-20260830-2243.zip`
- SHA-256 bản trước triển khai:
  `D282DF82737A8FC9594348ECC830A02A26F306200762BD3338984C41BB091D68`
- Sau triển khai và kiểm thử:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-21C-verified-20260830-2301.zip`
- SHA-256 bản sau triển khai:
  `D43CD37C7734BA73421BA63DC056D13DE753F950B2A10394A0B72C352D98F799`
