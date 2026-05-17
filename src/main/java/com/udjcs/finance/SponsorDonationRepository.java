package com.udjcs.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SponsorDonationRepository extends JpaRepository<SponsorDonation, Long> {
}
