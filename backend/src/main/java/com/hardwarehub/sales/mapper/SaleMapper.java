package com.hardwarehub.sales.mapper;

import com.hardwarehub.sales.domain.Sale;
import com.hardwarehub.sales.domain.SaleItem;
import com.hardwarehub.sales.dto.SaleItemResponse;
import com.hardwarehub.sales.dto.SaleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SaleMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerCode", source = "customer.customerCode")
    @Mapping(target = "customerName", source = "customer.businessName")
    @Mapping(target = "customerTin", source = "customer.taxIdentificationNumber")
    @Mapping(target = "customerAddress", source = "customer.address")
    @Mapping(target = "customerPhone", source = "customer.phone")
    SaleResponse toResponse(Sale sale);

    @Mapping(target = "productId", source = "product.id")
    SaleItemResponse toItemResponse(SaleItem item);
}
