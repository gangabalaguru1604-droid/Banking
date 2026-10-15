package com.loanproject.loan_app.bean;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AppRole role = AppRole.ROLE_CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private UserType userType = UserType.CUSTOMER;

    @Column
    private int failedAttemptCount = 0;

    @Column
    private boolean accountNonLocked = true;

    private LocalDateTime lockTime;

    @Column
    private boolean mfaEnrolled = false;

    @Column(length = 512)
    private String mfaSecret;

    @Column(length = 20)
    private String mfaType = "TOTP";

    private String phoneNumber;

    @Column(length = 50)
    private String branch = "MAIN";

    @Column(length = 50)
    private String customerId;

    @Column(length = 20)
    private String panNumber;

    @Column(length = 20)
    private String aadhaarNumber;

    private java.time.LocalDate dateOfBirth;

    @Column(length = 30)
    private String kycStatus = "VERIFIED";

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String preferredContactMethod = "EMAIL";

    public User() {
    }

    public User(String name, String email, String password, AppRole role, UserType userType) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : AppRole.ROLE_CUSTOMER;
        this.userType = userType != null ? userType : UserType.CUSTOMER;
        this.accountNonLocked = true;
        this.failedAttemptCount = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AppRole getRole() {
        return role != null ? role : AppRole.ROLE_CUSTOMER;
    }

    public void setRole(AppRole role) {
        this.role = role;
    }

    public UserType getUserType() {
        return userType != null ? userType : UserType.CUSTOMER;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public int getFailedAttemptCount() {
        return failedAttemptCount;
    }

    public void setFailedAttemptCount(int failedAttemptCount) {
        this.failedAttemptCount = failedAttemptCount;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    public boolean isMfaEnrolled() {
        return mfaEnrolled;
    }

    public void setMfaEnrolled(boolean mfaEnrolled) {
        this.mfaEnrolled = mfaEnrolled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public String getMfaType() {
        return mfaType;
    }

    public void setMfaType(String mfaType) {
        this.mfaType = mfaType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isStaff() {
        return this.userType == UserType.STAFF;
    }

    public String getBranch() {
        return branch != null ? branch : "MAIN";
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public java.time.LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(java.time.LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getKycStatus() {
        return kycStatus != null ? kycStatus : "VERIFIED";
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

    public String getPreferredContactMethod() {
        return preferredContactMethod != null ? preferredContactMethod : "EMAIL";
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }
}
