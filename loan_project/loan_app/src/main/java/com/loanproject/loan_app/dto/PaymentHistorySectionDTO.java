package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentHistorySectionDTO implements Serializable {

    private List<PaymentTransactionDTO> transactions = new ArrayList<>();
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    public PaymentHistorySectionDTO() {
    }

    public PaymentHistorySectionDTO(List<PaymentTransactionDTO> transactions, BigDecimal totalPaidAmount) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        this.totalPaidAmount = totalPaidAmount != null ? totalPaidAmount : BigDecimal.ZERO;
    }

    public List<PaymentTransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PaymentTransactionDTO> transactions) {
        this.transactions = transactions;
    }

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }
}
