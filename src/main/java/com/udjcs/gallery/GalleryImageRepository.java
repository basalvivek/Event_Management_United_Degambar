package com.udjcs.gallery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {

    @Query("SELECT g.id FROM GalleryImage g ORDER BY g.id ASC")
    List<Long> findAllIds();
}
