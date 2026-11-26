package com.planning.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// Захист від перебору паролів: після 10 невдалих спроб з однієї адреси
// вхід блокується на 10 хвилин. Успішний вхід скидає лічильник.
@Component
public class LoginAttemptLimiter {

    private static final int MAX_FAILURES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private record Attempts(int failures, Instant firstFailure) {
    }

    private final Map<String, Attempts> byAddress = new ConcurrentHashMap<>();

    public boolean isBlocked(String address) {
        Attempts attempts = byAddress.get(address);
        if (attempts == null) {
            return false;
        }
        if (attempts.firstFailure().plus(WINDOW).isBefore(Instant.now())) {
            byAddress.remove(address); // вікно минуло — починаємо з нуля
            return false;
        }
        return attempts.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String address) {
        byAddress.merge(address, new Attempts(1, Instant.now()),
                (old, ignored) -> new Attempts(old.failures() + 1, old.firstFailure()));
    }

    public void recordSuccess(String address) {
        byAddress.remove(address);
    }
}
