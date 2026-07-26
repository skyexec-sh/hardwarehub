package com.hardwarehub.pricing.dto;

import com.hardwarehub.pricing.domain.PriceChangeType;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductPriceHistoryResponse(
        Long id,
        Long productId,
        PriceChangeType priceType,
        Long priceLevelId,
        String priceLevelCode,
        String priceLevelName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        String reason,
        String changedBy,
        Instant changedAt
) {
}
