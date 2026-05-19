package com.udjcs.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorDonationRepository extends JpaRepository<SponsorDonation, Long> {

    @Query("SELECT s FROM SponsorDonation s LEFT JOIN FETCH s.event ORDER BY s.id DESC")
    List<SponsorDonation> findAllWithEvent();

    @Query("SELECT s FROM SponsorDonation s LEFT JOIN FETCH s.event WHERE s.id = :id")
    Optional<SponsorDonation> findByIdWithEvent(@org.springframework.data.repository.query.Param("id") Long id);
}
