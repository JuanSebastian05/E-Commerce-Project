package com.ecommerce.users.application;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ecommerce.users.domain.Role;
import com.ecommerce.users.domain.User;

/** Datos de un usuario para la administración (nunca incluye el hash de la contraseña). */
public record UserView(
    UUID id,
    String email,
    String firstName,
    String lastName,
    boolean enabled,
    Set<String> roles,
    Instant createdAt) {

    static UserView from(User user) {
        return new UserView(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.isEnabled(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toUnmodifiableSet()),
            user.getCreatedAt());
    }
}
