package com.ecommerce.users.api;

import java.util.Set;

import com.ecommerce.users.application.CreateUserCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank String password,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotEmpty Set<@NotBlank String> roles) {

    CreateUserCommand toCommand() {
        return new CreateUserCommand(email, password, firstName, lastName, roles);
    }
}
