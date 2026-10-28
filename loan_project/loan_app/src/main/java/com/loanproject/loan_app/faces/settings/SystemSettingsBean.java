package com.loanproject.loan_app.faces.settings;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

@Component("systemSettingsBean")
@SessionScope
public class SystemSettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SystemSettingsBean.class);

    private final UserRepository userRepository;
    private final AuthAuditService auditService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${server.port:8081}")
    private String serverPort;

    // Read-only Runtime Diagnostics
    private String appName = "Bank of India Smart Loan Management System";
    private String appVersion = "2.0.0-PROD";
    private String javaVersion;
    private String osName;
    private String jvmUptime;
    private long freeMemoryMb;
    private long totalMemoryMb;
    private long maxMemoryMb;
    private String serverTimestamp;

    // Security & Compliance Status
    private String securityFramework = "Spring Security 6.x (Jakarta EE 10)";
    private String mfaStatus = "Mandatory Two-Factor Authentication (TOTP / RFC 6238)";
    private String csrfStatus = "Strict CSRF Token Protection Active";
    private String lockingStrategy = "JPA Optimistic Concurrency Control (@Version)";
    private String sessionPolicy = "15-Minute Inactivity Invalidation + Fixation Migration";

    // Safe Operational Preferences (In-Memory / Configurable)
    private String announcementBanner = "Welcome to Bank of India Smart Loan Portal. Please ensure your KYC details are up to date.";
    private boolean maintenanceNoticeEnabled = false;
    private String maintenanceNotice = "Scheduled system maintenance every Sunday from 02:00 AM to 03:00 AM IST.";
    private boolean emailDispatchActive = true;

    // Feedback
    private String successMessage;
    private String errorMessage;

    public SystemSettingsBean(UserRepository userRepository, AuthAuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        refreshDiagnostics();
    }

    public void refreshDiagnostics() {
        this.javaVersion = System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
        this.osName = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";

        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptimeMillis / (1000 * 60 * 60);
        long minutes = (uptimeMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (uptimeMillis % (1000 * 60)) / 1000;
        this.jvmUptime = String.format("%d hrs, %d mins, %d secs", hours, minutes, seconds);

        Runtime rt = Runtime.getRuntime();
        this.freeMemoryMb = rt.freeMemory() / (1024 * 1024);
        this.totalMemoryMb = rt.totalMemory() / (1024 * 1024);
        this.maxMemoryMb = rt.maxMemory() / (1024 * 1024);

        this.serverTimestamp = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a z")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }

    public void saveOperationalSettings() {
        this.successMessage = null;
        this.errorMessage = null;

        User currentUser = getAuthenticatedUser();
        if (currentUser == null || currentUser.getRole() != AppRole.ROLE_ADMIN) {
            this.errorMessage = "Access Denied: Only administrators are authorized to update system settings.";
            throw new AccessDeniedException(this.errorMessage);
        }

        if (this.announcementBanner != null && this.announcementBanner.length() > 300) {
            this.errorMessage = "Announcement banner cannot exceed 300 characters.";
            return;
        }

        if (this.maintenanceNotice != null && this.maintenanceNotice.length() > 300) {
            this.errorMessage = "Maintenance notice cannot exceed 300 characters.";
            return;
        }

        // Security Audit Log
        HttpServletRequest req = getHttpServletRequest();
        auditService.logSystemSettingsUpdated(currentUser.getEmail(), req,
                "Maintenance=" + this.maintenanceNoticeEnabled + ", EmailDispatch=" + this.emailDispatchActive);

        this.successMessage = "Operational system preferences updated successfully!";
        log.info("Admin {} updated system operational preferences", currentUser.getEmail());
    }

    public boolean isUserAdmin() {
        User user = getAuthenticatedUser();
        return user != null && user.getRole() == AppRole.ROLE_ADMIN;
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
    public String getAppName() { return appName; }
    public String getAppVersion() { return appVersion; }
    public String getActiveProfile() { return activeProfile; }
    public String getServerPort() { return serverPort; }
    public String getJavaVersion() { return javaVersion; }
    public String getOsName() { return osName; }
    public String getJvmUptime() { return jvmUptime; }
    public long getFreeMemoryMb() { return freeMemoryMb; }
    public long getTotalMemoryMb() { return totalMemoryMb; }
    public long getMaxMemoryMb() { return maxMemoryMb; }
    public String getServerTimestamp() { return serverTimestamp; }

    public String getSecurityFramework() { return securityFramework; }
    public String getMfaStatus() { return mfaStatus; }
    public String getCsrfStatus() { return csrfStatus; }
    public String getLockingStrategy() { return lockingStrategy; }
    public String getSessionPolicy() { return sessionPolicy; }

    public String getAnnouncementBanner() { return announcementBanner; }
    public void setAnnouncementBanner(String announcementBanner) { this.announcementBanner = announcementBanner; }

    public boolean isMaintenanceNoticeEnabled() { return maintenanceNoticeEnabled; }
    public void setMaintenanceNoticeEnabled(boolean maintenanceNoticeEnabled) { this.maintenanceNoticeEnabled = maintenanceNoticeEnabled; }

    public String getMaintenanceNotice() { return maintenanceNotice; }
    public void setMaintenanceNotice(String maintenanceNotice) { this.maintenanceNotice = maintenanceNotice; }

    public boolean isEmailDispatchActive() { return emailDispatchActive; }
    public void setEmailDispatchActive(boolean emailDispatchActive) { this.emailDispatchActive = emailDispatchActive; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
