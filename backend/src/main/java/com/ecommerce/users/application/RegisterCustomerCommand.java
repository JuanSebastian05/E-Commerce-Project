package com.ecommerce.users.application;

public record RegisterCustomerCommand(String email, String rawPassword, String firstName, String lastName) {
}
