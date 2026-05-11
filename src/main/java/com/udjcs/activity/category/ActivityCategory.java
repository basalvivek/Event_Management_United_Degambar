package com.udjcs.activity.category;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "activity_categories")
public class ActivityCategory extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String status;

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
