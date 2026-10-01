package com.ecommerce.users.application;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class RoleNotFoundException extends ApiException {

    public RoleNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Rol no encontrado");
    }
}
