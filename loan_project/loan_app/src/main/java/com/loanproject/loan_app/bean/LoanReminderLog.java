package com.loanproject.loan_app.bean;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Persists loan repayment reminder attempt records, channel dispatch status,
 * provider delivery callbacks, and contact-detail update flags.
 */
@Entity
@Table(name = "loan_reminder_logs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_reminder_inst_dpd_channel_cycle", columnNames = {"installment_id", "dpd_day", "channel", "reminder_cycle"})
})
public class LoanReminderLog {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Loan loan;

    @ManyToOne(optional = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private RepaymentInstallment installment;

    @Column(name = "dpd_day", nullable = false)
    private int dpdDay;

    @Column(name = "reminder_cycle", length = 50, nullable = false)
    private String reminderCycle;

    @Column(length = 20, nullable = false)
    private String channel; // EMAIL, SMS

    @Column(length = 30, nullable = false)
    private String status; // CLAIMED, SENT, DELIVERED, BOUNCED, OPENED, FAILED, CANCELLED

    @Column(length = 255)
    private String recipientReference;

    private Instant sentTimestamp;
    private Instant deliveredTimestamp;
    private Instant openedTimestamp;
    private Instant bouncedTimestamp;

    @Column(length = 100)
    private String providerMessageId;

    @Column(length = 1000)
    private String failureDetails;

    @Column(nullable = false)
    private boolean contactDetailUpdateRequired = false;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public LoanReminderLog() {
    }

    public LoanReminderLog(Loan loan, RepaymentInstallment installment, int dpdDay, String reminderCycle,
                           String channel, String status, String recipientReference) {
        this.loan = loan;
        this.installment = installment;
        this.dpdDay = dpdDay;
        this.reminderCycle = reminderCycle;
        this.channel = channel;
        this.status = status;
        this.recipientReference = recipientReference;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public RepaymentInstallment getInstallment() {
        return installment;
    }

    public void setInstallment(RepaymentInstallment installment) {
        this.installment = installment;
    }

    public int getDpdDay() {
        return dpdDay;
    }

    public void setDpdDay(int dpdDay) {
        this.dpdDay = dpdDay;
    }

    public String getReminderCycle() {
        return reminderCycle;
    }

    public void setReminderCycle(String reminderCycle) {
        this.reminderCycle = reminderCycle;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecipientReference() {
        return recipientReference;
    }

    public void setRecipientReference(String recipientReference) {
        this.recipientReference = recipientReference;
    }

    public Instant getSentTimestamp() {
        return sentTimestamp;
    }

    public void setSentTimestamp(Instant sentTimestamp) {
        this.sentTimestamp = sentTimestamp;
    }

    public Instant getDeliveredTimestamp() {
        return deliveredTimestamp;
    }

    public void setDeliveredTimestamp(Instant deliveredTimestamp) {
        this.deliveredTimestamp = deliveredTimestamp;
    }

    public Instant getOpenedTimestamp() {
        return openedTimestamp;
    }

    public void setOpenedTimestamp(Instant openedTimestamp) {
        this.openedTimestamp = openedTimestamp;
    }

    public Instant getBouncedTimestamp() {
        return bouncedTimestamp;
    }

    public void setBouncedTimestamp(Instant bouncedTimestamp) {
        this.bouncedTimestamp = bouncedTimestamp;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getFailureDetails() {
        return failureDetails;
    }

    public void setFailureDetails(String failureDetails) {
        this.failureDetails = failureDetails;
    }

    public boolean isContactDetailUpdateRequired() {
        return contactDetailUpdateRequired;
    }

    public void setContactDetailUpdateRequired(boolean contactDetailUpdateRequired) {
        this.contactDetailUpdateRequired = contactDetailUpdateRequired;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
