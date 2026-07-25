package com.hardwarehub.catalog.service;

import com.hardwarehub.catalog.domain.Brand;
import com.hardwarehub.catalog.domain.Category;
import com.hardwarehub.catalog.domain.Product;
import com.hardwarehub.catalog.dto.ProductRequest;
import com.hardwarehub.catalog.dto.ProductResponse;
import com.hardwarehub.catalog.mapper.CatalogMapper;
import com.hardwarehub.catalog.repository.BrandRepository;
import com.hardwarehub.catalog.repository.CategoryRepository;
import com.hardwarehub.catalog.repository.ProductRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(
            String search,
            Long categoryId,
            Long brandId,
            String sku,
            String name,
            Boolean active,
            Boolean lowStockOnly,
            Pageable pageable) {
        return PageResponse.from(
                productRepository
                        .search(search, categoryId, brandId, sku, name, active, lowStockOnly, pageable)
                        .map(catalogMapper::toProductResponse));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return catalogMapper.toProductResponse(require(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse findByBarcode(String barcode) {
        return productRepository.findByBarcodeAndDeletedAtIsNull(barcode)
                .map(catalogMapper::toProductResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for barcode: " + barcode));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        validateUnique(request.sku(), request.barcode(), null);
        Product product = new Product();
        apply(product, request);
        product.setCreatedBy(SecurityUtils.currentUsername());
        product.setUpdatedBy(SecurityUtils.currentUsername());
        Product saved = productRepository.save(product);
        auditService.log("CREATE", "PRODUCT", String.valueOf(saved.getId()), "Product created: " + saved.getSku());
        return catalogMapper.toProductResponse(require(saved.getId()));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = require(id);
        validateUnique(request.sku(), request.barcode(), id);
        apply(product, request);
        product.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("UPDATE", "PRODUCT", String.valueOf(id), "Product updated");
        return catalogMapper.toProductResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = require(id);
        product.setActive(false);
        product.setDeletedAt(Instant.now());
        product.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("DELETE", "PRODUCT", String.valueOf(id), "Product soft-deleted");
    }

    private void validateUnique(String sku, String barcode, Long excludeId) {
        boolean skuExists = excludeId == null
                ? productRepository.existsBySkuIgnoreCaseAndDeletedAtIsNull(sku)
                : productRepository.existsBySkuIgnoreCaseAndDeletedAtIsNullAndIdNot(sku, excludeId);
        if (skuExists) {
            throw new BusinessException("SKU_EXISTS", "SKU already exists", HttpStatus.CONFLICT);
        }

        String normalizedBarcode = blankToNull(barcode);
        if (normalizedBarcode != null) {
            boolean barcodeExists = excludeId == null
                    ? productRepository.existsByBarcodeAndDeletedAtIsNull(normalizedBarcode)
                    : productRepository.existsByBarcodeAndDeletedAtIsNullAndIdNot(normalizedBarcode, excludeId);
            if (barcodeExists) {
                throw new BusinessException("BARCODE_EXISTS", "Barcode already exists", HttpStatus.CONFLICT);
            }
        }
    }

    private void apply(Product product, ProductRequest request) {
        product.setSku(request.sku().trim().toUpperCase());
        product.setBarcode(blankToNull(request.barcode()));
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBrand(resolveBrand(request.brandId()));
        product.setCategory(resolveCategory(request.categoryId()));
        product.setUnit(request.unit().trim().toUpperCase());
        product.setCostPrice(defaultZero(request.costPrice()));
        product.setSellingPrice(defaultZero(request.sellingPrice()));
        // Initial stock only on create; later changes go through inventory movements (M4).
        if (product.getId() == null) {
            product.setCurrentStock(defaultZero(request.currentStock()));
        }
        product.setMinimumStock(defaultZero(request.minimumStock()));
        product.setMaximumStock(request.maximumStock());
        product.setImageUrl(blankToNull(request.imageUrl()));
        product.setActive(request.active() == null || request.active());
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findByIdAndDeletedAtIsNull(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + brandId));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private Product require(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
