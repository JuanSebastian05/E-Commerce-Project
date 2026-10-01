package com.ecommerce.users.domain;

import java.util.Set;

/** Nombres de los roles de sistema creados por la migración V3. */
public final class SystemRoles {

    public static final String ADMIN = "ADMIN";
    public static final String CUSTOMER = "CUSTOMER";

    /** Permisos que el rol ADMIN conserva siempre, para no perder la administración. */
    public static final Set<String> ADMIN_REQUIRED_PERMISSIONS =
        Set.of(Permissions.USERS_MANAGE, Permissions.ROLES_MANAGE);

    private SystemRoles() {
    }
}
