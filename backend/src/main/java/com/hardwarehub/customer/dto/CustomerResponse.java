package com.hardwarehub.customer.dto;

import com.hardwarehub.customer.domain.CustomerStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerResponse(
        Long id,
        String customerCode,
        String businessName,
        String contactPerson,
        String phone,
        String email,
        String address,
        String city,
        String province,
        String taxIdentificationNumber,
        String notes,
        BigDecimal creditLimit,
        BigDecimal outstandingBalance,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
