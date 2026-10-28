package com.loanproject.loan_app.faces.settings;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.security.PasswordResetService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

@Component("securitySettingsBean")
@SessionScope
public class SecuritySettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SecuritySettingsBean.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final AuthAuditService auditService;

    // Security Status Display
    private String email;
    private boolean mfaEnrolled;
    private String mfaType = "TOTP";
    private boolean mandatoryMfa = true;
    private boolean accountNonLocked = true;
    private int failedAttemptCount = 0;
    private String sessionProtection = "Active (Session Fixation + Migration Protection)";

    // Change Password Form
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    // View Feedback
    private String successMessage;
    private String errorMessage;

    public SecuritySettingsBean(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordResetService passwordResetService,
                                AuthAuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        loadSecurityStatus();
    }

    public void loadSecurityStatus() {
        this.currentPassword = "";
        this.newPassword = "";
        this.confirmPassword = "";
        this.successMessage = null;
        this.errorMessage = null;

        User user = getAuthenticatedUser();
        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(user);
            this.email = user.getEmail();
            this.mfaEnrolled = user.isMfaEnrolled();
            this.mfaType = user.getMfaType() != null ? user.getMfaType() : "TOTP";
            this.accountNonLocked = user.isAccountNonLocked();
            this.failedAttemptCount = user.getFailedAttemptCount();
            this.mandatoryMfa = true; // Standard Bank Policy: MFA cannot be disabled
        }
    }

    public void changePassword() {
        this.successMessage = null;
        this.errorMessage = null;

        User user = getAuthenticatedUser();
        if (user == null) {
            this.errorMessage = "Session expired or user not authenticated. Please log in.";
            return;
        }

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null) {
            this.errorMessage = "User record not found in system.";
            return;
        }

        if (this.currentPassword == null || this.currentPassword.isBlank()) {
            this.errorMessage = "Please enter your current password.";
            return;
        }

        if (!passwordEncoder.matches(this.currentPassword, dbUser.getPassword())) {
            this.errorMessage = "Current password does not match our records.";
            return;
        }

        if (this.newPassword == null || this.newPassword.isBlank()) {
            this.errorMessage = "New password cannot be blank.";
            return;
        }

        if (this.newPassword.equals(this.currentPassword)) {
            this.errorMessage = "New password must be different from your current password.";
            return;
        }

        if (!this.newPassword.equals(this.confirmPassword)) {
            this.errorMessage = "New password and Confirm Password do not match.";
            return;
        }

        if (!passwordResetService.validatePasswordStrength(this.newPassword)) {
            this.errorMessage = "Password must be 8-64 characters long, with at least one uppercase letter, one lowercase letter, one number, and one special character.";
            return;
        }

        // Apply new hashed password
        dbUser.setPassword(passwordEncoder.encode(this.newPassword));
        dbUser.setFailedAttemptCount(0);
        dbUser.setAccountNonLocked(true);
        dbUser.setLockTime(null);
        userRepository.save(dbUser);

        // Security Audit Log
        HttpServletRequest req = getHttpServletRequest();
        auditService.logPasswordChanged(dbUser.getEmail(), req);

        // Clear sensitive inputs
        this.currentPassword = "";
        this.newPassword = "";
        this.confirmPassword = "";

        this.successMessage = "Your password has been changed successfully!";
        log.info("Password updated successfully for: {}", dbUser.getEmail());
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
    public String getEmail() { return email; }
    public boolean isMfaEnrolled() { return mfaEnrolled; }
    public String getMfaType() { return mfaType; }
    public boolean isMandatoryMfa() { return mandatoryMfa; }
    public boolean isAccountNonLocked() { return accountNonLocked; }
    public int getFailedAttemptCount() { return failedAttemptCount; }
    public String getSessionProtection() { return sessionProtection; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
