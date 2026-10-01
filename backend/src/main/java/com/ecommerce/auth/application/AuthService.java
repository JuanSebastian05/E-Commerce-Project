package com.ecommerce.auth.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.application.RefreshTokenService.ConsumedRefreshToken;
import com.ecommerce.users.application.AuthenticatedUser;
import com.ecommerce.users.application.RegisterCustomerCommand;
import com.ecommerce.users.application.UserDirectory;
import com.ecommerce.users.application.UserProfile;

@Service
public class AuthService {

    private final UserDirectory userDirectory;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(UserDirectory userDirectory, AccessTokenService accessTokenService,
            RefreshTokenService refreshTokenService, LoginRateLimiter loginRateLimiter) {
        this.userDirectory = userDirectory;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginRateLimiter = loginRateLimiter;
    }

    public UserProfile register(RegisterCustomerCommand command) {
        return userDirectory.registerCustomer(command);
    }

    /** HU-02: el límite de intentos se comprueba antes de mirar la contraseña. */
    public AuthSession login(String email, String rawPassword, String clientIp) {
        loginRateLimiter.checkAllowed(email, clientIp);
        AuthenticatedUser user = userDirectory.verifyCredentials(email, rawPassword).orElse(null);
        if (user == null) {
            loginRateLimiter.recordFailure(email, clientIp);
            throw new InvalidCredentialsException();
        }
        loginRateLimiter.recordSuccess(email, clientIp);
        return new AuthSession(accessTokenService.issue(user), refreshTokenService.issueNewSession(user.id()));
    }

    /**
     * HU-03: rota el refresh token y emite un access token con los permisos
     * actuales. Si el usuario ya no está activo, revoca la sesión (RN-04).
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthSession refresh(String rawRefreshToken) {
        ConsumedRefreshToken consumed = refreshTokenService.consume(rawRefreshToken);
        AuthenticatedUser user = userDirectory.findActiveUser(consumed.userId()).orElse(null);
        if (user == null) {
            refreshTokenService.revokeFamily(consumed.familyId());
            throw new InvalidRefreshTokenException();
        }
        return new AuthSession(accessTokenService.issue(user), refreshTokenService.rotate(consumed));
    }

    /** HU-04 */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeSessionOf(rawRefreshToken);
    }
}
