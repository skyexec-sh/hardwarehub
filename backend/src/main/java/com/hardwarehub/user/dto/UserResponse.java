package com.hardwarehub.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        boolean active,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
