package com.udjcs.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("SELECT a FROM Activity a JOIN FETCH a.activityCategory ORDER BY a.id DESC")
    List<Activity> findAllWithDetails();

    @Query("SELECT a FROM Activity a JOIN FETCH a.activityCategory WHERE a.id = :id")
    Optional<Activity> findByIdWithDetails(@Param("id") Long id);
}
