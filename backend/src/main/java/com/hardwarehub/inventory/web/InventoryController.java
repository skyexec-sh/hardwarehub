package com.hardwarehub.inventory.web;

import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.inventory.domain.InventoryTransactionType;
import com.hardwarehub.inventory.dto.InventorySummaryResponse;
import com.hardwarehub.inventory.dto.InventoryTransactionRequest;
import com.hardwarehub.inventory.dto.InventoryTransactionResponse;
import com.hardwarehub.inventory.dto.LowStockProductResponse;
import com.hardwarehub.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/transactions")
    @Operation(summary = "Inventory movement history")
    public PageResponse<InventoryTransactionResponse> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) InventoryTransactionType type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.list(
                productId,
                type,
                search,
                product,
                reference,
                createdBy,
                from,
                to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','INVENTORY_STAFF')")
    @Operation(summary = "Record stock in, stock out, or adjustment")
    public InventoryTransactionResponse create(@Valid @RequestBody InventoryTransactionRequest request) {
        return inventoryService.create(request);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Products at or below minimum stock")
    public PageResponse<LowStockProductResponse> lowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return inventoryService.lowStock(PageRequest.of(page, size, Sort.by("name")));
    }

    @GetMapping("/summary")
    @Operation(summary = "Inventory summary counters for dashboard")
    public InventorySummaryResponse summary() {
        return inventoryService.summary();
    }
}
