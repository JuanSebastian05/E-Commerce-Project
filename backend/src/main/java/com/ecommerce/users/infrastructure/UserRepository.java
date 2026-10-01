package com.ecommerce.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.users.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Recibe el email ya normalizado con {@link User#normalizeEmail(String)}. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
