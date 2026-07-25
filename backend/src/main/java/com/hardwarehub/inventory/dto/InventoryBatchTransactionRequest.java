package com.hardwarehub.inventory.dto;

import com.hardwarehub.inventory.domain.InventoryTransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InventoryBatchTransactionRequest(
        @NotNull InventoryTransactionType transactionType,
        @Size(max = 50) String referenceNo,
        String notes,
        @NotEmpty @Valid List<InventoryTransactionLineRequest> lines
) {
}
