package com.hardwarehub.fulfillment.repository;

import com.hardwarehub.fulfillment.domain.FulfillmentInvoice;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus;
import com.hardwarehub.sales.domain.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FulfillmentInvoiceRepository extends JpaRepository<FulfillmentInvoice, Long> {

    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    long nextInvoiceSequence();

    Optional<FulfillmentInvoice> findByInvoiceNumber(String invoiceNumber);

    List<FulfillmentInvoice> findBySalesOrderIdOrderByInvoicedAtDesc(Long salesOrderId);

    List<FulfillmentInvoice> findByCustomerIdAndPaymentMethodAndStatusNotOrderByInvoicedAtAscIdAsc(
            Long customerId, PaymentMethod paymentMethod, FulfillmentInvoiceStatus status);

    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0) FROM FulfillmentInvoice i
            WHERE i.customer.id = :customerId
              AND i.paymentMethod = com.hardwarehub.sales.domain.PaymentMethod.CREDIT
              AND i.status <> com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus.VOIDED
              AND i.invoicedAt < :before
            """)
    BigDecimal sumCreditChargesBefore(@Param("customerId") Long customerId, @Param("before") Instant before);

    @Query("""
            SELECT i FROM FulfillmentInvoice i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:customerId IS NULL OR i.customer.id = :customerId)
              AND (:search = '' OR
                   LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(i.customer.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(i.customer.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(i.salesOrder.soNumber) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<FulfillmentInvoice> search(
            @Param("search") String search,
            @Param("status") FulfillmentInvoiceStatus status,
            @Param("customerId") Long customerId,
            Pageable pageable);
}
