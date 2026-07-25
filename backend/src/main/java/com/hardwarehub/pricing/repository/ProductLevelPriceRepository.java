package com.hardwarehub.pricing.repository;

import com.hardwarehub.pricing.domain.ProductLevelPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductLevelPriceRepository extends JpaRepository<ProductLevelPrice, Long> {

    List<ProductLevelPrice> findByProductIdOrderByPriceLevelSortOrderAsc(Long productId);

    Optional<ProductLevelPrice> findByProductIdAndPriceLevelId(Long productId, Long priceLevelId);

    @Query("""
            SELECT p FROM ProductLevelPrice p
            JOIN FETCH p.priceLevel
            WHERE p.product.id = :productId
            ORDER BY p.priceLevel.sortOrder
            """)
    List<ProductLevelPrice> findDetailedByProductId(@Param("productId") Long productId);
}
