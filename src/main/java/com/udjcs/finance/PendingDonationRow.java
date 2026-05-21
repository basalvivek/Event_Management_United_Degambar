package com.udjcs.finance;

import java.math.BigDecimal;

public class PendingDonationRow {

    private final String sourceType;
    private final Long sourceId;
    private final String eventName;
    private final String donorName;
    private final String contactPerson;
    private final BigDecimal committedAmount;
    private final BigDecimal paidAmount;
    private final BigDecimal pendingAmount;
    private final long reminderCount;

    public PendingDonationRow(String sourceType, Long sourceId, String eventName,
                               String donorName, String contactPerson,
                               BigDecimal committedAmount, BigDecimal paidAmount,
                               long reminderCount) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.eventName = eventName != null ? eventName : "—";
        this.donorName = donorName;
        this.contactPerson = contactPerson;
        BigDecimal comm = committedAmount != null ? committedAmount : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.committedAmount = comm;
        this.paidAmount = paid;
        this.pendingAmount = comm.subtract(paid).max(BigDecimal.ZERO);
        this.reminderCount = reminderCount;
    }

    public String getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public String getEventName() { return eventName; }
    public String getDonorName() { return donorName; }
    public String getContactPerson() { return contactPerson; }
    public BigDecimal getCommittedAmount() { return committedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public long getReminderCount() { return reminderCount; }
}
