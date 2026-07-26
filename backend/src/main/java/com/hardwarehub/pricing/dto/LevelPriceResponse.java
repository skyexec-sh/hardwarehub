package com.hardwarehub.pricing.dto;

import java.math.BigDecimal;

public record LevelPriceResponse(
        Long priceLevelId,
        String priceLevelCode,
        String priceLevelName,
        BigDecimal unitPrice
) {
}
