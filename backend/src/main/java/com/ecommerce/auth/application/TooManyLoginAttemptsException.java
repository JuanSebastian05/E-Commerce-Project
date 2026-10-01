package com.ecommerce.auth.application;

import java.time.Duration;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class TooManyLoginAttemptsException extends ApiException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Inténtalo de nuevo más tarde");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
