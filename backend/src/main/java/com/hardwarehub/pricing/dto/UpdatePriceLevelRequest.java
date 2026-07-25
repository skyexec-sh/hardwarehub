package com.hardwarehub.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePriceLevelRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        Boolean active
) {
}
