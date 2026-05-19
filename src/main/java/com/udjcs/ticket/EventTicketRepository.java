package com.udjcs.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventTicketRepository extends JpaRepository<EventTicket, Long> {

    @Query("SELECT t FROM EventTicket t JOIN FETCH t.event JOIN FETCH t.member ORDER BY t.createdAt DESC")
    List<EventTicket> findAllWithDetails();

    @Query("SELECT t FROM EventTicket t JOIN FETCH t.event JOIN FETCH t.member WHERE t.id = :id")
    java.util.Optional<EventTicket> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM EventTicket t JOIN FETCH t.event JOIN FETCH t.member WHERE t.event.id = :eventId ORDER BY t.createdAt DESC")
    List<EventTicket> findByEventIdWithDetails(@Param("eventId") Long eventId);

    @Query("SELECT t FROM EventTicket t JOIN FETCH t.event JOIN FETCH t.member WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<EventTicket> findByStatusWithDetails(@Param("status") String status);

    @Query("SELECT t FROM EventTicket t JOIN FETCH t.event JOIN FETCH t.member WHERE t.event.id = :eventId AND t.status = :status ORDER BY t.createdAt DESC")
    List<EventTicket> findByEventIdAndStatusWithDetails(@Param("eventId") Long eventId, @Param("status") String status);

    boolean existsByEvent_IdAndMember_Id(Long eventId, Long memberId);

    @Query("SELECT t FROM EventTicket t JOIN FETCH t.event WHERE t.member.id = :memberId ORDER BY t.createdAt DESC")
    List<EventTicket> findByMemberIdWithEvent(@Param("memberId") Long memberId);

    @Query("SELECT SUM(t.totalAmount) FROM EventTicket t WHERE t.status = 'Accepted'")
    Long sumAcceptedTickets();

    @Query("SELECT COUNT(t) FROM EventTicket t WHERE t.status = 'Accepted'")
    long countAcceptedTickets();
}
