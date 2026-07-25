package com.hardwarehub.pricing.dto;

import java.time.Instant;

public record PriceLevelResponse(
        Long id,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
