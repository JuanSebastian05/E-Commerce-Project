package com.ecommerce.auth.api;

import com.ecommerce.auth.application.AccessToken;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    static TokenResponse from(AccessToken token) {
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}
