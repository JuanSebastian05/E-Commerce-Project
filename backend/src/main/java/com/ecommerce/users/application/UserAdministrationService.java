package com.ecommerce.users.application;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.shared.web.BusinessRuleException;
import com.ecommerce.users.domain.Permissions;
import com.ecommerce.users.domain.Role;
import com.ecommerce.users.domain.User;
import com.ecommerce.users.infrastructure.RoleRepository;
import com.ecommerce.users.infrastructure.UserRepository;
import com.ecommerce.users.infrastructure.UserSpecifications;

/**
 * Administración de usuarios (HU-06). La autorización por permisos se aplica en
 * el controlador; aquí viven las reglas de negocio.
 *
 * <p>Las acciones se registran en el log hasta que exista el módulo Audit.
 */
@Service
public class UserAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(UserAdministrationService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final RoleManagementGuard roleManagementGuard;
    private final ApplicationEventPublisher events;

    public UserAdministrationService(UserRepository userRepository, RoleRepository roleRepository,
            UserService userService, RoleManagementGuard roleManagementGuard, ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.roleManagementGuard = roleManagementGuard;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<UserView> search(String email, String role, Boolean enabled, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("email"));
        return userRepository.findAll(UserSpecifications.matching(email, role, enabled), pageRequest)
            .map(UserView::from);
    }

    @Transactional(readOnly = true)
    public UserView get(UUID userId) {
        return UserView.from(find(userId));
    }

    @Transactional
    public UserView create(UUID actorId, CreateUserCommand command) {
        List<Role> roles = resolveRoles(command.roles());
        User user = userService.createUser(command.email(), command.rawPassword(), command.firstName(),
            command.lastName(), roles);
        log.info("Usuario {} creado por {} con roles {}", user.getId(), actorId, command.roles());
        return UserView.from(user);
    }

    /** Activa o desactiva. Un usuario no puede desactivarse a sí mismo. */
    @Transactional
    public UserView changeStatus(UUID actorId, UUID userId, boolean enabled) {
        User user = find(userId);
        if (!enabled && actorId.equals(userId)) {
            throw new BusinessRuleException("No puedes desactivar tu propia cuenta");
        }
        if (user.isEnabled() == enabled) {
            return UserView.from(user);
        }
        if (enabled) {
            user.enable();
        } else {
            user.disable();
            roleManagementGuard.ensureSomeoneCanStillManageRoles();
            events.publishEvent(new UserAccessChangedEvent(userId));
        }
        log.info("Usuario {} {} por {}", userId, enabled ? "activado" : "desactivado", actorId);
        return UserView.from(user);
    }

    /**
     * Reemplaza los roles. Un usuario no puede quitarse a sí mismo el permiso
     * de gestionar usuarios, y siempre debe quedar alguien con roles:manage (RN-08).
     */
    @Transactional
    public UserView replaceRoles(UUID actorId, UUID userId, Set<String> roleNames) {
        User user = find(userId);
        List<Role> roles = resolveRoles(roleNames);
        user.replaceRoles(roles);
        if (actorId.equals(userId) && !user.permissionCodes().contains(Permissions.USERS_MANAGE)) {
            throw new BusinessRuleException("No puedes quitarte a ti mismo el permiso de gestionar usuarios");
        }
        roleManagementGuard.ensureSomeoneCanStillManageRoles();
        events.publishEvent(new UserAccessChangedEvent(userId));
        log.info("Roles del usuario {} cambiados a {} por {}", userId, roleNames, actorId);
        return UserView.from(user);
    }

    private User find(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private List<Role> resolveRoles(Collection<String> names) {
        Set<String> normalized = names.stream()
            .map(name -> name.strip().toUpperCase(Locale.ROOT))
            .collect(Collectors.toSet());
        List<Role> roles = roleRepository.findByNameIn(normalized);
        if (roles.size() != normalized.size()) {
            Set<String> found = roles.stream().map(Role::getName).collect(Collectors.toSet());
            throw new UnknownRoleException(normalized.stream().filter(n -> !found.contains(n)).sorted().toList());
        }
        return roles;
    }
}
