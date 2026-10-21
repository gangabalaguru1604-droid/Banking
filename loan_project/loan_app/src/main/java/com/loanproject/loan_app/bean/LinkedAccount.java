package com.loanproject.loan_app.bean;

@org.springframework.data.mongodb.core.mapping.Document(collection = "linked_accounts")

public class LinkedAccount {

    @org.springframework.data.annotation.Id

    private Long id;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private Loan loan;

    private String accountNumber;

    private String accountType = "SAVINGS"; // SAVINGS, CURRENT

    private String bankName = "Indian Bank Auto-Debit Account";

    private String ifscCode = "IDIB0001001";

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
