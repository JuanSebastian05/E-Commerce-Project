package com.ecommerce.users.domain;

import org.springframework.http.HttpStatus;

import com.ecommerce.shared.web.ApiException;

public class InvalidPasswordException extends ApiException {

    public InvalidPasswordException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
