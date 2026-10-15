package com.loanproject.loan_app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data transfer object encapsulating all reminder message attributes.
 */
public class ReminderMessageRequest {

    private Long loanId;
    private Long installmentId;
    private int installmentNumber;
    private String customerName;
    private String recipientEmail;
    private String recipientPhone;
    private String maskedAccountNumber;
    private BigDecimal overdueAmount;
    private LocalDate dueDate;
    private int dpdDay;
    private BigDecimal penaltyAmount;
    private String paymentUrl;
    private String helpdeskNumber;
    private String reminderCycle;

    public ReminderMessageRequest() {
    }

    public ReminderMessageRequest(Long loanId, Long installmentId, int installmentNumber, String customerName,
                                  String recipientEmail, String recipientPhone, String maskedAccountNumber,
                                  BigDecimal overdueAmount, LocalDate dueDate, int dpdDay,
                                  BigDecimal penaltyAmount, String paymentUrl, String helpdeskNumber,
                                  String reminderCycle) {
        this.loanId = loanId;
        this.installmentId = installmentId;
        this.installmentNumber = installmentNumber;
        this.customerName = customerName;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.maskedAccountNumber = maskedAccountNumber;
        this.overdueAmount = overdueAmount;
        this.dueDate = dueDate;
        this.dpdDay = dpdDay;
        this.penaltyAmount = penaltyAmount;
        this.paymentUrl = paymentUrl;
        this.helpdeskNumber = helpdeskNumber;
        this.reminderCycle = reminderCycle;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(Long installmentId) {
        this.installmentId = installmentId;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(int installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
    }

    public BigDecimal getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(BigDecimal overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getDpdDay() {
        return dpdDay;
    }

    public void setDpdDay(int dpdDay) {
        this.dpdDay = dpdDay;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount != null ? penaltyAmount : BigDecimal.ZERO;
    }

    public void setPenaltyAmount(BigDecimal penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getHelpdeskNumber() {
        return helpdeskNumber;
    }

    public void setHelpdeskNumber(String helpdeskNumber) {
        this.helpdeskNumber = helpdeskNumber;
    }

    public String getReminderCycle() {
        return reminderCycle;
    }

    public void setReminderCycle(String reminderCycle) {
        this.reminderCycle = reminderCycle;
    }
}
