package com.ecommerce.shared.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configuración de los access tokens.
 *
 * @param secret         clave HS256; mínimo 32 caracteres. Llega siempre por variable de entorno.
 * @param issuer         emisor que se escribe y se exige en cada token.
 * @param accessTokenTtl duración del access token.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    @NotBlank @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres") String secret,
    @NotBlank String issuer,
    @NotNull Duration accessTokenTtl) {
}
