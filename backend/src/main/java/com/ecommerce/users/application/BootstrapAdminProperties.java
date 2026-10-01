package com.ecommerce.users.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciales del primer ADMIN; llegan por variables de entorno (ver ADR-003). */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password) {

    boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
