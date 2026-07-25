package com.hardwarehub.customer.mapper;

import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.dto.CustomerResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerResponse toResponse(Customer customer);
}
