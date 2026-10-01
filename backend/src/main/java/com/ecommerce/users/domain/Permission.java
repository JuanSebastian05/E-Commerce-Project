package com.ecommerce.users.domain;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Permiso del catálogo fijo del sistema (por ejemplo {@code users:read}).
 * Se crean solo por migración, porque cada uno corresponde a una comprobación
 * en el código; por eso la entidad no expone constructor público ni setters.
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(nullable = false)
    private String description;

    protected Permission() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Permission permission)) {
            return false;
        }
        return id != null && Objects.equals(id, permission.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
