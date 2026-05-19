package com.udjcs.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("SELECT a FROM Assignment a JOIN FETCH a.activity act JOIN FETCH act.activityCategory JOIN FETCH a.member ORDER BY a.id DESC")
    List<Assignment> findAllWithDetails();

    @Query("SELECT a FROM Assignment a JOIN FETCH a.activity act JOIN FETCH act.activityCategory JOIN FETCH a.member WHERE a.id = :id")
    Optional<Assignment> findByIdWithDetails(@Param("id") Long id);

    boolean existsByActivity_IdAndMember_Id(Long activityId, Long memberId);

    @Query("SELECT a FROM Assignment a JOIN FETCH a.activity act JOIN FETCH act.activityCategory WHERE a.member.id = :memberId ORDER BY a.id DESC")
    List<Assignment> findByMemberIdWithDetails(@Param("memberId") Long memberId);
}
