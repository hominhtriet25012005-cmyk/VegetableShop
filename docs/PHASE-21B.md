# Giai đoạn 21B – Chatbot AI hiểu ngôn ngữ tự nhiên

## Kết quả

Giai đoạn 21B bổ sung OpenAI Responses API vào chatbot 21A theo mô hình hybrid:

- Hiểu câu hỏi tự nhiên, ngữ cảnh nhu cầu và cách diễn đạt linh hoạt hơn.
- Gợi ý tối đa 5 sản phẩm thật đang hoạt động và còn hàng trong MySQL.
- Dùng Structured Outputs (`json_schema`) để nhận câu trả lời, ID sản phẩm và
  gợi ý thao tác theo cấu trúc cố định.
- Backend kiểm tra lại toàn bộ ID do AI chọn; ID không có trong catalog bị loại.
- Giá, tồn kho, ảnh và URL chi tiết luôn do backend lấy từ entity `Product`.
- Khi tắt AI, thiếu mạng, timeout, API lỗi hoặc JSON không hợp lệ, hệ thống tự
  quay về chatbot rule-based 21A.
- Widget hiển thị rõ `AI · kiểm chứng bằng dữ liệu cửa hàng` hoặc `Chế độ cơ bản`.

Giai đoạn này không cần migration database.

## Luồng xử lý

```text
Browser -> ChatbotController -> ChatbotFacade
                              |-> OpenAI Responses API
                              |      -> structured JSON + product IDs
                              |-> kiểm tra IDs với catalog MySQL
                              |-> ChatbotResponse + product cards thật
                              `-> ChatbotService 21A khi AI không sẵn sàng
```

AI không truy vấn database trực tiếp và không được phép ghi dữ liệu. Backend chỉ
gửi một catalog rút gọn tối đa 60 sản phẩm đang bán/còn hàng trong mỗi yêu cầu.

## Cấu hình IntelliJ IDEA

Trong **Run > Edit Configurations > VegetableShopApplication**:

1. Giữ `Active profiles` là `mysql` hoặc `mysql,mail`.
2. Mở `Environment variables` và thêm:

```text
CHATBOT_AI_ENABLED=true
OPENAI_API_KEY=<OPENAI_API_KEY_CUA_BAN>
```

Tùy chọn:

```text
OPENAI_MODEL=gpt-5.4-nano
OPENAI_TIMEOUT_SECONDS=12
OPENAI_MAX_OUTPUT_TOKENS=500
```

Sau đó dừng và chạy lại ứng dụng. Không thêm API key vào `application.properties`,
ảnh chụp màn hình, README, Git hoặc JavaScript phía trình duyệt.

## Cấu hình PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:CHATBOT_AI_ENABLED = "true"
$env:OPENAI_API_KEY = "<OPENAI_API_KEY_CUA_BAN>"
.\mvnw.cmd spring-boot:run
```

Để tắt AI nhưng vẫn giữ chatbot 21A:

```powershell
$env:CHATBOT_AI_ENABLED = "false"
```

## Quy tắc an toàn và độ tin cậy

- API key chỉ tồn tại ở biến môi trường của backend.
- Request đặt `store=false`; project không lưu lịch sử hội thoại vào database.
- Câu hỏi vẫn bị giới hạn 300 ký tự và 30 lần/phút/session như 21A.
- Catalog được làm sạch ký tự xuống dòng/phân cách và giới hạn độ dài trước khi gửi.
- Prompt yêu cầu bỏ qua chỉ dẫn nằm trong dữ liệu sản phẩm và prompt injection.
- Structured Output chỉ cho phép intent, câu trả lời, tối đa 5 ID và 4 gợi ý.
- Backend loại ID lạ, ID trùng, sản phẩm hết hàng hoặc ngừng bán.
- Log không chứa API key hoặc toàn bộ nội dung câu hỏi của khách.
- Chatbot không yêu cầu mật khẩu, OTP hoặc thông tin thanh toán.

## Xử lý lỗi

| Tình huống | Kết quả |
| --- | --- |
| `CHATBOT_AI_ENABLED=false` | Dùng 21A, không gọi OpenAI |
| Bật AI nhưng thiếu API key | Ứng dụng dừng sớm và báo cấu hình thiếu |
| Timeout, HTTP 429/5xx hoặc mất mạng | Tự dùng 21A cho câu hỏi đó |
| AI trả ID không thuộc catalog | Backend loại ID |
| AI trả JSON thiếu/không hợp lệ | Tự dùng 21A |

## Chi phí và vận hành

OpenAI API là dịch vụ tính phí riêng với ChatGPT. Nên đặt giới hạn chi tiêu trong
tài khoản OpenAI, giữ rate limit của ứng dụng và chỉ bật AI khi cần demo. Model
mặc định là `gpt-5.4-nano`; có thể đổi bằng `OPENAI_MODEL` mà không sửa code.

## Tệp chính

- `config/OpenAiChatConfiguration.java`
- `config/OpenAiChatProperties.java`
- `service/AiChatClient.java`
- `service/OpenAiResponsesChatClient.java`
- `service/ChatbotFacade.java`
- `dto/AiChatDecision.java`
- `dto/ChatbotResponse.java`
- `static/js/chatbot.js`
- `application-mysql.properties`

## Kiểm thử

Test bao phủ request Structured Output và `store=false`, đọc `output_text`, lựa
chọn sản phẩm hợp lệ, loại ID lạ/trùng, tự fallback khi API lỗi và giữ nguyên
validation/rate limit ở controller.

```text
Tests run: 180, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Backup

- Trước triển khai:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-before-phase-21B-20260830-2216.zip`
- Sau triển khai và kiểm thử:
  `D:\JavaProjects\TrietHo\backups\VegetableShop-after-phase-21B-verified-20260830-2229.zip`
- SHA-256 bản sau triển khai:
  `47399D66E024731E0DF291842A73365017F03DA5D54F7ABF0AE135C4A1BFC2E8`
