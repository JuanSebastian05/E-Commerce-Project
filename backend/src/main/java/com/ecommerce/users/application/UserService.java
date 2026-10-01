package com.ecommerce.users.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.users.domain.EmailAlreadyRegisteredException;
import com.ecommerce.users.domain.PasswordPolicy;
import com.ecommerce.users.domain.Role;
import com.ecommerce.users.domain.SystemRoles;
import com.ecommerce.users.domain.User;
import com.ecommerce.users.infrastructure.RoleRepository;
import com.ecommerce.users.infrastructure.UserRepository;

@Service
public class UserService implements UserDirectory {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Hash de una contraseña cualquiera. Se compara contra él cuando el email
     * no existe, para que el tiempo de respuesta no revele qué emails están
     * registrados.
     */
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public UserProfile registerCustomer(RegisterCustomerCommand command) {
        User user = createUser(command.email(), command.rawPassword(), command.firstName(), command.lastName(),
            SystemRoles.CUSTOMER);
        return UserProfile.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> verifyCredentials(String email, String rawPassword) {
        Optional<User> user = userRepository.findByEmail(User.normalizeEmail(email));
        if (user.isEmpty()) {
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            return Optional.empty();
        }
        User found = user.get();
        boolean passwordMatches = passwordEncoder.matches(rawPassword, found.getPasswordHash());
        if (!passwordMatches || !found.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(found.getId(), found.permissionCodes()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> findActiveUser(UUID userId) {
        return userRepository.findById(userId)
            .filter(User::isEnabled)
            .map(user -> new AuthenticatedUser(user.getId(), user.permissionCodes()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID userId) {
        return userRepository.findById(userId)
            .map(UserProfile::from)
            .orElseThrow(UserNotFoundException::new);
    }

    /** Crea un usuario con un rol de sistema, aplicando RN-01 y RN-02. */
    User createUser(String email, String rawPassword, String firstName, String lastName, String roleName) {
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("No existe el rol de sistema " + roleName));
        return createUser(email, rawPassword, firstName, lastName, List.of(role));
    }

    /** Crea un usuario con los roles indicados, aplicando RN-01 y RN-02. */
    User createUser(String email, String rawPassword, String firstName, String lastName, List<Role> roles) {
        String normalizedEmail = User.normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        PasswordPolicy.validate(rawPassword);

        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword), firstName, lastName);
        user.replaceRoles(roles);
        return userRepository.save(user);
    }
}
