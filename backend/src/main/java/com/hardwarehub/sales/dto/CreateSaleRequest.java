package com.hardwarehub.sales.dto;

import com.hardwarehub.sales.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateSaleRequest(
        Long customerId,
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin("0.00") BigDecimal discountAmount,
        @NotNull @DecimalMin("0.00") BigDecimal taxAmount,
        BigDecimal amountTendered,
        String notes,
        @jakarta.validation.constraints.Size(max = 150) String cashierName,
        @jakarta.validation.constraints.Size(max = 150) String receivedBy,
        @NotEmpty @Valid List<SaleItemRequest> items
) {
}
