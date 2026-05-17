package com.udjcs.eventprogram;

import com.udjcs.event.Event;
import com.udjcs.event.EventRepository;
import com.udjcs.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EventProgramService {

    private final EventProgramRepository repository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;

    public EventProgramService(EventProgramRepository repository,
                               EventRepository eventRepository,
                               MemberRepository memberRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.memberRepository = memberRepository;
    }

    public List<EventProgram> findAll() {
        return repository.findAllWithDetails();
    }

    public EventProgram findById(Long id) {
        return repository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Event program not found: " + id));
    }

    public void save(EventProgram program) {
        program.setEvent(
            eventRepository.findById(program.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + program.getEventId()))
        );
        if (program.getResponsibleMemberId() != null) {
            program.setResponsibleMember(
                memberRepository.findById(program.getResponsibleMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("Member not found: " + program.getResponsibleMemberId()))
            );
        } else {
            program.setResponsibleMember(null);
        }
        repository.save(program);
    }

    public void saveAll(Long eventId, List<EventProgram> programs) {
        com.udjcs.event.Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        for (EventProgram p : programs) {
            p.setEvent(event);
            if (p.getResponsibleMemberId() != null) {
                p.setResponsibleMember(
                    memberRepository.findById(p.getResponsibleMemberId())
                        .orElseThrow(() -> new IllegalArgumentException("Member not found: " + p.getResponsibleMemberId()))
                );
            }
        }
        repository.saveAll(programs);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public void replaceAll(Long eventId, List<EventProgram> programs) {
        repository.deleteAll(repository.findByEventIdWithDetails(eventId));
        if (!programs.isEmpty()) {
            saveAll(eventId, programs);
        }
    }

    public List<EventProgram> findByActiveEvents() {
        return repository.findByEventStatusWithDetails("Active");
    }

    public List<EventProgram> findByEventStatus(String status) {
        return repository.findByEventStatusWithDetails(status);
    }

    public Map<Event, List<EventProgram>> findGroupedByEvent() {
        Map<Event, List<EventProgram>> grouped = new LinkedHashMap<>();
        // Seed all events so events with 0 programmes still appear
        eventRepository.findAll(org.springframework.data.domain.Sort.by("eventName"))
                .forEach(e -> grouped.put(e, new ArrayList<>()));
        // Populate programmes for events that have them
        for (EventProgram p : repository.findAllWithDetails()) {
            grouped.computeIfAbsent(p.getEvent(), k -> new ArrayList<>()).add(p);
        }
        return grouped;
    }

    public List<EventProgram> findByEventId(Long eventId) {
        return repository.findByEventIdWithDetails(eventId);
    }
}
