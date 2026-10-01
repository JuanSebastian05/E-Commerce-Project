package com.ecommerce.users.application;

import java.util.Collection;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class UnknownPermissionException extends ApiException {

    public UnknownPermissionException(Collection<String> codes) {
        super(HttpStatus.BAD_REQUEST, "Permisos inexistentes: " + String.join(", ", codes));
    }
}
