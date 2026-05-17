package com.udjcs.hall;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HallRegistrationRepository extends JpaRepository<HallRegistration, Long> {

    @Query("SELECT h FROM HallRegistration h LEFT JOIN FETCH h.event ORDER BY h.hireDate DESC")
    List<HallRegistration> findAllWithEvent();

    @Query("SELECT h FROM HallRegistration h LEFT JOIN FETCH h.event WHERE h.id = :id")
    java.util.Optional<HallRegistration> findByIdWithEvent(@org.springframework.data.repository.query.Param("id") Long id);
}
