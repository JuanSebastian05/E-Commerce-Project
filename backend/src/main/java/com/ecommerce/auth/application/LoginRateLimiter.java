package com.ecommerce.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Limita los intentos fallidos de login por email e IP (decisión D5, ADR-003).
 *
 * <p>Guarda las horas de los fallos en memoria con una ventana deslizante. Solo
 * es correcto con una única instancia del backend; con varias habría que mover
 * el estado a un almacenamiento compartido.
 */
@Component
@EnableConfigurationProperties(LoginRateLimitProperties.class)
public class LoginRateLimiter {

    /** Al superar este número de claves se purgan las que ya no tienen fallos vigentes. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final LoginRateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginRateLimiter(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Lanza {@link TooManyLoginAttemptsException} si la clave está bloqueada. */
    public void checkAllowed(String email, String clientIp) {
        Deque<Instant> attempts = failures.get(key(email, clientIp));
        if (attempts == null) {
            return;
        }
        Instant now = clock.instant();
        synchronized (attempts) {
            prune(attempts, now);
            if (attempts.size() >= properties.maxFailures()) {
                Duration retryAfter = Duration.between(now, attempts.peekFirst().plus(properties.window()));
                throw new TooManyLoginAttemptsException(retryAfter);
            }
        }
    }

    public void recordFailure(String email, String clientIp) {
        Instant now = clock.instant();
        if (failures.size() > CLEANUP_THRESHOLD) {
            purgeExpired(now);
        }
        Deque<Instant> attempts = failures.computeIfAbsent(key(email, clientIp), k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, now);
            attempts.addLast(now);
        }
    }

    public void recordSuccess(String email, String clientIp) {
        failures.remove(key(email, clientIp));
    }

    private void prune(Deque<Instant> attempts, Instant now) {
        Instant windowStart = now.minus(properties.window());
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(windowStart)) {
            attempts.removeFirst();
        }
    }

    private void purgeExpired(Instant now) {
        failures.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }

    private static String key(String email, String clientIp) {
        return email.strip().toLowerCase(Locale.ROOT) + "|" + clientIp;
    }
}
