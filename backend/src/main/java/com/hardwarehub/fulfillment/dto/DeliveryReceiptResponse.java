package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.fulfillment.domain.DeliveryReceiptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DeliveryReceiptResponse(
        Long id,
        String drNumber,
        Long salesOrderId,
        String soNumber,
        Long customerId,
        String customerCode,
        String customerName,
        DeliveryReceiptStatus status,
        String notes,
        Instant deliveredAt,
        String createdBy,
        List<DeliveryReceiptItemResponse> items
) {
}
