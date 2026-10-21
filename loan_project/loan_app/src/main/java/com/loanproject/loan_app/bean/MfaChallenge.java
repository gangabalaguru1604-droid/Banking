package com.loanproject.loan_app.bean;

import java.time.Instant;

@org.springframework.data.mongodb.core.mapping.Document(collection = "mfa_challenges")

public class MfaChallenge {

    @org.springframework.data.annotation.Id

    private Long id;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private User user;

    private String challengeToken;

    private String hashedOtpCode;

    private String mfaType = "TOTP";

    private Instant expiryTime;

    private int attemptCount = 0;

    private boolean verified = false;

    private boolean locked = false;

    @org.springframework.data.annotation.Version
    private Long version;

    private Instant createdAt = Instant.now();

    public MfaChallenge() {
    }

    public MfaChallenge(User user, String challengeToken, String mfaType, Instant expiryTime) {
        this.user = user;
        this.challengeToken = challengeToken;
        this.mfaType = mfaType != null ? mfaType : "TOTP";
        this.expiryTime = expiryTime;
        this.attemptCount = 0;
        this.verified = false;
        this.locked = false;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getChallengeToken() {
        return challengeToken;
    }

    public void setChallengeToken(String challengeToken) {
        this.challengeToken = challengeToken;
    }

    public String getHashedOtpCode() {
        return hashedOtpCode;
    }

    public void setHashedOtpCode(String hashedOtpCode) {
        this.hashedOtpCode = hashedOtpCode;
    }

    public String getMfaType() {
        return mfaType;
    }

    public void setMfaType(String mfaType) {
        this.mfaType = mfaType;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Instant expiryTime) {
        this.expiryTime = expiryTime;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryTime);
    }
}
