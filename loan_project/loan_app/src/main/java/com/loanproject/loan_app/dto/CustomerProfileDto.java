package com.loanproject.loan_app.dto;

import java.util.ArrayList;
import java.util.List;

public class CustomerProfileDto {

    private Long userId;
    private String customerId;
    private String customerName;
    private String maskedPhone;
    private String maskedEmail;
    private String maskedPan;
    private String maskedAadhaar;
    private String branch;
    private Long primaryLoanId;
    private String primaryAccountNumber;
    private List<LoanDetailDto> loans = new ArrayList<>();

    public CustomerProfileDto() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public String getMaskedPan() {
        return maskedPan;
    }

    public void setMaskedPan(String maskedPan) {
        this.maskedPan = maskedPan;
    }

    public String getMaskedAadhaar() {
        return maskedAadhaar;
    }

    public void setMaskedAadhaar(String maskedAadhaar) {
        this.maskedAadhaar = maskedAadhaar;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Long getPrimaryLoanId() {
        return primaryLoanId;
    }

    public void setPrimaryLoanId(Long primaryLoanId) {
        this.primaryLoanId = primaryLoanId;
    }

    public String getPrimaryAccountNumber() {
        return primaryAccountNumber;
    }

    public void setPrimaryAccountNumber(String primaryAccountNumber) {
        this.primaryAccountNumber = primaryAccountNumber;
    }

    public List<LoanDetailDto> getLoans() {
        return loans;
    }

    public void setLoans(List<LoanDetailDto> loans) {
        this.loans = loans;
    }
}
