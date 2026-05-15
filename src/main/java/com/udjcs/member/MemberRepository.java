package com.udjcs.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    long countByStatus(String status);
    long countByApprovalStatus(String approvalStatus);
    List<Member> findByApprovalStatusOrderByCreatedAtAsc(String approvalStatus);
    Optional<Member> findByEmailIgnoreCase(String email);
}
