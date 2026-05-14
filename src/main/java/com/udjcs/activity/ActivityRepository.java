package com.udjcs.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("SELECT a FROM Activity a JOIN FETCH a.activityCategory ORDER BY a.id DESC")
    List<Activity> findAllWithDetails();

    @Query("SELECT a FROM Activity a JOIN FETCH a.activityCategory WHERE a.id = :id")
    Optional<Activity> findByIdWithDetails(@Param("id") Long id);

    long countByStatus(String status);

    @Query("SELECT a FROM Activity a JOIN FETCH a.activityCategory WHERE a.status = :status ORDER BY a.id DESC")
    List<Activity> findByStatusWithDetails(@Param("status") String status);

    @Query("SELECT a FROM Activity a JOIN FETCH a.activityCategory WHERE a.status IN :statuses ORDER BY a.id DESC")
    List<Activity> findByStatusInWithDetails(@Param("statuses") Collection<String> statuses);

    @Query("SELECT a FROM Activity a JOIN FETCH a.activityCategory WHERE a.showOnPortal = true AND (a.endDate IS NULL OR a.endDate >= :today) ORDER BY a.startDate ASC NULLS LAST, a.id DESC")
    List<Activity> findVisibleOnPortal(@Param("today") LocalDate today);
}
