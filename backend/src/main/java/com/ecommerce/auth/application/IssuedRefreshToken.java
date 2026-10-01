package com.ecommerce.auth.application;

import java.time.Duration;

/** Valor en claro de un refresh token recién emitido; solo existe en memoria y en la cookie. */
public record IssuedRefreshToken(String value, Duration ttl) {
}
