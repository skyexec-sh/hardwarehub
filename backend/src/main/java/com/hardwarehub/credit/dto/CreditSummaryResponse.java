package com.hardwarehub.credit.dto;

import java.math.BigDecimal;

public record CreditSummaryResponse(
        long customersWithBalance,
        BigDecimal totalOutstanding
) {
}
