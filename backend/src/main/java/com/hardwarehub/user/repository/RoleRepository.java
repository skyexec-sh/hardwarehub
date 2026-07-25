package com.hardwarehub.user.repository;

import com.hardwarehub.user.domain.Role;
import com.hardwarehub.user.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
