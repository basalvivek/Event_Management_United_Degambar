package com.udjcs.finance;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sponsor_donations")
public class SponsorDonation extends BaseEntity {

    @NotBlank(message = "Sponsor organisation name is required")
    @Size(max = 200)
    @Column(name = "sponsor_org_name", nullable = false, length = 200)
    private String sponsorOrgName;

    @Size(max = 200)
    @Column(name = "sponsor_name", length = 200)
    private String sponsorName;

    @NotBlank(message = "Payment mode is required")
    @Size(max = 50)
    @Column(name = "payment_mode", nullable = false, length = 50)
    private String paymentMode;

    @NotNull(message = "Payment date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @NotNull(message = "Committed amount is required")
    @Column(name = "committed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal committedAmount;

    @NotNull(message = "Donated amount is required")
    @Column(name = "donated_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal donatedAmount;

    @Size(max = 1000)
    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "source", length = 50, nullable = false)
    private String source = "Sponsors";

    public BigDecimal getPendingAmount() {
        if (committedAmount == null || donatedAmount == null) return BigDecimal.ZERO;
        return committedAmount.subtract(donatedAmount);
    }

    public String getSponsorOrgName() { return sponsorOrgName; }
    public void setSponsorOrgName(String sponsorOrgName) { this.sponsorOrgName = sponsorOrgName; }

    public String getSponsorName() { return sponsorName; }
    public void setSponsorName(String sponsorName) { this.sponsorName = sponsorName; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getCommittedAmount() { return committedAmount; }
    public void setCommittedAmount(BigDecimal committedAmount) { this.committedAmount = committedAmount; }

    public BigDecimal getDonatedAmount() { return donatedAmount; }
    public void setDonatedAmount(BigDecimal donatedAmount) { this.donatedAmount = donatedAmount; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
