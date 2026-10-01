package com.ecommerce.users.application;

import org.springframework.stereotype.Component;

import com.ecommerce.shared.web.BusinessRuleException;
import com.ecommerce.users.domain.Permissions;
import com.ecommerce.users.infrastructure.UserRepository;

/** RN-08: tras cualquier cambio debe seguir existiendo un usuario activo con roles:manage. */
@Component
class RoleManagementGuard {

    private final UserRepository userRepository;

    RoleManagementGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Debe llamarse dentro de la transacción del cambio, después de aplicarlo. */
    void ensureSomeoneCanStillManageRoles() {
        userRepository.flush();
        if (userRepository.countActiveUsersWithPermission(Permissions.ROLES_MANAGE) == 0) {
            throw new BusinessRuleException(
                "El cambio dejaría el sistema sin ningún usuario activo que pueda administrar roles");
        }
    }
}
