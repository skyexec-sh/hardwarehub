package com.hardwarehub.pricing.repository;

import com.hardwarehub.pricing.domain.PriceLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceLevelRepository extends JpaRepository<PriceLevel, Long> {

    List<PriceLevel> findAllByOrderBySortOrderAscNameAsc();

    List<PriceLevel> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<PriceLevel> findByCodeIgnoreCase(String code);

    Optional<PriceLevel> findByIdAndActiveTrue(Long id);
}
