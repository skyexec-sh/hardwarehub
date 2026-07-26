package com.hardwarehub.inventory.dto;

import java.math.BigDecimal;

public record LowStockProductResponse(
        Long productId,
        String sku,
        String name,
        String unit,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        BigDecimal maximumStock,
        BigDecimal deficit
) {
}
