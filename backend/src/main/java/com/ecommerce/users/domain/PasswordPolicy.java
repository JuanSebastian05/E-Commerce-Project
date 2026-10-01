package com.ecommerce.users.domain;

import java.nio.charset.StandardCharsets;

/**
 * RN-02: entre 8 caracteres y 72 bytes (límite de BCrypt), con al menos una
 * letra y un número.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.codePointCount(0, rawPassword.length()) < MIN_LENGTH) {
            throw new InvalidPasswordException("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidPasswordException("La contraseña es demasiado larga");
        }
        boolean hasLetter = rawPassword.codePoints().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.codePoints().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new InvalidPasswordException("La contraseña debe contener al menos una letra y un número");
        }
    }
}
