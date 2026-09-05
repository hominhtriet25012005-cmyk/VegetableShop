package com.vegetableshop;

import com.vegetableshop.service.MailDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
    "spring.mail.username=test@example.com",
    "spring.mail.password=test-app-password",
    "app.mail.from=test@example.com",
    "app.base-url=http://localhost:8081"
})
@ActiveProfiles({"template", "mail"})
class MailProfileContextTests {

    @Autowired private JavaMailSender javaMailSender;
    @Autowired private MailDeliveryService mailDeliveryService;

    @Test
    void mailProfileCreatesSenderAndDeliveryService() {
        assertNotNull(javaMailSender);
        assertNotNull(mailDeliveryService);
    }
}
