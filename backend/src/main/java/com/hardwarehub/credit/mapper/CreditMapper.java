package com.hardwarehub.credit.mapper;

import com.hardwarehub.credit.domain.CustomerPayment;
import com.hardwarehub.credit.dto.CustomerPaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CreditMapper {

    @Mapping(target = "customerId", source = "customer.id")
    CustomerPaymentResponse toResponse(CustomerPayment payment);
}
