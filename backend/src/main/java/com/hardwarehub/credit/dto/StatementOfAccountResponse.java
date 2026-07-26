package com.hardwarehub.credit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StatementOfAccountResponse(
        Long customerId,
        String customerCode,
        String businessName,
        String contactPerson,
        String phone,
        String address,
        String city,
        String province,
        String taxIdentificationNumber,
        Instant periodFrom,
        Instant periodTo,
        BigDecimal openingBalance,
        BigDecimal totalCharges,
        BigDecimal totalPayments,
        BigDecimal closingBalance,
        BigDecimal creditLimit,
        BigDecimal currentOutstanding,
        List<LedgerEntryResponse> lines
) {
}
