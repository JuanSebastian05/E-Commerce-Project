package com.ecommerce.users.application;

import java.util.Collection;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class UnknownRoleException extends ApiException {

    public UnknownRoleException(Collection<String> names) {
        super(HttpStatus.BAD_REQUEST, "Roles inexistentes: " + String.join(", ", names));
    }
}
