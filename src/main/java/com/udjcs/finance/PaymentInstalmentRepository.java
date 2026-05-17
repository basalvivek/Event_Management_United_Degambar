package com.udjcs.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentInstalmentRepository extends JpaRepository<PaymentInstalment, Long> {

    List<PaymentInstalment> findBySourceTypeAndSourceIdOrderByPaymentDateAsc(String sourceType, Long sourceId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM PaymentInstalment i WHERE i.sourceType = :type AND i.sourceId = :id")
    BigDecimal sumBySourceTypeAndSourceId(@Param("type") String sourceType, @Param("id") Long sourceId);
}
