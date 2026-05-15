package com.udjcs.announcement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a WHERE a.active = true AND (a.expiryDate IS NULL OR a.expiryDate >= :today) ORDER BY a.publishedDate DESC")
    List<Announcement> findActiveForPortal(@Param("today") LocalDate today);
}
