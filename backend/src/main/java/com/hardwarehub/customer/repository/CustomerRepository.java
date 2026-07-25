package com.hardwarehub.customer.repository;

import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCustomerCodeIgnoreCaseAndDeletedAtIsNull(String customerCode);

    boolean existsByCustomerCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String customerCode, Long id);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.deletedAt IS NULL
              AND (:status IS NULL OR c.status = :status)
              AND (:code IS NULL OR :code = '' OR
                   LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :code, '%')))
              AND (:businessName IS NULL OR :businessName = '' OR
                   LOWER(c.businessName) LIKE LOWER(CONCAT('%', :businessName, '%')))
              AND (:contact IS NULL OR :contact = '' OR
                   LOWER(COALESCE(c.contactPerson, '')) LIKE LOWER(CONCAT('%', :contact, '%')))
              AND (:phone IS NULL OR :phone = '' OR
                   LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :phone, '%')))
              AND (:city IS NULL OR :city = '' OR
                   LOWER(COALESCE(c.city, '')) LIKE LOWER(CONCAT('%', :city, '%')))
              AND (:hasBalanceDue IS NULL OR :hasBalanceDue = false OR c.outstandingBalance > 0)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(c.contactPerson, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(c.city, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Customer> search(
            @Param("search") String search,
            @Param("status") CustomerStatus status,
            @Param("code") String code,
            @Param("businessName") String businessName,
            @Param("contact") String contact,
            @Param("phone") String phone,
            @Param("city") String city,
            @Param("hasBalanceDue") Boolean hasBalanceDue,
            Pageable pageable);
}
