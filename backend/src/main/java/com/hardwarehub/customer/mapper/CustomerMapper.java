package com.hardwarehub.customer.mapper;

import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.dto.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "priceLevelId", source = "priceLevel.id")
    @Mapping(target = "priceLevelCode", source = "priceLevel.code")
    @Mapping(target = "priceLevelName", source = "priceLevel.name")
    CustomerResponse toResponse(Customer customer);
}
