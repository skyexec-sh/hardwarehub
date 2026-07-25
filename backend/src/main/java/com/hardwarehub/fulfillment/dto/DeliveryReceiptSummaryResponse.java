package com.hardwarehub.fulfillment.dto;

import com.hardwarehub.fulfillment.domain.DeliveryReceiptStatus;

import java.time.Instant;

public record DeliveryReceiptSummaryResponse(
        Long id,
        String drNumber,
        DeliveryReceiptStatus status,
        Instant deliveredAt
) {
}
