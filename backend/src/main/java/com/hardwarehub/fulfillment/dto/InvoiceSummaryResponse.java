package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus;
import com.hardwarehub.sales.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceSummaryResponse(
        Long id,
        String invoiceNumber,
        FulfillmentInvoiceStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        Instant invoicedAt
) {
}
