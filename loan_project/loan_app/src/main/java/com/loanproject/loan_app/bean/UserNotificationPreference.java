package com.loanproject.loan_app.bean;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entity persisting customer and staff notification preferences across
 * email, SMS, loan updates, repayment reminders, and security alerts.
 */
@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(name = "loan_updates_enabled", nullable = false)
    private boolean loanUpdatesEnabled = true;

    @Column(name = "repayment_reminders_enabled", nullable = false)
    private boolean repaymentRemindersEnabled = true;

    @Column(name = "security_alerts_enabled", nullable = false)
    private boolean securityAlertsEnabled = true;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public UserNotificationPreference() {
    }

    public UserNotificationPreference(User user) {
        this.user = user;
        this.emailEnabled = true;
        this.smsEnabled = true;
        this.loanUpdatesEnabled = true;
        this.repaymentRemindersEnabled = true;
        this.securityAlertsEnabled = true;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
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

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public boolean isLoanUpdatesEnabled() {
        return loanUpdatesEnabled;
    }

    public void setLoanUpdatesEnabled(boolean loanUpdatesEnabled) {
        this.loanUpdatesEnabled = loanUpdatesEnabled;
    }

    public boolean isRepaymentRemindersEnabled() {
        return repaymentRemindersEnabled;
    }

    public void setRepaymentRemindersEnabled(boolean repaymentRemindersEnabled) {
        this.repaymentRemindersEnabled = repaymentRemindersEnabled;
    }

    public boolean isSecurityAlertsEnabled() {
        return securityAlertsEnabled;
    }

    public void setSecurityAlertsEnabled(boolean securityAlertsEnabled) {
        this.securityAlertsEnabled = securityAlertsEnabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
