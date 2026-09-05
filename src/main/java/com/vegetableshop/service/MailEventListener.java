package com.vegetableshop.service;

import com.vegetableshop.event.AccountActivationMailEvent;
import com.vegetableshop.event.OrderPlacedMailEvent;
import com.vegetableshop.event.OrderStatusChangedMailEvent;
import com.vegetableshop.event.PasswordResetMailEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("mail")
public class MailEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailEventListener.class);
    private final MailDeliveryService mailDeliveryService;

    public MailEventListener(MailDeliveryService mailDeliveryService) {
        this.mailDeliveryService = mailDeliveryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void accountActivation(AccountActivationMailEvent event) {
        safely("kích hoạt tài khoản", event.email(), () -> mailDeliveryService.sendAccountActivation(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void passwordReset(PasswordResetMailEvent event) {
        safely("đặt lại mật khẩu", event.email(), () -> mailDeliveryService.sendPasswordReset(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void orderConfirmation(OrderPlacedMailEvent event) {
        safely("xác nhận đơn hàng", event.email(), () -> mailDeliveryService.sendOrderConfirmation(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void orderStatus(OrderStatusChangedMailEvent event) {
        safely("trạng thái đơn hàng", event.email(), () -> mailDeliveryService.sendOrderStatusChanged(event));
    }

    private void safely(String type, String recipient, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            LOGGER.error("Gửi email {} tới {} thất bại; dữ liệu nghiệp vụ đã được lưu", type, recipient, exception);
        }
    }
}
