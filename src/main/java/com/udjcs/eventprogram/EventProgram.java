package com.udjcs.eventprogram;

import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import com.udjcs.member.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalTime;

@Entity
@Table(name = "event_programs")
public class EventProgram extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank
    @Size(max = 200)
    @Column(name = "program_name", nullable = false, length = 200)
    private String programName;

    @Size(max = 1000)
    @Column(name = "program_description", length = 1000)
    private String programDescription;

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_member_id")
    private Member responsibleMember;

    @Size(max = 500)
    @Column(length = 500)
    private String remark;

    @Transient
    private Long eventId;

    @Transient
    private Long responsibleMemberId;

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getProgramDescription() { return programDescription; }
    public void setProgramDescription(String programDescription) { this.programDescription = programDescription; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Member getResponsibleMember() { return responsibleMember; }
    public void setResponsibleMember(Member responsibleMember) { this.responsibleMember = responsibleMember; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getResponsibleMemberId() { return responsibleMemberId; }
    public void setResponsibleMemberId(Long responsibleMemberId) { this.responsibleMemberId = responsibleMemberId; }
}
