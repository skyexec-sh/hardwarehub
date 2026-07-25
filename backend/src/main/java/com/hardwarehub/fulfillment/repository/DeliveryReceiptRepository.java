package com.hardwarehub.fulfillment.repository;

import com.hardwarehub.fulfillment.domain.DeliveryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeliveryReceiptRepository extends JpaRepository<DeliveryReceipt, Long> {

    @Query(value = "SELECT nextval('dr_number_seq')", nativeQuery = true)
    long nextDrSequence();

    Optional<DeliveryReceipt> findByDrNumber(String drNumber);

    List<DeliveryReceipt> findBySalesOrderIdOrderByDeliveredAtDesc(Long salesOrderId);
}
