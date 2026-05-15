package com.udjcs.feedback;

import com.udjcs.event.EventRepository;
import com.udjcs.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class EventFeedbackService {

    private final EventFeedbackRepository repository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;

    public EventFeedbackService(EventFeedbackRepository repository,
                                EventRepository eventRepository,
                                MemberRepository memberRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.memberRepository = memberRepository;
    }

    public void submit(Long eventId, Long memberId,
                       String overallExperience, String satisfactionLevel,
                       Integer venueRating, Integer programCoordinationRating,
                       Integer hospitalityRating, Integer foodRating, Integer spiritualRating,
                       String enjoyedMost, String timingsManaged,
                       String suggestions, String participateFuture, String additionalComments) {
        EventFeedback ef = new EventFeedback();
        ef.setEvent(eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId)));
        ef.setMember(memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId)));
        ef.setOverallExperience(overallExperience);
        ef.setSatisfactionLevel(satisfactionLevel);
        ef.setVenueRating(venueRating);
        ef.setProgramCoordinationRating(programCoordinationRating);
        ef.setHospitalityRating(hospitalityRating);
        ef.setFoodRating(foodRating);
        ef.setSpiritualRating(spiritualRating);
        ef.setEnjoyedMost(enjoyedMost);
        ef.setTimingsManaged(timingsManaged);
        ef.setSuggestions(suggestions);
        ef.setParticipateFuture(participateFuture);
        ef.setAdditionalComments(additionalComments);
        repository.save(ef);
    }

    public boolean hasSubmitted(Long eventId, Long memberId) {
        return repository.existsByEvent_IdAndMember_Id(eventId, memberId);
    }

    public List<EventFeedback> findByEvent(Long eventId) {
        return repository.findByEvent_IdOrderByCreatedAtDesc(eventId);
    }

    public List<EventFeedback> findAll() {
        return repository.findAllWithDetails();
    }

    public void saveReply(Long id, String reply) {
        EventFeedback ef = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found: " + id));
        ef.setAdminReply(reply);
        ef.setRepliedAt(java.time.LocalDateTime.now());
        repository.save(ef);
    }
}
