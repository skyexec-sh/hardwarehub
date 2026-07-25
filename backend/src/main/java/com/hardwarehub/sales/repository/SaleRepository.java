package com.hardwarehub.sales.repository;

import com.hardwarehub.sales.domain.PaymentMethod;
import com.hardwarehub.sales.domain.Sale;
import com.hardwarehub.sales.domain.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<Sale> findById(Long id);

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<Sale> findByReceiptNumber(String receiptNumber);

    @EntityGraph(attributePaths = {"customer"})
    @Query("""
            SELECT s FROM Sale s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod)
              AND (:customerId IS NULL OR s.customer.id = :customerId)
              AND (:receipt IS NULL OR :receipt = '' OR
                   LOWER(s.receiptNumber) LIKE LOWER(CONCAT('%', :receipt, '%')))
              AND (:customer IS NULL OR :customer = '' OR
                   LOWER(COALESCE(s.customer.businessName, 'walk-in')) LIKE LOWER(CONCAT('%', :customer, '%')) OR
                   LOWER(COALESCE(s.customer.customerCode, '')) LIKE LOWER(CONCAT('%', :customer, '%')))
              AND (:cashier IS NULL OR :cashier = '' OR
                   LOWER(s.cashierUsername) LIKE LOWER(CONCAT('%', :cashier, '%')))
              AND (:soldFrom IS NULL OR s.soldAt >= :soldFrom)
              AND (:soldTo IS NULL OR s.soldAt < :soldTo)
              AND (:totalMin IS NULL OR s.totalAmount >= :totalMin)
              AND (:totalMax IS NULL OR s.totalAmount <= :totalMax)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(s.receiptNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.customer.businessName, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.customer.customerCode, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.cashierUsername) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Sale> search(
            @Param("search") String search,
            @Param("status") SaleStatus status,
            @Param("customerId") Long customerId,
            @Param("receipt") String receipt,
            @Param("customer") String customer,
            @Param("cashier") String cashier,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("soldFrom") Instant soldFrom,
            @Param("soldTo") Instant soldTo,
            @Param("totalMin") BigDecimal totalMin,
            @Param("totalMax") BigDecimal totalMax,
            Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    List<Sale> findByCustomerIdAndStatusOrderBySoldAtDesc(Long customerId, SaleStatus status);

    @Query("""
            SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
            WHERE s.status = com.hardwarehub.sales.domain.SaleStatus.COMPLETED
              AND s.soldAt >= :from AND s.soldAt < :to
            """)
    BigDecimal sumCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COUNT(s) FROM Sale s
            WHERE s.status = com.hardwarehub.sales.domain.SaleStatus.COMPLETED
              AND s.soldAt >= :from AND s.soldAt < :to
            """)
    long countCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT nextval('receipt_number_seq')", nativeQuery = true)
    long nextReceiptSequence();
}
