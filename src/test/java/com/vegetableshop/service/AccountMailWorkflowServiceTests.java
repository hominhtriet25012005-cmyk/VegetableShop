package com.vegetableshop.service;

import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.entity.User;
import com.vegetableshop.event.AccountActivationMailEvent;
import com.vegetableshop.event.PasswordResetMailEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountMailWorkflowServiceTests {

    @Mock private UserService userService;
    @Mock private AccountActivationService activationService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void mailRegistrationRequiresActivationAndPublishesAfterCreatingToken() {
        RegisterRequest request = new RegisterRequest();
        User user = user();
        when(userService.register(request, false)).thenReturn(user);
        when(activationService.createToken(user)).thenReturn("a".repeat(43));

        var result = service(true).register(request);

        assertTrue(result.activationRequired());
        verify(eventPublisher).publishEvent(any(AccountActivationMailEvent.class));
    }

    @Test
    void registrationWithoutMailRemainsImmediatelyUsable() {
        RegisterRequest request = new RegisterRequest();
        when(userService.register(request, true)).thenReturn(user());

        var result = service(false).register(request);

        assertFalse(result.activationRequired());
        verify(activationService, never()).createToken(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void passwordResetPublishesMailOnlyForEligibleAccount() {
        User user = user();
        when(passwordResetService.createToken("user@example.com")).thenReturn(Optional.of("a".repeat(43)));
        when(userService.findByEmail("user@example.com")).thenReturn(user);

        assertTrue(service(true).requestPasswordReset("user@example.com").isPresent());

        verify(eventPublisher).publishEvent(any(PasswordResetMailEvent.class));
    }

    private AccountMailWorkflowService service(boolean mailEnabled) {
        return new AccountMailWorkflowService(
            userService, activationService, passwordResetService, eventPublisher, mailEnabled
        );
    }

    private User user() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setFullName("Nguyễn Văn An");
        return user;
    }
}
