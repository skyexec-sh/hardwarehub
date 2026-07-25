package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus;
import com.hardwarehub.sales.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FulfillmentInvoiceResponse(
        Long id,
        String invoiceNumber,
        Long salesOrderId,
        String soNumber,
        Long customerId,
        String customerCode,
        String customerName,
        String customerTin,
        String customerAddress,
        String customerPhone,
        FulfillmentInvoiceStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        String notes,
        Instant invoicedAt,
        String createdBy,
        List<FulfillmentInvoiceItemResponse> items
) {
}
