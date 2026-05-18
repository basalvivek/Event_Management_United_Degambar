package com.udjcs.hall;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HallPaymentRow {

    private Long bookingId;
    private String eventName;
    private String hallName;
    private String bookedBy;
    private String phone;
    private LocalDate hireDate;
    private String fromTime;
    private String tillTime;
    private BigDecimal hallCharges;
    private String bookingStatus;
    private String paymentStatus;
    private LocalDate paymentDate;
    private String paymentMode;
    private BigDecimal amountPaid;
    private BigDecimal remaining;
    private String notes;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long v) { bookingId = v; }
    public String getEventName() { return eventName; }
    public void setEventName(String v) { eventName = v; }
    public String getHallName() { return hallName; }
    public void setHallName(String v) { hallName = v; }
    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String v) { bookedBy = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate v) { hireDate = v; }
    public String getFromTime() { return fromTime; }
    public void setFromTime(String v) { fromTime = v; }
    public String getTillTime() { return tillTime; }
    public void setTillTime(String v) { tillTime = v; }
    public BigDecimal getHallCharges() { return hallCharges; }
    public void setHallCharges(BigDecimal v) { hallCharges = v; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String v) { bookingStatus = v; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { paymentStatus = v; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate v) { paymentDate = v; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String v) { paymentMode = v; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal v) { amountPaid = v; }
    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal v) { remaining = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
