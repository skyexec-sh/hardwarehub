package com.hardwarehub.credit.dto;

import com.hardwarehub.credit.domain.CollectionPaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerPaymentResponse(
        Long id,
        String paymentNumber,
        Long customerId,
        BigDecimal amount,
        CollectionPaymentMethod paymentMethod,
        String referenceNo,
        String notes,
        Instant paidAt,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        Instant createdAt,
        String createdBy
) {
}
