package com.hardwarehub.catalog.repository;

import com.hardwarehub.catalog.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"brand", "category"})
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"brand", "category"})
    Optional<Product> findByBarcodeAndDeletedAtIsNull(String barcode);

    boolean existsBySkuIgnoreCaseAndDeletedAtIsNull(String sku);

    boolean existsBySkuIgnoreCaseAndDeletedAtIsNullAndIdNot(String sku, Long id);

    boolean existsByBarcodeAndDeletedAtIsNull(String barcode);

    boolean existsByBarcodeAndDeletedAtIsNullAndIdNot(String barcode, Long id);

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("""
            SELECT p FROM Product p
            WHERE p.deletedAt IS NULL
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:brandId IS NULL OR p.brand.id = :brandId)
              AND (:sku IS NULL OR :sku = '' OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :sku, '%')))
              AND (:name IS NULL OR :name = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:active IS NULL OR p.active = :active)
              AND (:lowStockOnly IS NULL OR :lowStockOnly = false OR p.currentStock <= p.minimumStock)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(p.barcode, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Product> search(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("sku") String sku,
            @Param("name") String name,
            @Param("active") Boolean active,
            @Param("lowStockOnly") Boolean lowStockOnly,
            Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("""
            SELECT p FROM Product p
            WHERE p.deletedAt IS NULL
              AND p.active = true
              AND p.currentStock <= p.minimumStock
            """)
    Page<Product> findLowStock(Pageable pageable);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.deletedAt IS NULL
              AND p.active = true
              AND p.currentStock <= p.minimumStock
            """)
    long countLowStock();

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.deletedAt IS NULL
              AND p.active = true
              AND p.currentStock <= 0
            """)
    long countOutOfStock();
}
