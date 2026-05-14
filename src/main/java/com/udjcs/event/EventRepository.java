package com.udjcs.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    long countByEventDateGreaterThanEqual(LocalDate date);
    List<Event> findTop5ByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate date);
    List<Event> findTop5ByEventDateGreaterThanEqualAndStatusInOrderByEventDateAsc(LocalDate date, Collection<String> statuses);
}
