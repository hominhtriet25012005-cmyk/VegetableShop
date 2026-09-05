package com.vegetableshop.service;

import com.vegetableshop.event.AccountActivationMailEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class MailEventListenerTests {

    @Mock private MailDeliveryService mailDeliveryService;

    @Test
    void smtpFailureDoesNotRollbackCompletedBusinessAction() {
        var event = new AccountActivationMailEvent("user@example.com", "User", "a".repeat(43));
        doThrow(new IllegalStateException("SMTP unavailable"))
            .when(mailDeliveryService).sendAccountActivation(event);

        assertDoesNotThrow(() -> new MailEventListener(mailDeliveryService).accountActivation(event));
    }
}
