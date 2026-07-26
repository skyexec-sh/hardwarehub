package com.hardwarehub.inventory.repository;

import com.hardwarehub.inventory.domain.InventoryTransaction;
import com.hardwarehub.inventory.domain.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @EntityGraph(attributePaths = {"product"})
    @Query("""
            SELECT t FROM InventoryTransaction t
            WHERE (:productId IS NULL OR t.product.id = :productId)
              AND (:type IS NULL OR t.transactionType = :type)
              AND (:product IS NULL OR :product = '' OR
                   LOWER(t.product.sku) LIKE LOWER(CONCAT('%', :product, '%')) OR
                   LOWER(t.product.name) LIKE LOWER(CONCAT('%', :product, '%')))
              AND (:reference IS NULL OR :reference = '' OR
                   LOWER(COALESCE(t.referenceNo, '')) LIKE LOWER(CONCAT('%', :reference, '%')))
              AND (:createdBy IS NULL OR :createdBy = '' OR
                   LOWER(COALESCE(t.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
              AND t.createdAt >= :fromDate
              AND t.createdAt < :toDate
              AND (:search IS NULL OR :search = '' OR
                   LOWER(t.product.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(t.product.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(t.referenceNo, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<InventoryTransaction> search(
            @Param("productId") Long productId,
            @Param("type") InventoryTransactionType type,
            @Param("search") String search,
            @Param("product") String product,
            @Param("reference") String reference,
            @Param("createdBy") String createdBy,
            @Param("fromDate") Instant from,
            @Param("toDate") Instant to,
            Pageable pageable);
}
