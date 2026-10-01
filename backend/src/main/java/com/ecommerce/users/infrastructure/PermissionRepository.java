package com.ecommerce.users.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.users.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<Permission> findByCodeIn(Collection<String> codes);
}
