package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentTransactionDTO implements Serializable {

    private String paymentDate;
    private BigDecimal amount;
    private String paymentMode;
    private String transactionReference;
    private String status;

    public PaymentTransactionDTO() {
    }

    public PaymentTransactionDTO(String paymentDate, BigDecimal amount, String paymentMode,
                                 String transactionReference, String status) {
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.transactionReference = transactionReference;
        this.status = status;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
