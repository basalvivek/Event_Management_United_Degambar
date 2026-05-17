package com.udjcs.payable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PayableTransactionRepository extends JpaRepository<PayableTransaction, Long> {

    @Query("SELECT p FROM PayableTransaction p ORDER BY p.paymentDate DESC NULLS LAST, p.id DESC")
    List<PayableTransaction> findAllOrdered();

    java.util.Optional<PayableTransaction> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
