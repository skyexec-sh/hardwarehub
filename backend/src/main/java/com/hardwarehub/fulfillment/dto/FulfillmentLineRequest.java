package com.hardwarehub.fulfillment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FulfillmentLineRequest(
        @NotNull Long productId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        BigDecimal unitPrice,
        @DecimalMin("0.00") BigDecimal lineDiscount
) {
}
