package com.udjcs.receivable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReceivableTransactionRepository extends JpaRepository<ReceivableTransaction, Long> {

    @Query("SELECT r FROM ReceivableTransaction r ORDER BY COALESCE(NULLIF(r.organisationName,''),'zzzzz') ASC, r.receiptDate DESC NULLS LAST, r.id DESC")
    List<ReceivableTransaction> findAllOrdered();

    java.util.Optional<ReceivableTransaction> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM ReceivableTransaction r")
    java.math.BigDecimal sumTotalAmount();
}
