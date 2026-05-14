package com.udjcs.rehearsal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RehearsalRepository extends JpaRepository<Rehearsal, Long> {

    @Query("SELECT r FROM Rehearsal r JOIN FETCH r.activity a JOIN FETCH a.activityCategory ORDER BY r.rehearsalDate DESC")
    List<Rehearsal> findAllWithDetails();

    @Query("SELECT r FROM Rehearsal r JOIN FETCH r.activity a JOIN FETCH a.activityCategory WHERE r.id = :id")
    Optional<Rehearsal> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM Rehearsal r JOIN FETCH r.activity a WHERE r.rehearsalDate >= :date ORDER BY r.rehearsalDate ASC")
    List<Rehearsal> findUpcomingWithActivity(@Param("date") LocalDate date);

    boolean existsByActivity_IdAndRehearsalDate(Long activityId, LocalDate rehearsalDate);
}
