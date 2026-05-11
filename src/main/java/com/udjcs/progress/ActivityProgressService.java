package com.udjcs.progress;

import com.udjcs.activity.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ActivityProgressService {

    private final ActivityProgressRepository repository;
    private final ActivityRepository activityRepository;

    public ActivityProgressService(ActivityProgressRepository repository,
                                   ActivityRepository activityRepository) {
        this.repository = repository;
        this.activityRepository = activityRepository;
    }

    public List<ActivityProgress> findAll() {
        return repository.findAllWithDetails();
    }

    public ActivityProgress findById(Long id) {
        return repository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Progress record not found: " + id));
    }

    public void save(ActivityProgress progress) {
        progress.setActivity(
            activityRepository.findById(progress.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + progress.getActivityId()))
        );
        repository.save(progress);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
