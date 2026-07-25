package com.hardwarehub.customer.service;

import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.domain.CustomerStatus;
import com.hardwarehub.customer.dto.CustomerPurchaseHistoryItem;
import com.hardwarehub.customer.dto.CustomerRequest;
import com.hardwarehub.customer.dto.CustomerResponse;
import com.hardwarehub.customer.mapper.CustomerMapper;
import com.hardwarehub.customer.repository.CustomerRepository;
import com.hardwarehub.sales.domain.SaleStatus;
import com.hardwarehub.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuditService auditService;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(
            String search,
            CustomerStatus status,
            String code,
            String businessName,
            String contact,
            String phone,
            String city,
            Boolean hasBalanceDue,
            Pageable pageable) {
        return PageResponse.from(
                customerRepository
                        .search(search, status, code, businessName, contact, phone, city, hasBalanceDue, pageable)
                        .map(customerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return customerMapper.toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerPurchaseHistoryItem> purchaseHistory(Long id) {
        require(id);
        return saleRepository.findByCustomerIdAndStatusOrderBySoldAtDesc(id, SaleStatus.COMPLETED).stream()
                .map(s -> new CustomerPurchaseHistoryItem(
                        s.getId(),
                        s.getReceiptNumber(),
                        s.getSoldAt(),
                        s.getTotalAmount(),
                        s.getStatus().name()))
                .toList();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        validateCode(request.customerCode(), null);
        validateEmail(request.email());
        Customer customer = new Customer();
        apply(customer, request);
        customer.setOutstandingBalance(BigDecimal.ZERO);
        customer.setCreatedBy(SecurityUtils.currentUsername());
        customer.setUpdatedBy(SecurityUtils.currentUsername());
        Customer saved = customerRepository.save(customer);
        auditService.log("CREATE", "CUSTOMER", String.valueOf(saved.getId()),
                "Customer created: " + saved.getCustomerCode());
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = require(id);
        validateCode(request.customerCode(), id);
        validateEmail(request.email());
        apply(customer, request);
        customer.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("UPDATE", "CUSTOMER", String.valueOf(id), "Customer updated");
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, CustomerStatus status) {
        Customer customer = require(id);
        customer.setStatus(status);
        customer.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("STATUS_CHANGE", "CUSTOMER", String.valueOf(id), "Status set to " + status);
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = require(id);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setDeletedAt(Instant.now());
        customer.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("DELETE", "CUSTOMER", String.valueOf(id), "Customer soft-deleted");
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setCustomerCode(request.customerCode().trim().toUpperCase());
        customer.setBusinessName(request.businessName().trim());
        customer.setContactPerson(blankToNull(request.contactPerson()));
        customer.setPhone(blankToNull(request.phone()));
        customer.setEmail(blankToNull(request.email()));
        customer.setAddress(blankToNull(request.address()));
        customer.setCity(blankToNull(request.city()));
        customer.setProvince(blankToNull(request.province()));
        customer.setTaxIdentificationNumber(blankToNull(request.taxIdentificationNumber()));
        customer.setNotes(blankToNull(request.notes()));
        customer.setCreditLimit(request.creditLimit() == null ? BigDecimal.ZERO : request.creditLimit());
        customer.setStatus(request.status() == null ? CustomerStatus.ACTIVE : request.status());
    }

    private void validateCode(String code, Long excludeId) {
        boolean exists = excludeId == null
                ? customerRepository.existsByCustomerCodeIgnoreCaseAndDeletedAtIsNull(code)
                : customerRepository.existsByCustomerCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, excludeId);
        if (exists) {
            throw new BusinessException("CUSTOMER_CODE_EXISTS", "Customer code already exists", HttpStatus.CONFLICT);
        }
    }

    private void validateEmail(String email) {
        String normalized = blankToNull(email);
        if (normalized != null && !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("INVALID_EMAIL", "Email format is invalid", HttpStatus.BAD_REQUEST);
        }
    }

    private Customer require(Long id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
