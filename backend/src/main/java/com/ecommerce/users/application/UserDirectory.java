package com.ecommerce.users.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato público del módulo Users para el resto de módulos (por ejemplo Auth).
 * Ningún otro módulo debe usar las entidades ni los repositorios de Users.
 */
public interface UserDirectory {

    /** Registra un cliente con el rol CUSTOMER (HU-01, RN-09). */
    UserProfile registerCustomer(RegisterCustomerCommand command);

    /**
     * Comprueba email y contraseña. Devuelve vacío si no coinciden o si el
     * usuario está desactivado, sin distinguir el motivo (RN-04).
     */
    Optional<AuthenticatedUser> verifyCredentials(String email, String rawPassword);

    /** Usuario activo con sus permisos actuales; vacío si no existe o está desactivado. */
    Optional<AuthenticatedUser> findActiveUser(UUID userId);

    UserProfile getProfile(UUID userId);
}
