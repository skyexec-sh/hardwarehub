package com.hardwarehub.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LevelPriceRequest(
        @NotNull Long priceLevelId,
        @NotNull @DecimalMin("0.0") BigDecimal unitPrice
) {
}
