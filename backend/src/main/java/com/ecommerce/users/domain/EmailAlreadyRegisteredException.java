package com.ecommerce.users.domain;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email");
    }
}
