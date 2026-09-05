package com.vegetableshop.service;

import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.event.AccountActivationMailEvent;
import com.vegetableshop.event.OrderLineMailData;
import com.vegetableshop.event.OrderPlacedMailEvent;
import com.vegetableshop.event.OrderStatusChangedMailEvent;
import com.vegetableshop.event.PasswordResetMailEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpMailDeliveryServiceTests {

    @Test
    void rendersAndSendsAllTransactionalEmailTemplates() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        SmtpMailDeliveryService service = new SmtpMailDeliveryService(
            sender, templateEngine(), "http://localhost:8081/", "sender@example.com", "Vegetable Shop"
        );

        MimeMessage activation = message();
        when(sender.createMimeMessage()).thenReturn(activation);
        service.sendAccountActivation(new AccountActivationMailEvent(
            "user@example.com", "Nguyễn Văn An", "a".repeat(43)
        ));
        assertTrue(activation.getContent().toString().contains("activate-account?token="));
        verify(sender).send(activation);

        MimeMessage reset = message();
        when(sender.createMimeMessage()).thenReturn(reset);
        service.sendPasswordReset(new PasswordResetMailEvent(
            "user@example.com", "Nguyễn Văn An", "b".repeat(43)
        ));
        assertTrue(reset.getContent().toString().contains("reset-password?token="));
        verify(sender).send(reset);

        MimeMessage order = message();
        when(sender.createMimeMessage()).thenReturn(order);
        service.sendOrderConfirmation(new OrderPlacedMailEvent(
            "user@example.com", "Nguyễn Văn An", "VS-001", "Nguyễn Văn An", "0901234567",
            "123 Nguyễn Trãi", new BigDecimal("50000"),
            List.of(new OrderLineMailData("Cà rốt", 2, new BigDecimal("25000"), new BigDecimal("50000")))
        ));
        assertTrue(order.getContent().toString().contains("VS-001"));
        verify(sender).send(order);

        MimeMessage status = message();
        when(sender.createMimeMessage()).thenReturn(status);
        service.sendOrderStatusChanged(new OrderStatusChangedMailEvent(
            "user@example.com", "Nguyễn Văn An", "VS-001", OrderStatus.SHIPPING
        ));
        assertTrue(status.getContent().toString().contains("Đang giao hàng"));
        verify(sender).send(status);
    }

    private MimeMessage message() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
