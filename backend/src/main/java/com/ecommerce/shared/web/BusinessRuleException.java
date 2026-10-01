package com.ecommerce.shared.web;

import org.springframework.http.HttpStatus;

/** Operación válida en forma pero prohibida por una regla de negocio (409). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
