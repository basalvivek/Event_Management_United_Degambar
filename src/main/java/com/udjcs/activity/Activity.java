package com.udjcs.activity;

import com.udjcs.activity.category.ActivityCategory;
import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "activities")
public class Activity extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(name = "activity_name", nullable = false, length = 200)
    private String activityName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ActivityCategory activityCategory;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "start_date")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "end_date")
    private LocalDate endDate;

    @Size(max = 200)
    @Column(length = 200)
    private String venue;

    @Size(max = 100)
    @Column(name = "responsible_person", length = 100)
    private String responsiblePerson;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String status;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @Column(name = "show_on_portal", nullable = false)
    private boolean showOnPortal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Transient
    private Long categoryId;

    @Transient
    private Long eventId;

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public ActivityCategory getActivityCategory() { return activityCategory; }
    public void setActivityCategory(ActivityCategory activityCategory) { this.activityCategory = activityCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isShowOnPortal() { return showOnPortal; }
    public void setShowOnPortal(boolean showOnPortal) { this.showOnPortal = showOnPortal; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}
