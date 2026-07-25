package com.hardwarehub.pricing.dto;

import java.math.BigDecimal;

public record ResolvedPriceResponse(
        Long productId,
        Long priceLevelId,
        String priceLevelCode,
        String priceLevelName,
        BigDecimal unitPrice,
        boolean fromLevelTable
) {
}
