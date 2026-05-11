package com.udjcs.activity;

import com.udjcs.activity.category.ActivityCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ActivityService {

    private final ActivityRepository repository;
    private final ActivityCategoryRepository categoryRepository;

    public ActivityService(ActivityRepository repository,
                           ActivityCategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    public List<Activity> findAll() {
        return repository.findAllWithDetails();
    }

    public Activity findById(Long id) {
        return repository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));
    }

    public void save(Activity activity) {
        activity.setActivityCategory(
            categoryRepository.findById(activity.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + activity.getCategoryId()))
        );
        repository.save(activity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
