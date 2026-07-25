package com.hardwarehub.sales.dto;

import com.hardwarehub.sales.domain.PaymentMethod;
import com.hardwarehub.sales.domain.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        String receiptNumber,
        Long customerId,
        String customerCode,
        String customerName,
        String customerTin,
        String customerAddress,
        String customerPhone,
        String cashierUsername,
        SaleStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal amountTendered,
        BigDecimal changeAmount,
        String notes,
        Instant soldAt,
        List<SaleItemResponse> items
) {
}
