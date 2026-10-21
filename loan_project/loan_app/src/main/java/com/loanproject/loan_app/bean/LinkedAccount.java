package com.loanproject.loan_app.bean;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "linked_accounts")
public class LinkedAccount {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Loan loan;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Column(nullable = false, length = 30)
    private String accountType = "SAVINGS"; // SAVINGS, CURRENT

    @Column(length = 100)
    private String bankName = "Indian Bank Auto-Debit Account";

    @Column(length = 30)
    private String ifscCode = "IDIB0001001";

    @Column(nullable = false)
    private boolean autoDebitEnabled = true;

    public LinkedAccount() {
    }

    public LinkedAccount(Loan loan, String accountNumber, String accountType, String bankName, String ifscCode, boolean autoDebitEnabled) {
        this.loan = loan;
        this.accountNumber = accountNumber;
        this.accountType = accountType != null ? accountType : "SAVINGS";
        this.bankName = bankName != null ? bankName : "Indian Bank Auto-Debit Account";
        this.ifscCode = ifscCode != null ? ifscCode : "IDIB0001001";
        this.autoDebitEnabled = autoDebitEnabled;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public boolean isAutoDebitEnabled() {
        return autoDebitEnabled;
    }

    public void setAutoDebitEnabled(boolean autoDebitEnabled) {
        this.autoDebitEnabled = autoDebitEnabled;
    }
}
