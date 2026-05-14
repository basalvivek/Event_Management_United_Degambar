package com.udjcs.rehearsal;

import com.udjcs.activity.Activity;
import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "rehearsals")
public class Rehearsal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "rehearsal_date", nullable = false)
    private LocalDate rehearsalDate;

    @Size(max = 10)
    @Column(name = "start_time", length = 10)
    private String startTime;

    @Size(max = 10)
    @Column(name = "end_time", length = 10)
    private String endTime;

    @Size(max = 200)
    @Column(length = 200)
    private String venue;

    @NotBlank
    @Size(max = 100)
    @Column(name = "conducted_by", nullable = false, length = 100)
    private String conductedBy;

    @Column
    private Integer attendance;

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

    public LocalDate getRehearsalDate() { return rehearsalDate; }
    public void setRehearsalDate(LocalDate rehearsalDate) { this.rehearsalDate = rehearsalDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getConductedBy() { return conductedBy; }
    public void setConductedBy(String conductedBy) { this.conductedBy = conductedBy; }

    public Integer getAttendance() { return attendance; }
    public void setAttendance(Integer attendance) { this.attendance = attendance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
}
