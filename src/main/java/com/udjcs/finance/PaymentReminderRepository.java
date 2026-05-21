package com.udjcs.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, Long> {
    long countBySourceTypeAndSourceId(String sourceType, Long sourceId);
    List<PaymentReminder> findBySourceTypeAndSourceIdOrderByReminderDateDesc(String sourceType, Long sourceId);
}
