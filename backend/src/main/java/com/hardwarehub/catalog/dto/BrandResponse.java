package com.hardwarehub.catalog.dto;

import java.time.Instant;

public record BrandResponse(
        Long id,
        String name,
        String description,
        String logoUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
