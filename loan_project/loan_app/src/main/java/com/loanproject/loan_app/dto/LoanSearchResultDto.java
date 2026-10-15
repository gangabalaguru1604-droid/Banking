package com.loanproject.loan_app.dto;

public class LoanSearchResultDto {

    private Long loanId;
    private String accountNumber;
    private String customerId;
    private String customerName;
    private String maskedPhone;
    private double amount;
    private double remaining;
    private double emi;
    private String status;
    private String branch;

    public LoanSearchResultDto() {
    }

    public LoanSearchResultDto(Long loanId, String accountNumber, String customerId,
                               String customerName, String maskedPhone, double amount,
                               double remaining, double emi, String status, String branch) {
        this.loanId = loanId;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.maskedPhone = maskedPhone;
        this.amount = amount;
        this.remaining = remaining;
        this.emi = emi;
        this.status = status;
        this.branch = branch;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getRemaining() {
        return remaining;
    }

    public void setRemaining(double remaining) {
        this.remaining = remaining;
    }

    public double getEmi() {
        return emi;
    }

    public void setEmi(double emi) {
        this.emi = emi;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
