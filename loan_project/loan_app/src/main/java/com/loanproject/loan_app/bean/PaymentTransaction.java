package com.loanproject.loan_app.bean;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Loan loan;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant paymentDate = Instant.now();

    @Column(length = 30, nullable = false)
    private String paymentMode = "UPI"; // NACH, UPI, NET_BANKING, CASH

    @Column(length = 100, nullable = false, unique = true)
    private String transactionReference;

    @Column(length = 30, nullable = false)
    private String status = "SUCCESS"; // SUCCESS, PENDING, FAILED

    public PaymentTransaction() {
    }

    public PaymentTransaction(Loan loan, BigDecimal amount, String paymentMode,
                              String transactionReference, String status) {
        this(loan, amount, Instant.now(), paymentMode, transactionReference, status);
    }

    public PaymentTransaction(Loan loan, BigDecimal amount, Instant paymentDate, String paymentMode,
                              String transactionReference, String status) {
        this.loan = loan;
        this.amount = amount;
        this.paymentDate = paymentDate != null ? paymentDate : Instant.now();
        this.paymentMode = paymentMode != null ? paymentMode : "UPI";
        this.transactionReference = transactionReference;
        this.status = status != null ? status : "SUCCESS";
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Instant paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
