package com.udjcs.rehearsal;

import com.udjcs.activity.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RehearsalService {

    private final RehearsalRepository repository;
    private final RehearsalMemberRepository memberRepository;
    private final ActivityRepository activityRepository;

    public RehearsalService(RehearsalRepository repository,
                            RehearsalMemberRepository memberRepository,
                            ActivityRepository activityRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.activityRepository = activityRepository;
    }

    public List<Rehearsal> findAll() {
        return repository.findAllWithDetails();
    }

    public Rehearsal findById(Long id) {
        return repository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Rehearsal not found: " + id));
    }

    public void save(Rehearsal rehearsal) {
        rehearsal.setActivity(
            activityRepository.findById(rehearsal.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + rehearsal.getActivityId()))
        );
        repository.save(rehearsal);
    }

    public void saveMembers(Long rehearsalId, List<Long> memberIds, List<String> memberRoles) {
        memberRepository.deleteByRehearsalId(rehearsalId);
        if (memberIds == null) return;
        for (int i = 0; i < memberIds.size(); i++) {
            Long memberId = memberIds.get(i);
            if (memberId == null) continue;
            if (memberRepository.existsByRehearsalIdAndMemberId(rehearsalId, memberId)) continue;
            RehearsalMember rm = new RehearsalMember();
            rm.setRehearsalId(rehearsalId);
            rm.setMemberId(memberId);
            rm.setRole(memberRoles != null && i < memberRoles.size() ? memberRoles.get(i) : "Participant");
            memberRepository.save(rm);
        }
    }

    public void appendMembers(Long rehearsalId, List<Long> memberIds, List<String> memberRoles) {
        if (memberIds == null) return;
        for (int i = 0; i < memberIds.size(); i++) {
            Long memberId = memberIds.get(i);
            if (memberId == null) continue;
            if (memberRepository.existsByRehearsalIdAndMemberId(rehearsalId, memberId)) continue;
            RehearsalMember rm = new RehearsalMember();
            rm.setRehearsalId(rehearsalId);
            rm.setMemberId(memberId);
            rm.setRole(memberRoles != null && i < memberRoles.size() ? memberRoles.get(i) : "Participant");
            memberRepository.save(rm);
        }
    }

    public List<RehearsalMember> findMembers(Long rehearsalId) {
        return memberRepository.findByRehearsalIdOrderByIdAsc(rehearsalId);
    }

    public long countMembers(Long rehearsalId) {
        return memberRepository.countByRehearsalId(rehearsalId);
    }

    public Map<Long, Long> memberCountByRehearsal(List<Rehearsal> rehearsals) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        rehearsals.forEach(r -> counts.put(r.getId(), memberRepository.countByRehearsalId(r.getId())));
        return counts;
    }

    public Map<Long, Long> attendedCountByRehearsal(List<Rehearsal> rehearsals) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        rehearsals.forEach(r -> counts.put(r.getId(), memberRepository.countByRehearsalIdAndAttendedTrue(r.getId())));
        return counts;
    }

    public void saveAttendance(Long rehearsalId, List<Long> attendedMemberIds) {
        List<RehearsalMember> members = memberRepository.findByRehearsalIdOrderByIdAsc(rehearsalId);
        for (RehearsalMember rm : members) {
            rm.setAttended(attendedMemberIds != null && attendedMemberIds.contains(rm.getMemberId()));
            memberRepository.save(rm);
        }
    }

    public Map<Long, List<Object[]>> memberDetailsByRehearsal(List<Rehearsal> rehearsals) {
        List<Long> ids = rehearsals.stream().map(Rehearsal::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return new LinkedHashMap<>();
        Map<Long, List<Object[]>> result = new LinkedHashMap<>();
        rehearsals.forEach(r -> result.put(r.getId(), new java.util.ArrayList<>()));
        memberRepository.findAllMembersForRehearsalIds(ids).forEach(row -> {
            Long rid = (Long) row[0];
            result.computeIfAbsent(rid, k -> new java.util.ArrayList<>()).add(row);
        });
        return result;
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public Map<Long, List<Rehearsal>> findGroupedByActivity() {
        return repository.findAllWithDetails().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getActivity().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    public boolean isDuplicate(Long activityId, LocalDate rehearsalDate) {
        return repository.existsByActivity_IdAndRehearsalDate(activityId, rehearsalDate);
    }

    public boolean isDuplicateExcluding(Long activityId, LocalDate rehearsalDate, Long excludeId) {
        return repository.findAllWithDetails().stream()
                .anyMatch(r -> r.getActivity().getId().equals(activityId)
                        && r.getRehearsalDate().equals(rehearsalDate)
                        && !r.getId().equals(excludeId));
    }
}
