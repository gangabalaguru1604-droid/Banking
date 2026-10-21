package com.loanproject.loan_app.bean;

import java.time.Instant;

@org.springframework.data.mongodb.core.mapping.Document(collection = "auth_audit_logs")

public class AuthAuditLog {

    @org.springframework.data.annotation.Id

    private Long id;

    private String username;

    private Instant timestamp = Instant.now();

    private String clientIp;

    private String userAgent;

    private String eventType;

    private String outcome;

    private String failureCategory;

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
