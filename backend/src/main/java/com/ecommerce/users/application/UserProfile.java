package com.ecommerce.users.application;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ecommerce.users.domain.Role;
import com.ecommerce.users.domain.User;

public record UserProfile(
    UUID id,
    String email,
    String firstName,
    String lastName,
    Set<String> roles,
    Set<String> permissions) {

    static UserProfile from(User user) {
        return new UserProfile(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toUnmodifiableSet()),
            user.permissionCodes());
    }
}
