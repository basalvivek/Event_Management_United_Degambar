package com.udjcs.payable;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payable_transactions")
public class PayableTransaction extends BaseEntity {

    @NotBlank(message = "Payment type is required")
    @Column(name = "payment_type", nullable = false, length = 50)
    private String paymentType;

    @NotBlank(message = "Name / description is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 200)
    @Column(name = "organisation_name", length = 200)
    private String organisationName;

    @Size(max = 150)
    @Column(name = "payable_person", length = 150)
    private String payablePerson;

    @Size(max = 10)
    @Column(name = "sort_code", length = 10)
    private String sortCode;

    @Size(max = 20)
    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "initial_deposit", precision = 12, scale = 2)
    private BigDecimal initialDeposit;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String v) { this.paymentType = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getOrganisationName() { return organisationName; }
    public void setOrganisationName(String v) { this.organisationName = v; }
    public String getPayablePerson() { return payablePerson; }
    public void setPayablePerson(String v) { this.payablePerson = v; }
    public String getSortCode() { return sortCode; }
    public void setSortCode(String v) { this.sortCode = v; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String v) { this.accountNumber = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public BigDecimal getInitialDeposit() { return initialDeposit; }
    public void setInitialDeposit(BigDecimal v) { this.initialDeposit = v; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate v) { this.paymentDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long v) { this.sourceId = v; }
}
