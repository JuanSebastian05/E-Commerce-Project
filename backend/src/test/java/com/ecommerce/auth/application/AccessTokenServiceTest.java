package com.ecommerce.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.ecommerce.shared.security.JwtConfig;
import com.ecommerce.shared.security.JwtProperties;
import com.ecommerce.users.application.AuthenticatedUser;

class AccessTokenServiceTest {

    private final JwtProperties properties =
        new JwtProperties("secreto-de-prueba-0123456789-abcdefghij", "ecommerce-api", Duration.ofMinutes(15));
    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void issuesTokenWithSubjectPermissionsIssuerAndExpiry() {
        Instant now = Instant.now();
        AccessTokenService service = new AccessTokenService(
            jwtConfig.jwtEncoder(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
        UUID userId = UUID.randomUUID();

        AccessToken token = service.issue(new AuthenticatedUser(userId, Set.of("users:read")));
        Jwt decoded = jwtConfig.jwtDecoder(properties).decode(token.value());

        assertThat(token.expiresInSeconds()).isEqualTo(900);
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("ecommerce-api");
        assertThat(decoded.getClaimAsStringList(JwtConfig.PERMISSIONS_CLAIM)).containsExactly("users:read");
        assertThat(decoded.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }
}
