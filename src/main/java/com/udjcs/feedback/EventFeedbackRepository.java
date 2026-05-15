package com.udjcs.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {

    @Query("SELECT ef FROM EventFeedback ef JOIN FETCH ef.event JOIN FETCH ef.member ORDER BY ef.createdAt DESC")
    List<EventFeedback> findAllWithDetails();

    @Query("SELECT ef FROM EventFeedback ef JOIN FETCH ef.event JOIN FETCH ef.member WHERE ef.event.id = :eventId ORDER BY ef.createdAt DESC")
    List<EventFeedback> findByEvent_IdOrderByCreatedAtDesc(Long eventId);

    boolean existsByEvent_IdAndMember_Id(Long eventId, Long memberId);
}
