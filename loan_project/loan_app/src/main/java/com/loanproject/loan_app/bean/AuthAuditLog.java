package com.loanproject.loan_app.bean;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_audit_logs", indexes = {
    @Index(name = "idx_audit_username", columnList = "username"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_type", columnList = "eventType")
})
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String username;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(nullable = false, length = 64)
    private String clientIp;

    @Column(length = 256)
    private String userAgent;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(length = 50)
    private String failureCategory;

    @Column(length = 500)
    private String details;

    public AuthAuditLog() {
    }

    public AuthAuditLog(String username, String clientIp, String userAgent,
                        String eventType, String outcome, String failureCategory, String details) {
        this.username = username != null ? username : "UNKNOWN";
        this.timestamp = Instant.now();
        this.clientIp = clientIp != null ? clientIp : "UNKNOWN";
        this.userAgent = userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
        this.eventType = eventType;
        this.outcome = outcome;
        this.failureCategory = failureCategory;
        this.details = details != null && details.length() > 500 ? details.substring(0, 500) : details;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public String getDetails() {
        return details;
    }
}
