package com.vegetableshop.service;

import com.vegetableshop.entity.PasswordResetToken;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.InvalidPasswordResetTokenException;
import com.vegetableshop.repository.PasswordResetTokenRepository;
import com.vegetableshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@Profile("mysql")
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long tokenMinutes;

    public PasswordResetService(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        UserService userService,
        @Value("${app.password-reset.token-minutes:30}") long tokenMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.tokenMinutes = Math.max(5, tokenMinutes);
    }

    @Transactional
    public Optional<String> createToken(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        Optional<User> candidate = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT));
        if (candidate.isEmpty() || !candidate.get().isStatus() || !candidate.get().isEmailVerified()
            || candidate.get().getPassword() == null) {
            return Optional.empty();
        }

        User user = candidate.get();
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.findAllByUserAndUsedAtIsNull(user).forEach(token -> token.setUsedAt(now));

        String rawToken = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(now.plusMinutes(tokenMinutes));
        tokenRepository.save(token);
        return Optional.of(rawToken);
    }

    @Transactional(readOnly = true)
    public boolean isValid(String rawToken) {
        if (!isRawTokenFormat(rawToken)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findByTokenHash(hash(rawToken))
            .filter(token -> !token.isUsed())
            .filter(token -> !token.isExpired(now))
            .filter(token -> token.getUser().isStatus())
            .filter(token -> token.getUser().isEmailVerified())
            .isPresent();
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findForUpdateByTokenHash(hash(rawToken))
            .orElseThrow(() -> invalidToken());
        LocalDateTime now = LocalDateTime.now();
        if (token.isUsed() || token.isExpired(now) || !token.getUser().isStatus()
            || !token.getUser().isEmailVerified()) {
            throw invalidToken();
        }

        userService.setPassword(token.getUser(), newPassword);
        token.setUsedAt(now);
        tokenRepository.save(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        if (!isRawTokenFormat(rawToken)) {
            throw invalidToken();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", exception);
        }
    }

    private boolean isRawTokenFormat(String rawToken) {
        return rawToken != null && rawToken.matches("^[A-Za-z0-9_-]{43}$");
    }

    private InvalidPasswordResetTokenException invalidToken() {
        return new InvalidPasswordResetTokenException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
    }
}
