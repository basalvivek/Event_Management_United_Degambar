package com.udjcs.food;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRegistrationRepository extends JpaRepository<FoodRegistration, Long> {

    @Query("SELECT f FROM FoodRegistration f LEFT JOIN FETCH f.event ORDER BY f.id DESC")
    List<FoodRegistration> findAllOrdered();

    @Query("SELECT DISTINCT f FROM FoodRegistration f LEFT JOIN FETCH f.foodItems LEFT JOIN FETCH f.event WHERE f.id = :id")
    Optional<FoodRegistration> findByIdWithItems(Long id);
}
