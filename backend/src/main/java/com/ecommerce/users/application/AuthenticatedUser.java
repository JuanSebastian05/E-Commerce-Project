package com.ecommerce.users.application;

import java.util.Set;
import java.util.UUID;

/** Usuario con credenciales verificadas y sus permisos efectivos. */
public record AuthenticatedUser(UUID id, Set<String> permissions) {
}
