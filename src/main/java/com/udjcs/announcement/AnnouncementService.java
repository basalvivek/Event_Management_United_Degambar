package com.udjcs.announcement;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository repository;

    public AnnouncementService(AnnouncementRepository repository) {
        this.repository = repository;
    }

    public List<Announcement> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "publishedDate", "id"));
    }

    public Announcement findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Announcement not found: " + id));
    }

    public void save(Announcement announcement) {
        repository.save(announcement);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<Announcement> findActiveForPortal() {
        return repository.findActiveForPortal(LocalDate.now());
    }
}
