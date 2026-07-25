package com.hardwarehub.customer.dto;

import com.hardwarehub.customer.domain.CustomerStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomerRequest(
        @NotBlank @Size(max = 30) String customerCode,
        @NotBlank @Size(max = 200) String businessName,
        @Size(max = 150) String contactPerson,
        @Size(max = 30) String phone,
        @Size(max = 255) String email,
        @Size(max = 500) String address,
        @Size(max = 100) String city,
        @Size(max = 100) String province,
        @Size(max = 50) String taxIdentificationNumber,
        String notes,
        @NotNull @DecimalMin("0.0") BigDecimal creditLimit,
        Long priceLevelId,
        @NotNull CustomerStatus status
) {
}
