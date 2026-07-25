package com.hardwarehub.fulfillment.web;

import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus;
import com.hardwarehub.fulfillment.domain.QuotationStatus;
import com.hardwarehub.fulfillment.domain.SalesOrderStatus;
import com.hardwarehub.fulfillment.dto.CreateDeliveryRequest;
import com.hardwarehub.fulfillment.dto.CreateInvoiceRequest;
import com.hardwarehub.fulfillment.dto.CreateQuotationRequest;
import com.hardwarehub.fulfillment.dto.CreateSalesOrderRequest;
import com.hardwarehub.fulfillment.dto.DeliveryReceiptResponse;
import com.hardwarehub.fulfillment.dto.FulfillmentInvoiceResponse;
import com.hardwarehub.fulfillment.dto.FulfillmentSummaryResponse;
import com.hardwarehub.fulfillment.dto.QuotationResponse;
import com.hardwarehub.fulfillment.dto.RecordInvoicePaymentRequest;
import com.hardwarehub.fulfillment.dto.SalesOrderResponse;
import com.hardwarehub.fulfillment.service.FulfillmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fulfillment")
@RequiredArgsConstructor
@Tag(name = "Fulfillment")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    @GetMapping("/summary")
    @Operation(summary = "Pending quotes and open order counters")
    public FulfillmentSummaryResponse summary() {
        return fulfillmentService.summary();
    }

    // Quotes

    @GetMapping("/quotes")
    @Operation(summary = "Search quotations")
    public PageResponse<QuotationResponse> listQuotes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return fulfillmentService.listQuotes(
                search, status, customerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/quotes/{id}")
    @Operation(summary = "Get quotation")
    public QuotationResponse getQuote(@PathVariable Long id) {
        return fulfillmentService.getQuote(id);
    }

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Create draft quotation")
    public QuotationResponse createQuote(@Valid @RequestBody CreateQuotationRequest request) {
        return fulfillmentService.createQuote(request);
    }

    @PutMapping("/quotes/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Update draft quotation")
    public QuotationResponse updateQuote(@PathVariable Long id, @Valid @RequestBody CreateQuotationRequest request) {
        return fulfillmentService.updateQuote(id, request);
    }

    @PostMapping("/quotes/{id}/send")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Mark quotation as sent")
    public QuotationResponse sendQuote(@PathVariable Long id) {
        return fulfillmentService.sendQuote(id);
    }

    @PostMapping("/quotes/{id}/accept")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Accept quotation")
    public QuotationResponse acceptQuote(@PathVariable Long id) {
        return fulfillmentService.acceptQuote(id);
    }

    @PostMapping("/quotes/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Reject quotation")
    public QuotationResponse rejectQuote(@PathVariable Long id) {
        return fulfillmentService.rejectQuote(id);
    }

    @PostMapping("/quotes/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    @Operation(summary = "Cancel quotation")
    public QuotationResponse cancelQuote(@PathVariable Long id) {
        return fulfillmentService.cancelQuote(id);
    }

    @PostMapping("/quotes/{id}/convert")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Convert quotation to sales order")
    public SalesOrderResponse convertQuote(@PathVariable Long id) {
        return fulfillmentService.convertQuote(id);
    }

    // Orders

    @GetMapping("/orders")
    @Operation(summary = "Search sales orders")
    public PageResponse<SalesOrderResponse> listOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return fulfillmentService.listOrders(
                search, status, customerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get sales order")
    public SalesOrderResponse getOrder(@PathVariable Long id) {
        return fulfillmentService.getOrder(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Create sales order (optionally from quotation)")
    public SalesOrderResponse createOrder(@Valid @RequestBody CreateSalesOrderRequest request) {
        return fulfillmentService.createOrder(request);
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    @Operation(summary = "Cancel sales order with no deliveries")
    public SalesOrderResponse cancelOrder(@PathVariable Long id) {
        return fulfillmentService.cancelOrder(id);
    }

    @PostMapping("/orders/{id}/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','INVENTORY_STAFF','CASHIER')")
    @Operation(summary = "Post delivery receipt (partial allowed); stock-out delivered qty")
    public DeliveryReceiptResponse createDelivery(
            @PathVariable Long id, @Valid @RequestBody CreateDeliveryRequest request) {
        return fulfillmentService.createDelivery(id, request);
    }

    @PostMapping("/orders/{id}/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Create invoice for delivered (uninvoiced) quantities")
    public FulfillmentInvoiceResponse createInvoice(
            @PathVariable Long id, @Valid @RequestBody CreateInvoiceRequest request) {
        return fulfillmentService.createInvoice(id, request);
    }

    // Deliveries / invoices

    @GetMapping("/deliveries/{id}")
    @Operation(summary = "Get delivery receipt")
    public DeliveryReceiptResponse getDelivery(@PathVariable Long id) {
        return fulfillmentService.getDelivery(id);
    }

    @GetMapping("/invoices")
    @Operation(summary = "Search fulfillment invoices")
    public PageResponse<FulfillmentInvoiceResponse> listInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FulfillmentInvoiceStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return fulfillmentService.listInvoices(
                search, status, customerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoicedAt")));
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Get fulfillment invoice")
    public FulfillmentInvoiceResponse getInvoice(@PathVariable Long id) {
        return fulfillmentService.getInvoice(id);
    }

    @PostMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','CASHIER')")
    @Operation(summary = "Record payment against credit invoice (M6 ledger)")
    public FulfillmentInvoiceResponse recordPayment(
            @PathVariable Long id, @Valid @RequestBody RecordInvoicePaymentRequest request) {
        return fulfillmentService.recordInvoicePayment(id, request);
    }
}
