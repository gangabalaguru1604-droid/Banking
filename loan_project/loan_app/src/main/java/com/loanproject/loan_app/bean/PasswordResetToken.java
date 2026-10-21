package com.loanproject.loan_app.bean;

import java.time.Instant;

@org.springframework.data.mongodb.core.mapping.Document(collection = "password_reset_tokens")

public class PasswordResetToken {

    @org.springframework.data.annotation.Id

    private Long id;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private User user;

    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String tokenHash;

    private Instant expiryTime;

    private boolean used = false;

    private Instant createdAt = Instant.now();

    public PasswordResetToken() {
    }

    public PasswordResetToken(User user, String tokenHash, Instant expiryTime) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiryTime = expiryTime;
        this.used = false;
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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Instant expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
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
