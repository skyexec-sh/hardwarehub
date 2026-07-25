package com.hardwarehub.customer.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Purchase history entries will be populated once Sales (Milestone 5) is available.
 */
public record CustomerPurchaseHistoryItem(
        Long saleId,
        String receiptNumber,
        Instant soldAt,
        BigDecimal totalAmount,
        String paymentMethod,
        String status
) {
}
