package com.hardwarehub.fulfillment.dto;

import java.math.BigDecimal;

public record DeliveryReceiptItemResponse(
        Long id,
        Integer lineNo,
        Long salesOrderItemId,
        Long productId,
        String productSku,
        String productName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice
) {
}
