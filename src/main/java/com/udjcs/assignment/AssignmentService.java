package com.udjcs.assignment;

import com.udjcs.activity.ActivityRepository;
import com.udjcs.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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
}
