package com.hardwarehub.credit.repository;

import com.hardwarehub.credit.domain.CustomerPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, Long> {

    Page<CustomerPayment> findByCustomerIdOrderByPaidAtDesc(Long customerId, Pageable pageable);

    List<CustomerPayment> findByCustomerIdOrderByPaidAtAscIdAsc(Long customerId);

    List<CustomerPayment> findByCustomerIdAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtAscIdAsc(
            Long customerId, Instant from, Instant to);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM CustomerPayment p
            WHERE p.customer.id = :customerId AND p.paidAt < :before
            """)
    BigDecimal sumAmountBefore(@Param("customerId") Long customerId, @Param("before") Instant before);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM CustomerPayment p
            WHERE p.customer.id = :customerId
              AND p.paidAt >= :from AND p.paidAt < :to
            """)
    BigDecimal sumAmountBetween(
            @Param("customerId") Long customerId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = "SELECT nextval('payment_number_seq')", nativeQuery = true)
    long nextPaymentSequence();
}
