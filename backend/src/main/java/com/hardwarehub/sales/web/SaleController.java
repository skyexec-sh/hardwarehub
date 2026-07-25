package com.hardwarehub.sales.web;

import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.sales.domain.PaymentMethod;
import com.hardwarehub.sales.domain.SaleStatus;
import com.hardwarehub.sales.dto.CreateSaleRequest;
import com.hardwarehub.sales.dto.SaleResponse;
import com.hardwarehub.sales.dto.SalesSummaryResponse;
import com.hardwarehub.sales.service.SaleService;
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

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales")
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    @Operation(summary = "Search sales")
    public PageResponse<SaleResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String receipt,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String cashier,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant soldFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant soldTo,
            @RequestParam(required = false) BigDecimal totalMin,
            @RequestParam(required = false) BigDecimal totalMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return saleService.list(
                search,
                status,
                customerId,
                receipt,
                customer,
                cashier,
                paymentMethod,
                soldFrom,
                soldTo,
                totalMin,
                totalMax,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "soldAt")));
    }

    @GetMapping("/summary")
    @Operation(summary = "Today and month sales totals")
    public SalesSummaryResponse summary() {
        return saleService.summary();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale by id")
    public SaleResponse get(@PathVariable Long id) {
        return saleService.get(id);
    }

    @GetMapping("/receipt/{receiptNumber}")
    @Operation(summary = "Get sale by receipt number")
    public SaleResponse getByReceipt(@PathVariable String receiptNumber) {
        return saleService.getByReceipt(receiptNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Checkout POS sale")
    public SaleResponse checkout(@Valid @RequestBody CreateSaleRequest request) {
        return saleService.checkout(request);
    }
}
