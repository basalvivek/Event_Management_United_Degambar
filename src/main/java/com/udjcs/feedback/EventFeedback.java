package com.udjcs.feedback;

import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import com.udjcs.member.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "event_feedback")
public class EventFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Size(max = 50)
    @Column(name = "overall_experience", length = 50)
    private String overallExperience;

    @Size(max = 50)
    @Column(name = "satisfaction_level", length = 50)
    private String satisfactionLevel;

    @Column(name = "venue_rating")
    private Integer venueRating;

    @Column(name = "program_coordination_rating")
    private Integer programCoordinationRating;

    @Column(name = "hospitality_rating")
    private Integer hospitalityRating;

    @Column(name = "food_rating")
    private Integer foodRating;

    @Column(name = "spiritual_rating")
    private Integer spiritualRating;

    @Size(max = 1000)
    @Column(name = "enjoyed_most", length = 1000)
    private String enjoyedMost;

    @Size(max = 20)
    @Column(name = "timings_managed", length = 20)
    private String timingsManaged;

    @Size(max = 1000)
    @Column(length = 1000)
    private String suggestions;

    @Size(max = 20)
    @Column(name = "participate_future", length = 20)
    private String participateFuture;

    @Size(max = 2000)
    @Column(name = "additional_comments", length = 2000)
    private String additionalComments;

    @Size(max = 2000)
    @Column(name = "admin_reply", length = 2000)
    private String adminReply;

    @Column(name = "replied_at")
    private java.time.LocalDateTime repliedAt;

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public String getOverallExperience() { return overallExperience; }
    public void setOverallExperience(String overallExperience) { this.overallExperience = overallExperience; }

    public String getSatisfactionLevel() { return satisfactionLevel; }
    public void setSatisfactionLevel(String satisfactionLevel) { this.satisfactionLevel = satisfactionLevel; }

    public Integer getVenueRating() { return venueRating; }
    public void setVenueRating(Integer venueRating) { this.venueRating = venueRating; }

    public Integer getProgramCoordinationRating() { return programCoordinationRating; }
    public void setProgramCoordinationRating(Integer programCoordinationRating) { this.programCoordinationRating = programCoordinationRating; }

    public Integer getHospitalityRating() { return hospitalityRating; }
    public void setHospitalityRating(Integer hospitalityRating) { this.hospitalityRating = hospitalityRating; }

    public Integer getFoodRating() { return foodRating; }
    public void setFoodRating(Integer foodRating) { this.foodRating = foodRating; }

    public Integer getSpiritualRating() { return spiritualRating; }
    public void setSpiritualRating(Integer spiritualRating) { this.spiritualRating = spiritualRating; }

    public String getEnjoyedMost() { return enjoyedMost; }
    public void setEnjoyedMost(String enjoyedMost) { this.enjoyedMost = enjoyedMost; }

    public String getTimingsManaged() { return timingsManaged; }
    public void setTimingsManaged(String timingsManaged) { this.timingsManaged = timingsManaged; }

    public String getSuggestions() { return suggestions; }
    public void setSuggestions(String suggestions) { this.suggestions = suggestions; }

    public String getParticipateFuture() { return participateFuture; }
    public void setParticipateFuture(String participateFuture) { this.participateFuture = participateFuture; }

    public String getAdditionalComments() { return additionalComments; }
    public void setAdditionalComments(String additionalComments) { this.additionalComments = additionalComments; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }

    public java.time.LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(java.time.LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
}
