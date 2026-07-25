package com.hardwarehub.credit.service;

import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.dto.PageResponse;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.ResourceNotFoundException;
import com.hardwarehub.common.security.SecurityUtils;
import com.hardwarehub.credit.domain.CustomerPayment;
import com.hardwarehub.credit.domain.LedgerEntryType;
import com.hardwarehub.credit.dto.CreditSummaryResponse;
import com.hardwarehub.credit.dto.CustomerPaymentResponse;
import com.hardwarehub.credit.dto.LedgerEntryResponse;
import com.hardwarehub.credit.dto.RecordPaymentRequest;
import com.hardwarehub.credit.dto.StatementOfAccountResponse;
import com.hardwarehub.credit.mapper.CreditMapper;
import com.hardwarehub.credit.repository.CustomerPaymentRepository;
import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.repository.CustomerRepository;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus;
import com.hardwarehub.fulfillment.repository.FulfillmentInvoiceRepository;
import com.hardwarehub.sales.domain.PaymentMethod;
import com.hardwarehub.sales.domain.SaleStatus;
import com.hardwarehub.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditLedgerService {

    private static final Instant OPEN_RANGE_START = Instant.EPOCH;
    private static final Instant OPEN_RANGE_END = Instant.parse("9999-12-31T23:59:59.999Z");
    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Manila");

    private final CustomerRepository customerRepository;
    private final CustomerPaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final FulfillmentInvoiceRepository fulfillmentInvoiceRepository;
    private final CreditMapper creditMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CreditSummaryResponse summary() {
        return new CreditSummaryResponse(
                customerRepository.countWithBalanceDue(),
                customerRepository.sumOutstandingBalances());
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerPaymentResponse> listPayments(Long customerId, Pageable pageable) {
        requireCustomer(customerId);
        return PageResponse.from(
                paymentRepository.findByCustomerIdOrderByPaidAtDesc(customerId, pageable).map(creditMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> ledger(Long customerId) {
        requireCustomer(customerId);
        return buildLedger(customerId, OPEN_RANGE_START, OPEN_RANGE_END, BigDecimal.ZERO).lines();
    }

    @Transactional(readOnly = true)
    public StatementOfAccountResponse statement(Long customerId, Instant from, Instant to) {
        Customer customer = requireCustomer(customerId);
        Instant periodFrom = from != null ? from : OPEN_RANGE_START;
        Instant periodTo = to != null ? to : OPEN_RANGE_END;
        if (!periodFrom.isBefore(periodTo)) {
            throw new BusinessException(
                    "VALIDATION_ERROR", "Statement 'from' must be before 'to'", HttpStatus.BAD_REQUEST);
        }

        BigDecimal chargesBefore = saleRepository
                .sumCreditChargesBefore(customerId, periodFrom)
                .add(fulfillmentInvoiceRepository.sumCreditChargesBefore(customerId, periodFrom));
        BigDecimal paymentsBefore = paymentRepository.sumAmountBefore(customerId, periodFrom);
        BigDecimal opening = chargesBefore.subtract(paymentsBefore);

        LedgerBuild built = buildLedger(customerId, periodFrom, periodTo, opening);

        return new StatementOfAccountResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getBusinessName(),
                customer.getContactPerson(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getCity(),
                customer.getProvince(),
                customer.getTaxIdentificationNumber(),
                periodFrom,
                periodTo,
                opening,
                built.totalCharges(),
                built.totalPayments(),
                built.closingBalance(),
                customer.getCreditLimit(),
                customer.getOutstandingBalance(),
                built.lines());
    }

    @Transactional
    public CustomerPaymentResponse recordPayment(Long customerId, RecordPaymentRequest request) {
        Customer customer = customerRepository
                .findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "Payment amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal before = customer.getOutstandingBalance() == null ? BigDecimal.ZERO : customer.getOutstandingBalance();
        if (before.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("NO_BALANCE_DUE", "Customer has no outstanding balance", HttpStatus.CONFLICT);
        }
        if (amount.compareTo(before) > 0) {
            throw new BusinessException(
                    "PAYMENT_EXCEEDS_BALANCE",
                    "Payment exceeds outstanding balance. Due: " + before,
                    HttpStatus.CONFLICT);
        }

        BigDecimal after = before.subtract(amount);
        customer.setOutstandingBalance(after);
        customer.setUpdatedBy(SecurityUtils.currentUsername());

        CustomerPayment payment = new CustomerPayment();
        payment.setPaymentNumber(nextPaymentNumber());
        payment.setCustomer(customer);
        payment.setAmount(amount);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setReferenceNo(blankToNull(request.referenceNo()));
        payment.setNotes(blankToNull(request.notes()));
        payment.setPaidAt(request.paidAt() != null ? request.paidAt() : Instant.now());
        payment.setBalanceBefore(before);
        payment.setBalanceAfter(after);
        payment.setCreatedBy(SecurityUtils.currentUsername());

        CustomerPayment saved = paymentRepository.save(payment);
        auditService.log(
                "PAYMENT",
                "CREDIT",
                String.valueOf(saved.getId()),
                customer.getCustomerCode() + " " + amount + " (" + before + " → " + after + ")");

        return creditMapper.toResponse(saved);
    }

    private LedgerBuild buildLedger(Long customerId, Instant from, Instant to, BigDecimal openingBalance) {
        record Timed(Instant at, long id, LedgerEntryType type, LedgerEntryResponse provisional) {}

        List<Timed> events = new ArrayList<>();
        saleRepository
                .findByCustomerIdAndPaymentMethodAndStatusOrderBySoldAtAscIdAsc(
                        customerId, PaymentMethod.CREDIT, SaleStatus.COMPLETED)
                .stream()
                .filter(s -> !s.getSoldAt().isBefore(from) && s.getSoldAt().isBefore(to))
                .forEach(sale -> events.add(new Timed(
                        sale.getSoldAt(),
                        sale.getId(),
                        LedgerEntryType.CHARGE,
                        new LedgerEntryResponse(
                                LedgerEntryType.CHARGE,
                                sale.getSoldAt(),
                                sale.getReceiptNumber(),
                                "Credit sale",
                                sale.getTotalAmount(),
                                BigDecimal.ZERO,
                                null,
                                sale.getId(),
                                null))));

        fulfillmentInvoiceRepository
                .findByCustomerIdAndPaymentMethodAndStatusNotOrderByInvoicedAtAscIdAsc(
                        customerId, PaymentMethod.CREDIT, FulfillmentInvoiceStatus.VOIDED)
                .stream()
                .filter(i -> !i.getInvoicedAt().isBefore(from) && i.getInvoicedAt().isBefore(to))
                .forEach(invoice -> events.add(new Timed(
                        invoice.getInvoicedAt(),
                        invoice.getId(),
                        LedgerEntryType.CHARGE,
                        new LedgerEntryResponse(
                                LedgerEntryType.CHARGE,
                                invoice.getInvoicedAt(),
                                invoice.getInvoiceNumber(),
                                "Credit invoice",
                                invoice.getTotalAmount(),
                                BigDecimal.ZERO,
                                null,
                                null,
                                null))));

        paymentRepository
                .findByCustomerIdAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtAscIdAsc(
                        customerId, from, to)
                .forEach(payment -> events.add(new Timed(
                        payment.getPaidAt(),
                        payment.getId(),
                        LedgerEntryType.PAYMENT,
                        new LedgerEntryResponse(
                                LedgerEntryType.PAYMENT,
                                payment.getPaidAt(),
                                payment.getPaymentNumber(),
                                paymentMethodLabel(payment),
                                BigDecimal.ZERO,
                                payment.getAmount(),
                                null,
                                null,
                                payment.getId()))));

        events.sort(Comparator
                .comparing(Timed::at)
                .thenComparing(Timed::type)
                .thenComparingLong(Timed::id));

        BigDecimal running = openingBalance;
        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        List<LedgerEntryResponse> lines = new ArrayList<>(events.size());

        for (Timed event : events) {
            LedgerEntryResponse p = event.provisional();
            if (p.entryType() == LedgerEntryType.CHARGE) {
                running = running.add(p.chargeAmount());
                totalCharges = totalCharges.add(p.chargeAmount());
            } else {
                running = running.subtract(p.paymentAmount());
                totalPayments = totalPayments.add(p.paymentAmount());
            }
            lines.add(new LedgerEntryResponse(
                    p.entryType(),
                    p.occurredAt(),
                    p.reference(),
                    p.description(),
                    p.chargeAmount(),
                    p.paymentAmount(),
                    running,
                    p.saleId(),
                    p.paymentId()));
        }

        return new LedgerBuild(lines, totalCharges, totalPayments, running);
    }

    private String paymentMethodLabel(CustomerPayment payment) {
        String method = payment.getPaymentMethod().name().replace('_', ' ');
        if (payment.getReferenceNo() != null && !payment.getReferenceNo().isBlank()) {
            return "Payment (" + method + ") ref " + payment.getReferenceNo();
        }
        return "Payment (" + method + ")";
    }

    private String nextPaymentNumber() {
        long seq = paymentRepository.nextPaymentSequence();
        String day = LocalDate.now(STORE_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PAY-" + day + "-" + String.format("%05d", seq);
    }

    private Customer requireCustomer(Long id) {
        return customerRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record LedgerBuild(
            List<LedgerEntryResponse> lines,
            BigDecimal totalCharges,
            BigDecimal totalPayments,
            BigDecimal closingBalance) {}
}
