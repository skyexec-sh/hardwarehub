package com.hardwarehub.catalog.repository;

import com.hardwarehub.catalog.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, Long id);

    List<Category> findByDeletedAtIsNullAndActiveTrueOrderByNameAsc();

    @Query("""
            SELECT c FROM Category c
            WHERE c.deletedAt IS NULL
              AND (:search IS NULL OR :search = '' OR
                   LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Category> search(@Param("search") String search, Pageable pageable);
}
