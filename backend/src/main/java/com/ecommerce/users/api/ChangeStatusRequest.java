package com.ecommerce.users.api;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull Boolean enabled) {
}
