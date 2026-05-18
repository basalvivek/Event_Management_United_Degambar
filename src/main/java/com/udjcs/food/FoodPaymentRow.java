package com.udjcs.food;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FoodPaymentRow {

    private Long registrationId;
    private String eventName;
    private String vendorName;
    private String postcode;
    private Integer headCount;
    private BigDecimal finalPricePerPlate;
    private BigDecimal fullAmount;
    private BigDecimal overallRating;
    private String foodSelectionStatus;
    private String paymentStatus;
    private LocalDate paymentDate;
    private String paymentMode;
    private BigDecimal amountPaid;
    private BigDecimal remaining;
    private String notes;

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long v) { registrationId = v; }
    public String getEventName() { return eventName; }
    public void setEventName(String v) { eventName = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { vendorName = v; }
    public String getPostcode() { return postcode; }
    public void setPostcode(String v) { postcode = v; }
    public Integer getHeadCount() { return headCount; }
    public void setHeadCount(Integer v) { headCount = v; }
    public BigDecimal getFinalPricePerPlate() { return finalPricePerPlate; }
    public void setFinalPricePerPlate(BigDecimal v) { finalPricePerPlate = v; }
    public BigDecimal getFullAmount() { return fullAmount; }
    public void setFullAmount(BigDecimal v) { fullAmount = v; }
    public BigDecimal getOverallRating() { return overallRating; }
    public void setOverallRating(BigDecimal v) { overallRating = v; }
    public String getFoodSelectionStatus() { return foodSelectionStatus; }
    public void setFoodSelectionStatus(String v) { foodSelectionStatus = v; }
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
