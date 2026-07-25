package com.hardwarehub.inventory.dto;

public record InventorySummaryResponse(
        long lowStockCount,
        long outOfStockCount,
        long transactionCount
) {
}
