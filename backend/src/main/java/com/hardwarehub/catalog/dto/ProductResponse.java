package com.hardwarehub.catalog.dto;

import com.hardwarehub.pricing.dto.LevelPriceResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Long id,
        String sku,
        String barcode,
        String name,
        String description,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        String unit,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        BigDecimal maximumStock,
        String imageUrl,
        boolean active,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt,
        List<LevelPriceResponse> levelPrices
) {
}
