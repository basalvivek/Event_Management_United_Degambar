package com.udjcs.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    @Query("SELECT p FROM Participation p JOIN FETCH p.event JOIN FETCH p.supportiveOrganization ORDER BY p.id DESC")
    List<Participation> findAllWithDetails();

    @Query("SELECT p FROM Participation p JOIN FETCH p.event JOIN FETCH p.supportiveOrganization WHERE p.id = :id")
    Optional<Participation> findByIdWithDetails(@Param("id") Long id);
}
