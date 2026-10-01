package com.ecommerce.users.application;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Usuario no encontrado");
    }
}
