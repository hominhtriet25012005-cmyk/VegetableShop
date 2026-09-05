package com.vegetableshop.service;

import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.entity.User;
import com.vegetableshop.event.AccountActivationMailEvent;
import com.vegetableshop.event.PasswordResetMailEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Profile("mysql")
public class AccountMailWorkflowService {

    private final UserService userService;
    private final AccountActivationService activationService;
    private final PasswordResetService passwordResetService;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean mailEnabled;

    public AccountMailWorkflowService(
        UserService userService,
        AccountActivationService activationService,
        PasswordResetService passwordResetService,
        ApplicationEventPublisher eventPublisher,
        @Value("${app.mail.enabled:false}") boolean mailEnabled
    ) {
        this.userService = userService;
        this.activationService = activationService;
        this.passwordResetService = passwordResetService;
        this.eventPublisher = eventPublisher;
        this.mailEnabled = mailEnabled;
    }

    @Transactional
    public RegistrationResult register(RegisterRequest request) {
        User user = userService.register(request, !mailEnabled);
        if (!mailEnabled) {
            return new RegistrationResult(user, false);
        }
        String rawToken = activationService.createToken(user);
        eventPublisher.publishEvent(new AccountActivationMailEvent(
            user.getEmail(), user.getFullName(), rawToken
        ));
        return new RegistrationResult(user, true);
    }

    @Transactional
    public Optional<String> requestPasswordReset(String email) {
        Optional<String> token = passwordResetService.createToken(email);
        if (mailEnabled) {
            token.ifPresent(rawToken -> {
                User user = userService.findByEmail(email);
                eventPublisher.publishEvent(new PasswordResetMailEvent(
                    user.getEmail(), user.getFullName(), rawToken
                ));
            });
        }
        return token;
    }

    @Transactional
    public void resendActivation(String email) {
        if (!mailEnabled) {
            return;
        }
        activationService.createTokenForUnverifiedEmail(email).ifPresent(request ->
            eventPublisher.publishEvent(new AccountActivationMailEvent(
                request.user().getEmail(), request.user().getFullName(), request.rawToken()
            ))
        );
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public record RegistrationResult(User user, boolean activationRequired) {
    }
}
