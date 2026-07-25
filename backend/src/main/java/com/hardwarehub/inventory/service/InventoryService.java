package com.hardwarehub.inventory.service;

import com.hardwarehub.catalog.domain.Product;
import com.hardwarehub.catalog.repository.ProductRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import com.hardwarehub.inventory.domain.InventoryTransaction;
import com.hardwarehub.inventory.domain.InventoryTransactionType;
import com.hardwarehub.inventory.dto.InventoryBatchTransactionRequest;
import com.hardwarehub.inventory.dto.InventorySummaryResponse;
import com.hardwarehub.inventory.dto.InventoryTransactionLineRequest;
import com.hardwarehub.inventory.dto.InventoryTransactionRequest;
import com.hardwarehub.inventory.dto.InventoryTransactionResponse;
import com.hardwarehub.inventory.dto.LowStockProductResponse;
import com.hardwarehub.inventory.mapper.InventoryMapper;
import com.hardwarehub.inventory.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Instant OPEN_RANGE_START = Instant.EPOCH;
    private static final Instant OPEN_RANGE_END = Instant.parse("9999-12-31T23:59:59.999Z");

    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> list(
            Long productId,
            InventoryTransactionType type,
            String search,
            String product,
            String reference,
            String createdBy,
            java.time.Instant from,
            java.time.Instant to,
            Pageable pageable) {
        Instant fromDate = from != null ? from : OPEN_RANGE_START;
        Instant toDate = to != null ? to : OPEN_RANGE_END;
        return PageResponse.from(
                transactionRepository
                        .search(productId, type, search, product, reference, createdBy, fromDate, toDate, pageable)
                        .map(inventoryMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<LowStockProductResponse> lowStock(Pageable pageable) {
        return PageResponse.from(productRepository.findLowStock(pageable).map(this::toLowStock));
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse summary() {
        return new InventorySummaryResponse(
                productRepository.countLowStock(),
                productRepository.countOutOfStock(),
                transactionRepository.count());
    }

    @Transactional
    public InventoryTransactionResponse create(InventoryTransactionRequest request) {
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        BigDecimal before = product.getCurrentStock() == null ? BigDecimal.ZERO : product.getCurrentStock();
        BigDecimal qty = request.quantity();
        if (qty == null) {
            throw new BusinessException("VALIDATION_ERROR", "quantity is required", HttpStatus.BAD_REQUEST);
        }

        BigDecimal after;
        BigDecimal recordedQuantity;

        switch (request.transactionType()) {
            case STOCK_IN -> {
                requirePositive(qty, "Stock in quantity must be greater than zero");
                after = before.add(qty);
                recordedQuantity = qty;
            }
            case STOCK_OUT -> {
                requirePositive(qty, "Stock out quantity must be greater than zero");
                if (before.compareTo(qty) < 0) {
                    throw new BusinessException(
                            "INSUFFICIENT_STOCK",
                            "Insufficient stock for " + product.getSku() + ". Available: " + before,
                            HttpStatus.CONFLICT);
                }
                after = before.subtract(qty);
                recordedQuantity = qty;
            }
            case ADJUSTMENT -> {
                // quantity = new absolute stock level
                if (qty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException(
                            "VALIDATION_ERROR", "Adjusted stock cannot be negative", HttpStatus.BAD_REQUEST);
                }
                after = qty;
                recordedQuantity = after.subtract(before);
            }
            default -> throw new BusinessException(
                    "VALIDATION_ERROR", "Unsupported transaction type", HttpStatus.BAD_REQUEST);
        }

        product.setCurrentStock(after);
        product.setUpdatedBy(SecurityUtils.currentUsername());

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProduct(product);
        tx.setTransactionType(request.transactionType());
        tx.setQuantity(recordedQuantity);
        tx.setQuantityBefore(before);
        tx.setQuantityAfter(after);
        tx.setUnitCost(request.unitCost());
        tx.setReferenceNo(blankToNull(request.referenceNo()));
        tx.setNotes(blankToNull(request.notes()));
        tx.setCreatedBy(SecurityUtils.currentUsername());

        InventoryTransaction saved = transactionRepository.save(tx);
        auditService.log(
                request.transactionType().name(),
                "INVENTORY",
                String.valueOf(saved.getId()),
                product.getSku() + " " + before + " → " + after);

        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public List<InventoryTransactionResponse> createBatch(InventoryBatchTransactionRequest request) {
        List<InventoryTransactionLineRequest> lines = request.lines();
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "At least one product line is required", HttpStatus.BAD_REQUEST);
        }

        Set<Long> seen = new HashSet<>();
        for (InventoryTransactionLineRequest line : lines) {
            if (line.productId() == null) {
                throw new BusinessException("VALIDATION_ERROR", "productId is required on each line", HttpStatus.BAD_REQUEST);
            }
            if (!seen.add(line.productId())) {
                throw new BusinessException(
                        "VALIDATION_ERROR",
                        "Duplicate product in batch: " + line.productId(),
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Lock products in stable id order to reduce deadlock risk.
        List<InventoryTransactionLineRequest> ordered = new ArrayList<>(lines);
        ordered.sort(Comparator.comparing(InventoryTransactionLineRequest::productId));

        List<InventoryTransactionResponse> results = new ArrayList<>(ordered.size());
        for (InventoryTransactionLineRequest line : ordered) {
            results.add(create(new InventoryTransactionRequest(
                    line.productId(),
                    request.transactionType(),
                    line.quantity(),
                    line.unitCost(),
                    request.referenceNo(),
                    request.notes())));
        }
        return results;
    }

    private LowStockProductResponse toLowStock(Product product) {
        BigDecimal current = product.getCurrentStock() == null ? BigDecimal.ZERO : product.getCurrentStock();
        BigDecimal minimum = product.getMinimumStock() == null ? BigDecimal.ZERO : product.getMinimumStock();
        return new LowStockProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getUnit(),
                current,
                minimum,
                product.getMaximumStock(),
                minimum.subtract(current).max(BigDecimal.ZERO));
    }

    private void requirePositive(BigDecimal qty, String message) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
