package com.ecommerce.auth.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Genera tokens con apariencia válida pero firmados con una clave ajena, para pruebas de seguridad. */
final class ForgedTokens {

    private ForgedTokens() {
    }

    static String signedWithWrongKey(String subject) {
        var key = new SecretKeySpec("clave-de-un-atacante-0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("ecommerce-api")
            .subject(subject)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(600))
            .claim("permissions", List.of("users:manage", "roles:manage"))
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    }
}
