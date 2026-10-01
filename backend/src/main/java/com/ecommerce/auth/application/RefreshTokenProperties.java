package com.ecommerce.auth.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param ttl          duración de cada refresh token.
 * @param cookieSecure marca la cookie como {@code Secure}. Los navegadores la
 *                     aceptan también en http://localhost.
 */
@ConfigurationProperties(prefix = "app.security.refresh-token")
public record RefreshTokenProperties(Duration ttl, boolean cookieSecure) {
}
