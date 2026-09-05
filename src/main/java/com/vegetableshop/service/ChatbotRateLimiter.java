package com.vegetableshop.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;

@Component
public class ChatbotRateLimiter {

    private static final String SESSION_ATTRIBUTE = ChatbotRateLimiter.class.getName() + ".window";
    private static final int DEFAULT_REQUEST_LIMIT = 30;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

    private final int requestLimit;
    private final long windowMillis;
    private final Clock clock;

    public ChatbotRateLimiter() {
        this(DEFAULT_REQUEST_LIMIT, DEFAULT_WINDOW, Clock.systemUTC());
    }

    ChatbotRateLimiter(int requestLimit, Duration window, Clock clock) {
        this.requestLimit = requestLimit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public boolean tryAcquire(HttpSession session) {
        synchronized (session) {
            long now = clock.millis();
            RequestWindow current = (RequestWindow) session.getAttribute(SESSION_ATTRIBUTE);
            if (current == null || now - current.startedAt() >= windowMillis) {
                session.setAttribute(SESSION_ATTRIBUTE, new RequestWindow(now, 1));
                return true;
            }
            if (current.count() >= requestLimit) {
                return false;
            }
            session.setAttribute(SESSION_ATTRIBUTE, new RequestWindow(current.startedAt(), current.count() + 1));
            return true;
        }
    }

    private record RequestWindow(long startedAt, int count) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
