package com.udjcs.event;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String eventName;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate eventDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String eventType;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String venue;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String organizerName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "contact_member1_name", nullable = false, length = 100)
    private String contactMember1Name;

    @NotBlank
    @Size(max = 20)
    @Column(name = "contact_member1_phone", nullable = false, length = 20)
    private String contactMember1Phone;

    @Size(max = 100)
    @Column(name = "contact_member2_name", length = 100)
    private String contactMember2Name;

    @Size(max = 20)
    @Column(name = "contact_member2_phone", length = 20)
    private String contactMember2Phone;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String status;

    @Min(0)
    @Column(name = "ticket_adult")
    private Integer ticketAdult;

    @Min(0)
    @Column(name = "ticket_younger")
    private Integer ticketYounger;

    @Min(0)
    @Column(name = "ticket_child")
    private Integer ticketChild;

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getContactMember1Name() { return contactMember1Name; }
    public void setContactMember1Name(String contactMember1Name) { this.contactMember1Name = contactMember1Name; }

    public String getContactMember1Phone() { return contactMember1Phone; }
    public void setContactMember1Phone(String contactMember1Phone) { this.contactMember1Phone = contactMember1Phone; }

    public String getContactMember2Name() { return contactMember2Name; }
    public void setContactMember2Name(String contactMember2Name) { this.contactMember2Name = contactMember2Name; }

    public String getContactMember2Phone() { return contactMember2Phone; }
    public void setContactMember2Phone(String contactMember2Phone) { this.contactMember2Phone = contactMember2Phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTicketAdult() { return ticketAdult; }
    public void setTicketAdult(Integer ticketAdult) { this.ticketAdult = ticketAdult; }

    public Integer getTicketYounger() { return ticketYounger; }
    public void setTicketYounger(Integer ticketYounger) { this.ticketYounger = ticketYounger; }

    public Integer getTicketChild() { return ticketChild; }
    public void setTicketChild(Integer ticketChild) { this.ticketChild = ticketChild; }
}
