package com.loanproject.loan_app.bean;

@org.springframework.data.mongodb.core.mapping.Document(collection = "loans")

public class Loan {

    @org.springframework.data.annotation.Id

    private Long loanId;

    private double amount;
    private double interest;
    private int months;
    private double emi;
    private double remaining;

    @org.springframework.data.mongodb.core.index.Indexed(unique = true, sparse = true)
    private String accountNumber;

    private String branch = "MAIN";

    private String status = "ACTIVE";

    private String productType = "Personal Loan";

    private String interestType = "FIXED";

    private double disbursedAmount;

    private java.time.LocalDate disbursementDate;

    private java.time.Instant createdAt = java.time.Instant.now();

    @org.springframework.data.mongodb.core.mapping.DBRef

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
		return disbursedAmount > 0 ? disbursedAmount : amount;
	}

	public void setDisbursedAmount(double disbursedAmount) {
		this.disbursedAmount = disbursedAmount;
	}

	public java.time.LocalDate getDisbursementDate() {
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
}
