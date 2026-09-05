-- Vegetable Shop - Phase 21C: managed chatbot FAQ and privacy-aware analytics.
-- MySQL 8.x. Safe to execute more than once.

USE vegetable_shop;

CREATE TABLE IF NOT EXISTS chatbot_faqs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question VARCHAR(200) NOT NULL,
    answer VARCHAR(1500) NOT NULL,
    keywords VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(150) NOT NULL DEFAULT 'SYSTEM',
    updated_by VARCHAR(150) NOT NULL DEFAULT 'SYSTEM',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_chatbot_faqs_question UNIQUE (question),
    CONSTRAINT chk_chatbot_faqs_display_order CHECK (display_order >= 0),
    INDEX idx_chatbot_faqs_status_order (status, display_order, id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS chatbot_interactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question VARCHAR(300) NOT NULL,
    response_source VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    product_count INT NOT NULL DEFAULT 0,
    response_time_ms BIGINT NOT NULL DEFAULT 0,
    matched_faq_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_chatbot_interactions_source CHECK (
        response_source IN ('MANAGED_FAQ', 'AI', 'RULE_BASED', 'AI_FALLBACK')
    ),
    CONSTRAINT chk_chatbot_interactions_status CHECK (
        status IN ('RESOLVED', 'NEEDS_REVIEW')
    ),
    CONSTRAINT chk_chatbot_interactions_product_count CHECK (product_count >= 0),
    CONSTRAINT chk_chatbot_interactions_response_time CHECK (response_time_ms >= 0),
    CONSTRAINT fk_chatbot_interactions_faq FOREIGN KEY (matched_faq_id)
        REFERENCES chatbot_faqs(id) ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_chatbot_interactions_created (created_at),
    INDEX idx_chatbot_interactions_source_created (response_source, created_at),
    INDEX idx_chatbot_interactions_status_created (status, created_at),
    INDEX idx_chatbot_interactions_faq (matched_faq_id)
) ENGINE = InnoDB;

INSERT IGNORE INTO chatbot_faqs
    (question, answer, keywords, display_order, status, created_by, updated_by)
VALUES
    ('Phí giao hàng được tính như thế nào?',
     'Phí giao hàng được hiển thị rõ ở bước thanh toán và phụ thuộc vào địa chỉ nhận hàng. Bạn hãy mở giỏ hàng, chọn Thanh toán và nhập địa chỉ để xem thông tin áp dụng.',
     'phí giao hàng, phí ship, tiền ship, vận chuyển', 10, TRUE, 'SYSTEM', 'SYSTEM'),
    ('Cửa hàng hỗ trợ những phương thức thanh toán nào?',
     'Cửa hàng hỗ trợ thanh toán khi nhận hàng (COD). Các phương thức trực tuyến chỉ xuất hiện khi đã được quản trị viên cấu hình và kích hoạt.',
     'thanh toán, COD, trả tiền, phương thức thanh toán', 20, TRUE, 'SYSTEM', 'SYSTEM'),
    ('Tôi xem trạng thái đơn hàng ở đâu?',
     'Sau khi đăng nhập, bạn mở Tiện ích và chọn Đơn hàng của tôi để xem mã đơn, trạng thái xử lý và chi tiết từng sản phẩm.',
     'trạng thái đơn, theo dõi đơn, đơn hàng của tôi, kiểm tra đơn', 30, TRUE, 'SYSTEM', 'SYSTEM'),
    ('Tôi quên mật khẩu thì phải làm sao?',
     'Tại trang đăng nhập, chọn Quên mật khẩu, nhập email đã đăng ký và làm theo liên kết được gửi đến email của bạn.',
     'quên mật khẩu, đặt lại mật khẩu, không đăng nhập được', 40, TRUE, 'SYSTEM', 'SYSTEM'),
    ('Tôi liên hệ cửa hàng bằng cách nào?',
     'Bạn có thể mở trang Liên hệ trên thanh menu và gửi nội dung cần hỗ trợ. Cửa hàng sẽ phản hồi qua thông tin liên hệ bạn cung cấp.',
     'liên hệ, hỗ trợ, hotline, email cửa hàng', 50, TRUE, 'SYSTEM', 'SYSTEM');

-- Verification:
-- SELECT * FROM chatbot_faqs ORDER BY display_order, id;
-- SELECT response_source, status, COUNT(*) FROM chatbot_interactions GROUP BY response_source, status;
