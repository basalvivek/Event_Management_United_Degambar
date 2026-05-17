package com.udjcs.hall;

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
@Table(name = "hall_registrations")
public class HallRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Transient
    private Long eventId;

    @Size(max = 200)
    @Column(name = "hall_name", length = 200)
    private String hallName;

    @NotBlank(message = "Booked by is required")
    @Size(max = 150)
    @Column(name = "booked_by", nullable = false, length = 150)
    private String bookedBy;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @Size(max = 100)
    @Column(length = 100)
    private String email;

    @Size(max = 10)
    @Column(name = "sort_code", length = 10)
    private String sortCode;

    @Size(max = 20)
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Size(max = 150)
    @Column(name = "payable_name", length = 150)
    private String payableName;

    @NotNull(message = "Hire date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Size(max = 5)
    @Column(name = "from_time", length = 5)
    private String fromTime;

    @Size(max = 5)
    @Column(name = "till_time", length = 5)
    private String tillTime;

    @Column(name = "hall_charges", precision = 12, scale = 2)
    private BigDecimal hallCharges;

    @Column(name = "initial_deposit", precision = 12, scale = 2)
    private BigDecimal initialDeposit;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status = "InProgress";

    @Column(name = "booking_status", nullable = false, length = 20)
    private String bookingStatus = "INITIAL";

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }
    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSortCode() { return sortCode; }
    public void setSortCode(String sortCode) { this.sortCode = sortCode; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getPayableName() { return payableName; }
    public void setPayableName(String payableName) { this.payableName = payableName; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getFromTime() { return fromTime; }
    public void setFromTime(String fromTime) { this.fromTime = fromTime; }
    public String getTillTime() { return tillTime; }
    public void setTillTime(String tillTime) { this.tillTime = tillTime; }
    public BigDecimal getHallCharges() { return hallCharges; }
    public void setHallCharges(BigDecimal hallCharges) { this.hallCharges = hallCharges; }
    public BigDecimal getInitialDeposit() { return initialDeposit; }
    public void setInitialDeposit(BigDecimal initialDeposit) { this.initialDeposit = initialDeposit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
