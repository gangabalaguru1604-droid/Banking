package com.loanproject.loan_app.dto;

public class StaffDashboardLoanDto {

    private Long loanId;
    private String accountNumber;
    private double amount;
    private double interest;
    private int months;
    private double emi;
    private double remaining;
    private String status;
    private String branch;
    private BorrowerSummary user;

    public StaffDashboardLoanDto() {
    }

    public StaffDashboardLoanDto(Long loanId, String accountNumber, String borrowerName, String maskedEmail,
                                 double amount, double interest, int months, double emi, double remaining,
                                 String status, String branch) {
        this.loanId = loanId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.interest = interest;
        this.months = months;
        this.emi = emi;
        this.remaining = remaining;
        this.status = status;
        this.branch = branch;
        this.user = new BorrowerSummary(borrowerName, maskedEmail);
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

    public BorrowerSummary getUser() {
        return user;
    }

    public void setUser(BorrowerSummary user) {
        this.user = user;
    }

    public static class BorrowerSummary {
        private String name;
        private String email;

        public BorrowerSummary() {
        }

        public BorrowerSummary(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
