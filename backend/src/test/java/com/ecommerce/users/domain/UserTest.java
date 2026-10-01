package com.ecommerce.users.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void normalizesEmailToLowercaseWithoutSurroundingSpaces() {
        User user = new User("  Ana.Perez@Example.COM ", "hash", " Ana ", " Pérez ");

        assertThat(user.getEmail()).isEqualTo("ana.perez@example.com");
        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isEqualTo("Pérez");
    }

    @Test
    void newUserIsEnabledWithoutRolesOrPermissions() {
        User user = new User("ana@example.com", "hash", "Ana", "Pérez");

        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getRoles()).isEmpty();
        assertThat(user.permissionCodes()).isEmpty();
    }

    @Test
    void disableAndEnableToggleTheStatus() {
        User user = new User("ana@example.com", "hash", "Ana", "Pérez");

        user.disable();
        assertThat(user.isEnabled()).isFalse();

        user.enable();
        assertThat(user.isEnabled()).isTrue();
    }
}
