package com.udjcs.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    long countByEventDateGreaterThanEqual(LocalDate date);
    List<Event> findTop5ByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate date);
    List<Event> findTop5ByEventDateGreaterThanEqualAndStatusInOrderByEventDateAsc(LocalDate date, Collection<String> statuses);
    @Query("SELECT e FROM Event e WHERE e.status IN :statuses ORDER BY e.eventDate ASC")
    List<Event> findByStatusInOrderByEventDateAsc(@Param("statuses") Collection<String> statuses);
    List<Event> findByStatusOrderByEventNameAsc(String status);
    List<Event> findByStatusOrderByEventDateDesc(String status);
    List<Event> findByStatusOrderByEventDateAsc(String status);
    List<Event> findByStatusNotOrderByEventNameAsc(String status);
}
