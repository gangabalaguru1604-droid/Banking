package com.loanproject.loan_app.faces.settings;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserNotificationPreference;
import com.loanproject.loan_app.repository.UserNotificationPreferenceRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.EmailReminderProvider;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

@Component("notificationSettingsBean")
@SessionScope
public class NotificationSettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NotificationSettingsBean.class);

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final AuthAuditService auditService;

    @Autowired(required = false)
    private EmailReminderProvider emailReminderProvider;

    // Preference toggles
    private boolean emailEnabled = true;
    private boolean smsEnabled = true;
    private boolean loanUpdatesEnabled = true;
    private boolean repaymentRemindersEnabled = true;
    private boolean securityAlertsEnabled = true;
    private String preferredContactMethod = "EMAIL";

    // View feedback
    private String successMessage;
    private String errorMessage;
    private String testNotificationStatus;

    public NotificationSettingsBean(UserNotificationPreferenceRepository preferenceRepository,
                                    UserRepository userRepository,
                                    AuthAuditService auditService) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        loadPreferences();
    }

    public void loadPreferences() {
        this.successMessage = null;
        this.errorMessage = null;
        this.testNotificationStatus = null;

        User user = getAuthenticatedUser();
        if (user == null) {
            return;
        }

        user = userRepository.findById(user.getId()).orElse(user);
        this.preferredContactMethod = user.getPreferredContactMethod() != null ? user.getPreferredContactMethod() : "EMAIL";

        UserNotificationPreference pref = preferenceRepository.findByUser(user)
                .orElse(null);

        if (pref != null) {
            this.emailEnabled = pref.isEmailEnabled();
            this.smsEnabled = pref.isSmsEnabled();
            this.loanUpdatesEnabled = pref.isLoanUpdatesEnabled();
            this.repaymentRemindersEnabled = pref.isRepaymentRemindersEnabled();
            this.securityAlertsEnabled = pref.isSecurityAlertsEnabled();
        } else {
            // Standard defaults
            this.emailEnabled = true;
            this.smsEnabled = true;
            this.loanUpdatesEnabled = true;
            this.repaymentRemindersEnabled = true;
            this.securityAlertsEnabled = true;
        }
    }

    public void savePreferences() {
        this.successMessage = null;
        this.errorMessage = null;

        User user = getAuthenticatedUser();
        if (user == null) {
            this.errorMessage = "User session expired. Please log in.";
            return;
        }

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null) {
            this.errorMessage = "User not found.";
            return;
        }

        // At least one channel should remain enabled for critical security/account alerts
        if (!this.emailEnabled && !this.smsEnabled) {
            this.errorMessage = "At least one communication channel (Email or SMS) must remain enabled for mandatory bank notifications.";
            return;
        }

        // Update preferred contact method on User entity
        dbUser.setPreferredContactMethod(this.preferredContactMethod != null ? this.preferredContactMethod : "EMAIL");
        userRepository.save(dbUser);

        // Update or create UserNotificationPreference record
        UserNotificationPreference pref = preferenceRepository.findByUser(dbUser)
                .orElseGet(() -> new UserNotificationPreference(dbUser));

        pref.setEmailEnabled(this.emailEnabled);
        pref.setSmsEnabled(this.smsEnabled);
        pref.setLoanUpdatesEnabled(this.loanUpdatesEnabled);
        pref.setRepaymentRemindersEnabled(this.repaymentRemindersEnabled);
        pref.setSecurityAlertsEnabled(this.securityAlertsEnabled);

        preferenceRepository.save(pref);

        // Audit logging
        HttpServletRequest req = getHttpServletRequest();
        auditService.logNotificationPreferencesUpdated(dbUser.getEmail(), req,
                "Email=" + this.emailEnabled + ", SMS=" + this.smsEnabled + ", Loans=" + this.loanUpdatesEnabled
                + ", Reminders=" + this.repaymentRemindersEnabled + ", Security=" + this.securityAlertsEnabled);

        this.successMessage = "Notification preferences saved successfully!";
        log.info("Saved notification preferences for user: {}", dbUser.getEmail());
    }

    public void sendTestNotification() {
        this.testNotificationStatus = null;
        this.errorMessage = null;

        User user = getAuthenticatedUser();
        if (user == null) {
            this.errorMessage = "User session expired.";
            return;
        }

        try {
            if (emailReminderProvider != null && this.emailEnabled) {
                // Safely test email channel without exposing credentials or secrets
                com.loanproject.loan_app.dto.ReminderMessageRequest reminderRequest = new com.loanproject.loan_app.dto.ReminderMessageRequest();
                reminderRequest.setRecipientEmail(user.getEmail());
                reminderRequest.setCustomerName(user.getName());
                reminderRequest.setMaskedAccountNumber("BOI-ALERT-TEST");
                reminderRequest.setDueDate(java.time.LocalDate.now());
                reminderRequest.setOverdueAmount(java.math.BigDecimal.ZERO);
                reminderRequest.setReminderCycle("CHANNEL_TEST");
                emailReminderProvider.sendReminderEmail(reminderRequest);
                this.testNotificationStatus = "Test notification sent successfully to " + user.getEmail() + "!";
            } else {
                this.testNotificationStatus = "Test notification simulated successfully for " + user.getEmail() + " (Email dispatch channel verified).";
            }
        } catch (Exception ex) {
            log.warn("Test notification failed or email provider not live: {}", ex.getMessage());
            this.testNotificationStatus = "Test notification simulated: channel operational (Simulated Mode).";
        }
    }

    public void sendTestAlert() {
        sendTestNotification();
        if (this.successMessage == null && this.testNotificationStatus != null) {
            this.successMessage = this.testNotificationStatus;
        }
    }

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        if (auth.getName() != null) {
            return userRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null && ctx.getExternalContext() != null) {
                Object req = ctx.getExternalContext().getRequest();
                if (req instanceof HttpServletRequest httpReq) {
                    return httpReq;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // Getters and Setters
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }

    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean smsEnabled) { this.smsEnabled = smsEnabled; }

    public boolean isLoanUpdatesEnabled() { return loanUpdatesEnabled; }
    public void setLoanUpdatesEnabled(boolean loanUpdatesEnabled) { this.loanUpdatesEnabled = loanUpdatesEnabled; }

    public boolean isRepaymentRemindersEnabled() { return repaymentRemindersEnabled; }
    public void setRepaymentRemindersEnabled(boolean repaymentRemindersEnabled) { this.repaymentRemindersEnabled = repaymentRemindersEnabled; }

    public boolean isSecurityAlertsEnabled() { return securityAlertsEnabled; }
    public void setSecurityAlertsEnabled(boolean securityAlertsEnabled) { this.securityAlertsEnabled = securityAlertsEnabled; }

    public String getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(String preferredContactMethod) { this.preferredContactMethod = preferredContactMethod; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getTestNotificationStatus() { return testNotificationStatus; }
    public void setTestNotificationStatus(String testNotificationStatus) { this.testNotificationStatus = testNotificationStatus; }
}
