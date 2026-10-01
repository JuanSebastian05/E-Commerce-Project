package com.ecommerce.auth.application;

/** Resultado de un login o una renovación: access token y nuevo refresh token. */
public record AuthSession(AccessToken accessToken, IssuedRefreshToken refreshToken) {
}
