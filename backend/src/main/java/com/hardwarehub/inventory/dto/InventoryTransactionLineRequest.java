package com.hardwarehub.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InventoryTransactionLineRequest(
        @NotNull Long productId,
        @NotNull BigDecimal quantity,
        BigDecimal unitCost
) {
}
