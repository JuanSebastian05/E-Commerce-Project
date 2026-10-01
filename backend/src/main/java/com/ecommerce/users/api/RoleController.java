package com.ecommerce.users.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.users.application.PermissionView;
import com.ecommerce.users.application.RoleService;
import com.ecommerce.users.application.RoleView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Roles", description = "Roles y permisos")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('roles:read')")
    @Operation(summary = "Lista los roles con sus permisos (HU-07)")
    public List<RoleView> listRoles() {
        return roleService.listRoles();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('roles:manage')")
    @Operation(summary = "Crea un rol con los permisos indicados (HU-07)")
    public ResponseEntity<RoleView> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRoleRequest request) {
        RoleView role = roleService.create(actorId(jwt), request.name(), request.description(), request.permissions());
        return ResponseEntity.created(URI.create("/api/v1/roles/" + role.id())).body(role);
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('roles:manage')")
    @Operation(summary = "Reemplaza los permisos de un rol (HU-07)")
    public RoleView replacePermissions(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ReplacePermissionsRequest request) {
        return roleService.replacePermissions(actorId(jwt), id, request.permissions());
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('roles:manage')")
    @Operation(summary = "Borra un rol que no sea de sistema ni tenga usuarios (HU-07)")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        roleService.delete(actorId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('roles:read')")
    @Operation(summary = "Catálogo de permisos del sistema (HU-07)")
    public List<PermissionView> listPermissions() {
        return roleService.listPermissions();
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
