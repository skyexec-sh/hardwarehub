package com.hardwarehub.credit.dto;

import com.hardwarehub.credit.domain.CollectionPaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordPaymentRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        @NotNull CollectionPaymentMethod paymentMethod,
        @Size(max = 80) String referenceNo,
        String notes,
        Instant paidAt
) {
}
