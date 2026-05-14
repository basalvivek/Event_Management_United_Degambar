package com.udjcs.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationDisplayPictureRepository extends JpaRepository<OrganizationDisplayPicture, Long> {
    List<OrganizationDisplayPicture> findByOrganizationIdOrderByDisplayOrderAsc(Long organizationId);
    long countByOrganizationId(Long organizationId);
}
