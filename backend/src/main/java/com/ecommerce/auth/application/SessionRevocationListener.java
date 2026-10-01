package com.ecommerce.auth.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ecommerce.users.application.UserAccessChangedEvent;

/**
 * Revoca los refresh tokens de un usuario cuando se desactiva o cambian sus
 * roles. Se ejecuta en la misma transacción que el cambio, así que si este se
 * deshace, la revocación también.
 */
@Component
public class SessionRevocationListener {

    private final RefreshTokenService refreshTokenService;

    public SessionRevocationListener(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @EventListener
    public void onUserAccessChanged(UserAccessChangedEvent event) {
        refreshTokenService.revokeAllForUser(event.userId());
    }
}
