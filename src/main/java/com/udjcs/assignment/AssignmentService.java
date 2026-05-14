package com.udjcs.assignment;

import com.udjcs.activity.ActivityRepository;
import com.udjcs.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository repository;
    private final ActivityRepository activityRepository;
    private final MemberRepository memberRepository;

    public AssignmentService(AssignmentRepository repository,
                             ActivityRepository activityRepository,
                             MemberRepository memberRepository) {
        this.repository = repository;
        this.activityRepository = activityRepository;
        this.memberRepository = memberRepository;
    }

    public List<Assignment> findAll() {
        return repository.findAllWithDetails();
    }

    public Map<Long, List<Assignment>> findGroupedByActivity() {
        return repository.findAllWithDetails().stream()
                .collect(Collectors.groupingBy(
                        a -> a.getActivity().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    public Assignment findById(Long id) {
        return repository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    public void save(Assignment assignment) {
        assignment.setActivity(
            activityRepository.findById(assignment.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + assignment.getActivityId()))
        );
        assignment.setMember(
            memberRepository.findById(assignment.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + assignment.getMemberId()))
        );
        repository.save(assignment);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public boolean isDuplicate(Long activityId, Long memberId) {
        return repository.existsByActivity_IdAndMember_Id(activityId, memberId);
    }
}
