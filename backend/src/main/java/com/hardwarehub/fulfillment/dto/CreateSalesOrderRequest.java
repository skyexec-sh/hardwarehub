package com.hardwarehub.fulfillment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateSalesOrderRequest(
        @NotNull Long customerId,
        Long quotationId,
        @NotNull @DecimalMin("0.00") BigDecimal discountAmount,
        @NotNull @DecimalMin("0.00") BigDecimal taxAmount,
        String notes,
        @NotEmpty @Valid List<FulfillmentLineRequest> items
) {
}
