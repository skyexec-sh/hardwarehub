package com.hardwarehub.fulfillment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateDeliveryRequest(
        String notes,
        @NotEmpty @Valid List<DeliveryLineRequest> items
) {
    public record DeliveryLineRequest(
            @NotNull Long salesOrderItemId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity
    ) {
    }
}
