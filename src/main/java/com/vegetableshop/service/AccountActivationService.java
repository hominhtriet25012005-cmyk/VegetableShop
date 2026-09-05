package com.vegetableshop.service;

import com.vegetableshop.entity.AccountActivationToken;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.InvalidAccountActivationTokenException;
import com.vegetableshop.repository.AccountActivationTokenRepository;
import com.vegetableshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@Profile("mysql")
public class AccountActivationService {

    private final AccountActivationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final long tokenHours;

    public AccountActivationService(
        AccountActivationTokenRepository tokenRepository,
        UserRepository userRepository,
        @Value("${app.account-activation.token-hours:24}") long tokenHours
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.tokenHours = Math.max(1, tokenHours);
    }

    @Transactional
    public String createToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.findAllByUserAndUsedAtIsNull(user).forEach(token -> token.setUsedAt(now));
        String rawToken = SecureTokenSupport.generate();
        AccountActivationToken token = new AccountActivationToken();
        token.setUser(user);
        token.setTokenHash(SecureTokenSupport.hash(rawToken));
        token.setExpiresAt(now.plusHours(tokenHours));
        tokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public Optional<ActivationRequest> createTokenForUnverifiedEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
            .filter(User::isStatus)
            .filter(user -> !user.isEmailVerified())
            .map(user -> new ActivationRequest(user, createToken(user)));
    }

    @Transactional
    public User activate(String rawToken) {
        AccountActivationToken token;
        try {
            token = tokenRepository.findForUpdateByTokenHash(SecureTokenSupport.hash(rawToken))
                .orElseThrow(this::invalidToken);
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }

        LocalDateTime now = LocalDateTime.now();
        User user = token.getUser();
        if (token.isUsed() || token.isExpired(now) || !user.isStatus()) {
            throw invalidToken();
        }
        user.setEmailVerified(true);
        token.setUsedAt(now);
        userRepository.save(user);
        tokenRepository.save(token);
        return user;
    }

    private InvalidAccountActivationTokenException invalidToken() {
        return new InvalidAccountActivationTokenException(
            "Liên kết kích hoạt không hợp lệ, đã hết hạn hoặc đã được sử dụng"
        );
    }

    public record ActivationRequest(User user, String rawToken) {
    }
}
