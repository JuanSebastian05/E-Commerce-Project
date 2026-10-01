package com.ecommerce.users.application;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ecommerce.users.domain.Permission;
import com.ecommerce.users.domain.Role;

public record RoleView(UUID id, String name, String description, boolean system, Set<String> permissions) {

    static RoleView from(Role role) {
        return new RoleView(
            role.getId(),
            role.getName(),
            role.getDescription(),
            role.isSystem(),
            role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toUnmodifiableSet()));
    }
}
