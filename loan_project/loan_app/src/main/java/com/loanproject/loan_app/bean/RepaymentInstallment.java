package com.loanproject.loan_app.bean;

import java.math.BigDecimal;
import java.time.LocalDate;

@org.springframework.data.mongodb.core.mapping.Document(collection = "repayment_installments")

public class RepaymentInstallment {

    @org.springframework.data.annotation.Id

    private Long id;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private Loan loan;

    private int installmentNumber;

    private LocalDate dueDate;

    private BigDecimal emiAmount;

    private BigDecimal principalComponent;

    private BigDecimal interestComponent;

    private BigDecimal paidAmount = BigDecimal.ZERO;

    private String status = "DUE"; // PAID, DUE, OVERDUE

    private int dpd = 0; // Days Past Due

    public RepaymentInstallment() {
    }

    public RepaymentInstallment(Loan loan, int installmentNumber, LocalDate dueDate, BigDecimal emiAmount,
                                BigDecimal principalComponent, BigDecimal interestComponent, BigDecimal paidAmount,
                                String status, int dpd) {
        this.loan = loan;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.emiAmount = emiAmount;
        this.principalComponent = principalComponent;
        this.interestComponent = interestComponent;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.status = status != null ? status : "DUE";
        this.dpd = dpd;
    }

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

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(int installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public void setEmiAmount(BigDecimal emiAmount) {
        this.emiAmount = emiAmount;
    }

    public BigDecimal getPrincipalComponent() {
        return principalComponent;
    }

    public void setPrincipalComponent(BigDecimal principalComponent) {
        this.principalComponent = principalComponent;
    }

    public BigDecimal getInterestComponent() {
        return interestComponent;
    }

    public void setInterestComponent(BigDecimal interestComponent) {
        this.interestComponent = interestComponent;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount != null ? paidAmount : BigDecimal.ZERO;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getStatus() {
        return status != null ? status : "DUE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDpd() {
        return dpd;
    }

    public void setDpd(int dpd) {
        this.dpd = dpd;
    }
}
