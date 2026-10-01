package com.ecommerce.users.application;

import java.util.UUID;

/**
 * Se publica cuando un usuario se desactiva o cambian sus roles. El módulo Auth
 * lo escucha para revocar las sesiones del usuario sin que Users dependa de Auth.
 */
public record UserAccessChangedEvent(UUID userId) {
}
