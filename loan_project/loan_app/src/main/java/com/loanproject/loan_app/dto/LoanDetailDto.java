package com.loanproject.loan_app.dto;

public class LoanDetailDto {

    private Long loanId;
    private String accountNumber;
    private double amount;
    private double interest;
    private int months;
    private double emi;
    private double remaining;
    private String status;
    private String branch;
    private String formattedCreatedAt;

    public LoanDetailDto() {
    }

    public LoanDetailDto(Long loanId, String accountNumber, double amount, double interest,
                         int months, double emi, double remaining, String status,
                         String branch, String formattedCreatedAt) {
        this.loanId = loanId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.interest = interest;
        this.months = months;
        this.emi = emi;
        this.remaining = remaining;
        this.status = status;
        this.branch = branch;
        this.formattedCreatedAt = formattedCreatedAt;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getInterest() {
        return interest;
    }

    public void setInterest(double interest) {
        this.interest = interest;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public double getEmi() {
        return emi;
    }

    public void setEmi(double emi) {
        this.emi = emi;
    }

    public double getRemaining() {
        return remaining;
    }

    public void setRemaining(double remaining) {
        this.remaining = remaining;
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

    public String getFormattedCreatedAt() {
        return formattedCreatedAt;
    }

    public void setFormattedCreatedAt(String formattedCreatedAt) {
        this.formattedCreatedAt = formattedCreatedAt;
    }
}
