package com.hardwarehub.catalog.service;

import com.hardwarehub.catalog.domain.Brand;
import com.hardwarehub.catalog.dto.BrandRequest;
import com.hardwarehub.catalog.dto.BrandResponse;
import com.hardwarehub.catalog.mapper.CatalogMapper;
import com.hardwarehub.catalog.repository.BrandRepository;
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
public class BrandService {

    private final BrandRepository brandRepository;
    private final CatalogMapper catalogMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> list(String search, Pageable pageable) {
        return PageResponse.from(brandRepository.search(search, pageable).map(catalogMapper::toBrandResponse));
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> listActive() {
        return brandRepository.findByDeletedAtIsNullAndActiveTrueOrderByNameAsc().stream()
                .map(catalogMapper::toBrandResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse get(Long id) {
        return catalogMapper.toBrandResponse(require(id));
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        if (brandRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.name())) {
            throw new BusinessException("BRAND_EXISTS", "Brand name already exists", HttpStatus.CONFLICT);
        }
        Brand brand = new Brand();
        apply(brand, request);
        brand.setCreatedBy(SecurityUtils.currentUsername());
        brand.setUpdatedBy(SecurityUtils.currentUsername());
        Brand saved = brandRepository.save(brand);
        auditService.log("CREATE", "BRAND", String.valueOf(saved.getId()), "Brand created: " + saved.getName());
        return catalogMapper.toBrandResponse(saved);
    }

    @Transactional
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = require(id);
        if (brandRepository.existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(request.name(), id)) {
            throw new BusinessException("BRAND_EXISTS", "Brand name already exists", HttpStatus.CONFLICT);
        }
        apply(brand, request);
        brand.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("UPDATE", "BRAND", String.valueOf(id), "Brand updated");
        return catalogMapper.toBrandResponse(brand);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = require(id);
        brand.setActive(false);
        brand.setDeletedAt(Instant.now());
        brand.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("DELETE", "BRAND", String.valueOf(id), "Brand soft-deleted");
    }

    private Brand require(Long id) {
        return brandRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + id));
    }

    private void apply(Brand brand, BrandRequest request) {
        brand.setName(request.name().trim());
        brand.setDescription(request.description());
        brand.setLogoUrl(blankToNull(request.logoUrl()));
        brand.setActive(request.active() == null || request.active());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
