package com.ecommerce.users.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.users.domain.SystemRoles;
import com.ecommerce.users.infrastructure.UserRepository;

/**
 * Crea el primer usuario ADMIN al arrancar si todavía no existe ninguno
 * (decisión D4 del diseño, ADR-003). Si ya hay un ADMIN no hace nada, así
 * que cambiar las variables después no modifica cuentas existentes.
 */
@Component
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final UserService userService;

    public AdminBootstrap(BootstrapAdminProperties properties, UserRepository userRepository, UserService userService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRoles_Name(SystemRoles.ADMIN)) {
            return;
        }
        if (!properties.isConfigured()) {
            log.warn("No hay ningún usuario ADMIN. Define ADMIN_EMAIL y ADMIN_PASSWORD para crearlo al arrancar.");
            return;
        }
        userService.createUser(properties.email(), properties.password(), "Administrador", "Inicial", SystemRoles.ADMIN);
        log.info("Usuario ADMIN inicial creado");
    }
}
