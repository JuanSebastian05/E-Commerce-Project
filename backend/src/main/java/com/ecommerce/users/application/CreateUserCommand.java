package com.ecommerce.users.application;

import java.util.Set;

public record CreateUserCommand(String email, String rawPassword, String firstName, String lastName, Set<String> roles) {
}
