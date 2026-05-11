package com.udjcs.progress;

import com.udjcs.activity.Activity;
import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "progress")
public class ActivityProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;

    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "progress_percentage", nullable = false)
    private Integer progressPercentage;

    @Size(max = 200)
    @Column(length = 200)
    private String milestone;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Size(max = 100)
    @Column(name = "recorded_by", length = 100)
    private String recordedBy;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String status;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @Transient
    private Long activityId;

    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }

    public LocalDate getProgressDate() { return progressDate; }
    public void setProgressDate(LocalDate progressDate) { this.progressDate = progressDate; }

    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getMilestone() { return milestone; }
    public void setMilestone(String milestone) { this.milestone = milestone; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
}
