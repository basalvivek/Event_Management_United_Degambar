package com.udjcs.invitation;

import com.udjcs.common.BaseEntity;
import com.udjcs.organization.Organization;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invitations")
public class Invitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @NotBlank(message = "Event title is required")
    @Size(max = 200)
    @Column(name = "event_title", nullable = false, length = 200)
    private String eventTitle;

    @Size(max = 100)
    @Column(name = "event_tag", length = 100)
    private String eventTag;

    @Column(name = "banner_image", columnDefinition = "BYTEA")
    private byte[] bannerImage;

    @Column(name = "banner_mime_type", length = 50)
    private String bannerMimeType;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Size(max = 20)
    @Column(name = "postcode", length = 20)
    private String postcode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "about_event", columnDefinition = "TEXT")
    private String aboutEvent;

    @Column(name = "ticket_price_adult", precision = 10, scale = 2)
    private BigDecimal ticketPriceAdult;

    @Column(name = "ticket_price_youth", precision = 10, scale = 2)
    private BigDecimal ticketPriceYouth;

    @Column(name = "ticket_price_under8", precision = 10, scale = 2)
    private BigDecimal ticketPriceUnder8;

    @Column(name = "slug", unique = true, nullable = false, length = 250)
    private String slug;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<InvitationActivity> activities = new ArrayList<>();

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getEventTag() { return eventTag; }
    public void setEventTag(String eventTag) { this.eventTag = eventTag; }

    public byte[] getBannerImage() { return bannerImage; }
    public void setBannerImage(byte[] bannerImage) { this.bannerImage = bannerImage; }

    public String getBannerMimeType() { return bannerMimeType; }
    public void setBannerMimeType(String bannerMimeType) { this.bannerMimeType = bannerMimeType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getAboutEvent() { return aboutEvent; }
    public void setAboutEvent(String aboutEvent) { this.aboutEvent = aboutEvent; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getTicketPriceAdult() { return ticketPriceAdult; }
    public void setTicketPriceAdult(BigDecimal ticketPriceAdult) { this.ticketPriceAdult = ticketPriceAdult; }

    public BigDecimal getTicketPriceYouth() { return ticketPriceYouth; }
    public void setTicketPriceYouth(BigDecimal ticketPriceYouth) { this.ticketPriceYouth = ticketPriceYouth; }

    public BigDecimal getTicketPriceUnder8() { return ticketPriceUnder8; }
    public void setTicketPriceUnder8(BigDecimal ticketPriceUnder8) { this.ticketPriceUnder8 = ticketPriceUnder8; }

    public List<InvitationActivity> getActivities() { return activities; }
    public void setActivities(List<InvitationActivity> activities) { this.activities = activities; }
}
