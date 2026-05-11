package com.udjcs.venue;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class VenueService {

    private final VenueRepository repository;

    public VenueService(VenueRepository repository) {
        this.repository = repository;
    }

    public List<Venue> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "venueName"));
    }

    public Venue findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + id));
    }

    public void save(Venue venue) {
        repository.save(venue);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
