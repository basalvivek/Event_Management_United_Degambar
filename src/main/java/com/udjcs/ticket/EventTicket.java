package com.udjcs.ticket;

import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import com.udjcs.member.Member;
import jakarta.persistence.*;

@Entity
@Table(name = "event_tickets")
public class EventTicket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "adult_count", nullable = false)
    private Integer adultCount = 0;

    @Column(name = "younger_count", nullable = false)
    private Integer youngerCount = 0;

    @Column(name = "child_count", nullable = false)
    private Integer childCount = 0;

    @Column(name = "adult_amount", nullable = false)
    private Integer adultAmount = 0;

    @Column(name = "younger_amount", nullable = false)
    private Integer youngerAmount = 0;

    @Column(name = "child_amount", nullable = false)
    private Integer childAmount = 0;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount = 0;

    @Column(nullable = false, length = 20)
    private String status = "Pending";

    @Transient
    private Long eventId;

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public Integer getAdultCount() { return adultCount; }
    public void setAdultCount(Integer adultCount) { this.adultCount = adultCount; }

    public Integer getYoungerCount() { return youngerCount; }
    public void setYoungerCount(Integer youngerCount) { this.youngerCount = youngerCount; }

    public Integer getChildCount() { return childCount; }
    public void setChildCount(Integer childCount) { this.childCount = childCount; }

    public Integer getAdultAmount() { return adultAmount; }
    public void setAdultAmount(Integer adultAmount) { this.adultAmount = adultAmount; }

    public Integer getYoungerAmount() { return youngerAmount; }
    public void setYoungerAmount(Integer youngerAmount) { this.youngerAmount = youngerAmount; }

    public Integer getChildAmount() { return childAmount; }
    public void setChildAmount(Integer childAmount) { this.childAmount = childAmount; }

    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}
