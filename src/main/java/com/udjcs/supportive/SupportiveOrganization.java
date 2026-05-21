package com.udjcs.supportive;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "supportive_organizations")
public class SupportiveOrganization extends BaseEntity {

    @NotBlank(message = "Organization name is required")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 100)
    @Column(name = "organization_type", length = 100)
    private String organizationType;

    @Size(max = 100)
    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Size(max = 20)
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Email(message = "Enter a valid email address")
    @Size(max = 100)
    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Size(max = 100)
    @Column(length = 100)
    private String state;

    @Size(max = 150)
    @Column(length = 150)
    private String website;

    @Size(max = 1000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_path", length = 255)
    private String logoPath;

    @Size(max = 50)
    @Column(name = "sponsorship_type", length = 50)
    private String sponsorshipType;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOrganizationType() { return organizationType; }
    public void setOrganizationType(String organizationType) { this.organizationType = organizationType; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getSponsorshipType() { return sponsorshipType; }
    public void setSponsorshipType(String sponsorshipType) { this.sponsorshipType = sponsorshipType; }
}
