package com.udjcs.ticket;

import com.udjcs.event.Event;
import com.udjcs.event.EventRepository;
import com.udjcs.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class EventTicketService {

    private final EventTicketRepository repository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;

    public EventTicketService(EventTicketRepository repository,
                              EventRepository eventRepository,
                              MemberRepository memberRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.memberRepository = memberRepository;
    }

    public EventTicket register(Long eventId, Long memberId,
                                Integer adultCount, Integer youngerCount, Integer childCount) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        int adultPrice   = event.getTicketAdult()   != null ? event.getTicketAdult()   : 0;
        int youngerPrice = event.getTicketYounger()  != null ? event.getTicketYounger() : 0;
        int childPrice   = event.getTicketChild()    != null ? event.getTicketChild()   : 0;

        int aC = adultCount   != null ? adultCount   : 0;
        int yC = youngerCount != null ? youngerCount : 0;
        int cC = childCount   != null ? childCount   : 0;

        int adultAmt   = aC * adultPrice;
        int youngerAmt = yC * youngerPrice;
        int childAmt   = cC * childPrice;

        EventTicket ticket = new EventTicket();
        ticket.setEvent(event);
        ticket.setMember(memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId)));
        ticket.setAdultCount(aC);
        ticket.setYoungerCount(yC);
        ticket.setChildCount(cC);
        ticket.setAdultAmount(adultAmt);
        ticket.setYoungerAmount(youngerAmt);
        ticket.setChildAmount(childAmt);
        ticket.setTotalAmount(adultAmt + youngerAmt + childAmt);
        ticket.setStatus("Pending");
        return repository.save(ticket);
    }

    public void approve(Long id) {
        EventTicket t = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
        t.setStatus("Accepted");
        repository.save(t);
    }

    public void reject(Long id) {
        EventTicket t = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
        t.setStatus("Rejected");
        repository.save(t);
    }

    public List<EventTicket> findAll() {
        return repository.findAllWithDetails();
    }

    public List<EventTicket> findFiltered(Long eventId, String status) {
        if (eventId != null && status != null && !status.isBlank()) {
            return repository.findByEventIdAndStatusWithDetails(eventId, status);
        } else if (eventId != null) {
            return repository.findByEventIdWithDetails(eventId);
        } else if (status != null && !status.isBlank()) {
            return repository.findByStatusWithDetails(status);
        }
        return repository.findAllWithDetails();
    }

    public boolean isRegistered(Long eventId, Long memberId) {
        return repository.existsByEvent_IdAndMember_Id(eventId, memberId);
    }

    public EventTicket findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }
}
