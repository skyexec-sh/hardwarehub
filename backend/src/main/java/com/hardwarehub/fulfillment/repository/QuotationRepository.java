package com.hardwarehub.fulfillment.repository;

import com.hardwarehub.fulfillment.domain.Quotation;
import com.hardwarehub.fulfillment.domain.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    @Query(value = "SELECT nextval('quote_number_seq')", nativeQuery = true)
    long nextQuoteSequence();

    Optional<Quotation> findByQuoteNumber(String quoteNumber);

    long countByStatusIn(java.util.Collection<QuotationStatus> statuses);

    @Query("""
            SELECT q FROM Quotation q
            WHERE (:status IS NULL OR q.status = :status)
              AND (:customerId IS NULL OR q.customer.id = :customerId)
              AND (:search = '' OR
                   LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(q.customer.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(q.customer.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Quotation> search(
            @Param("search") String search,
            @Param("status") QuotationStatus status,
            @Param("customerId") Long customerId,
            Pageable pageable);
}
