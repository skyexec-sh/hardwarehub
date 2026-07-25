package com.hardwarehub.user.web;

import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.user.domain.RoleName;
import com.hardwarehub.user.dto.AdminResetPasswordRequest;
import com.hardwarehub.user.dto.CreateUserRequest;
import com.hardwarehub.user.dto.UpdateUserRequest;
import com.hardwarehub.user.dto.UserResponse;
import com.hardwarehub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(
                search, username, name, email, role, active, PageRequest.of(page, size, Sort.by("username")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public UserResponse get(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate user")
    public UserResponse activate(@PathVariable Long id) {
        return userService.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user")
    public UserResponse deactivate(@PathVariable Long id) {
        return userService.deactivate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete user")
    public void delete(@PathVariable Long id) {
        userService.softDelete(id);
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Admin reset user password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.adminResetPassword(id, request);
    }
}
