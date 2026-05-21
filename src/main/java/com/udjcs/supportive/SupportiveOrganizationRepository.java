package com.udjcs.supportive;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportiveOrganizationRepository extends JpaRepository<SupportiveOrganization, Long> {
    java.util.List<SupportiveOrganization> findByOrganizationTypeOrderByNameAsc(String organizationType);
    java.util.List<SupportiveOrganization> findByOrganizationTypeNotOrderByNameAsc(String organizationType);

    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM SupportiveOrganization s WHERE s.organizationType IN ('Group Sponsor','Individual Sponsor','Sponsor') OR s.sponsorshipType IS NOT NULL ORDER BY s.name ASC")
    java.util.List<SupportiveOrganization> findAllSponsors();
}
