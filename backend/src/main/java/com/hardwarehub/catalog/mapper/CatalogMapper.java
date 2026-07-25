package com.hardwarehub.catalog.mapper;

import com.hardwarehub.catalog.domain.Brand;
import com.hardwarehub.catalog.domain.Category;
import com.hardwarehub.catalog.domain.Product;
import com.hardwarehub.catalog.dto.BrandResponse;
import com.hardwarehub.catalog.dto.CategoryResponse;
import com.hardwarehub.catalog.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    CategoryResponse toCategoryResponse(Category category);

    BrandResponse toBrandResponse(Brand brand);

    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "lowStock", expression = "java(isLowStock(product))")
    ProductResponse toProductResponse(Product product);

    default boolean isLowStock(Product product) {
        if (product.getCurrentStock() == null || product.getMinimumStock() == null) {
            return false;
        }
        return product.getCurrentStock().compareTo(product.getMinimumStock()) <= 0;
    }

    default BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
