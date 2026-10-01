package com.ecommerce.auth.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Máximo de intentos fallidos de login por email e IP dentro de una ventana. */
@ConfigurationProperties(prefix = "app.security.login-rate-limit")
public record LoginRateLimitProperties(int maxFailures, Duration window) {
}
