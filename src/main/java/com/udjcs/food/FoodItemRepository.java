package com.udjcs.food;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM FoodItem fi WHERE fi.foodRegistration.id = :parentId")
    void deleteByFoodRegistrationId(@Param("parentId") Long parentId);
}
