package com.hardwarehub.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 50) String sku,
        @Size(max = 64) String barcode,
        @NotBlank @Size(max = 200) String name,
        String description,
        Long brandId,
        Long categoryId,
        @NotBlank @Size(max = 30) String unit,
        @NotNull @DecimalMin("0.0") BigDecimal costPrice,
        @NotNull @DecimalMin("0.0") BigDecimal sellingPrice,
        @NotNull @DecimalMin("0.0") BigDecimal currentStock,
        @NotNull @DecimalMin("0.0") BigDecimal minimumStock,
        @DecimalMin("0.0") BigDecimal maximumStock,
        @Size(max = 500) String imageUrl,
        Boolean active
) {
}
