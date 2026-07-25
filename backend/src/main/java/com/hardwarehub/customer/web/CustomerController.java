package com.hardwarehub.customer.web;

import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.customer.domain.CustomerStatus;
import com.hardwarehub.customer.dto.CustomerPurchaseHistoryItem;
import com.hardwarehub.customer.dto.CustomerRequest;
import com.hardwarehub.customer.dto.CustomerResponse;
import com.hardwarehub.customer.service.CustomerService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Search customers")
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String businessName,
            @RequestParam(required = false) String contact,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean hasBalanceDue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerService.list(
                search,
                status,
                code,
                businessName,
                contact,
                phone,
                city,
                hasBalanceDue,
                PageRequest.of(page, size, Sort.by("businessName")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.get(id);
    }

    @GetMapping("/{id}/purchase-history")
    @Operation(summary = "Customer purchase history (populated after Sales milestone)")
    public List<CustomerPurchaseHistoryItem> purchaseHistory(@PathVariable Long id) {
        return customerService.purchaseHistory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Create customer")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Update customer")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    @Operation(summary = "Update customer status")
    public CustomerResponse updateStatus(@PathVariable Long id, @RequestBody Map<String, CustomerStatus> body) {
        CustomerStatus status = body.get("status");
        if (status == null) {
            throw new com.hardwarehub.common.exception.BusinessException(
                    "VALIDATION_ERROR", "status is required", HttpStatus.BAD_REQUEST);
        }
        return customerService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    @Operation(summary = "Soft-delete customer")
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }
}
