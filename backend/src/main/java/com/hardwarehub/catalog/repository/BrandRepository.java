package com.hardwarehub.catalog.repository;

import com.hardwarehub.catalog.domain.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, Long id);

    List<Brand> findByDeletedAtIsNullAndActiveTrueOrderByNameAsc();

    @Query("""
            SELECT b FROM Brand b
            WHERE b.deletedAt IS NULL
              AND (:search IS NULL OR :search = '' OR
                   LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(b.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Brand> search(@Param("search") String search, Pageable pageable);
}
