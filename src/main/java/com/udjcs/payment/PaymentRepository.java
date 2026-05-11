package com.udjcs.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.supportiveOrganization ORDER BY p.id DESC")
    List<Payment> findAllWithDetails();

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.supportiveOrganization WHERE p.id = :id")
    Optional<Payment> findByIdWithDetails(@Param("id") Long id);
}
