package com.hardwarehub.pricing.repository;

import com.hardwarehub.pricing.domain.ProductPriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

    Page<ProductPriceHistory> findByProductIdOrderByChangedAtDesc(Long productId, Pageable pageable);
}
