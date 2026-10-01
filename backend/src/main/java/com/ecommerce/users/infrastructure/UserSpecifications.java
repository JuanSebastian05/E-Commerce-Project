package com.ecommerce.users.infrastructure;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.ecommerce.users.domain.Role;
import com.ecommerce.users.domain.User;

import jakarta.persistence.criteria.Join;

/** Filtros del listado de usuarios. Cada filtro nulo se ignora. */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> matching(String emailContains, String roleName, Boolean enabled) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (emailContains != null && !emailContains.isBlank()) {
            String pattern = "%" + escapeLike(emailContains.strip().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.like(root.get("email"), pattern, '\\'));
        }
        if (roleName != null && !roleName.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<User, Role> roles = root.join("roles");
                return cb.equal(roles.get("name"), roleName.strip().toUpperCase(Locale.ROOT));
            });
        }
        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }
        return spec;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
