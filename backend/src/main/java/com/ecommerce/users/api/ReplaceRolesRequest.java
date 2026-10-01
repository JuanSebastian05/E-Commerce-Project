package com.ecommerce.users.api;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

public record ReplaceRolesRequest(@NotEmpty Set<@NotBlank String> roles) {
}
