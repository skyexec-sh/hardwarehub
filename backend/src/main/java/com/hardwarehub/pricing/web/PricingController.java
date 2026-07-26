package com.hardwarehub.pricing.web;

import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.pricing.dto.LevelPriceResponse;
import com.hardwarehub.pricing.dto.PriceLevelResponse;
import com.hardwarehub.pricing.dto.ProductPriceHistoryResponse;
import com.hardwarehub.pricing.dto.ResolvedPriceResponse;
import com.hardwarehub.pricing.dto.UpdatePriceLevelRequest;
import com.hardwarehub.pricing.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Pricing")
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/price-levels")
    @Operation(summary = "List price levels (Retail / Contractor / VIP)")
    public List<PriceLevelResponse> listLevels(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return pricingService.listLevels(activeOnly);
    }

    @PutMapping("/price-levels/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    @Operation(summary = "Update price level label or active flag")
    public PriceLevelResponse updateLevel(@PathVariable Long id, @Valid @RequestBody UpdatePriceLevelRequest request) {
        return pricingService.updateLevel(id, request);
    }

    @GetMapping("/products/{productId}/level-prices")
    @Operation(summary = "List per-level prices for a product")
    public List<LevelPriceResponse> productPrices(@PathVariable Long productId) {
        return pricingService.listProductPrices(productId);
    }

    @GetMapping("/products/{productId}/price-history")
    @Operation(summary = "Price change history for a product")
    public PageResponse<ProductPriceHistoryResponse> history(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return pricingService.history(productId, PageRequest.of(page, size));
    }

    @GetMapping("/pricing/resolve")
    @Operation(summary = "Resolve unit price for product + customer or price level")
    public ResolvedPriceResponse resolve(
            @RequestParam Long productId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long priceLevelId) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        return pricingService.resolve(productId, customerId, priceLevelId);
    }
}
