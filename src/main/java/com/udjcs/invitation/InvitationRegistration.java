package com.udjcs.invitation;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invitation_registrations")
public class InvitationRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "age")
    private Integer age;

    @Column(name = "sex", length = 20)
    private String sex;

    @Column(name = "phone", length = 20)
    private String phone;

    @Email(message = "Enter a valid email address")
    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "attendees_adult")
    private Integer attendeesAdult = 0;

    @Column(name = "attendees_youth")
    private Integer attendeesYouth = 0;

    @Column(name = "attendees_under8")
    private Integer attendeesUnder8 = 0;

    @Column(name = "donation_amount", precision = 10, scale = 2)
    private BigDecimal donationAmount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    public Invitation getInvitation() { return invitation; }
    public void setInvitation(Invitation invitation) { this.invitation = invitation; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAttendeesAdult() { return attendeesAdult; }
    public void setAttendeesAdult(Integer attendeesAdult) { this.attendeesAdult = attendeesAdult; }

    public Integer getAttendeesYouth() { return attendeesYouth; }
    public void setAttendeesYouth(Integer attendeesYouth) { this.attendeesYouth = attendeesYouth; }

    public Integer getAttendeesUnder8() { return attendeesUnder8; }
    public void setAttendeesUnder8(Integer attendeesUnder8) { this.attendeesUnder8 = attendeesUnder8; }

    public BigDecimal getDonationAmount() { return donationAmount; }
    public void setDonationAmount(BigDecimal donationAmount) { this.donationAmount = donationAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
}
