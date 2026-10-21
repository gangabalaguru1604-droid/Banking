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
    private String repaymentStatus;
    private int dpd;
    private double overdueAmount;
    private String customerId;
    private String maskedPhone;
    private String createdAt;
    private String productType = "Personal Loan";
    private String startDate;
    private String borrowerName;
    private String rawEmail;
    private String rawPhone;
    private Long userId;

    public StaffDashboardLoanDto() {
    }

    public StaffDashboardLoanDto(Long loanId, String accountNumber, String borrowerName, String maskedEmail,
                                 double amount, double interest, int months, double emi, double remaining,
                                 String status, String branch) {
        this(loanId, accountNumber, borrowerName, maskedEmail, amount, interest, months, emi, remaining,
             status, branch, "CURRENT", 0, 0.0, "N/A", "N/A", "N/A");
    }

    public StaffDashboardLoanDto(Long loanId, String accountNumber, String borrowerName, String maskedEmail,
                                 double amount, double interest, int months, double emi, double remaining,
                                 String status, String branch, String repaymentStatus, int dpd, double overdueAmount,
                                 String customerId, String maskedPhone, String createdAt) {
        this.loanId = loanId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.interest = interest;
        this.months = months;
        this.emi = emi;
        this.remaining = remaining;
        this.status = status;
        this.branch = branch;
        this.borrowerName = borrowerName;
        this.user = new BorrowerSummary(borrowerName, maskedEmail);
        this.repaymentStatus = repaymentStatus;
        this.dpd = dpd;
        this.overdueAmount = overdueAmount;
        this.customerId = customerId;
        this.maskedPhone = maskedPhone;
        this.createdAt = createdAt;
        this.startDate = createdAt;
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

    public String getRepaymentStatus() {
        return repaymentStatus;
    }

    public void setRepaymentStatus(String repaymentStatus) {
        this.repaymentStatus = repaymentStatus;
    }

    public int getDpd() {
        return dpd;
    }

    public void setDpd(int dpd) {
        this.dpd = dpd;
    }

    public double getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(double overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getProductType() {
        return productType != null ? productType : "Personal Loan";
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getStartDate() {
        return startDate != null ? startDate : (createdAt != null ? createdAt : "N/A");
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getBorrowerName() {
        return borrowerName != null ? borrowerName : (user != null ? user.getName() : "Unknown");
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
        if (this.user != null) {
            this.user.setName(borrowerName);
        }
    }

    public String getRawEmail() {
        return rawEmail;
    }

    public void setRawEmail(String rawEmail) {
        this.rawEmail = rawEmail;
    }

    public String getRawPhone() {
        return rawPhone;
    }

    public void setRawPhone(String rawPhone) {
        this.rawPhone = rawPhone;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
