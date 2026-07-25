package com.hardwarehub.inventory.mapper;

import com.hardwarehub.inventory.domain.InventoryTransaction;
import com.hardwarehub.inventory.dto.InventoryTransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "unit", source = "product.unit")
    InventoryTransactionResponse toResponse(InventoryTransaction transaction);
}
