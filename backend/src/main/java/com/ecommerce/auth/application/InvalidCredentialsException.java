package com.ecommerce.auth.application;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

/** Mismo mensaje para credenciales incorrectas y usuario desactivado (HU-02). */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
    }
}
