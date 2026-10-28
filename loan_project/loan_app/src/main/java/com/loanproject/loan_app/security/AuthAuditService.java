package com.loanproject.loan_app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AuthAuditService {

    private static final Logger log = LoggerFactory.getLogger(AuthAuditService.class);

    private final AuthAuditLogRepository auditRepo;
    private final ClientIpResolver ipResolver;

    public AuthAuditService(AuthAuditLogRepository auditRepo, ClientIpResolver ipResolver) {
        this.auditRepo = auditRepo;
        this.ipResolver = ipResolver;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, HttpServletRequest request, String eventType,
                       String outcome, String failureCategory, String details) {
        String clientIp = ipResolver.resolveClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";

        AuthAuditLog auditLog = new AuthAuditLog(
                username != null ? username : "UNKNOWN",
                clientIp,
                userAgent,
                eventType,
                outcome,
                failureCategory,
                details
        );

        auditRepo.save(auditLog);
        log.info("AUDIT EVENT: type={}, user={}, outcome={}, failureCategory={}, ip={}",
                eventType, auditLog.getUsername(), outcome, failureCategory, clientIp);
    }

    public void logLoginSuccess(String username, HttpServletRequest request) {
        record(username, request, "LOGIN_STAGE1_SUCCESS", "SUCCESS", null, "Primary password authentication successful");
    }

    public void logLoginFailure(String username, HttpServletRequest request, String failureCategory) {
        record(username, request, "LOGIN_STAGE1_FAILURE", "FAILURE", failureCategory, "Primary authentication failed");
    }

    public void logAccountLocked(String username, HttpServletRequest request) {
        record(username, request, "ACCOUNT_LOCKED", "FAILURE", "MAX_ATTEMPTS_EXCEEDED", "Account locked after 5 consecutive failed attempts");
    }

    public void logAccountUnlocked(String username, HttpServletRequest request, String actor) {
        record(username, request, "ACCOUNT_UNLOCKED", "SUCCESS", null, "Account unlocked by admin: " + actor);
    }

    public void logMfaChallengeCreated(String username, HttpServletRequest request) {
        record(username, request, "MFA_CHALLENGE_CREATED", "SUCCESS", null, "MFA challenge initiated");
    }

    public void logMfaSuccess(String username, HttpServletRequest request) {
        record(username, request, "MFA_VERIFY_SUCCESS", "SUCCESS", null, "MFA code verified successfully");
    }

    public void logMfaFailure(String username, HttpServletRequest request, String failureCategory) {
        record(username, request, "MFA_VERIFY_FAILURE", "FAILURE", failureCategory, "MFA verification failed");
    }

    public void logMfaLocked(String username, HttpServletRequest request) {
        record(username, request, "MFA_CHALLENGE_LOCKED", "FAILURE", "MAX_MFA_ATTEMPTS", "MFA challenge locked after 3 failed attempts");
    }

    public void logMfaEnrollmentInitiated(String username, HttpServletRequest request) {
        record(username, request, "MFA_ENROLLMENT_INITIATED", "SUCCESS", null, "MFA enrollment started with generated QR code");
    }

    public void logMfaEnrollmentSuccess(String username, HttpServletRequest request) {
        record(username, request, "MFA_ENROLLMENT_SUCCESS", "SUCCESS", null, "MFA enrolled and verified with first valid OTP");
    }

    public void logMfaEnrollmentFailure(String username, HttpServletRequest request, String failureCategory) {
        record(username, request, "MFA_ENROLLMENT_FAILURE", "FAILURE", failureCategory, "Initial MFA enrollment code verification failed");
    }

    public void logMfaEnrollmentLocked(String username, HttpServletRequest request) {
        record(username, request, "MFA_ENROLLMENT_LOCKED", "FAILURE", "MAX_ENROLLMENT_ATTEMPTS", "MFA enrollment locked after 3 failed attempts");
    }

    public void logLogout(String username, HttpServletRequest request) {
        record(username, request, "LOGOUT", "SUCCESS", null, "User logged out securely");
    }

    public void logPasswordResetRequested(String username, HttpServletRequest request) {
        record(username, request, "PASSWORD_RESET_REQUEST", "SUCCESS", null, "Password reset request initiated");
    }

    public void logPasswordResetSuccess(String username, HttpServletRequest request) {
        record(username, request, "PASSWORD_RESET_SUCCESS", "SUCCESS", null, "Password reset completed successfully");
    }

    public void logStaffProvisioned(String adminUsername, HttpServletRequest request, String staffEmail, String role) {
        record(adminUsername, request, "STAFF_PROVISIONED", "SUCCESS", null, "New staff account created: " + staffEmail + " with role: " + role);
    }

    public void logRoleChanged(String adminUsername, HttpServletRequest request, String staffEmail, String newRole) {
        record(adminUsername, request, "ROLE_CHANGED", "SUCCESS", null, "Staff role updated for: " + staffEmail + " to: " + newRole);
    }

    public void logLoanSearch(String username, HttpServletRequest request, String searchType, int resultCount, String branch) {
        record(username, request, "LOAN_SEARCH_PERFORMED", "SUCCESS", null,
                "Loan search type: " + searchType + ", results returned: " + resultCount + ", officer branch: " + branch);
    }

    public void logLoanSearchNotFound(String username, HttpServletRequest request, String searchType) {
        record(username, request, "LOAN_SEARCH_NOT_FOUND", "SUCCESS", null,
                "Account search yielded no authorized matches for type: " + searchType);
    }

    public void logProfileAccessed(String username, HttpServletRequest request, Long loanId, String branch) {
        record(username, request, "CUSTOMER_PROFILE_ACCESSED", "SUCCESS", null,
                "Profile accessed for loanId: " + loanId + " at branch: " + branch);
    }

    public void logProfileAccessDenied(String username, HttpServletRequest request, Long loanId, String reason) {
        record(username, request, "PROFILE_ACCESS_DENIED", "FAILURE", "UNAUTHORIZED_BRANCH_OR_ROLE",
                "Profile access denied for loanId: " + loanId + " - " + reason);
    }

    public void logSensitiveDataRevealed(String username, HttpServletRequest request, Long loanId, String fieldCategory, String purpose, String correlationId) {
        record(username, request, "SENSITIVE_DATA_REVEAL_SUCCESS", "SUCCESS", null,
                "Field category: " + fieldCategory + " revealed for loanId: " + loanId + ", correlationId: " + correlationId + ", purpose: " + purpose);
    }

    public void logSensitiveDataRevealDenied(String username, HttpServletRequest request, Long loanId, String fieldCategory, String reason) {
        record(username, request, "SENSITIVE_DATA_REVEAL_DENIED", "FAILURE", "UNAUTHORIZED_FIELD_ACCESS",
                "Field category: " + fieldCategory + " reveal denied for loanId: " + loanId + " - " + reason);
    }

    public void logLoan360Accessed(String username, HttpServletRequest request, Long loanId, String branch) {
        record(username, request, "LOAN_360_VIEW_ACCESSED", "SUCCESS", null,
                "360 Degree View accessed for loanId: " + loanId + " at branch: " + branch);
    }

    public void logLoan360AccessDenied(String username, HttpServletRequest request, Long loanId, String reason) {
        record(username, request, "LOAN_360_VIEW_DENIED", "FAILURE", "UNAUTHORIZED_BRANCH_OR_ROLE",
                "360 Degree View access denied for loanId: " + loanId + " - " + reason);
    }

    public void logStatementDownloaded(String username, HttpServletRequest request, Long loanId, String branch) {
        record(username, request, "LOAN_STATEMENT_DOWNLOADED", "SUCCESS", null,
                "Loan Statement PDF downloaded for loanId: " + loanId + " at branch: " + branch);
    }

    public void logStatementDownloadDenied(String username, HttpServletRequest request, Long loanId, String reason) {
        record(username, request, "LOAN_STATEMENT_DOWNLOAD_DENIED", "FAILURE", "UNAUTHORIZED_BRANCH_OR_ROLE",
                "Loan Statement PDF download denied for loanId: " + loanId + " - " + reason);
    }

    public void logLoanApplicationApproved(String username, HttpServletRequest request, Long loanId, String branch) {
        record(username, request, "LOAN_APPLICATION_APPROVED", "SUCCESS", null,
                "Loan application approved and activated for loanId: " + loanId + " at branch: " + branch);
    }

    public void logLoanApplicationRejected(String username, HttpServletRequest request, Long loanId, String reason, String branch) {
        record(username, request, "LOAN_APPLICATION_REJECTED", "SUCCESS", null,
                "Loan application rejected for loanId: " + loanId + " at branch: " + branch + " - Reason: " + reason);
    }

    public void logCollectionRecorded(String username, HttpServletRequest request, Long loanId, double amount, String mode, String reference, String branch) {
        record(username, request, "COLLECTION_RECORDED", "SUCCESS", null,
                "Collection of ₹" + amount + " recorded via " + mode + " (Ref: " + reference + ") for loanId: " + loanId + " at branch: " + branch);
    }

    public void logCollectionDenied(String username, HttpServletRequest request, Long loanId, String reason) {
        record(username, request, "COLLECTION_RECORD_DENIED", "FAILURE", "UNAUTHORIZED_OR_INVALID",
                "Collection recording denied for loanId: " + loanId + " - " + reason);
    }

    public void logProfileUpdated(String username, HttpServletRequest request, String details) {
        record(username, request, "PROFILE_UPDATED", "SUCCESS", null, "User profile updated: " + details);
    }

    public void logPasswordChanged(String username, HttpServletRequest request) {
        record(username, request, "PASSWORD_CHANGED", "SUCCESS", null, "User updated their account password via Security Settings");
    }

    public void logNotificationPreferencesUpdated(String username, HttpServletRequest request, String details) {
        record(username, request, "NOTIFICATION_PREFERENCES_UPDATED", "SUCCESS", null, "Notification preferences updated: " + details);
    }

    public void logLoanPolicyUpdated(String username, HttpServletRequest request, String details) {
        record(username, request, "LOAN_POLICY_UPDATED", "SUCCESS", null, "Bank loan underwriting policy updated by admin: " + details);
    }

    public void logSystemSettingsUpdated(String username, HttpServletRequest request, String details) {
        record(username, request, "SYSTEM_SETTINGS_UPDATED", "SUCCESS", null, "Operational system settings updated: " + details);
    }
}
