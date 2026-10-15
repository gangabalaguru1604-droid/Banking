package com.loanproject.loan_app.dto;

import java.io.Serializable;

public class CustomerProfileSectionDTO implements Serializable {

    private String customerName;
    private String customerId;
    private String dateOfBirth;
    private String kycStatus;
    private String address;
    private String maskedPhone;
    private String maskedEmail;
    private String preferredContactMethod;
    private boolean contactDetailUpdateRequired;

    public CustomerProfileSectionDTO() {
    }

    public CustomerProfileSectionDTO(String customerName, String customerId, String dateOfBirth, String kycStatus,
                                     String address, String maskedPhone, String maskedEmail, String preferredContactMethod) {
        this.customerName = customerName;
        this.customerId = customerId;
        this.dateOfBirth = dateOfBirth;
        this.kycStatus = kycStatus;
        this.address = address;
        this.maskedPhone = maskedPhone;
        this.maskedEmail = maskedEmail;
        this.preferredContactMethod = preferredContactMethod;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getFullName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

    public boolean isContactDetailUpdateRequired() {
        return contactDetailUpdateRequired;
    }

    public void setContactDetailUpdateRequired(boolean contactDetailUpdateRequired) {
        this.contactDetailUpdateRequired = contactDetailUpdateRequired;
    }
}
