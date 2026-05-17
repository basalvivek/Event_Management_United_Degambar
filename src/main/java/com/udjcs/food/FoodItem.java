package com.udjcs.food;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "food_items")
public class FoodItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_registration_id", nullable = false)
    private FoodRegistration foodRegistration;

    @Column(name = "food_category", length = 100)
    private String foodCategory;

    @Column(name = "food_name", length = 150)
    private String foodName;

    @Column
    private Integer quantity;

    @Column
    private Integer rating;

    public FoodRegistration getFoodRegistration() { return foodRegistration; }
    public void setFoodRegistration(FoodRegistration foodRegistration) { this.foodRegistration = foodRegistration; }
    public String getFoodCategory() { return foodCategory; }
    public void setFoodCategory(String foodCategory) { this.foodCategory = foodCategory; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
