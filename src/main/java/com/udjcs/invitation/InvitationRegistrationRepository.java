package com.udjcs.invitation;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvitationRegistrationRepository extends JpaRepository<InvitationRegistration, Long> {
    List<InvitationRegistration> findByInvitationId(Long invitationId, Sort sort);
    long countByInvitationId(Long invitationId);

    @Query("SELECT r FROM InvitationRegistration r JOIN FETCH r.invitation ORDER BY r.id DESC")
    List<InvitationRegistration> findAllWithInvitation();

    @Query("SELECT r FROM InvitationRegistration r JOIN FETCH r.invitation WHERE r.id = :id")
    java.util.Optional<InvitationRegistration> findByIdWithInvitation(@Param("id") Long id);
}
