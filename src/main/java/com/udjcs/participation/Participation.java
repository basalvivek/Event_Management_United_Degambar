package com.udjcs.participation;

import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import com.udjcs.member.Member;
import com.udjcs.supportive.SupportiveOrganization;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "participations")
public class Participation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supportive_organization_id", nullable = false)
    private SupportiveOrganization supportiveOrganization;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String participationType;

    @Column
    private Integer numberOfParticipants;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "participation_members",
        joinColumns = @JoinColumn(name = "participation_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private Set<Member> members = new HashSet<>();

    @Transient
    private Long eventId;

    @Transient
    private Long organizationId;

    @Transient
    private Set<Long> memberIds = new HashSet<>();

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public SupportiveOrganization getSupportiveOrganization() { return supportiveOrganization; }
    public void setSupportiveOrganization(SupportiveOrganization supportiveOrganization) { this.supportiveOrganization = supportiveOrganization; }

    public String getParticipationType() { return participationType; }
    public void setParticipationType(String participationType) { this.participationType = participationType; }

    public Integer getNumberOfParticipants() { return numberOfParticipants; }
    public void setNumberOfParticipants(Integer numberOfParticipants) { this.numberOfParticipants = numberOfParticipants; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public Set<Member> getMembers() { return members; }
    public void setMembers(Set<Member> members) { this.members = members; }

    public Set<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(Set<Long> memberIds) { this.memberIds = memberIds; }
}
