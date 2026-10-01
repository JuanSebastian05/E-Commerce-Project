package com.ecommerce.users.api;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,49}$",
        message = "Solo letras, números y guiones bajos; entre 2 y 50 caracteres, empezando por letra")
    String name,
    @Size(max = 255) String description,
    @NotNull Set<@NotBlank String> permissions) {
}
