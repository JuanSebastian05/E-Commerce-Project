package com.ecommerce.users.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.shared.web.PageResponse;
import com.ecommerce.users.application.UserAdministrationService;
import com.ecommerce.users.application.UserDirectory;
import com.ecommerce.users.application.UserProfile;
import com.ecommerce.users.application.UserView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Usuarios")
public class UserController {

    private final UserDirectory userDirectory;
    private final UserAdministrationService administration;

    public UserController(UserDirectory userDirectory, UserAdministrationService administration) {
        this.userDirectory = userDirectory;
        this.administration = administration;
    }

    /** HU-05: los datos se leen de la base de datos, no del token, para que estén al día. */
    @GetMapping("/me")
    @Operation(summary = "Perfil, roles y permisos del usuario autenticado")
    public UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        return userDirectory.getProfile(actorId(jwt));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "Lista usuarios con filtros, ordenados por email (HU-06)")
    public PageResponse<UserView> search(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(administration.search(email, role, enabled, page, size), view -> view);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "Detalle de un usuario (HU-06)")
    public UserView get(@PathVariable UUID id) {
        return administration.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users:manage')")
    @Operation(summary = "Crea un usuario con los roles indicados (HU-06)")
    public ResponseEntity<UserView> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserRequest request) {
        UserView created = administration.create(actorId(jwt), request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('users:manage')")
    @Operation(summary = "Activa o desactiva un usuario; desactivar revoca sus sesiones (HU-06)")
    public UserView changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return administration.changeStatus(actorId(jwt), id, request.enabled());
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('users:manage')")
    @Operation(summary = "Reemplaza los roles de un usuario y revoca sus sesiones (HU-06)")
    public UserView replaceRoles(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ReplaceRolesRequest request) {
        return administration.replaceRoles(actorId(jwt), id, request.roles());
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
