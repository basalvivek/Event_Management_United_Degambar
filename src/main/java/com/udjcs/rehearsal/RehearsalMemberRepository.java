package com.udjcs.rehearsal;

import com.udjcs.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RehearsalMemberRepository extends JpaRepository<RehearsalMember, Long> {

    List<RehearsalMember> findByRehearsalIdOrderByIdAsc(Long rehearsalId);

    void deleteByRehearsalId(Long rehearsalId);

    boolean existsByRehearsalIdAndMemberId(Long rehearsalId, Long memberId);

    long countByRehearsalId(Long rehearsalId);

    long countByRehearsalIdAndAttendedTrue(Long rehearsalId);

    @Query("SELECT rm, m FROM RehearsalMember rm JOIN Member m ON rm.memberId = m.id WHERE rm.rehearsalId = :rid")
    List<Object[]> findWithMembersByRehearsalId(@Param("rid") Long rehearsalId);

    @Query("SELECT rm.rehearsalId, m.firstName, m.lastName, rm.role, rm.attended, rm.memberId FROM RehearsalMember rm JOIN Member m ON rm.memberId = m.id WHERE rm.rehearsalId IN :ids ORDER BY rm.rehearsalId, m.firstName")
    List<Object[]> findAllMembersForRehearsalIds(@Param("ids") List<Long> ids);
}
