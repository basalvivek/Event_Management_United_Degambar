package com.udjcs.supportive;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportiveOrganizationRepository extends JpaRepository<SupportiveOrganization, Long> {
}
