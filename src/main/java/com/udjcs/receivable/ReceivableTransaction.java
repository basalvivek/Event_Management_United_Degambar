package com.udjcs.receivable;

import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "receivable_transactions")
public class ReceivableTransaction extends BaseEntity {

    @NotBlank(message = "Income type is required")
    @Column(name = "income_type", nullable = false, length = 50)
    private String incomeType;

    @NotBlank(message = "Name / description is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 200)
    @Column(name = "organisation_name", length = 200)
    private String organisationName;

    @Size(max = 150)
    @Column(name = "received_from", length = 150)
    private String receivedFrom;

    @Size(max = 10)
    @Column(name = "sort_code", length = 10)
    private String sortCode;

    @Size(max = 20)
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "received_amount", precision = 12, scale = 2)
    private BigDecimal receivedAmount;

    @NotNull(message = "Receipt date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Transient
    private Long eventId;

    public String getIncomeType() { return incomeType; }
    public void setIncomeType(String v) { this.incomeType = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getOrganisationName() { return organisationName; }
    public void setOrganisationName(String v) { this.organisationName = v; }
    public String getReceivedFrom() { return receivedFrom; }
    public void setReceivedFrom(String v) { this.receivedFrom = v; }
    public String getSortCode() { return sortCode; }
    public void setSortCode(String v) { this.sortCode = v; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { this.accountNumber = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal v) { this.receivedAmount = v; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate v) { this.receiptDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long v) { this.sourceId = v; }
    public Event getEvent() { return event; }
    public void setEvent(Event v) { this.event = v; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long v) { this.eventId = v; }
}
