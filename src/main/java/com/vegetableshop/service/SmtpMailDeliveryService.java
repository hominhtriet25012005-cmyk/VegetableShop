package com.vegetableshop.service;

import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.event.AccountActivationMailEvent;
import com.vegetableshop.event.OrderPlacedMailEvent;
import com.vegetableshop.event.OrderStatusChangedMailEvent;
import com.vegetableshop.event.PasswordResetMailEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("mail")
public class SmtpMailDeliveryService implements MailDeliveryService {

    private static final Map<OrderStatus, String> STATUS_LABELS = Map.of(
        OrderStatus.PENDING, "Chờ xác nhận",
        OrderStatus.CONFIRMED, "Đã xác nhận",
        OrderStatus.SHIPPING, "Đang giao hàng",
        OrderStatus.COMPLETED, "Hoàn tất",
        OrderStatus.CANCELLED, "Đã hủy"
    );

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String baseUrl;
    private final String fromAddress;
    private final String fromName;

    public SmtpMailDeliveryService(
        JavaMailSender mailSender,
        TemplateEngine templateEngine,
        @Value("${app.base-url}") String baseUrl,
        @Value("${app.mail.from}") String fromAddress,
        @Value("${app.mail.from-name:Vegetable Shop}") String fromName
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void sendAccountActivation(AccountActivationMailEvent event) {
        Context context = context();
        context.setVariable("fullName", event.fullName());
        context.setVariable("activationUrl", baseUrl + "/activate-account?token=" + event.rawToken());
        send(event.email(), "Kích hoạt tài khoản Vegetable Shop", "mail/account-activation", context);
    }

    @Override
    public void sendPasswordReset(PasswordResetMailEvent event) {
        Context context = context();
        context.setVariable("fullName", event.fullName());
        context.setVariable("resetUrl", baseUrl + "/reset-password?token=" + event.rawToken());
        send(event.email(), "Đặt lại mật khẩu Vegetable Shop", "mail/password-reset", context);
    }

    @Override
    public void sendOrderConfirmation(OrderPlacedMailEvent event) {
        Context context = context();
        context.setVariable("order", event);
        context.setVariable("orderUrl", baseUrl + "/my-orders");
        send(event.email(), "Xác nhận đơn hàng " + event.orderCode(), "mail/order-confirmation", context);
    }

    @Override
    public void sendOrderStatusChanged(OrderStatusChangedMailEvent event) {
        Context context = context();
        context.setVariable("order", event);
        context.setVariable("statusLabel", STATUS_LABELS.getOrDefault(event.status(), event.status().name()));
        context.setVariable("orderUrl", baseUrl + "/my-orders");
        send(event.email(), "Cập nhật đơn hàng " + event.orderCode(), "mail/order-status", context);
    }

    private void send(String recipient, String subject, String template, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, context), true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Không thể tạo email", exception);
        }
    }

    private Context context() {
        return new Context(Locale.forLanguageTag("vi-VN"));
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
