package com.udjcs.venue;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Table(name = "venues")
public class Venue extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(name = "venue_name", nullable = false, length = 200)
    private String venueName;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String address;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String state;

    @Size(max = 20)
    @Column(name = "post_code", length = 20)
    private String postCode;

    @Column
    private Integer capacity;

    @NotBlank(message = "Primary contact name is required")
    @Size(max = 100)
    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @NotBlank(message = "Primary contact phone is required")
    @Size(max = 20)
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Size(max = 100)
    @Column(name = "secondary_contact_name", length = 100)
    private String secondaryContactName;

    @Size(max = 20)
    @Column(name = "secondary_contact_phone", length = 20)
    private String secondaryContactPhone;

    @NotBlank
    @Size(max = 50)
    @Column(name = "venue_type", nullable = false, length = 50)
    private String venueType;

    @Size(max = 1000)
    @Column(length = 1000)
    private String facilities;

    @Column(name = "booking_price", precision = 12, scale = 2)
    private BigDecimal bookingPrice;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String status;

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getSecondaryContactName() { return secondaryContactName; }
    public void setSecondaryContactName(String secondaryContactName) { this.secondaryContactName = secondaryContactName; }

    public String getSecondaryContactPhone() { return secondaryContactPhone; }
    public void setSecondaryContactPhone(String secondaryContactPhone) { this.secondaryContactPhone = secondaryContactPhone; }

    public String getVenueType() { return venueType; }
    public void setVenueType(String venueType) { this.venueType = venueType; }

    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }

    public BigDecimal getBookingPrice() { return bookingPrice; }
    public void setBookingPrice(BigDecimal bookingPrice) { this.bookingPrice = bookingPrice; }

    public BigDecimal getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
