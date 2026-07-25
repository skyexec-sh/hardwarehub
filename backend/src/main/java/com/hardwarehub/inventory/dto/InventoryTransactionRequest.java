package com.hardwarehub.inventory.dto;

import com.hardwarehub.inventory.domain.InventoryTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InventoryTransactionRequest(
        @NotNull Long productId,
        @NotNull InventoryTransactionType transactionType,
        @NotNull BigDecimal quantity,
        BigDecimal unitCost,
        @Size(max = 50) String referenceNo,
        String notes
) {
}
