package com.hardwarehub.common.config;

import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.user.domain.RoleName;
import com.hardwarehub.user.domain.User;
import com.hardwarehub.user.repository.RoleRepository;
import com.hardwarehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsernameAndDeletedAtIsNull("owner").isPresent()) {
            return;
        }

        User owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@hardwarehub.local");
        owner.setPasswordHash(passwordEncoder.encode("Owner@123"));
        owner.setFirstName("Store");
        owner.setLastName("Owner");
        owner.setActive(true);
        owner.setCreatedBy("system");
        owner.setUpdatedBy("system");
        owner.setRoles(Set.of(
                roleRepository.findByName(RoleName.OWNER)
                        .orElseThrow(() -> new IllegalStateException("OWNER role missing"))
        ));
        userRepository.save(owner);
        auditService.log("SEED", "USER", String.valueOf(owner.getId()), "Default owner user created");
        log.info("Seeded default owner user (username=owner)");
    }
}
