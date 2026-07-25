package com.hardwarehub.inventory.dto;

import com.hardwarehub.inventory.domain.InventoryTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryTransactionResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String unit,
        InventoryTransactionType transactionType,
        BigDecimal quantity,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        BigDecimal unitCost,
        String referenceNo,
        String notes,
        Instant createdAt,
        String createdBy
) {
}
