package com.ecommerce.shared.web;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que se devuelve al cliente con un código HTTP concreto.
 * El mensaje debe ser seguro para mostrarse: nunca datos internos.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
