package com.hardwarehub.fulfillment.service;

import com.hardwarehub.catalog.domain.Product;
import com.hardwarehub.catalog.repository.ProductRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import com.hardwarehub.credit.domain.CustomerPayment;
import com.hardwarehub.credit.repository.CustomerPaymentRepository;
import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.domain.CustomerStatus;
import com.hardwarehub.customer.repository.CustomerRepository;
import com.hardwarehub.fulfillment.domain.DeliveryReceipt;
import com.hardwarehub.fulfillment.domain.DeliveryReceiptItem;
import com.hardwarehub.fulfillment.domain.DeliveryReceiptStatus;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoice;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceItem;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus;
import com.hardwarehub.fulfillment.domain.Quotation;
import com.hardwarehub.fulfillment.domain.QuotationItem;
import com.hardwarehub.fulfillment.domain.QuotationStatus;
import com.hardwarehub.fulfillment.domain.SalesOrder;
import com.hardwarehub.fulfillment.domain.SalesOrderItem;
import com.hardwarehub.fulfillment.domain.SalesOrderStatus;
import com.hardwarehub.fulfillment.dto.CreateDeliveryRequest;
import com.hardwarehub.fulfillment.dto.CreateInvoiceRequest;
import com.hardwarehub.fulfillment.dto.CreateQuotationRequest;
import com.hardwarehub.fulfillment.dto.CreateSalesOrderRequest;
import com.hardwarehub.fulfillment.dto.DeliveryReceiptItemResponse;
import com.hardwarehub.fulfillment.dto.DeliveryReceiptResponse;
import com.hardwarehub.fulfillment.dto.DeliveryReceiptSummaryResponse;
import com.hardwarehub.fulfillment.dto.FulfillmentInvoiceItemResponse;
import com.hardwarehub.fulfillment.dto.FulfillmentInvoiceResponse;
import com.hardwarehub.fulfillment.dto.FulfillmentLineRequest;
import com.hardwarehub.fulfillment.dto.FulfillmentSummaryResponse;
import com.hardwarehub.fulfillment.dto.InvoiceSummaryResponse;
import com.hardwarehub.fulfillment.dto.QuotationItemResponse;
import com.hardwarehub.fulfillment.dto.QuotationResponse;
import com.hardwarehub.fulfillment.dto.RecordInvoicePaymentRequest;
import com.hardwarehub.fulfillment.dto.SalesOrderItemResponse;
import com.hardwarehub.fulfillment.dto.SalesOrderResponse;
import com.hardwarehub.fulfillment.repository.DeliveryReceiptRepository;
import com.hardwarehub.fulfillment.repository.FulfillmentInvoiceRepository;
import com.hardwarehub.fulfillment.repository.QuotationRepository;
import com.hardwarehub.fulfillment.repository.SalesOrderRepository;
import com.hardwarehub.inventory.domain.InventoryTransactionType;
import com.hardwarehub.inventory.dto.InventoryTransactionRequest;
import com.hardwarehub.inventory.service.InventoryService;
import com.hardwarehub.pricing.service.PricingService;
import com.hardwarehub.sales.domain.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FulfillmentService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Manila");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final QuotationRepository quotationRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final DeliveryReceiptRepository deliveryReceiptRepository;
    private final FulfillmentInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerPaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public FulfillmentSummaryResponse summary() {
        long pendingQuotes = quotationRepository.countByStatusIn(
                EnumSet.of(QuotationStatus.DRAFT, QuotationStatus.SENT, QuotationStatus.ACCEPTED));
        long openOrders = salesOrderRepository.countByStatus(SalesOrderStatus.OPEN);
        long partial = salesOrderRepository.countByStatus(SalesOrderStatus.PARTIALLY_DELIVERED);
        return new FulfillmentSummaryResponse(pendingQuotes, openOrders, partial);
    }

    // --- Quotations ---

    @Transactional(readOnly = true)
    public PageResponse<QuotationResponse> listQuotes(
            String search, QuotationStatus status, Long customerId, Pageable pageable) {
        return PageResponse.from(
                quotationRepository.search(searchOrEmpty(search), status, customerId, pageable).map(this::toQuoteResponse));
    }

    @Transactional(readOnly = true)
    public QuotationResponse getQuote(Long id) {
        return toQuoteResponse(requireQuote(id));
    }

    @Transactional
    public QuotationResponse createQuote(CreateQuotationRequest request) {
        Customer customer = requireActiveCustomer(request.customerId());
        Quotation quote = new Quotation();
        quote.setQuoteNumber(nextDocNumber("QUO", quotationRepository.nextQuoteSequence()));
        quote.setCustomer(customer);
        quote.setStatus(QuotationStatus.DRAFT);
        quote.setNotes(blankToNull(request.notes()));
        quote.setValidUntil(request.validUntil());
        quote.setDiscountAmount(money(request.discountAmount()));
        quote.setTaxAmount(money(request.taxAmount()));
        quote.setCreatedBy(SecurityUtils.currentUsername());
        quote.setUpdatedBy(SecurityUtils.currentUsername());
        applyQuoteLines(quote, customer, request.items());
        Quotation saved = quotationRepository.save(quote);
        auditService.log("CREATE", "QUOTATION", String.valueOf(saved.getId()), saved.getQuoteNumber());
        return toQuoteResponse(saved);
    }

    @Transactional
    public QuotationResponse updateQuote(Long id, CreateQuotationRequest request) {
        Quotation quote = requireQuote(id);
        if (quote.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Only draft quotations can be edited", HttpStatus.CONFLICT);
        }
        Customer customer = requireActiveCustomer(request.customerId());
        quote.setCustomer(customer);
        quote.setNotes(blankToNull(request.notes()));
        quote.setValidUntil(request.validUntil());
        quote.setDiscountAmount(money(request.discountAmount()));
        quote.setTaxAmount(money(request.taxAmount()));
        quote.setUpdatedBy(SecurityUtils.currentUsername());
        quote.clearItems();
        applyQuoteLines(quote, customer, request.items());
        return toQuoteResponse(quotationRepository.save(quote));
    }

    @Transactional
    public QuotationResponse sendQuote(Long id) {
        return transitionQuote(id, QuotationStatus.DRAFT, QuotationStatus.SENT, "SEND");
    }

    @Transactional
    public QuotationResponse acceptQuote(Long id) {
        Quotation quote = requireQuote(id);
        if (quote.getStatus() != QuotationStatus.SENT && quote.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Quote cannot be accepted from status " + quote.getStatus(),
                    HttpStatus.CONFLICT);
        }
        quote.setStatus(QuotationStatus.ACCEPTED);
        quote.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("ACCEPT", "QUOTATION", String.valueOf(id), quote.getQuoteNumber());
        return toQuoteResponse(quotationRepository.save(quote));
    }

    @Transactional
    public QuotationResponse rejectQuote(Long id) {
        Quotation quote = requireQuote(id);
        if (quote.getStatus() != QuotationStatus.SENT && quote.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Quote cannot be rejected from status " + quote.getStatus(),
                    HttpStatus.CONFLICT);
        }
        quote.setStatus(QuotationStatus.REJECTED);
        quote.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("REJECT", "QUOTATION", String.valueOf(id), quote.getQuoteNumber());
        return toQuoteResponse(quotationRepository.save(quote));
    }

    @Transactional
    public QuotationResponse cancelQuote(Long id) {
        Quotation quote = requireQuote(id);
        if (quote.getStatus() == QuotationStatus.CONVERTED || quote.getStatus() == QuotationStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATUS", "Quote cannot be cancelled", HttpStatus.CONFLICT);
        }
        quote.setStatus(QuotationStatus.CANCELLED);
        quote.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("CANCEL", "QUOTATION", String.valueOf(id), quote.getQuoteNumber());
        return toQuoteResponse(quotationRepository.save(quote));
    }

    @Transactional
    public SalesOrderResponse convertQuote(Long id) {
        Quotation quote = requireQuote(id);
        if (quote.getStatus() != QuotationStatus.SENT && quote.getStatus() != QuotationStatus.ACCEPTED) {
            throw new BusinessException(
                    "INVALID_STATUS",
                    "Only sent or accepted quotes can be converted",
                    HttpStatus.CONFLICT);
        }
        SalesOrder order = buildOrderFromQuote(quote);
        SalesOrder saved = salesOrderRepository.save(order);
        quote.setStatus(QuotationStatus.CONVERTED);
        quote.setUpdatedBy(SecurityUtils.currentUsername());
        quotationRepository.save(quote);
        auditService.log("CONVERT", "QUOTATION", String.valueOf(id), quote.getQuoteNumber() + " → " + saved.getSoNumber());
        return toOrderResponse(saved);
    }

    // --- Sales orders ---

    @Transactional(readOnly = true)
    public PageResponse<SalesOrderResponse> listOrders(
            String search, SalesOrderStatus status, Long customerId, Pageable pageable) {
        return PageResponse.from(
                salesOrderRepository.search(searchOrEmpty(search), status, customerId, pageable).map(this::toOrderResponse));
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getOrder(Long id) {
        return toOrderResponse(requireOrder(id));
    }

    @Transactional
    public SalesOrderResponse createOrder(CreateSalesOrderRequest request) {
        Customer customer = requireActiveCustomer(request.customerId());
        Quotation quote = null;
        if (request.quotationId() != null) {
            quote = requireQuote(request.quotationId());
            if (quote.getStatus() == QuotationStatus.CONVERTED) {
                throw new BusinessException("QUOTE_CONVERTED", "Quotation already converted", HttpStatus.CONFLICT);
            }
        }
        SalesOrder order = new SalesOrder();
        order.setSoNumber(nextDocNumber("SO", salesOrderRepository.nextSoSequence()));
        order.setCustomer(customer);
        order.setQuotation(quote);
        order.setStatus(SalesOrderStatus.OPEN);
        order.setNotes(blankToNull(request.notes()));
        order.setDiscountAmount(money(request.discountAmount()));
        order.setTaxAmount(money(request.taxAmount()));
        order.setCreatedBy(SecurityUtils.currentUsername());
        order.setUpdatedBy(SecurityUtils.currentUsername());
        applyOrderLines(order, customer, request.items());
        SalesOrder saved = salesOrderRepository.save(order);
        if (quote != null) {
            quote.setStatus(QuotationStatus.CONVERTED);
            quote.setUpdatedBy(SecurityUtils.currentUsername());
            quotationRepository.save(quote);
        }
        auditService.log("CREATE", "SALES_ORDER", String.valueOf(saved.getId()), saved.getSoNumber());
        return toOrderResponse(saved);
    }

    @Transactional
    public SalesOrderResponse cancelOrder(Long id) {
        SalesOrder order = requireOrder(id);
        boolean anyDelivered = order.getItems().stream()
                .anyMatch(i -> i.getQuantityDelivered().compareTo(BigDecimal.ZERO) > 0);
        if (anyDelivered) {
            throw new BusinessException(
                    "HAS_DELIVERIES", "Cannot cancel an order with deliveries", HttpStatus.CONFLICT);
        }
        if (order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATUS", "Order already cancelled", HttpStatus.CONFLICT);
        }
        order.setStatus(SalesOrderStatus.CANCELLED);
        order.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log("CANCEL", "SALES_ORDER", String.valueOf(id), order.getSoNumber());
        return toOrderResponse(salesOrderRepository.save(order));
    }

    // --- Deliveries ---

    @Transactional
    public DeliveryReceiptResponse createDelivery(Long orderId, CreateDeliveryRequest request) {
        SalesOrder order = requireOrder(orderId);
        if (order.getStatus() == SalesOrderStatus.CANCELLED || order.getStatus() == SalesOrderStatus.FULLY_DELIVERED) {
            throw new BusinessException(
                    "INVALID_STATUS", "Cannot deliver against status " + order.getStatus(), HttpStatus.CONFLICT);
        }
        Map<Long, SalesOrderItem> itemMap = order.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        DeliveryReceipt dr = new DeliveryReceipt();
        dr.setDrNumber(nextDocNumber("DR", deliveryReceiptRepository.nextDrSequence()));
        dr.setSalesOrder(order);
        dr.setStatus(DeliveryReceiptStatus.POSTED);
        dr.setNotes(blankToNull(request.notes()));
        dr.setCreatedBy(SecurityUtils.currentUsername());

        int lineNo = 1;
        for (CreateDeliveryRequest.DeliveryLineRequest line : request.items()) {
            SalesOrderItem soItem = itemMap.get(line.salesOrderItemId());
            if (soItem == null) {
                throw new BusinessException(
                        "VALIDATION_ERROR",
                        "Sales order item not found on this order: " + line.salesOrderItemId(),
                        HttpStatus.BAD_REQUEST);
            }
            BigDecimal open = soItem.getQuantityOrdered().subtract(soItem.getQuantityDelivered());
            if (line.quantity().compareTo(open) > 0) {
                throw new BusinessException(
                        "OVER_DELIVERY",
                        "Cannot deliver " + line.quantity() + " of " + soItem.getProductSku()
                                + "; open qty is " + open,
                        HttpStatus.CONFLICT);
            }
            Product product = soItem.getProduct();
            if (product == null) {
                throw new BusinessException(
                        "PRODUCT_MISSING", "Product missing for line " + soItem.getProductSku(), HttpStatus.CONFLICT);
            }
            Product locked = productRepository
                    .findByIdForUpdate(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + product.getId()));

            DeliveryReceiptItem dri = new DeliveryReceiptItem();
            dri.setSalesOrderItem(soItem);
            dri.setLineNo(lineNo++);
            dri.setProduct(locked);
            dri.setProductSku(soItem.getProductSku());
            dri.setProductName(soItem.getProductName());
            dri.setUnit(soItem.getUnit());
            dri.setQuantity(line.quantity());
            dri.setUnitPrice(soItem.getUnitPrice());
            dr.addItem(dri);

            soItem.setQuantityDelivered(soItem.getQuantityDelivered().add(line.quantity()));

            inventoryService.create(new InventoryTransactionRequest(
                    locked.getId(),
                    InventoryTransactionType.STOCK_OUT,
                    line.quantity(),
                    locked.getCostPrice(),
                    dr.getDrNumber(),
                    "Delivery " + dr.getDrNumber() + " / " + order.getSoNumber()));
        }

        refreshOrderDeliveryStatus(order);
        order.setUpdatedBy(SecurityUtils.currentUsername());
        DeliveryReceipt saved = deliveryReceiptRepository.save(dr);
        salesOrderRepository.save(order);
        auditService.log("CREATE", "DELIVERY", String.valueOf(saved.getId()), saved.getDrNumber());
        return toDeliveryResponse(saved);
    }

    @Transactional(readOnly = true)
    public DeliveryReceiptResponse getDelivery(Long id) {
        return toDeliveryResponse(requireDelivery(id));
    }

    // --- Invoices ---

    @Transactional(readOnly = true)
    public PageResponse<FulfillmentInvoiceResponse> listInvoices(
            String search, FulfillmentInvoiceStatus status, Long customerId, Pageable pageable) {
        return PageResponse.from(
                invoiceRepository.search(searchOrEmpty(search), status, customerId, pageable).map(this::toInvoiceResponse));
    }

    @Transactional(readOnly = true)
    public FulfillmentInvoiceResponse getInvoice(Long id) {
        return toInvoiceResponse(requireInvoice(id));
    }

    @Transactional
    public FulfillmentInvoiceResponse createInvoice(Long orderId, CreateInvoiceRequest request) {
        SalesOrder order = requireOrder(orderId);
        if (order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATUS", "Cannot invoice a cancelled order", HttpStatus.CONFLICT);
        }

        Map<Long, BigDecimal> requested = new HashMap<>();
        if (request.items() != null && !request.items().isEmpty()) {
            for (CreateInvoiceRequest.InvoiceLineRequest line : request.items()) {
                requested.merge(line.salesOrderItemId(), line.quantity(), BigDecimal::add);
            }
        } else {
            for (SalesOrderItem item : order.getItems()) {
                BigDecimal billable = item.getQuantityDelivered().subtract(item.getQuantityInvoiced());
                if (billable.compareTo(BigDecimal.ZERO) > 0) {
                    requested.put(item.getId(), billable);
                }
            }
        }
        if (requested.isEmpty()) {
            throw new BusinessException(
                    "NOTHING_TO_INVOICE", "No delivered quantity available to invoice", HttpStatus.CONFLICT);
        }

        Customer customer = order.getCustomer();
        if (request.paymentMethod() == PaymentMethod.CREDIT) {
            customer = customerRepository
                    .findByIdForUpdate(customer.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        FulfillmentInvoice invoice = new FulfillmentInvoice();
        invoice.setInvoiceNumber(nextDocNumber("INV", invoiceRepository.nextInvoiceSequence()));
        invoice.setSalesOrder(order);
        invoice.setCustomer(customer);
        invoice.setPaymentMethod(request.paymentMethod());
        invoice.setDiscountAmount(money(request.discountAmount()));
        invoice.setTaxAmount(money(request.taxAmount()));
        invoice.setNotes(blankToNull(request.notes()));
        invoice.setCreatedBy(SecurityUtils.currentUsername());
        invoice.setUpdatedBy(SecurityUtils.currentUsername());

        Map<Long, SalesOrderItem> itemMap = order.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        BigDecimal subtotal = BigDecimal.ZERO;
        int lineNo = 1;
        for (Map.Entry<Long, BigDecimal> entry : requested.entrySet()) {
            SalesOrderItem soItem = itemMap.get(entry.getKey());
            if (soItem == null) {
                throw new BusinessException(
                        "VALIDATION_ERROR", "Sales order item not on order: " + entry.getKey(), HttpStatus.BAD_REQUEST);
            }
            BigDecimal billable = soItem.getQuantityDelivered().subtract(soItem.getQuantityInvoiced());
            if (entry.getValue().compareTo(billable) > 0) {
                throw new BusinessException(
                        "OVER_INVOICE",
                        "Cannot invoice " + entry.getValue() + " of " + soItem.getProductSku()
                                + "; billable qty is " + billable,
                        HttpStatus.CONFLICT);
            }
            BigDecimal qty = entry.getValue();
            BigDecimal proportionalDiscount = BigDecimal.ZERO;
            if (soItem.getQuantityOrdered().compareTo(BigDecimal.ZERO) > 0
                    && soItem.getLineDiscount().compareTo(BigDecimal.ZERO) > 0) {
                proportionalDiscount = soItem
                        .getLineDiscount()
                        .multiply(qty)
                        .divide(soItem.getQuantityOrdered(), 2, RoundingMode.HALF_UP);
            }
            BigDecimal lineTotal = money(soItem.getUnitPrice().multiply(qty).subtract(proportionalDiscount));
            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
                lineTotal = BigDecimal.ZERO;
            }

            FulfillmentInvoiceItem ii = new FulfillmentInvoiceItem();
            ii.setSalesOrderItem(soItem);
            ii.setLineNo(lineNo++);
            ii.setProduct(soItem.getProduct());
            ii.setProductSku(soItem.getProductSku());
            ii.setProductName(soItem.getProductName());
            ii.setUnit(soItem.getUnit());
            ii.setQuantity(qty);
            ii.setUnitPrice(soItem.getUnitPrice());
            ii.setLineDiscount(proportionalDiscount);
            ii.setLineTotal(lineTotal);
            invoice.addItem(ii);

            soItem.setQuantityInvoiced(soItem.getQuantityInvoiced().add(qty));
            subtotal = subtotal.add(lineTotal);
        }

        invoice.setSubtotal(money(subtotal));
        BigDecimal total = money(invoice.getSubtotal().subtract(invoice.getDiscountAmount()).add(invoice.getTaxAmount()));
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("VALIDATION_ERROR", "Invoice total cannot be negative", HttpStatus.BAD_REQUEST);
        }
        invoice.setTotalAmount(total);

        if (request.paymentMethod() == PaymentMethod.CREDIT) {
            BigDecimal outstanding = customer.getOutstandingBalance() == null
                    ? BigDecimal.ZERO
                    : customer.getOutstandingBalance();
            BigDecimal available = customer.getCreditLimit().subtract(outstanding);
            if (total.compareTo(available) > 0) {
                throw new BusinessException(
                        "CREDIT_LIMIT_EXCEEDED",
                        "Invoice exceeds available credit. Available: " + available,
                        HttpStatus.CONFLICT);
            }
            customer.setOutstandingBalance(outstanding.add(total));
            customer.setUpdatedBy(SecurityUtils.currentUsername());
            invoice.setAmountPaid(BigDecimal.ZERO);
            invoice.setStatus(FulfillmentInvoiceStatus.OPEN);
        } else {
            invoice.setAmountPaid(total);
            invoice.setStatus(FulfillmentInvoiceStatus.PAID);
        }

        order.setUpdatedBy(SecurityUtils.currentUsername());
        FulfillmentInvoice saved = invoiceRepository.save(invoice);
        salesOrderRepository.save(order);
        auditService.log("CREATE", "FULFILLMENT_INVOICE", String.valueOf(saved.getId()), saved.getInvoiceNumber());
        return toInvoiceResponse(saved);
    }

    @Transactional
    public FulfillmentInvoiceResponse recordInvoicePayment(Long invoiceId, RecordInvoicePaymentRequest request) {
        FulfillmentInvoice invoice = requireInvoice(invoiceId);
        if (invoice.getPaymentMethod() != PaymentMethod.CREDIT) {
            throw new BusinessException(
                    "NOT_CREDIT_INVOICE", "Only credit invoices accept collection payments", HttpStatus.CONFLICT);
        }
        if (invoice.getStatus() == FulfillmentInvoiceStatus.PAID || invoice.getStatus() == FulfillmentInvoiceStatus.VOIDED) {
            throw new BusinessException(
                    "INVALID_STATUS", "Invoice cannot accept payments in status " + invoice.getStatus(),
                    HttpStatus.CONFLICT);
        }

        BigDecimal due = invoice.amountDue();
        if (request.amount().compareTo(due) > 0) {
            throw new BusinessException(
                    "PAYMENT_EXCEEDS_DUE", "Payment exceeds invoice due amount: " + due, HttpStatus.CONFLICT);
        }

        Customer customer = customerRepository
                .findByIdForUpdate(invoice.getCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        BigDecimal before = customer.getOutstandingBalance() == null ? BigDecimal.ZERO : customer.getOutstandingBalance();
        if (before.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("NO_BALANCE_DUE", "Customer has no outstanding balance", HttpStatus.CONFLICT);
        }
        if (request.amount().compareTo(before) > 0) {
            throw new BusinessException(
                    "PAYMENT_EXCEEDS_BALANCE",
                    "Payment exceeds outstanding balance. Due: " + before,
                    HttpStatus.CONFLICT);
        }

        BigDecimal after = before.subtract(request.amount());
        customer.setOutstandingBalance(after);
        customer.setUpdatedBy(SecurityUtils.currentUsername());

        CustomerPayment payment = new CustomerPayment();
        payment.setPaymentNumber(nextDocNumber("PAY", paymentRepository.nextPaymentSequence()));
        payment.setCustomer(customer);
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setReferenceNo(blankToNull(request.referenceNo()));
        payment.setNotes(blankToNull(request.notes()) != null
                ? blankToNull(request.notes())
                : "Invoice " + invoice.getInvoiceNumber());
        payment.setPaidAt(request.paidAt() != null ? request.paidAt() : java.time.Instant.now());
        payment.setBalanceBefore(before);
        payment.setBalanceAfter(after);
        payment.setCreatedBy(SecurityUtils.currentUsername());
        paymentRepository.save(payment);

        invoice.setAmountPaid(invoice.getAmountPaid().add(request.amount()));
        if (invoice.amountDue().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(FulfillmentInvoiceStatus.PAID);
        } else {
            invoice.setStatus(FulfillmentInvoiceStatus.PARTIALLY_PAID);
        }
        invoice.setUpdatedBy(SecurityUtils.currentUsername());

        auditService.log(
                "PAYMENT",
                "FULFILLMENT_INVOICE",
                String.valueOf(invoice.getId()),
                invoice.getInvoiceNumber() + " " + request.amount());
        return toInvoiceResponse(invoiceRepository.save(invoice));
    }

    // --- helpers ---

    private QuotationResponse transitionQuote(
            Long id, QuotationStatus from, QuotationStatus to, String action) {
        Quotation quote = requireQuote(id);
        if (quote.getStatus() != from) {
            throw new BusinessException(
                    "INVALID_STATUS",
                    "Expected status " + from + " but was " + quote.getStatus(),
                    HttpStatus.CONFLICT);
        }
        quote.setStatus(to);
        quote.setUpdatedBy(SecurityUtils.currentUsername());
        auditService.log(action, "QUOTATION", String.valueOf(id), quote.getQuoteNumber());
        return toQuoteResponse(quotationRepository.save(quote));
    }

    private void applyQuoteLines(Quotation quote, Customer customer, List<FulfillmentLineRequest> lines) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int lineNo = 1;
        for (FulfillmentLineRequest line : lines) {
            Product product = requireActiveProduct(line.productId());
            BigDecimal unitPrice = line.unitPrice() == null
                    ? money(pricingService.resolveUnitPrice(product, customer))
                    : money(line.unitPrice());
            BigDecimal lineDiscount = money(line.lineDiscount());
            BigDecimal lineTotal = money(unitPrice.multiply(line.quantity()).subtract(lineDiscount));
            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("VALIDATION_ERROR", "Line total cannot be negative", HttpStatus.BAD_REQUEST);
            }
            QuotationItem item = new QuotationItem();
            item.setLineNo(lineNo++);
            item.setProduct(product);
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setUnit(product.getUnit());
            item.setQuantity(line.quantity());
            item.setUnitPrice(unitPrice);
            item.setLineDiscount(lineDiscount);
            item.setLineTotal(lineTotal);
            quote.addItem(item);
            subtotal = subtotal.add(lineTotal);
        }
        quote.setSubtotal(money(subtotal));
        BigDecimal total = money(quote.getSubtotal().subtract(quote.getDiscountAmount()).add(quote.getTaxAmount()));
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("VALIDATION_ERROR", "Quote total cannot be negative", HttpStatus.BAD_REQUEST);
        }
        quote.setTotalAmount(total);
    }

    private void applyOrderLines(SalesOrder order, Customer customer, List<FulfillmentLineRequest> lines) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int lineNo = 1;
        for (FulfillmentLineRequest line : lines) {
            Product product = requireActiveProduct(line.productId());
            BigDecimal unitPrice = line.unitPrice() == null
                    ? money(pricingService.resolveUnitPrice(product, customer))
                    : money(line.unitPrice());
            BigDecimal lineDiscount = money(line.lineDiscount());
            BigDecimal lineTotal = money(unitPrice.multiply(line.quantity()).subtract(lineDiscount));
            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("VALIDATION_ERROR", "Line total cannot be negative", HttpStatus.BAD_REQUEST);
            }
            SalesOrderItem item = new SalesOrderItem();
            item.setLineNo(lineNo++);
            item.setProduct(product);
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setUnit(product.getUnit());
            item.setQuantityOrdered(line.quantity());
            item.setQuantityDelivered(BigDecimal.ZERO);
            item.setQuantityInvoiced(BigDecimal.ZERO);
            item.setUnitPrice(unitPrice);
            item.setLineDiscount(lineDiscount);
            item.setLineTotal(lineTotal);
            order.addItem(item);
            subtotal = subtotal.add(lineTotal);
        }
        order.setSubtotal(money(subtotal));
        BigDecimal total = money(order.getSubtotal().subtract(order.getDiscountAmount()).add(order.getTaxAmount()));
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("VALIDATION_ERROR", "Order total cannot be negative", HttpStatus.BAD_REQUEST);
        }
        order.setTotalAmount(total);
    }

    private SalesOrder buildOrderFromQuote(Quotation quote) {
        SalesOrder order = new SalesOrder();
        order.setSoNumber(nextDocNumber("SO", salesOrderRepository.nextSoSequence()));
        order.setQuotation(quote);
        order.setCustomer(quote.getCustomer());
        order.setStatus(SalesOrderStatus.OPEN);
        order.setSubtotal(quote.getSubtotal());
        order.setDiscountAmount(quote.getDiscountAmount());
        order.setTaxAmount(quote.getTaxAmount());
        order.setTotalAmount(quote.getTotalAmount());
        order.setNotes(quote.getNotes());
        order.setCreatedBy(SecurityUtils.currentUsername());
        order.setUpdatedBy(SecurityUtils.currentUsername());
        for (QuotationItem qi : quote.getItems()) {
            SalesOrderItem item = new SalesOrderItem();
            item.setLineNo(qi.getLineNo());
            item.setProduct(qi.getProduct());
            item.setProductSku(qi.getProductSku());
            item.setProductName(qi.getProductName());
            item.setUnit(qi.getUnit());
            item.setQuantityOrdered(qi.getQuantity());
            item.setQuantityDelivered(BigDecimal.ZERO);
            item.setQuantityInvoiced(BigDecimal.ZERO);
            item.setUnitPrice(qi.getUnitPrice());
            item.setLineDiscount(qi.getLineDiscount());
            item.setLineTotal(qi.getLineTotal());
            order.addItem(item);
        }
        return order;
    }

    private void refreshOrderDeliveryStatus(SalesOrder order) {
        boolean any = false;
        boolean all = true;
        for (SalesOrderItem item : order.getItems()) {
            if (item.getQuantityDelivered().compareTo(BigDecimal.ZERO) > 0) {
                any = true;
            }
            if (item.getQuantityDelivered().compareTo(item.getQuantityOrdered()) < 0) {
                all = false;
            }
        }
        if (all && any) {
            order.setStatus(SalesOrderStatus.FULLY_DELIVERED);
        } else if (any) {
            order.setStatus(SalesOrderStatus.PARTIALLY_DELIVERED);
        } else {
            order.setStatus(SalesOrderStatus.OPEN);
        }
    }

    private QuotationResponse toQuoteResponse(Quotation q) {
        List<QuotationItemResponse> items = q.getItems().stream()
                .map(i -> new QuotationItemResponse(
                        i.getId(),
                        i.getLineNo(),
                        i.getProduct() != null ? i.getProduct().getId() : null,
                        i.getProductSku(),
                        i.getProductName(),
                        i.getUnit(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getLineDiscount(),
                        i.getLineTotal()))
                .toList();
        return new QuotationResponse(
                q.getId(),
                q.getQuoteNumber(),
                q.getCustomer().getId(),
                q.getCustomer().getCustomerCode(),
                q.getCustomer().getBusinessName(),
                q.getStatus(),
                q.getSubtotal(),
                q.getDiscountAmount(),
                q.getTaxAmount(),
                q.getTotalAmount(),
                q.getNotes(),
                q.getValidUntil(),
                q.getCreatedAt(),
                q.getUpdatedAt(),
                q.getCreatedBy(),
                items);
    }

    private SalesOrderResponse toOrderResponse(SalesOrder o) {
        List<SalesOrderItemResponse> items = o.getItems().stream()
                .map(i -> {
                    BigDecimal open = i.getQuantityOrdered().subtract(i.getQuantityDelivered());
                    BigDecimal billable = i.getQuantityDelivered().subtract(i.getQuantityInvoiced());
                    return new SalesOrderItemResponse(
                            i.getId(),
                            i.getLineNo(),
                            i.getProduct() != null ? i.getProduct().getId() : null,
                            i.getProductSku(),
                            i.getProductName(),
                            i.getUnit(),
                            i.getQuantityOrdered(),
                            i.getQuantityDelivered(),
                            i.getQuantityInvoiced(),
                            open,
                            billable,
                            i.getUnitPrice(),
                            i.getLineDiscount(),
                            i.getLineTotal());
                })
                .toList();

        List<DeliveryReceiptSummaryResponse> deliveries = deliveryReceiptRepository
                .findBySalesOrderIdOrderByDeliveredAtDesc(o.getId())
                .stream()
                .map(d -> new DeliveryReceiptSummaryResponse(
                        d.getId(), d.getDrNumber(), d.getStatus(), d.getDeliveredAt()))
                .toList();

        List<InvoiceSummaryResponse> invoices = invoiceRepository
                .findBySalesOrderIdOrderByInvoicedAtDesc(o.getId())
                .stream()
                .map(inv -> new InvoiceSummaryResponse(
                        inv.getId(),
                        inv.getInvoiceNumber(),
                        inv.getStatus(),
                        inv.getPaymentMethod(),
                        inv.getTotalAmount(),
                        inv.getAmountPaid(),
                        inv.getInvoicedAt()))
                .toList();

        return new SalesOrderResponse(
                o.getId(),
                o.getSoNumber(),
                o.getQuotation() != null ? o.getQuotation().getId() : null,
                o.getQuotation() != null ? o.getQuotation().getQuoteNumber() : null,
                o.getCustomer().getId(),
                o.getCustomer().getCustomerCode(),
                o.getCustomer().getBusinessName(),
                o.getStatus(),
                o.getSubtotal(),
                o.getDiscountAmount(),
                o.getTaxAmount(),
                o.getTotalAmount(),
                o.getNotes(),
                o.getCreatedAt(),
                o.getUpdatedAt(),
                o.getCreatedBy(),
                items,
                deliveries,
                invoices);
    }

    private DeliveryReceiptResponse toDeliveryResponse(DeliveryReceipt d) {
        SalesOrder order = d.getSalesOrder();
        List<DeliveryReceiptItemResponse> items = d.getItems().stream()
                .map(i -> new DeliveryReceiptItemResponse(
                        i.getId(),
                        i.getLineNo(),
                        i.getSalesOrderItem().getId(),
                        i.getProduct() != null ? i.getProduct().getId() : null,
                        i.getProductSku(),
                        i.getProductName(),
                        i.getUnit(),
                        i.getQuantity(),
                        i.getUnitPrice()))
                .toList();
        return new DeliveryReceiptResponse(
                d.getId(),
                d.getDrNumber(),
                order.getId(),
                order.getSoNumber(),
                order.getCustomer().getId(),
                order.getCustomer().getCustomerCode(),
                order.getCustomer().getBusinessName(),
                d.getStatus(),
                d.getNotes(),
                d.getDeliveredAt(),
                d.getCreatedBy(),
                items);
    }

    private FulfillmentInvoiceResponse toInvoiceResponse(FulfillmentInvoice inv) {
        Customer c = inv.getCustomer();
        List<FulfillmentInvoiceItemResponse> items = inv.getItems().stream()
                .map(i -> new FulfillmentInvoiceItemResponse(
                        i.getId(),
                        i.getLineNo(),
                        i.getSalesOrderItem().getId(),
                        i.getProduct() != null ? i.getProduct().getId() : null,
                        i.getProductSku(),
                        i.getProductName(),
                        i.getUnit(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getLineDiscount(),
                        i.getLineTotal()))
                .toList();
        return new FulfillmentInvoiceResponse(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getSalesOrder().getId(),
                inv.getSalesOrder().getSoNumber(),
                c.getId(),
                c.getCustomerCode(),
                c.getBusinessName(),
                c.getTaxIdentificationNumber(),
                c.getAddress(),
                c.getPhone(),
                inv.getStatus(),
                inv.getPaymentMethod(),
                inv.getSubtotal(),
                inv.getDiscountAmount(),
                inv.getTaxAmount(),
                inv.getTotalAmount(),
                inv.getAmountPaid(),
                inv.amountDue(),
                inv.getNotes(),
                inv.getInvoicedAt(),
                inv.getCreatedBy(),
                items);
    }

    private Quotation requireQuote(Long id) {
        return quotationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found: " + id));
    }

    private SalesOrder requireOrder(Long id) {
        return salesOrderRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found: " + id));
    }

    private DeliveryReceipt requireDelivery(Long id) {
        return deliveryReceiptRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery receipt not found: " + id));
    }

    private FulfillmentInvoice requireInvoice(Long id) {
        return invoiceRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private Customer requireActiveCustomer(Long id) {
        Customer customer = customerRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException("CUSTOMER_INACTIVE", "Customer is not active", HttpStatus.BAD_REQUEST);
        }
        return customer;
    }

    private Product requireActiveProduct(Long id) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        if (!product.isActive() || product.isDeleted()) {
            throw new BusinessException(
                    "PRODUCT_INACTIVE", "Product is not available: " + product.getSku(), HttpStatus.BAD_REQUEST);
        }
        return product;
    }

    private String nextDocNumber(String prefix, long seq) {
        String day = LocalDate.now(STORE_ZONE).format(DAY_FMT);
        return prefix + "-" + day + "-" + String.format("%05d", seq);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Empty string (never null) so PostgreSQL does not bind LIKE params as bytea. */
    private String searchOrEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
