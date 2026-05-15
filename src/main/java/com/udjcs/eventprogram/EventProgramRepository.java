package com.udjcs.eventprogram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventProgramRepository extends JpaRepository<EventProgram, Long> {

    @Query("SELECT ep FROM EventProgram ep JOIN FETCH ep.event e LEFT JOIN FETCH ep.responsibleMember ORDER BY e.eventName ASC, ep.startTime ASC")
    List<EventProgram> findAllWithDetails();

    @Query("SELECT ep FROM EventProgram ep JOIN FETCH ep.event e LEFT JOIN FETCH ep.responsibleMember WHERE ep.id = :id")
    Optional<EventProgram> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT ep FROM EventProgram ep JOIN FETCH ep.event e LEFT JOIN FETCH ep.responsibleMember WHERE e.status = :status ORDER BY e.eventName ASC, ep.startTime ASC")
    List<EventProgram> findByEventStatusWithDetails(@Param("status") String status);

    @Query("SELECT ep FROM EventProgram ep JOIN FETCH ep.event e LEFT JOIN FETCH ep.responsibleMember WHERE e.id = :eventId ORDER BY ep.startTime ASC")
    List<EventProgram> findByEventIdWithDetails(@Param("eventId") Long eventId);
}
