package com.ecommerce.users.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.ecommerce.shared.web.BusinessRuleException;
import com.ecommerce.users.domain.Permissions;
import com.ecommerce.users.domain.User;
import com.ecommerce.users.infrastructure.RoleRepository;
import com.ecommerce.users.infrastructure.UserRepository;

class UserAdministrationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final UserAdministrationService service = new UserAdministrationService(
        userRepository, mock(RoleRepository.class), mock(UserService.class), events);

    @Test
    void refusesToDisableTheLastActiveUserWhoCanManageRoles() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId))
            .thenReturn(Optional.of(new User("ultimo-admin@example.com", "hash", "Ana", "Pérez")));
        when(userRepository.countActiveUsersWithPermission(Permissions.ROLES_MANAGE)).thenReturn(0L);

        assertThatThrownBy(() -> service.changeStatus(UUID.randomUUID(), targetId, false))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("sin ningún usuario activo que pueda administrar roles");
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void publishesAccessChangedEventWhenDisabling() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId))
            .thenReturn(Optional.of(new User("ana@example.com", "hash", "Ana", "Pérez")));
        when(userRepository.countActiveUsersWithPermission(Permissions.ROLES_MANAGE)).thenReturn(1L);

        service.changeStatus(UUID.randomUUID(), targetId, false);

        verify(events).publishEvent(new UserAccessChangedEvent(targetId));
    }
}
