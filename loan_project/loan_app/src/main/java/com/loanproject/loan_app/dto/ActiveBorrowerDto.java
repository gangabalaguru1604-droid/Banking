package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing a distinct borrower who holds one or more active loans.
 * Consolidates multiple active loans into a single borrower record to eliminate duplicates.
 */
public class ActiveBorrowerDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String customerId;
    private String borrowerName;
    private List<String> accountNumbers = new ArrayList<>();
    private int activeLoanCount;
    private double totalSanctionedAmount;
    private double totalOutstandingBalance;
    private String branch;
    private String contactEmail;
    private String contactPhone;
    private String contactInfo;
    private String status = "ACTIVE";
    private Long primaryLoanId;

    public ActiveBorrowerDto() {
    }

    public ActiveBorrowerDto(Long userId, String customerId, String borrowerName, List<String> accountNumbers,
                             int activeLoanCount, double totalSanctionedAmount, double totalOutstandingBalance,
                             String branch, String contactEmail, String contactPhone, String contactInfo,
                             String status, Long primaryLoanId) {
        this.userId = userId;
        this.customerId = customerId;
        this.borrowerName = borrowerName;
        if (accountNumbers != null) {
            this.accountNumbers.addAll(accountNumbers);
        }
        this.activeLoanCount = activeLoanCount;
        this.totalSanctionedAmount = totalSanctionedAmount;
        this.totalOutstandingBalance = totalOutstandingBalance;
        this.branch = branch;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.contactInfo = contactInfo;
        this.status = status != null ? status : "ACTIVE";
        this.primaryLoanId = primaryLoanId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public List<String> getAccountNumbers() {
        return accountNumbers;
    }

    public void setAccountNumbers(List<String> accountNumbers) {
        this.accountNumbers = accountNumbers;
    }

    public void addAccountNumber(String accountNumber) {
        if (accountNumber != null && !accountNumber.isBlank() && !this.accountNumbers.contains(accountNumber)) {
            this.accountNumbers.add(accountNumber);
        }
    }

    public int getActiveLoanCount() {
        return activeLoanCount;
    }

    public void setActiveLoanCount(int activeLoanCount) {
        this.activeLoanCount = activeLoanCount;
    }

    public double getTotalSanctionedAmount() {
        return totalSanctionedAmount;
    }

    public void setTotalSanctionedAmount(double totalSanctionedAmount) {
        this.totalSanctionedAmount = totalSanctionedAmount;
    }

    public double getTotalOutstandingBalance() {
        return totalOutstandingBalance;
    }

    public void setTotalOutstandingBalance(double totalOutstandingBalance) {
        this.totalOutstandingBalance = totalOutstandingBalance;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPrimaryLoanId() {
        return primaryLoanId;
    }

    public void setPrimaryLoanId(Long primaryLoanId) {
        this.primaryLoanId = primaryLoanId;
    }
}
