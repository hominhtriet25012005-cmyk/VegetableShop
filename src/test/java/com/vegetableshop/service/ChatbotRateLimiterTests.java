package com.vegetableshop.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatbotRateLimiterTests {

    @Test
    void rejectsRequestsAfterSessionLimitIsReached() {
        ChatbotRateLimiter limiter = new ChatbotRateLimiter(
            2,
            Duration.ofMinutes(1),
            Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC)
        );
        MockHttpSession session = new MockHttpSession();

        assertTrue(limiter.tryAcquire(session));
        assertTrue(limiter.tryAcquire(session));
        assertFalse(limiter.tryAcquire(session));
    }
}
