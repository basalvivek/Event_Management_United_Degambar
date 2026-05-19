package com.udjcs.payment;

import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import com.udjcs.member.Member;
import com.udjcs.supportive.SupportiveOrganization;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supportive_organization_id")
    private SupportiveOrganization supportiveOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "committed_amount", precision = 12, scale = 2)
    private BigDecimal committedAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column
    private LocalDate paymentDate;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String paymentMode;

    @Size(max = 100)
    @Column(length = 100)
    private String referenceNumber;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Transient
    private Long organizationId;

    @Transient
    private Long memberId;

    @Transient
    private Long eventId;

    public SupportiveOrganization getSupportiveOrganization() { return supportiveOrganization; }
    public void setSupportiveOrganization(SupportiveOrganization supportiveOrganization) { this.supportiveOrganization = supportiveOrganization; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public BigDecimal getCommittedAmount() { return committedAmount; }
    public void setCommittedAmount(BigDecimal committedAmount) { this.committedAmount = committedAmount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}
