package com.hardwarehub.sales.dto;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        BigDecimal todaySales,
        long todayReceiptCount,
        BigDecimal monthSales,
        long monthReceiptCount
) {
}
