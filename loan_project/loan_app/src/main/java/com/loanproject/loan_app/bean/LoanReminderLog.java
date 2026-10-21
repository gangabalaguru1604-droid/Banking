package com.loanproject.loan_app.bean;

import java.time.Instant;

/**
 * Persists loan repayment reminder attempt records, channel dispatch status,
 * provider delivery callbacks, and contact-detail update flags.
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "loan_reminder_logs")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "uk_reminder_inst_dpd_channel_cycle", def = "{'installmentId': 1, 'dpdDay': 1, 'channel': 1, 'reminderCycle': 1}", unique = true)

public class LoanReminderLog {

    @org.springframework.data.annotation.Id

    private Long id;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private Loan loan;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private RepaymentInstallment installment;

    private Long installmentId;

    private int dpdDay;

    private String reminderCycle;

    private String channel; // EMAIL, SMS

    private String status; // CLAIMED, SENT, DELIVERED, BOUNCED, OPENED, FAILED, CANCELLED

    private String recipientReference;

    private Instant sentTimestamp;
    private Instant deliveredTimestamp;
    private Instant openedTimestamp;
    private Instant bouncedTimestamp;

    private String providerMessageId;

    private String failureDetails;

    private boolean contactDetailUpdateRequired = false;

    private int retryCount = 0;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public LoanReminderLog() {
    }

    public LoanReminderLog(Loan loan, RepaymentInstallment installment, int dpdDay, String reminderCycle,
                           String channel, String status, String recipientReference) {
        this.loan = loan;
        this.setInstallment(installment);
        this.dpdDay = dpdDay;
        this.reminderCycle = reminderCycle;
        this.channel = channel;
        this.status = status;
        this.recipientReference = recipientReference;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

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
        this.installmentId = installment != null ? installment.getId() : null;
    }

    public Long getInstallmentId() { return installmentId; }

    public void setInstallmentId(Long installmentId) { this.installmentId = installmentId; }

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
