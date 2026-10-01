package com.ecommerce.auth.api;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.auth.application.AuthService;
import com.ecommerce.auth.application.AuthSession;
import com.ecommerce.auth.application.InvalidRefreshTokenException;
import com.ecommerce.auth.application.RefreshTokenProperties;
import com.ecommerce.auth.application.TooManyLoginAttemptsException;
import com.ecommerce.users.application.UserProfile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Registro y sesión. El access token va en el cuerpo de la respuesta y el
 * refresh token en una cookie httpOnly que solo se envía a /api/v1/auth
 * (decisión D1, ADR-003).
 */
@RestController
@RequestMapping(AuthController.BASE_PATH)
@Tag(name = "Auth", description = "Registro e inicio de sesión")
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class AuthController {

    static final String BASE_PATH = "/api/v1/auth";
    static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final RefreshTokenProperties refreshTokenProperties;

    public AuthController(AuthService authService, RefreshTokenProperties refreshTokenProperties) {
        this.authService = authService;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    @PostMapping("/register")
    @Operation(summary = "Registra un cliente (HU-01)")
    public ResponseEntity<UserProfile> register(@Valid @RequestBody RegisterRequest request) {
        UserProfile profile = authService.register(request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/users/me")).body(profile);
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión: access token en el cuerpo y refresh token en cookie (HU-02)")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthSession session = authService.login(request.email(), request.password(), http.getRemoteAddr());
        return withSession(session);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva la sesión con la cookie del refresh token (HU-03)")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return withSession(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra la sesión y borra la cookie (HU-04)")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
            .build();
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    ResponseEntity<ProblemDetail> handleTooManyAttempts(TooManyLoginAttemptsException ex) {
        long seconds = Math.max(1, ex.getRetryAfter().toSeconds());
        return ResponseEntity.status(ex.getStatus())
            .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
            .body(ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage()));
    }

    private ResponseEntity<TokenResponse> withSession(AuthSession session) {
        ResponseCookie cookie = refreshCookie(session.refreshToken().value(), session.refreshToken().ttl());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(TokenResponse.from(session.accessToken()));
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
            .httpOnly(true)
            .secure(refreshTokenProperties.cookieSecure())
            .sameSite("Strict")
            .path(BASE_PATH)
            .maxAge(maxAge)
            .build();
    }
}
