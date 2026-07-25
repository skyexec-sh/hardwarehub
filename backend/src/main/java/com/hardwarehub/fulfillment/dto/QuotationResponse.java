package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.fulfillment.domain.QuotationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuotationResponse(
        Long id,
        String quoteNumber,
        Long customerId,
        String customerCode,
        String customerName,
        QuotationStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String notes,
        LocalDate validUntil,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        List<QuotationItemResponse> items
) {
}
