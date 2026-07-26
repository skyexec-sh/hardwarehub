package com.hardwarehub.catalog.service;

import com.hardwarehub.catalog.domain.Category;
import com.hardwarehub.catalog.dto.CategoryRequest;
import com.hardwarehub.catalog.dto.CategoryResponse;
import com.hardwarehub.catalog.mapper.CatalogMapper;
import com.hardwarehub.catalog.repository.CategoryRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> list(String search, Pageable pageable) {
        return PageResponse.from(categoryRepository.search(search, pageable).map(catalogMapper::toCategoryResponse));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByDeletedAtIsNullAndActiveTrueOrderByNameAsc().stream()
                .map(catalogMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return catalogMapper.toCategoryResponse(require(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.name())) {
            throw new BusinessException("CATEGORY_EXISTS", "Category name already exists", HttpStatus.CONFLICT);
        }
        Category category = new Category();
        apply(category, request);
        category.setCreatedBy(SecurityUtils.currentUsername());
        category.setUpdatedBy(SecurityUtils.currentUsername());
        Category saved = categoryRepository.save(category);
        auditService.log("CREATE", "CATEGORY", String.valueOf(saved.getId()), "Category created: " + saved.getName());
        return catalogMapper.toCategoryResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = require(id);
        if (categoryRepository.existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(request.name(), id)) {
            throw new BusinessException("CATEGORY_EXISTS", "Category name already exists", HttpStatus.CONFLICT);
        }
        apply(category, request);
        category.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("UPDATE", "CATEGORY", String.valueOf(id), "Category updated");
        return catalogMapper.toCategoryResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = require(id);
        category.setActive(false);
        category.setDeletedAt(Instant.now());
        category.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("DELETE", "CATEGORY", String.valueOf(id), "Category soft-deleted");
    }

    private Category require(Long id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(request.active() == null || request.active());
    }
}
