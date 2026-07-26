package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.sales.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateInvoiceRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin("0.00") BigDecimal discountAmount,
        @NotNull @DecimalMin("0.00") BigDecimal taxAmount,
        String notes,
        /** When null/empty, invoice all delivered-but-uninvoiced quantities. */
        List<InvoiceLineRequest> items
) {
    public record InvoiceLineRequest(
            @NotNull Long salesOrderItemId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity
    ) {
    }
}
