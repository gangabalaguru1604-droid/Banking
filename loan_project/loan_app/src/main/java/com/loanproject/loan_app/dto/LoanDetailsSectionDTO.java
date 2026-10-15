package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class LoanDetailsSectionDTO implements Serializable {

    private String accountNumber;
    private String productType;
    private BigDecimal sanctionedAmount;
    private BigDecimal disbursedAmount;
    private String disbursementDate;
    private BigDecimal interestRate;
    private String interestType;
    private int tenureMonths;
    private BigDecimal monthlyEmi;
    private String loanStatus;

    public LoanDetailsSectionDTO() {
    }

    public LoanDetailsSectionDTO(String accountNumber, String productType, BigDecimal sanctionedAmount,
                                 BigDecimal disbursedAmount, String disbursementDate, BigDecimal interestRate,
                                 String interestType, int tenureMonths, BigDecimal monthlyEmi) {
        this.accountNumber = accountNumber;
        this.productType = productType;
        this.sanctionedAmount = sanctionedAmount;
        this.disbursedAmount = disbursedAmount;
        this.disbursementDate = disbursementDate;
        this.interestRate = interestRate;
        this.interestType = interestType;
        this.tenureMonths = tenureMonths;
        this.monthlyEmi = monthlyEmi;
    }

    public LoanDetailsSectionDTO(String accountNumber, String productType, BigDecimal sanctionedAmount,
                                 BigDecimal disbursedAmount, String disbursementDate, BigDecimal interestRate,
                                 String interestType, int tenureMonths, BigDecimal monthlyEmi, String loanStatus) {
        this(accountNumber, productType, sanctionedAmount, disbursedAmount, disbursementDate, interestRate, interestType, tenureMonths, monthlyEmi);
        this.loanStatus = loanStatus;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getLoanAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getLoanStatus() {
        return loanStatus;
    }

    public void setLoanStatus(String loanStatus) {
        this.loanStatus = loanStatus;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public BigDecimal getSanctionedAmount() {
        return sanctionedAmount;
    }

    public void setSanctionedAmount(BigDecimal sanctionedAmount) {
        this.sanctionedAmount = sanctionedAmount;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public String getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(String disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public String getInterestType() {
        return interestType;
    }

    public void setInterestType(String interestType) {
        this.interestType = interestType;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getMonthlyEmi() {
        return monthlyEmi;
    }

    public void setMonthlyEmi(BigDecimal monthlyEmi) {
        this.monthlyEmi = monthlyEmi;
    }
}
