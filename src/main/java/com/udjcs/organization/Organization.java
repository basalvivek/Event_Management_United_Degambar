package com.udjcs.organization;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @NotBlank(message = "Organization name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 50)
    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Size(max = 100)
    @Column(name = "trust_type", length = 100)
    private String trustType;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Size(max = 100)
    @Column(length = 100)
    private String state;

    @Size(max = 10)
    @Column(length = 10)
    private String pincode;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 100)
    @Column(length = 100)
    private String email;

    @Size(max = 150)
    @Column(length = 150)
    private String website;

    @Min(value = 1800, message = "Enter a valid year")
    @Max(value = 2100, message = "Enter a valid year")
    @Column(name = "established_year")
    private Integer establishedYear;

    @Size(max = 1000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_path", length = 255)
    private String logoPath;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getTrustType() { return trustType; }
    public void setTrustType(String trustType) { this.trustType = trustType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public Integer getEstablishedYear() { return establishedYear; }
    public void setEstablishedYear(Integer establishedYear) { this.establishedYear = establishedYear; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
}
