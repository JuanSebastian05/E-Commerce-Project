package com.ecommerce.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.users.domain.User;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /** Recibe el email ya normalizado con {@link User#normalizeEmail(String)}. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRoles_Name(String roleName);

    /** Usuarios activos que tienen el permiso indicado a través de alguno de sus roles (RN-08). */
    @Query("""
        select count(distinct u) from User u
        join u.roles r
        join r.permissions p
        where u.enabled = true and p.code = :permissionCode
        """)
    long countActiveUsersWithPermission(@Param("permissionCode") String permissionCode);
}
