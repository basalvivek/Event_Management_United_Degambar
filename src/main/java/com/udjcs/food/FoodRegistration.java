package com.udjcs.food;

import com.udjcs.common.BaseEntity;
import com.udjcs.event.Event;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "food_registrations")
public class FoodRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Transient
    private Long eventId;

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    // Vendor
    @NotBlank(message = "Vendor name is required")
    @Size(max = 150)
    @Column(name = "vendor_name", nullable = false, length = 150)
    private String vendorName;

    @Size(max = 300)
    @Column(name = "vendor_address", length = 300)
    private String vendorAddress;

    @Size(max = 10)
    @Column(length = 10)
    private String postcode;

    @Column(name = "price_per_plate", precision = 10, scale = 2)
    private BigDecimal pricePerPlate;

    @Column(name = "total_expected_head_count")
    private Integer totalExpectedHeadCount;

    @Column(name = "negotiated_price_per_plate", precision = 10, scale = 2)
    private BigDecimal negotiatedPricePerPlate;

    // Food items
    @OneToMany(mappedBy = "foodRegistration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodItem> foodItems = new ArrayList<>();

    // Tasting panel
    @Size(max = 100) @Column(name = "member1_name", length = 100) private String member1Name;
    @Size(max = 100) @Column(name = "member2_name", length = 100) private String member2Name;
    @Size(max = 100) @Column(name = "member3_name", length = 100) private String member3Name;
    @Size(max = 100) @Column(name = "member4_name", length = 100) private String member4Name;

    @Column(name = "member1_rating") private Integer member1Rating;
    @Column(name = "member2_rating") private Integer member2Rating;
    @Column(name = "member3_rating") private Integer member3Rating;
    @Column(name = "member4_rating") private Integer member4Rating;

    @Column(name = "overall_food_rating", precision = 3, scale = 1)
    private BigDecimal overallFoodRating;

    // Financial
    @Column(name = "final_price_per_plate", precision = 10, scale = 2)
    private BigDecimal finalPricePerPlate;

    @Column(name = "full_amount", precision = 12, scale = 2)
    private BigDecimal fullAmount;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "deposit_date")
    private LocalDate depositDate;

    // Vendor payment
    @Size(max = 150) @Column(name = "vendor_payable_name", length = 150) private String vendorPayableName;
    @Size(max = 10)  @Column(name = "sort_code", length = 10)            private String sortCode;
    @Size(max = 20)  @Column(name = "account_number", length = 20)       private String accountNumber;

    // Statuses
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "InProgress";

    @Column(name = "food_selection_status", nullable = false, length = 20)
    private String foodSelectionStatus = "OPEN";

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    // Getters / Setters
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public String getVendorAddress() { return vendorAddress; }
    public void setVendorAddress(String v) { this.vendorAddress = v; }
    public String getPostcode() { return postcode; }
    public void setPostcode(String v) { this.postcode = v; }
    public BigDecimal getPricePerPlate() { return pricePerPlate; }
    public void setPricePerPlate(BigDecimal v) { this.pricePerPlate = v; }
    public Integer getTotalExpectedHeadCount() { return totalExpectedHeadCount; }
    public void setTotalExpectedHeadCount(Integer v) { this.totalExpectedHeadCount = v; }
    public BigDecimal getNegotiatedPricePerPlate() { return negotiatedPricePerPlate; }
    public void setNegotiatedPricePerPlate(BigDecimal v) { this.negotiatedPricePerPlate = v; }
    public List<FoodItem> getFoodItems() { return foodItems; }
    public void setFoodItems(List<FoodItem> v) { this.foodItems = v; }
    public String getMember1Name() { return member1Name; }
    public void setMember1Name(String v) { this.member1Name = v; }
    public String getMember2Name() { return member2Name; }
    public void setMember2Name(String v) { this.member2Name = v; }
    public String getMember3Name() { return member3Name; }
    public void setMember3Name(String v) { this.member3Name = v; }
    public String getMember4Name() { return member4Name; }
    public void setMember4Name(String v) { this.member4Name = v; }
    public Integer getMember1Rating() { return member1Rating; }
    public void setMember1Rating(Integer v) { this.member1Rating = v; }
    public Integer getMember2Rating() { return member2Rating; }
    public void setMember2Rating(Integer v) { this.member2Rating = v; }
    public Integer getMember3Rating() { return member3Rating; }
    public void setMember3Rating(Integer v) { this.member3Rating = v; }
    public Integer getMember4Rating() { return member4Rating; }
    public void setMember4Rating(Integer v) { this.member4Rating = v; }
    public BigDecimal getOverallFoodRating() { return overallFoodRating; }
    public void setOverallFoodRating(BigDecimal v) { this.overallFoodRating = v; }
    public BigDecimal getFinalPricePerPlate() { return finalPricePerPlate; }
    public void setFinalPricePerPlate(BigDecimal v) { this.finalPricePerPlate = v; }
    public BigDecimal getFullAmount() { return fullAmount; }
    public void setFullAmount(BigDecimal v) { this.fullAmount = v; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal v) { this.depositAmount = v; }
    public LocalDate getDepositDate() { return depositDate; }
    public void setDepositDate(LocalDate v) { this.depositDate = v; }
    public String getVendorPayableName() { return vendorPayableName; }
    public void setVendorPayableName(String v) { this.vendorPayableName = v; }
    public String getSortCode() { return sortCode; }
    public void setSortCode(String v) { this.sortCode = v; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { this.accountNumber = v; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { this.paymentStatus = v; }
    public String getFoodSelectionStatus() { return foodSelectionStatus; }
    public void setFoodSelectionStatus(String v) { this.foodSelectionStatus = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
