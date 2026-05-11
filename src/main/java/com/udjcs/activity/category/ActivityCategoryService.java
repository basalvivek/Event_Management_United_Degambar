package com.udjcs.activity.category;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ActivityCategoryService {

    private final ActivityCategoryRepository repository;

    public ActivityCategoryService(ActivityCategoryRepository repository) {
        this.repository = repository;
    }

    public List<ActivityCategory> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "categoryName"));
    }

    public ActivityCategory findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Activity category not found: " + id));
    }

    public void save(ActivityCategory category) {
        repository.save(category);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
