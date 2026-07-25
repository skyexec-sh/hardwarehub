package com.hardwarehub.user.service;

import com.hardwarehub.auth.repository.RefreshTokenRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.UserPrincipal;
import com.hardwarehub.user.domain.Role;
import com.hardwarehub.user.domain.RoleName;
import com.hardwarehub.user.domain.User;
import com.hardwarehub.user.dto.AdminResetPasswordRequest;
import com.hardwarehub.user.dto.CreateUserRequest;
import com.hardwarehub.user.dto.UpdateUserRequest;
import com.hardwarehub.user.dto.UserResponse;
import com.hardwarehub.user.mapper.UserMapper;
import com.hardwarehub.user.repository.RoleRepository;
import com.hardwarehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(
            String search,
            String username,
            String name,
            String email,
            RoleName role,
            Boolean active,
            Pageable pageable) {
        Page<UserResponse> page =
                userRepository.search(search, username, name, email, role, active, pageable).map(userMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return userMapper.toResponse(requireUser(id));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.username())) {
            throw new BusinessException("USERNAME_EXISTS", "Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new BusinessException("EMAIL_EXISTS", "Email already exists", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setActive(true);
        user.setRoles(resolveRoles(request.roles()));
        user.setCreatedBy(currentUsername());
        user.setUpdatedBy(currentUsername());

        User saved = userRepository.save(user);
        auditService.log("CREATE", "USER", String.valueOf(saved.getId()), "User created: " + saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = requireUser(id);

        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new BusinessException("EMAIL_EXISTS", "Email already exists", HttpStatus.CONFLICT);
        }

        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setRoles(resolveRoles(request.roles()));
        user.setUpdatedBy(currentUsername());

        auditService.log("UPDATE", "USER", String.valueOf(user.getId()), "User updated");
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse activate(Long id) {
        User user = requireUser(id);
        user.setActive(true);
        user.setUpdatedBy(currentUsername());
        auditService.log("ACTIVATE", "USER", String.valueOf(user.getId()), "User activated");
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse deactivate(Long id) {
        User user = requireUser(id);
        ensureNotSelf(user);
        user.setActive(false);
        user.setUpdatedBy(currentUsername());
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.log("DEACTIVATE", "USER", String.valueOf(user.getId()), "User deactivated");
        return userMapper.toResponse(user);
    }

    @Transactional
    public void softDelete(Long id) {
        User user = requireUser(id);
        ensureNotSelf(user);
        user.setActive(false);
        user.setDeletedAt(Instant.now());
        user.setUpdatedBy(currentUsername());
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.log("DELETE", "USER", String.valueOf(user.getId()), "User soft-deleted");
    }

    @Transactional
    public void adminResetPassword(Long id, AdminResetPasswordRequest request) {
        User user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedBy(currentUsername());
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.log("RESET_PASSWORD", "USER", String.valueOf(user.getId()), "Admin reset password");
    }

    private User requireUser(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : roleNames) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name));
            roles.add(role);
        }
        return roles;
    }

    private void ensureNotSelf(User user) {
        String current = currentUsername();
        if (current != null && current.equals(user.getUsername())) {
            throw new BusinessException("SELF_ACTION_FORBIDDEN", "Cannot perform this action on your own account",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String currentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getUsername();
    }
}
