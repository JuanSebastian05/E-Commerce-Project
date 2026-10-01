package com.ecommerce.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.domain.RefreshToken;
import com.ecommerce.auth.infrastructure.RefreshTokenRepository;

/**
 * Emite, rota y revoca refresh tokens (HU-03, HU-04, ADR-003).
 *
 * <p>El token es un valor aleatorio de 256 bits; en la base de datos solo se
 * guarda su SHA-256. Cada renovación revoca el token usado y emite otro de la
 * misma familia. Si llega un token ya revocado, alguien lo está reutilizando
 * (posible robo) y se revoca toda la familia.
 */
@Service
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final RefreshTokenProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Emite el primer token de una sesión nueva. */
    @Transactional
    public IssuedRefreshToken issueNewSession(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /**
     * Consume un token válido y devuelve el id de su usuario y su familia.
     * Lanza {@link InvalidRefreshTokenException} si no existe, caducó o ya se
     * usó; en este último caso revoca antes toda la familia.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public ConsumedRefreshToken consume(String rawToken) {
        Instant now = clock.instant();
        RefreshToken token = repository.findByTokenHashForUpdate(hash(rawToken))
            .orElseThrow(InvalidRefreshTokenException::new);
        if (token.getRevokedAt() != null) {
            log.warn("Reutilización de un refresh token revocado; se revoca la sesión completa del usuario {}",
                token.getUserId());
            repository.revokeFamily(token.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (!token.isActive(now)) {
            throw new InvalidRefreshTokenException();
        }
        token.revoke(now);
        return new ConsumedRefreshToken(token.getUserId(), token.getFamilyId());
    }

    /** Emite el siguiente token de la misma sesión, tras {@link #consume(String)}. */
    @Transactional(propagation = Propagation.MANDATORY)
    public IssuedRefreshToken rotate(ConsumedRefreshToken consumed) {
        return issue(consumed.userId(), consumed.familyId());
    }

    @Transactional
    public void revokeFamily(UUID familyId) {
        repository.revokeFamily(familyId, clock.instant());
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }

    /** Logout: revoca la sesión del token si existe. Un token desconocido se ignora. */
    @Transactional
    public void revokeSessionOf(String rawToken) {
        Optional<RefreshToken> token = repository.findByTokenHash(hash(rawToken));
        token.ifPresent(found -> repository.revokeFamily(found.getFamilyId(), clock.instant()));
    }

    private IssuedRefreshToken issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        repository.save(new RefreshToken(userId, hash(raw), familyId, now, now.plus(properties.ttl())));
        return new IssuedRefreshToken(raw, properties.ttl());
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public record ConsumedRefreshToken(UUID userId, UUID familyId) {
    }
}
