package com.udjcs.rehearsal;

import com.udjcs.activity.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class RehearsalService {

    private final RehearsalRepository repository;
    private final ActivityRepository activityRepository;

    public RehearsalService(RehearsalRepository repository,
                            ActivityRepository activityRepository) {
        this.repository = repository;
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

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
