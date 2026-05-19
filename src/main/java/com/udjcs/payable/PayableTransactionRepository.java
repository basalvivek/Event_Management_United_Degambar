package com.udjcs.payable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PayableTransactionRepository extends JpaRepository<PayableTransaction, Long> {

    @Query("SELECT p FROM PayableTransaction p LEFT JOIN FETCH p.event ORDER BY p.paymentDate DESC NULLS LAST, p.id DESC")
    List<PayableTransaction> findAllOrdered();

    @Query("SELECT p FROM PayableTransaction p LEFT JOIN FETCH p.event WHERE p.id = :id")
    java.util.Optional<PayableTransaction> findByIdWithEvent(@org.springframework.data.repository.query.Param("id") Long id);

    java.util.Optional<PayableTransaction> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
