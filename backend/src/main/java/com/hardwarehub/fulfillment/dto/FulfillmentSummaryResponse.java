package com.hardwarehub.fulfillment.dto;

public record FulfillmentSummaryResponse(
        long pendingQuotes,
        long openOrders,
        long partialDeliveries
) {
}
