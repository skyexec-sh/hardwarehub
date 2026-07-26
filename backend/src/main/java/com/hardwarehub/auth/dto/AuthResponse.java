package com.hardwarehub.auth.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName,
            List<String> roles
    ) {
    }
}
