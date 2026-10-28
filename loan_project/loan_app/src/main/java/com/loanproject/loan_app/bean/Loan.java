package com.loanproject.loan_app.bean;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity 
public class Loan {
 
    @Id
    @GeneratedValue
    private Long loanId;

    private double amount;
    private double interest;
    private int months;
    private double emi;
    private double remaining;

    @Column(length = 50, unique = true)
    private String accountNumber;

    @Column(length = 50)
    private String branch = "MAIN";

    @Column(length = 30)
    private String status = "ACTIVE";

    @Column(length = 50)
    private String productType = "Personal Loan";

    @Column(length = 20)
    private String interestType = "FIXED";

    private double disbursedAmount;

    private java.time.LocalDate disbursementDate;

    private java.time.Instant createdAt = java.time.Instant.now();

    @jakarta.persistence.Version
    private Long version = 0L;

    @ManyToOne
    private User user;

	public Long getLoanId() {
		return loanId;
	}

	public void setLoanId(Long loanId) {
		this.loanId = loanId;
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getBranch() {
		return branch != null ? branch : "MAIN";
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getStatus() {
		return status != null ? status : "ACTIVE";
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public java.time.Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(java.time.Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getProductType() {
		return productType != null ? productType : "Personal Loan";
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public String getInterestType() {
		return interestType != null ? interestType : "FIXED";
	}

	public void setInterestType(String interestType) {
		this.interestType = interestType;
	}

	public double getDisbursedAmount() {
		if ("PENDING".equalsIgnoreCase(status) || "SUBMITTED".equalsIgnoreCase(status)
				|| "IN_REVIEW".equalsIgnoreCase(status) || "APPLIED".equalsIgnoreCase(status)
				|| "REJECTED".equalsIgnoreCase(status)) {
			return disbursedAmount;
		}
		return disbursedAmount > 0 ? disbursedAmount : amount;
	}

	public void setDisbursedAmount(double disbursedAmount) {
		this.disbursedAmount = disbursedAmount;
	}

	public java.time.LocalDate getDisbursementDate() {
		if ("PENDING".equalsIgnoreCase(status) || "SUBMITTED".equalsIgnoreCase(status)
				|| "IN_REVIEW".equalsIgnoreCase(status) || "APPLIED".equalsIgnoreCase(status)
				|| "REJECTED".equalsIgnoreCase(status)) {
			return disbursementDate;
		}
		if (disbursementDate != null) {
			return disbursementDate;
		}
		if (createdAt != null) {
			return createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		}
		return java.time.LocalDate.now();
	}

	public void setDisbursementDate(java.time.LocalDate disbursementDate) {
		this.disbursementDate = disbursementDate;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
