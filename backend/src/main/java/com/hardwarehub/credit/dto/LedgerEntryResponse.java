package com.hardwarehub.credit.dto;

import com.hardwarehub.credit.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        LedgerEntryType entryType,
        Instant occurredAt,
        String reference,
        String description,
        BigDecimal chargeAmount,
        BigDecimal paymentAmount,
        BigDecimal runningBalance,
        Long saleId,
        Long paymentId
) {
}
