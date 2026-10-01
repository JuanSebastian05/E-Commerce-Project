package com.ecommerce.users.application;

import com.ecommerce.users.domain.Permission;

public record PermissionView(String code, String description) {

    static PermissionView from(Permission permission) {
        return new PermissionView(permission.getCode(), permission.getDescription());
    }
}
