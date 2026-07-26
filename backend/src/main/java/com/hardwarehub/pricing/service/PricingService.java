package com.hardwarehub.pricing.service;

import com.hardwarehub.catalog.domain.Product;
import com.hardwarehub.catalog.repository.ProductRepository;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.repository.CustomerRepository;
import com.hardwarehub.pricing.domain.PriceChangeType;
import com.hardwarehub.pricing.domain.PriceLevel;
import com.hardwarehub.pricing.domain.ProductLevelPrice;
import com.hardwarehub.pricing.domain.ProductPriceHistory;
import com.hardwarehub.pricing.dto.LevelPriceRequest;
import com.hardwarehub.pricing.dto.LevelPriceResponse;
import com.hardwarehub.pricing.dto.PriceLevelResponse;
import com.hardwarehub.pricing.dto.ProductPriceHistoryResponse;
import com.hardwarehub.pricing.dto.ResolvedPriceResponse;
import com.hardwarehub.pricing.dto.UpdatePriceLevelRequest;
import com.hardwarehub.pricing.repository.PriceLevelRepository;
import com.hardwarehub.pricing.repository.ProductLevelPriceRepository;
import com.hardwarehub.pricing.repository.ProductPriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingService {

    public static final String RETAIL_CODE = "RETAIL";

    private final PriceLevelRepository priceLevelRepository;
    private final ProductLevelPriceRepository productLevelPriceRepository;
    private final ProductPriceHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<PriceLevelResponse> listLevels(boolean activeOnly) {
        List<PriceLevel> levels = activeOnly
                ? priceLevelRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                : priceLevelRepository.findAllByOrderBySortOrderAscNameAsc();
        return levels.stream().map(this::toLevelResponse).toList();
    }

    @Transactional
    public PriceLevelResponse updateLevel(Long id, UpdatePriceLevelRequest request) {
        PriceLevel level = priceLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price level not found: " + id));
        level.setName(request.name().trim());
        level.setDescription(blankToNull(request.description()));
        if (request.active() != null) {
            if (RETAIL_CODE.equalsIgnoreCase(level.getCode()) && !request.active()) {
                throw new BusinessException(
                        "VALIDATION_ERROR", "Retail price level cannot be deactivated", HttpStatus.BAD_REQUEST);
            }
            level.setActive(request.active());
        }
        return toLevelResponse(level);
    }

    @Transactional(readOnly = true)
    public List<LevelPriceResponse> listProductPrices(Long productId) {
        requireProduct(productId);
        return productLevelPriceRepository.findDetailedByProductId(productId).stream()
                .map(this::toLevelPriceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductPriceHistoryResponse> history(Long productId, Pageable pageable) {
        requireProduct(productId);
        return PageResponse.from(
                historyRepository.findByProductIdOrderByChangedAtDesc(productId, pageable).map(this::toHistoryResponse));
    }

    @Transactional(readOnly = true)
    public ResolvedPriceResponse resolve(Long productId, Long customerId, Long priceLevelId) {
        Product product = requireProduct(productId);
        PriceLevel level = resolveLevel(customerId, priceLevelId);
        return resolveForProduct(product, level);
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveUnitPrice(Product product, Customer customer) {
        PriceLevel level = customer != null && customer.getPriceLevel() != null
                ? customer.getPriceLevel()
                : requireRetail();
        return resolveForProduct(product, level).unitPrice();
    }

    @Transactional
    public void syncProductPrices(
            Product product,
            BigDecimal previousCost,
            BigDecimal previousSelling,
            BigDecimal newCost,
            BigDecimal newSelling,
            List<LevelPriceRequest> levelPrices,
            String reason) {
        String actor = SecurityUtils.currentUsername();
        String changeReason = blankToNull(reason);

        if (previousCost == null || previousCost.compareTo(money(newCost)) != 0) {
            recordHistory(product, PriceChangeType.COST, null, previousCost, money(newCost), changeReason, actor);
        }

        Map<Long, BigDecimal> requested = new HashMap<>();
        if (levelPrices != null) {
            for (LevelPriceRequest row : levelPrices) {
                if (row.priceLevelId() == null) {
                    continue;
                }
                requested.put(row.priceLevelId(), money(row.unitPrice()));
            }
        }

        PriceLevel retail = requireRetail();
        // Selling price on product always mirrors Retail.
        requested.put(retail.getId(), money(newSelling));

        List<PriceLevel> levels = priceLevelRepository.findAllByOrderBySortOrderAscNameAsc();
        for (PriceLevel level : levels) {
            BigDecimal target = requested.get(level.getId());
            if (target == null) {
                // Keep existing or default new products to selling/retail price.
                ProductLevelPrice existing = productLevelPriceRepository
                        .findByProductIdAndPriceLevelId(product.getId(), level.getId())
                        .orElse(null);
                if (existing == null) {
                    target = money(newSelling);
                } else {
                    continue;
                }
            }

            ProductLevelPrice row = productLevelPriceRepository
                    .findByProductIdAndPriceLevelId(product.getId(), level.getId())
                    .orElseGet(() -> {
                        ProductLevelPrice created = new ProductLevelPrice();
                        created.setProduct(product);
                        created.setPriceLevel(level);
                        return created;
                    });

            BigDecimal old = row.getId() == null ? null : row.getUnitPrice();
            if (old == null || old.compareTo(target) != 0) {
                row.setUnitPrice(target);
                row.setUpdatedBy(actor);
                productLevelPriceRepository.save(row);
                recordHistory(product, PriceChangeType.LEVEL, level, old, target, changeReason, actor);
            } else if (row.getId() == null) {
                row.setUnitPrice(target);
                row.setUpdatedBy(actor);
                productLevelPriceRepository.save(row);
            }
        }

        if (previousSelling == null || previousSelling.compareTo(money(newSelling)) != 0) {
            // Retail LEVEL history already recorded above; no duplicate COST-style selling entry.
        }
    }

    private ResolvedPriceResponse resolveForProduct(Product product, PriceLevel level) {
        return productLevelPriceRepository
                .findByProductIdAndPriceLevelId(product.getId(), level.getId())
                .map(row -> new ResolvedPriceResponse(
                        product.getId(),
                        level.getId(),
                        level.getCode(),
                        level.getName(),
                        money(row.getUnitPrice()),
                        true))
                .orElseGet(() -> new ResolvedPriceResponse(
                        product.getId(),
                        level.getId(),
                        level.getCode(),
                        level.getName(),
                        money(product.getSellingPrice()),
                        false));
    }

    private PriceLevel resolveLevel(Long customerId, Long priceLevelId) {
        if (priceLevelId != null) {
            return priceLevelRepository
                    .findByIdAndActiveTrue(priceLevelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Price level not found: " + priceLevelId));
        }
        if (customerId != null) {
            Customer customer = customerRepository
                    .findByIdAndDeletedAtIsNull(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
            if (customer.getPriceLevel() != null && customer.getPriceLevel().isActive()) {
                return customer.getPriceLevel();
            }
        }
        return requireRetail();
    }

    private PriceLevel requireRetail() {
        return priceLevelRepository
                .findByCodeIgnoreCase(RETAIL_CODE)
                .orElseThrow(() -> new BusinessException(
                        "PRICE_LEVEL_MISSING", "Retail price level is not configured", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private void recordHistory(
            Product product,
            PriceChangeType type,
            PriceLevel level,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            String reason,
            String actor) {
        ProductPriceHistory history = new ProductPriceHistory();
        history.setProduct(product);
        history.setPriceType(type);
        history.setPriceLevel(level);
        history.setOldPrice(oldPrice);
        history.setNewPrice(newPrice);
        history.setReason(reason);
        history.setChangedBy(actor);
        historyRepository.save(history);
    }

    private Product requireProduct(Long id) {
        return productRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private PriceLevelResponse toLevelResponse(PriceLevel level) {
        return new PriceLevelResponse(
                level.getId(),
                level.getCode(),
                level.getName(),
                level.getDescription(),
                level.getSortOrder(),
                level.isActive(),
                level.getCreatedAt(),
                level.getUpdatedAt());
    }

    private LevelPriceResponse toLevelPriceResponse(ProductLevelPrice row) {
        PriceLevel level = row.getPriceLevel();
        return new LevelPriceResponse(level.getId(), level.getCode(), level.getName(), row.getUnitPrice());
    }

    private ProductPriceHistoryResponse toHistoryResponse(ProductPriceHistory h) {
        PriceLevel level = h.getPriceLevel();
        return new ProductPriceHistoryResponse(
                h.getId(),
                h.getProduct().getId(),
                h.getPriceType(),
                level != null ? level.getId() : null,
                level != null ? level.getCode() : null,
                level != null ? level.getName() : null,
                h.getOldPrice(),
                h.getNewPrice(),
                h.getReason(),
                h.getChangedBy(),
                h.getChangedAt());
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
