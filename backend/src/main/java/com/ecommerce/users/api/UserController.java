package com.ecommerce.users.api;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.users.application.UserDirectory;
import com.ecommerce.users.application.UserProfile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Usuarios")
public class UserController {

    private final UserDirectory userDirectory;

    public UserController(UserDirectory userDirectory) {
        this.userDirectory = userDirectory;
    }

    /** HU-05: los datos se leen de la base de datos, no del token, para que estén al día. */
    @GetMapping("/me")
    @Operation(summary = "Perfil, roles y permisos del usuario autenticado")
    public UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        return userDirectory.getProfile(UUID.fromString(jwt.getSubject()));
    }
}
