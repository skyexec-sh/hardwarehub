package com.hardwarehub.fulfillment.dto;

import java.math.BigDecimal;

public record SalesOrderItemResponse(
        Long id,
        Integer lineNo,
        Long productId,
        String productSku,
        String productName,
        String unit,
        BigDecimal quantityOrdered,
        BigDecimal quantityDelivered,
        BigDecimal quantityInvoiced,
        BigDecimal quantityOpen,
        BigDecimal quantityBillable,
        BigDecimal unitPrice,
        BigDecimal lineDiscount,
        BigDecimal lineTotal
) {
}
