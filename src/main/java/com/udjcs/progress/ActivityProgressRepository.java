package com.udjcs.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityProgressRepository extends JpaRepository<ActivityProgress, Long> {

    @Query("SELECT p FROM ActivityProgress p JOIN FETCH p.activity a JOIN FETCH a.activityCategory ORDER BY p.progressDate DESC")
    List<ActivityProgress> findAllWithDetails();

    @Query("SELECT p FROM ActivityProgress p JOIN FETCH p.activity a JOIN FETCH a.activityCategory WHERE p.id = :id")
    Optional<ActivityProgress> findByIdWithDetails(@Param("id") Long id);
}
