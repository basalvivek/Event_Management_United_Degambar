package com.udjcs.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.supportiveOrganization LEFT JOIN FETCH p.member ORDER BY p.id DESC")
    List<Payment> findAllWithDetails();

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.supportiveOrganization LEFT JOIN FETCH p.member WHERE p.id = :id")
    Optional<Payment> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT SUM(p.amount) FROM Payment p")
    BigDecimal sumAllAmounts();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.supportiveOrganization IS NOT NULL")
    BigDecimal sumOrgDonations();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.supportiveOrganization IS NOT NULL")
    long countOrgDonations();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.member IS NOT NULL")
    BigDecimal sumMemberDonations();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.member IS NOT NULL")
    long countMemberDonations();
}
