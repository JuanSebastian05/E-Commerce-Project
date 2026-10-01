package com.ecommerce.auth.application;

public record AccessToken(String value, long expiresInSeconds) {
}
