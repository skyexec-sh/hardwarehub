package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.credit.domain.CollectionPaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record RecordInvoicePaymentRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull CollectionPaymentMethod paymentMethod,
        String referenceNo,
        String notes,
        Instant paidAt
) {
}
