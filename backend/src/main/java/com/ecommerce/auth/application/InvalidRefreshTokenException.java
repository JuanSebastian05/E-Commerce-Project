package com.ecommerce.auth.application;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "La sesión no es válida o ha caducado");
    }
}
