package com.hardwarehub.credit.web;

import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.credit.dto.CreditSummaryResponse;
import com.hardwarehub.credit.dto.CustomerPaymentResponse;
import com.hardwarehub.credit.dto.LedgerEntryResponse;
import com.hardwarehub.credit.dto.RecordPaymentRequest;
import com.hardwarehub.credit.dto.StatementOfAccountResponse;
import com.hardwarehub.credit.service.CreditLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Credit ledger")
public class CreditLedgerController {

    private final CreditLedgerService creditLedgerService;

    @GetMapping("/credit/summary")
    @Operation(summary = "Credit receivables summary for dashboard")
    public CreditSummaryResponse summary() {
        return creditLedgerService.summary();
    }

    @GetMapping("/customers/{customerId}/payments")
    @Operation(summary = "Payment history for a customer")
    public PageResponse<CustomerPaymentResponse> listPayments(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return creditLedgerService.listPayments(
                customerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt")));
    }

    @PostMapping("/customers/{customerId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Record a customer payment against outstanding credit")
    public CustomerPaymentResponse recordPayment(
            @PathVariable Long customerId, @Valid @RequestBody RecordPaymentRequest request) {
        return creditLedgerService.recordPayment(customerId, request);
    }

    @GetMapping("/customers/{customerId}/ledger")
    @Operation(summary = "Full credit ledger (charges + payments) with running balance")
    public List<LedgerEntryResponse> ledger(@PathVariable Long customerId) {
        return creditLedgerService.ledger(customerId);
    }

    @GetMapping("/customers/{customerId}/statement")
    @Operation(summary = "Statement of Account for a date range")
    public StatementOfAccountResponse statement(
            @PathVariable Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return creditLedgerService.statement(customerId, from, to);
    }
}
