package com.ecommerce.users.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"clave123", "Contraseña9", "a1234567"})
    void acceptsPasswordsWithLetterDigitAndMinimumLength(String password) {
        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc123", "solamenteletras", "12345678"})
    void rejectsShortPasswordsOrWithoutLetterAndDigit(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password)).isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void acceptsExactly72BytesAndRejectsMore() {
        assertThatCode(() -> PasswordPolicy.validate("a1" + "x".repeat(70))).doesNotThrowAnyException();
        assertThatThrownBy(() -> PasswordPolicy.validate("a1" + "x".repeat(71)))
            .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void countsBytesNotCharactersForTheUpperLimit() {
        // 40 caracteres "ñ" ocupan 80 bytes en UTF-8: supera el límite de BCrypt.
        assertThatThrownBy(() -> PasswordPolicy.validate("1" + "ñ".repeat(40)))
            .isInstanceOf(InvalidPasswordException.class);
    }
}
