package com.ecommerce.users.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.TestcontainersConfig;
import com.ecommerce.auth.domain.RefreshToken;
import com.ecommerce.auth.infrastructure.RefreshTokenRepository;
import com.ecommerce.users.domain.Permission;
import com.ecommerce.users.domain.Role;
import com.ecommerce.users.domain.User;

@SpringBootTest
@Import(TestcontainersConfig.class)
@Transactional
class UserPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsSystemRolesWithTheirPermissions() {
        assertThat(permissionCodesOf("ADMIN"))
            .containsExactlyInAnyOrder("backoffice:access", "users:read", "users:manage", "roles:read", "roles:manage");
        assertThat(permissionCodesOf("SUPPORT")).containsExactlyInAnyOrder("backoffice:access", "users:read");
        assertThat(permissionCodesOf("WAREHOUSE")).containsExactly("backoffice:access");
        assertThat(permissionCodesOf("CUSTOMER")).isEmpty();

        assertThat(roleRepository.findAll()).allMatch(Role::isSystem);
    }

    @Test
    void persistsUserWithRolesAndResolvesEffectivePermissions() {
        User user = new User("Ana@Example.com", "hash", "Ana", "Pérez");
        user.replaceRoles(List.of(role("SUPPORT"), role("WAREHOUSE")));
        userRepository.saveAndFlush(user);

        User found = userRepository.findByEmail("ana@example.com").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getRoles()).extracting(Role::getName).containsExactlyInAnyOrder("SUPPORT", "WAREHOUSE");
        assertThat(found.permissionCodes()).containsExactlyInAnyOrder("backoffice:access", "users:read");
        assertThat(userRepository.existsByEmail("ana@example.com")).isTrue();
    }

    @Test
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(new User("ana@example.com", "hash", "Ana", "Pérez"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("ANA@example.com", "hash", "Otra", "Persona")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsEmailNotInLowercase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, first_name, last_name) VALUES (?, 'hash', 'Ana', 'Pérez')",
                "Ana@Example.com"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsMalformedPermissionCode() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO permissions (code, description) VALUES ('Users-Read', 'mal formado')"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void customRoleCanReceivePermissions() {
        Role role = new Role("CATALOG_EDITOR", "Edita el catálogo");
        role.replacePermissions(permissionRepository.findByCodeIn(List.of("backoffice:access")));
        roleRepository.saveAndFlush(role);

        Role found = roleRepository.findByName("CATALOG_EDITOR").orElseThrow();
        assertThat(found.isSystem()).isFalse();
        assertThat(found.getPermissions()).extracting(Permission::getCode).containsExactly("backoffice:access");
    }

    @Test
    void storesRefreshTokenByHashAndTracksRevocation() {
        User user = userRepository.saveAndFlush(new User("ana@example.com", "hash", "Ana", "Pérez"));
        Instant now = Instant.now();
        String hash = "a".repeat(64);
        refreshTokenRepository.saveAndFlush(
            new RefreshToken(user.getId(), hash, UUID.randomUUID(), now, now.plus(7, ChronoUnit.DAYS)));

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        assertThat(token.isActive(now)).isTrue();

        token.revoke(now);
        assertThat(token.isActive(now)).isFalse();
        assertThat(token.isActive(now.plus(8, ChronoUnit.DAYS))).isFalse();
    }

    private Role role(String name) {
        return roleRepository.findByName(name).orElseThrow();
    }

    private List<String> permissionCodesOf(String roleName) {
        return role(roleName).getPermissions().stream().map(Permission::getCode).toList();
    }
}
