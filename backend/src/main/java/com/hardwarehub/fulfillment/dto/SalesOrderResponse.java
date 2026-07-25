package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.fulfillment.domain.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesOrderResponse(
        Long id,
        String soNumber,
        Long quotationId,
        String quoteNumber,
        Long customerId,
        String customerCode,
        String customerName,
        SalesOrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        List<SalesOrderItemResponse> items,
        List<DeliveryReceiptSummaryResponse> deliveries,
        List<InvoiceSummaryResponse> invoices
) {
}
