package com.hardwarehub.fulfillment.dto;

import java.math.BigDecimal;

public record QuotationItemResponse(
        Long id,
        Integer lineNo,
        Long productId,
        String productSku,
        String productName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineDiscount,
        BigDecimal lineTotal
) {
}
