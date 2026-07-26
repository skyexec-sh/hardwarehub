package com.hardwarehub.catalog.web;

import com.hardwarehub.catalog.dto.BrandRequest;
import com.hardwarehub.catalog.dto.BrandResponse;
import com.hardwarehub.catalog.service.BrandService;
import com.hardwarehub.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brands")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "List brands")
    public PageResponse<BrandResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return brandService.list(search, PageRequest.of(page, size, Sort.by("name")));
    }

    @GetMapping("/active")
    @Operation(summary = "List active brands for dropdowns")
    public List<BrandResponse> active() {
        return brandService.listActive();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand")
    public BrandResponse get(@PathVariable Long id) {
        return brandService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','INVENTORY_STAFF')")
    @Operation(summary = "Create brand")
    public BrandResponse create(@Valid @RequestBody BrandRequest request) {
        return brandService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','INVENTORY_STAFF')")
    @Operation(summary = "Update brand")
    public BrandResponse update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return brandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','INVENTORY_STAFF')")
    @Operation(summary = "Soft-delete brand")
    public void delete(@PathVariable Long id) {
        brandService.delete(id);
    }
}
