package com.udjcs.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    boolean existsBySlug(String slug);

    @Query("SELECT i FROM Invitation i LEFT JOIN FETCH i.organization LEFT JOIN FETCH i.activities WHERE i.slug = :slug")
    Optional<Invitation> findBySlugWithDetails(@Param("slug") String slug);

    @Query("SELECT i FROM Invitation i LEFT JOIN FETCH i.organization LEFT JOIN FETCH i.activities WHERE i.id = :id")
    Optional<Invitation> findByIdWithDetails(@Param("id") Long id);
}
