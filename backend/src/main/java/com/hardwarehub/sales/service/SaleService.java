package com.hardwarehub.sales.service;

import com.hardwarehub.catalog.domain.Product;
import com.hardwarehub.catalog.repository.ProductRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.domain.CustomerStatus;
import com.hardwarehub.customer.repository.CustomerRepository;
import com.hardwarehub.inventory.domain.InventoryTransactionType;
import com.hardwarehub.inventory.dto.InventoryTransactionRequest;
import com.hardwarehub.inventory.service.InventoryService;
import com.hardwarehub.sales.domain.PaymentMethod;
import com.hardwarehub.sales.domain.Sale;
import com.hardwarehub.sales.domain.SaleItem;
import com.hardwarehub.sales.domain.SaleStatus;
import com.hardwarehub.sales.dto.CreateSaleRequest;
import com.hardwarehub.sales.dto.SaleItemRequest;
import com.hardwarehub.sales.dto.SaleResponse;
import com.hardwarehub.sales.dto.SalesSummaryResponse;
import com.hardwarehub.sales.mapper.SaleMapper;
import com.hardwarehub.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Manila");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;
    private final SaleMapper saleMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> list(
            String search,
            SaleStatus status,
            Long customerId,
            String receipt,
            String customer,
            String cashier,
            PaymentMethod paymentMethod,
            Instant soldFrom,
            Instant soldTo,
            BigDecimal totalMin,
            BigDecimal totalMax,
            Pageable pageable) {
        return PageResponse.from(
                saleRepository
                        .search(
                                blankToNull(search),
                                status,
                                customerId,
                                blankToNull(receipt),
                                blankToNull(customer),
                                blankToNull(cashier),
                                paymentMethod,
                                soldFrom,
                                soldTo,
                                totalMin,
                                totalMax,
                                pageable)
                        .map(saleMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public SaleResponse get(Long id) {
        return saleMapper.toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public SaleResponse getByReceipt(String receiptNumber) {
        return saleRepository.findByReceiptNumber(receiptNumber)
                .map(saleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + receiptNumber));
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse summary() {
        LocalDate today = LocalDate.now(STORE_ZONE);
        Instant dayStart = today.atStartOfDay(STORE_ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();
        LocalDate monthStartDate = today.withDayOfMonth(1);
        Instant monthStart = monthStartDate.atStartOfDay(STORE_ZONE).toInstant();
        Instant monthEnd = monthStartDate.plusMonths(1).atStartOfDay(STORE_ZONE).toInstant();

        return new SalesSummaryResponse(
                saleRepository.sumCompletedBetween(dayStart, dayEnd),
                saleRepository.countCompletedBetween(dayStart, dayEnd),
                saleRepository.sumCompletedBetween(monthStart, monthEnd),
                saleRepository.countCompletedBetween(monthStart, monthEnd));
    }

    @Transactional
    public SaleResponse checkout(CreateSaleRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "Sale must have at least one item", HttpStatus.BAD_REQUEST);
        }

        Customer customer = resolveCustomer(request.customerId(), request.paymentMethod());
        String username = SecurityUtils.currentUsername();
        String receiptNumber = nextReceiptNumber();

        Sale sale = new Sale();
        sale.setReceiptNumber(receiptNumber);
        sale.setCustomer(customer);
        sale.setCashierUsername(username);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setPaymentMethod(request.paymentMethod());
        sale.setDiscountAmount(money(request.discountAmount()));
        sale.setTaxAmount(money(request.taxAmount()));
        sale.setNotes(blankToNull(request.notes()));
        sale.setCreatedBy(username);
        sale.setUpdatedBy(username);

        BigDecimal subtotal = BigDecimal.ZERO;
        int lineNo = 1;
        for (SaleItemRequest line : request.items()) {
            Product product = productRepository.findByIdForUpdate(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.productId()));
            if (!product.isActive() || product.isDeleted()) {
                throw new BusinessException("PRODUCT_INACTIVE", "Product is not available: " + product.getSku(),
                        HttpStatus.BAD_REQUEST);
            }
            BigDecimal qty = line.quantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("VALIDATION_ERROR", "Item quantity must be greater than zero",
                        HttpStatus.BAD_REQUEST);
            }
            BigDecimal unitPrice = line.unitPrice() == null ? product.getSellingPrice() : money(line.unitPrice());
            BigDecimal lineDiscount = money(line.lineDiscount());
            BigDecimal lineTotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP).subtract(lineDiscount);
            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("VALIDATION_ERROR", "Line total cannot be negative", HttpStatus.BAD_REQUEST);
            }

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setUnit(product.getUnit());
            item.setQuantity(qty);
            item.setUnitPrice(unitPrice);
            item.setLineDiscount(lineDiscount);
            item.setLineTotal(lineTotal);
            item.setLineNo(lineNo++);
            sale.addItem(item);
            subtotal = subtotal.add(lineTotal);

            inventoryService.create(new InventoryTransactionRequest(
                    product.getId(),
                    InventoryTransactionType.STOCK_OUT,
                    qty,
                    product.getCostPrice(),
                    receiptNumber,
                    "POS sale " + receiptNumber));
        }

        sale.setSubtotal(money(subtotal));
        BigDecimal total = sale.getSubtotal().subtract(sale.getDiscountAmount()).add(sale.getTaxAmount());
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("VALIDATION_ERROR", "Sale total cannot be negative", HttpStatus.BAD_REQUEST);
        }
        sale.setTotalAmount(money(total));

        applyPayment(sale, request, customer);

        Sale saved = saleRepository.save(sale);
        auditService.log("CREATE", "SALE", String.valueOf(saved.getId()),
                "Sale " + receiptNumber + " total=" + saved.getTotalAmount());
        return saleMapper.toResponse(require(saved.getId()));
    }

    private void applyPayment(Sale sale, CreateSaleRequest request, Customer customer) {
        PaymentMethod method = request.paymentMethod();
        if (method == PaymentMethod.CASH) {
            BigDecimal tendered = request.amountTendered() == null ? sale.getTotalAmount() : money(request.amountTendered());
            if (tendered.compareTo(sale.getTotalAmount()) < 0) {
                throw new BusinessException("INSUFFICIENT_TENDER", "Amount tendered is less than total",
                        HttpStatus.BAD_REQUEST);
            }
            sale.setAmountTendered(tendered);
            sale.setChangeAmount(money(tendered.subtract(sale.getTotalAmount())));
            return;
        }

        if (method == PaymentMethod.CARD) {
            sale.setAmountTendered(sale.getTotalAmount());
            sale.setChangeAmount(BigDecimal.ZERO);
            return;
        }

        // CREDIT
        if (customer == null) {
            throw new BusinessException("CUSTOMER_REQUIRED", "Credit sales require a registered customer",
                    HttpStatus.BAD_REQUEST);
        }
        BigDecimal available = customer.getCreditLimit().subtract(customer.getOutstandingBalance());
        if (sale.getTotalAmount().compareTo(available) > 0) {
            throw new BusinessException(
                    "CREDIT_LIMIT_EXCEEDED",
                    "Credit limit exceeded. Available: " + available,
                    HttpStatus.CONFLICT);
        }
        customer.setOutstandingBalance(customer.getOutstandingBalance().add(sale.getTotalAmount()));
        customer.setUpdatedBy(SecurityUtils.currentUsername());
        sale.setAmountTendered(BigDecimal.ZERO);
        sale.setChangeAmount(BigDecimal.ZERO);
    }

    private Customer resolveCustomer(Long customerId, PaymentMethod paymentMethod) {
        if (customerId == null) {
            if (paymentMethod == PaymentMethod.CREDIT) {
                throw new BusinessException("CUSTOMER_REQUIRED", "Credit sales require a registered customer",
                        HttpStatus.BAD_REQUEST);
            }
            return null;
        }
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException("CUSTOMER_INACTIVE", "Customer is not active for sales", HttpStatus.BAD_REQUEST);
        }
        return customer;
    }

    private String nextReceiptNumber() {
        long seq = saleRepository.nextReceiptSequence();
        String day = LocalDate.now(STORE_ZONE).format(DAY_FMT);
        return "RCP-" + day + "-" + String.format("%05d", seq);
    }

    private Sale require(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
