package com.vegetableshop.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
@Profile("mysql")
public class CaptchaChallengeService {

    static final String SESSION_PREFIX = "captcha.challenge.";
    private static final Duration VALIDITY = Duration.ofMinutes(10);
    private final SecureRandom random;

    public CaptchaChallengeService() {
        this(new SecureRandom());
    }

    CaptchaChallengeService(SecureRandom random) {
        this.random = random;
    }

    public String issue(HttpSession session, String purpose) {
        int left = random.nextInt(8) + 2;
        int right = random.nextInt(8) + 2;
        CaptchaChallenge challenge = new CaptchaChallenge(
            left + " + " + right + " = ?",
            Integer.toString(left + right),
            Instant.now().plus(VALIDITY)
        );
        session.setAttribute(key(purpose), challenge);
        return challenge.question();
    }

    public boolean verify(HttpSession session, String purpose, String submittedAnswer) {
        Object stored = session.getAttribute(key(purpose));
        session.removeAttribute(key(purpose));
        if (!(stored instanceof CaptchaChallenge challenge)
            || challenge.expiresAt().isBefore(Instant.now())
            || submittedAnswer == null) {
            return false;
        }
        return challenge.answer().equals(submittedAnswer.trim());
    }

    private String key(String purpose) {
        if (purpose == null || !purpose.matches("[a-z-]{3,30}")) {
            throw new IllegalArgumentException("Mục đích CAPTCHA không hợp lệ");
        }
        return SESSION_PREFIX + purpose;
    }

    record CaptchaChallenge(String question, String answer, Instant expiresAt) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
