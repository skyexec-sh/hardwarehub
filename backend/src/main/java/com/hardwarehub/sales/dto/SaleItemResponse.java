package com.hardwarehub.sales.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineDiscount,
        BigDecimal lineTotal,
        int lineNo
) {
}
