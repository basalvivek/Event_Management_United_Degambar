package com.udjcs.event;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public List<Event> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "eventDate"));
    }

    public Event findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
    }

    public void save(Event event) {
        repository.save(event);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<Event> findByStatus(String status) {
        return repository.findByStatusOrderByEventNameAsc(status);
    }

    public List<Event> findExcludingStatus(String status) {
        return repository.findByStatusNotOrderByEventNameAsc(status);
    }
}
