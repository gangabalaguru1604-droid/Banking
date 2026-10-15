package com.loanproject.loan_app.bean;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "mfa_challenges", indexes = {
    @Index(name = "idx_mfa_challenge_token", columnList = "challengeToken", unique = true)
})
public class MfaChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String challengeToken;

    @Column(length = 256)
    private String hashedOtpCode;

    @Column(nullable = false, length = 20)
    private String mfaType = "TOTP";

    @Column(nullable = false)
    private Instant expiryTime;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean locked = false;

    @Version
    private Long version;

    @Column(nullable = false)
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
