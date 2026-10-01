package com.ecommerce.users.application;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.shared.web.BusinessRuleException;
import com.ecommerce.users.domain.Permission;
import com.ecommerce.users.domain.Role;
import com.ecommerce.users.domain.SystemRoles;
import com.ecommerce.users.infrastructure.PermissionRepository;
import com.ecommerce.users.infrastructure.RoleRepository;
import com.ecommerce.users.infrastructure.UserRepository;

/**
 * Gestión de roles y de los permisos de cada rol (HU-07).
 *
 * <p>Los permisos son un catálogo fijo (ADR-003): aquí solo se consultan y se
 * asignan. Un cambio en los permisos de un rol llega a sus usuarios en su
 * siguiente renovación de sesión, porque el refresh vuelve a leer los permisos.
 */
@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RoleManagementGuard roleManagementGuard;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
            UserRepository userRepository, RoleManagementGuard roleManagementGuard) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.roleManagementGuard = roleManagementGuard;
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles() {
        return roleRepository.findAll(Sort.by("name")).stream().map(RoleView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionView> listPermissions() {
        return permissionRepository.findAll(Sort.by("code")).stream().map(PermissionView::from).toList();
    }

    @Transactional
    public RoleView create(UUID actorId, String name, String description, Set<String> permissionCodes) {
        String normalizedName = name.strip().toUpperCase(Locale.ROOT);
        if (roleRepository.existsByName(normalizedName)) {
            throw new BusinessRuleException("Ya existe un rol con ese nombre");
        }
        Role role = new Role(normalizedName, description);
        role.replacePermissions(resolvePermissions(permissionCodes));
        roleRepository.save(role);
        log.info("Rol {} creado por {} con permisos {}", normalizedName, actorId, permissionCodes);
        return RoleView.from(role);
    }

    /** El rol ADMIN siempre conserva users:manage y roles:manage (HU-07), y se respeta RN-08. */
    @Transactional
    public RoleView replacePermissions(UUID actorId, UUID roleId, Set<String> permissionCodes) {
        Role role = find(roleId);
        List<Permission> permissions = resolvePermissions(permissionCodes);
        if (SystemRoles.ADMIN.equals(role.getName())) {
            Set<String> codes = permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
            if (!codes.containsAll(SystemRoles.ADMIN_REQUIRED_PERMISSIONS)) {
                throw new BusinessRuleException("El rol ADMIN debe conservar los permisos users:manage y roles:manage");
            }
        }
        role.replacePermissions(permissions);
        roleManagementGuard.ensureSomeoneCanStillManageRoles();
        log.info("Permisos del rol {} cambiados a {} por {}", role.getName(), permissionCodes, actorId);
        return RoleView.from(role);
    }

    /** RN-07: los roles de sistema no se borran. Tampoco un rol que tenga usuarios. */
    @Transactional
    public void delete(UUID actorId, UUID roleId) {
        Role role = find(roleId);
        if (role.isSystem()) {
            throw new BusinessRuleException("Los roles de sistema no se pueden borrar");
        }
        if (userRepository.existsByRoles_Id(roleId)) {
            throw new BusinessRuleException("El rol tiene usuarios asignados; quítaselo antes de borrarlo");
        }
        roleRepository.delete(role);
        log.info("Rol {} borrado por {}", role.getName(), actorId);
    }

    private Role find(UUID roleId) {
        return roleRepository.findById(roleId).orElseThrow(RoleNotFoundException::new);
    }

    private List<Permission> resolvePermissions(Collection<String> codes) {
        Set<String> normalized = codes.stream().map(code -> code.strip().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        List<Permission> permissions = permissionRepository.findByCodeIn(normalized);
        if (permissions.size() != normalized.size()) {
            Set<String> found = permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
            throw new UnknownPermissionException(normalized.stream().filter(c -> !found.contains(c)).sorted().toList());
        }
        return permissions;
    }
}
