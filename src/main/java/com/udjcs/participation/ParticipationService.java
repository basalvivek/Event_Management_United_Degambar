package com.udjcs.participation;

import com.udjcs.event.EventRepository;
import com.udjcs.member.MemberRepository;
import com.udjcs.supportive.SupportiveOrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class ParticipationService {

    private final ParticipationRepository repository;
    private final EventRepository eventRepository;
    private final SupportiveOrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;

    public ParticipationService(ParticipationRepository repository,
                                EventRepository eventRepository,
                                SupportiveOrganizationRepository organizationRepository,
                                MemberRepository memberRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
    }

    public List<Participation> findAll() {
        List<Participation> list = repository.findAllWithDetails();
        list.forEach(p -> p.getMembers().size());
        return list;
    }

    public Participation findById(Long id) {
        Participation p = repository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Participation not found: " + id));
        p.getMembers().size();
        return p;
    }

    public void save(Participation participation) {
        participation.setEvent(
            eventRepository.findById(participation.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + participation.getEventId()))
        );
        participation.setSupportiveOrganization(
            organizationRepository.findById(participation.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + participation.getOrganizationId()))
        );
        if (participation.getMemberIds() != null && !participation.getMemberIds().isEmpty()) {
            participation.setMembers(new HashSet<>(memberRepository.findAllById(participation.getMemberIds())));
        } else {
            participation.setMembers(new HashSet<>());
        }
        repository.save(participation);
    }

    public void deleteById(Long id) {
        Participation p = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Participation not found: " + id));
        p.getMembers().clear();
        repository.delete(p);
    }
}
