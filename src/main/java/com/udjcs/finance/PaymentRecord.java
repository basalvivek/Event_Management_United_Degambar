package com.udjcs.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentRecord {

    private Long sourceId;
    private String source;
    private String sourceIcon;
    private String sourceBadgeColor;
    private String orgName;
    private String personName;
    private String eventName;
    private LocalDate eventDate;
    private String paymentMode;
    private LocalDate paymentDate;
    private BigDecimal committedAmount;
    private BigDecimal donatedAmount;
    private BigDecimal pendingAmount;
    private String status;
    private String remark;
    private String editUrl;

    public static PaymentRecord fromSponsor(SponsorDonation s) {
        PaymentRecord r = new PaymentRecord();
        r.sourceId         = s.getId();
        r.source           = "Sponsors";
        r.sourceIcon       = "bi-building-fill";
        r.sourceBadgeColor = "#7c3aed";
        r.orgName          = s.getSponsorOrgName();
        r.personName       = s.getSponsorName();
        r.eventName        = s.getEvent() != null ? s.getEvent().getEventName() : null;
        r.paymentMode      = s.getPaymentMode();
        r.paymentDate      = s.getPaymentDate();
        r.committedAmount  = s.getCommittedAmount();
        r.donatedAmount    = s.getDonatedAmount();
        r.pendingAmount    = s.getPendingAmount();
        r.status           = r.pendingAmount != null && r.pendingAmount.compareTo(BigDecimal.ZERO) > 0 ? "Partial" : "Received";
        r.remark           = s.getRemark();
        r.editUrl          = "/finance/sponsors/" + s.getId() + "/edit";
        return r;
    }

    public static PaymentRecord fromOrgDonation(com.udjcs.payment.Payment p) {
        PaymentRecord r = new PaymentRecord();
        r.sourceId         = p.getId();
        boolean isMember   = p.getMember() != null;
        r.source           = isMember ? "Member" : "Organisation";
        r.sourceIcon       = isMember ? "bi-person-fill" : "bi-building-fill";
        r.sourceBadgeColor = isMember ? "#0ea5e9" : "#6366f1";
        r.orgName          = p.getSupportiveOrganization() != null ? p.getSupportiveOrganization().getName() : "—";
        r.personName       = isMember
                ? p.getMember().getFirstName() + " " + p.getMember().getLastName()
                : "By Organisation";
        r.eventName        = p.getEvent() != null ? p.getEvent().getEventName() : null;
        r.paymentMode      = p.getPaymentMode();
        r.paymentDate      = p.getPaymentDate();
        BigDecimal donated = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
        BigDecimal committed = p.getCommittedAmount() != null ? p.getCommittedAmount() : donated;
        r.committedAmount  = committed;
        r.donatedAmount    = donated;
        r.pendingAmount    = committed.subtract(donated);
        r.status           = p.getStatus();
        r.remark           = p.getNotes();
        r.editUrl          = "/finance/org-donations/" + p.getId() + "/edit";
        return r;
    }

    public static PaymentRecord fromTicket(com.udjcs.ticket.EventTicket t) {
        PaymentRecord r = new PaymentRecord();
        r.sourceId         = t.getId();
        r.source           = "Members";
        r.sourceIcon       = "bi-ticket-perforated-fill";
        r.sourceBadgeColor = "#10b981";
        r.orgName          = t.getEvent() != null ? t.getEvent().getEventName() : "—";
        r.personName       = t.getMember() != null
                ? t.getMember().getFirstName() + " " + t.getMember().getLastName() : "—";
        r.eventName        = t.getEvent() != null ? t.getEvent().getEventName() : "—";
        r.eventDate        = t.getEvent() != null ? t.getEvent().getEventDate() : null;
        r.paymentMode      = t.getPaymentMode();
        r.paymentDate      = t.getPaymentDate();
        BigDecimal total   = t.getTotalAmount()    != null ? BigDecimal.valueOf(t.getTotalAmount())    : BigDecimal.ZERO;
        BigDecimal committed = t.getCommittedAmount() != null ? BigDecimal.valueOf(t.getCommittedAmount()) : total;
        BigDecimal received  = t.getReceivedAmount()  != null ? BigDecimal.valueOf(t.getReceivedAmount())
                             : ("Accepted".equals(t.getStatus()) ? total : BigDecimal.ZERO);
        r.committedAmount  = committed;
        r.donatedAmount    = received;
        r.pendingAmount    = committed.subtract(received);
        r.status           = t.getStatus();
        r.editUrl          = "/finance/tickets/" + t.getId() + "/edit";
        return r;
    }

    public static PaymentRecord fromSponsorInstalment(SponsorDonation s, PaymentInstalment inst, BigDecimal pendingAfterThis) {
        PaymentRecord r = new PaymentRecord();
        r.sourceId         = s.getId();
        r.source           = "Sponsors";
        r.sourceIcon       = "bi-building-fill";
        r.sourceBadgeColor = "#7c3aed";
        r.orgName          = s.getSponsorOrgName();
        r.personName       = s.getSponsorName();
        r.eventName        = s.getEvent() != null ? s.getEvent().getEventName() : null;
        r.paymentMode      = inst.getPaymentMode();
        r.paymentDate      = inst.getPaymentDate();
        r.committedAmount  = s.getCommittedAmount();
        r.donatedAmount    = inst.getAmount();
        r.pendingAmount    = pendingAfterThis.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pendingAfterThis;
        r.status           = "Received";
        r.remark           = inst.getNotes();
        r.editUrl          = "/finance/sponsors/" + s.getId() + "/edit";
        return r;
    }

    public static PaymentRecord fromOrgDonationInstalment(com.udjcs.payment.Payment p, PaymentInstalment inst, BigDecimal pendingAfterThis) {
        PaymentRecord r = new PaymentRecord();
        r.sourceId         = p.getId();
        boolean isMember   = p.getMember() != null;
        r.source           = isMember ? "Member" : "Organisation";
        r.sourceIcon       = isMember ? "bi-person-fill" : "bi-building-fill";
        r.sourceBadgeColor = isMember ? "#0ea5e9" : "#6366f1";
        r.orgName          = p.getSupportiveOrganization() != null ? p.getSupportiveOrganization().getName() : "—";
        r.personName       = isMember
                ? p.getMember().getFirstName() + " " + p.getMember().getLastName()
                : "By Organisation";
        r.eventName        = p.getEvent() != null ? p.getEvent().getEventName() : null;
        r.paymentMode      = inst.getPaymentMode();
        r.paymentDate      = inst.getPaymentDate();
        r.committedAmount  = p.getCommittedAmount() != null ? p.getCommittedAmount() : BigDecimal.ZERO;
        r.donatedAmount    = inst.getAmount();
        r.pendingAmount    = pendingAfterThis.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pendingAfterThis;
        r.status           = "Received";
        r.remark           = inst.getNotes();
        r.editUrl          = "/finance/org-donations/" + p.getId() + "/edit";
        return r;
    }

    public static PaymentRecord fromTicketInstalment(com.udjcs.ticket.EventTicket t, PaymentInstalment inst, BigDecimal pendingAfterThis) {
        PaymentRecord r = new PaymentRecord();
        r.sourceId         = t.getId();
        r.source           = "Members";
        r.sourceIcon       = "bi-ticket-perforated-fill";
        r.sourceBadgeColor = "#10b981";
        r.orgName          = t.getEvent() != null ? t.getEvent().getEventName() : "—";
        r.personName       = t.getMember() != null
                ? t.getMember().getFirstName() + " " + t.getMember().getLastName() : "—";
        r.eventName        = t.getEvent() != null ? t.getEvent().getEventName() : null;
        r.eventDate        = t.getEvent() != null ? t.getEvent().getEventDate() : null;
        r.paymentMode      = inst.getPaymentMode();
        r.paymentDate      = inst.getPaymentDate();
        BigDecimal total     = t.getTotalAmount()    != null ? BigDecimal.valueOf(t.getTotalAmount())    : BigDecimal.ZERO;
        BigDecimal committed = t.getCommittedAmount() != null ? BigDecimal.valueOf(t.getCommittedAmount()) : total;
        r.committedAmount  = committed;
        r.donatedAmount    = inst.getAmount();
        r.pendingAmount    = pendingAfterThis.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pendingAfterThis;
        r.status           = "Received";
        r.remark           = inst.getNotes();
        r.editUrl          = "/finance/tickets/" + t.getId() + "/edit";
        return r;
    }

    public static PaymentRecord fromInvitationReg(com.udjcs.invitation.InvitationRegistration reg) {
        PaymentRecord r = new PaymentRecord();
        r.sourceId         = reg.getId();
        r.source           = "Invitation";
        r.sourceIcon       = "bi-envelope-paper-fill";
        r.sourceBadgeColor = "#f59e0b";
        r.orgName          = reg.getInvitation() != null ? reg.getInvitation().getEventTitle() : "—";
        r.personName       = reg.getFirstName() + " " + reg.getLastName();
        r.eventName        = reg.getInvitation() != null ? reg.getInvitation().getEventTitle() : "—";
        r.eventDate        = reg.getInvitation() != null ? reg.getInvitation().getEventDate() : null;
        r.paymentMode      = reg.getPaymentMode();
        r.paymentDate      = reg.getPaymentDate();
        BigDecimal total   = reg.getTotalAmount() != null ? reg.getTotalAmount() : BigDecimal.ZERO;
        r.committedAmount  = total;
        r.donatedAmount    = total;
        r.pendingAmount    = BigDecimal.ZERO;
        r.status           = "Registered";
        r.editUrl          = "/finance/inv-registrations/" + reg.getId() + "/edit";
        return r;
    }

    public Long getSourceId() { return sourceId; }
    public String getSource() { return source; }
    public String getSourceIcon() { return sourceIcon; }
    public String getSourceBadgeColor() { return sourceBadgeColor; }
    public String getOrgName() { return orgName; }
    public String getPersonName() { return personName; }
    public String getEventName() { return eventName; }
    public LocalDate getEventDate() { return eventDate; }
    public String getPaymentMode() { return paymentMode; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public BigDecimal getCommittedAmount() { return committedAmount; }
    public BigDecimal getDonatedAmount() { return donatedAmount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public String getStatus() { return status; }
    public String getRemark() { return remark; }
    public String getEditUrl() { return editUrl; }
}
