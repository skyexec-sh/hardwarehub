package com.hardwarehub.fulfillment.repository;

import com.hardwarehub.fulfillment.domain.SalesOrder;
import com.hardwarehub.fulfillment.domain.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    @Query(value = "SELECT nextval('so_number_seq')", nativeQuery = true)
    long nextSoSequence();

    Optional<SalesOrder> findBySoNumber(String soNumber);

    long countByStatus(SalesOrderStatus status);

    long countByStatusIn(java.util.Collection<SalesOrderStatus> statuses);

    @Query("""
            SELECT o FROM SalesOrder o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:customerId IS NULL OR o.customer.id = :customerId)
              AND (:search = '' OR
                   LOWER(o.soNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(o.customer.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(o.customer.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<SalesOrder> search(
            @Param("search") String search,
            @Param("status") SalesOrderStatus status,
            @Param("customerId") Long customerId,
            Pageable pageable);
}
