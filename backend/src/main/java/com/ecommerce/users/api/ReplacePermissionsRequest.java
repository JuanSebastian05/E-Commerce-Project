package com.ecommerce.users.api;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReplacePermissionsRequest(@NotNull Set<@NotBlank String> permissions) {
}
