package com.ecommerce.users.domain;

/** Códigos de permiso del módulo Users, tal como los crea la migración V3. */
public final class Permissions {

    public static final String USERS_READ = "users:read";
    public static final String USERS_MANAGE = "users:manage";
    public static final String ROLES_READ = "roles:read";
    public static final String ROLES_MANAGE = "roles:manage";

    private Permissions() {
    }
}
