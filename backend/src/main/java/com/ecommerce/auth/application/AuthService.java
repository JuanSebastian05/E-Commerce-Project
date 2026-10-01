package com.ecommerce.auth.application;

import org.springframework.stereotype.Service;

import com.ecommerce.users.application.RegisterCustomerCommand;
import com.ecommerce.users.application.UserDirectory;
import com.ecommerce.users.application.UserProfile;

@Service
public class AuthService {

    private final UserDirectory userDirectory;
    private final AccessTokenService accessTokenService;

    public AuthService(UserDirectory userDirectory, AccessTokenService accessTokenService) {
        this.userDirectory = userDirectory;
        this.accessTokenService = accessTokenService;
    }

    public UserProfile register(RegisterCustomerCommand command) {
        return userDirectory.registerCustomer(command);
    }

    public AccessToken login(String email, String rawPassword) {
        return userDirectory.verifyCredentials(email, rawPassword)
            .map(accessTokenService::issue)
            .orElseThrow(InvalidCredentialsException::new);
    }
}
