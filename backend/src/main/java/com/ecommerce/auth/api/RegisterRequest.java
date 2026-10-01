package com.ecommerce.auth.api;

import com.ecommerce.users.application.RegisterCustomerCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank String password,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName) {

    RegisterCustomerCommand toCommand() {
        return new RegisterCustomerCommand(email, password, firstName, lastName);
    }
}
