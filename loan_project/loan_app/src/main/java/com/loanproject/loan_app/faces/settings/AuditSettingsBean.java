package com.loanproject.loan_app.faces.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;

import jakarta.annotation.PostConstruct;

@Component("auditSettingsBean")
@SessionScope
public class AuditSettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AuditSettingsBean.class);

    private final AuthAuditLogRepository auditRepo;
    private final UserRepository userRepository;

    // Filter criteria
    private String keyword = "";
    private String selectedEventType = "ALL";
    private String selectedOutcome = "ALL";

    // Dataset
    private List<AuthAuditLog> allFilteredLogs = new ArrayList<>();
    private List<AuthAuditLog> pagedLogs = new ArrayList<>();

    // Pagination
    private int page = 0;
    private int pageSize = 15;
    private int totalPages = 1;
    private int totalRecords = 0;

    // Selected record for details modal
    private AuthAuditLog selectedLog;

    // Feedback
    private String errorMessage;

    public AuditSettingsBean(AuthAuditLogRepository auditRepo, UserRepository userRepository) {
        this.auditRepo = auditRepo;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        applyFilters();
    }

    public void applyFilters() {
        this.errorMessage = null;

        User currentUser = getAuthenticatedUser();
        if (currentUser == null || currentUser.getRole() != AppRole.ROLE_ADMIN) {
            this.errorMessage = "Access Denied: Only administrators are authorized to access security audit logs.";
            this.allFilteredLogs = new ArrayList<>();
            this.pagedLogs = new ArrayList<>();
            return;
        }

        try {
            List<AuthAuditLog> rawLogs = auditRepo.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp")
            );

            this.allFilteredLogs = rawLogs.stream().filter(item -> {
                // Event type filter
                if (!"ALL".equalsIgnoreCase(this.selectedEventType) && !item.getEventType().equalsIgnoreCase(this.selectedEventType)) {
                    return false;
                }

                // Outcome filter
                if (!"ALL".equalsIgnoreCase(this.selectedOutcome) && !item.getOutcome().equalsIgnoreCase(this.selectedOutcome)) {
                    return false;
                }

                // Keyword search
                if (this.keyword != null && !this.keyword.trim().isEmpty()) {
                    String cleanKey = this.keyword.trim().toLowerCase();
                    boolean matchUser = item.getUsername() != null && item.getUsername().toLowerCase().contains(cleanKey);
                    boolean matchIp = item.getClientIp() != null && item.getClientIp().toLowerCase().contains(cleanKey);
                    boolean matchDetails = item.getDetails() != null && item.getDetails().toLowerCase().contains(cleanKey);
                    boolean matchType = item.getEventType() != null && item.getEventType().toLowerCase().contains(cleanKey);
                    return matchUser || matchIp || matchDetails || matchType;
                }

                return true;
            }).collect(Collectors.toList());

            this.totalRecords = this.allFilteredLogs.size();
            this.totalPages = (int) Math.ceil((double) this.totalRecords / this.pageSize);
            if (this.totalPages == 0) {
                this.totalPages = 1;
            }

            // Reset page if out of bounds
            if (this.page >= this.totalPages) {
                this.page = 0;
            }

            refreshPagedSlice();
        } catch (Exception ex) {
            log.error("Failed to query audit records: {}", ex.getMessage());
            this.errorMessage = "Failed to load audit records: " + ex.getMessage();
        }
    }

    private void refreshPagedSlice() {
        if (this.allFilteredLogs == null || this.allFilteredLogs.isEmpty()) {
            this.pagedLogs = new ArrayList<>();
            return;
        }
        int fromIndex = this.page * this.pageSize;
        int toIndex = Math.min(fromIndex + this.pageSize, this.allFilteredLogs.size());
        if (fromIndex > toIndex) {
            fromIndex = 0;
            this.page = 0;
        }
        this.pagedLogs = new ArrayList<>(this.allFilteredLogs.subList(fromIndex, toIndex));
    }

    public void nextPage() {
        if (this.page < this.totalPages - 1) {
            this.page++;
            refreshPagedSlice();
        }
    }

    public void prevPage() {
        if (this.page > 0) {
            this.page--;
            refreshPagedSlice();
        }
    }

    public void firstPage() {
        this.page = 0;
        refreshPagedSlice();
    }

    public void resetFilters() {
        this.keyword = "";
        this.selectedEventType = "ALL";
        this.selectedOutcome = "ALL";
        this.page = 0;
        applyFilters();
    }

    public void selectLog(Long logId) {
        if (logId == null) {
            this.selectedLog = null;
            return;
        }
        this.selectedLog = auditRepo.findById(logId).orElse(null);
    }

    public void closeDetails() {
        this.selectedLog = null;
    }

    public boolean isUserAdmin() {
        User user = getAuthenticatedUser();
        return user != null && user.getRole() == AppRole.ROLE_ADMIN;
    }

    public List<String> getEventTypes() {
        return List.of(
            "ALL",
            "LOGIN_STAGE1_SUCCESS",
            "LOGIN_STAGE1_FAILURE",
            "MFA_VERIFY_SUCCESS",
            "MFA_VERIFY_FAILURE",
            "ACCOUNT_LOCKED",
            "ACCOUNT_UNLOCKED",
            "LOAN_APPLICATION_APPROVED",
            "LOAN_APPLICATION_REJECTED",
            "ROLE_CHANGED",
            "STAFF_PROVISIONED",
            "PROFILE_UPDATED",
            "PASSWORD_CHANGED",
            "NOTIFICATION_PREFERENCES_UPDATED",
            "LOAN_POLICY_UPDATED",
            "SYSTEM_SETTINGS_UPDATED",
            "COLLECTION_RECORDED"
        );
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

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getSelectedEventType() { return selectedEventType; }
    public void setSelectedEventType(String selectedEventType) { this.selectedEventType = selectedEventType; }

    public String getSelectedOutcome() { return selectedOutcome; }
    public void setSelectedOutcome(String selectedOutcome) { this.selectedOutcome = selectedOutcome; }

    public List<AuthAuditLog> getPagedLogs() { return pagedLogs; }
    public List<AuthAuditLog> getAllFilteredLogs() { return allFilteredLogs; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getTotalPages() { return totalPages; }
    public int getTotalRecords() { return totalRecords; }

    public AuthAuditLog getSelectedLog() { return selectedLog; }
    public void setSelectedLog(AuthAuditLog selectedLog) { this.selectedLog = selectedLog; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
