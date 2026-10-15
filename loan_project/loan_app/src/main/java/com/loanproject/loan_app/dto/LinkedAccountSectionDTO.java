package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class LinkedAccountSectionDTO implements Serializable {

    private String maskedAccountNumber;
    private String accountType;
    private String bankName;
    private String ifscCode;
    private boolean autoDebitEnabled;
    private BigDecimal liveCbsBalance;
    private String cbsBalanceStatus; // AVAILABLE, UNAVAILABLE, ERROR
    private String cbsExplanation;

    public LinkedAccountSectionDTO() {
    }

    public LinkedAccountSectionDTO(String maskedAccountNumber, String accountType, String bankName,
                                   String ifscCode, boolean autoDebitEnabled, BigDecimal liveCbsBalance,
                                   String cbsBalanceStatus, String cbsExplanation) {
        this.maskedAccountNumber = maskedAccountNumber;
        this.accountType = accountType;
        this.bankName = bankName;
        this.ifscCode = ifscCode;
        this.autoDebitEnabled = autoDebitEnabled;
        this.liveCbsBalance = liveCbsBalance;
        this.cbsBalanceStatus = cbsBalanceStatus != null ? cbsBalanceStatus : "UNAVAILABLE";
        this.cbsExplanation = cbsExplanation;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
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

    public BigDecimal getLiveCbsBalance() {
        return liveCbsBalance;
    }

    public void setLiveCbsBalance(BigDecimal liveCbsBalance) {
        this.liveCbsBalance = liveCbsBalance;
    }

    public String getCbsBalanceStatus() {
        return cbsBalanceStatus;
    }

    public void setCbsBalanceStatus(String cbsBalanceStatus) {
        this.cbsBalanceStatus = cbsBalanceStatus;
    }

    public String getCbsExplanation() {
        return cbsExplanation;
    }

    public void setCbsExplanation(String cbsExplanation) {
        this.cbsExplanation = cbsExplanation;
    }
}
