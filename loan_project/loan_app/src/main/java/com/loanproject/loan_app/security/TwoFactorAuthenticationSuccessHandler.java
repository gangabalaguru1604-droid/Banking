package com.loanproject.loan_app.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.loanproject.loan_app.bean.MfaChallenge;
import com.loanproject.loan_app.bean.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorAuthenticationSuccessHandler.class);

    private final MfaService mfaService;
    private final TotpService totpService;
    private final AesGcmEncryptionService encryptionService;
    private final LoginAttemptService loginAttemptService;
    private final AuthAuditService auditService;

    public TwoFactorAuthenticationSuccessHandler(MfaService mfaService,
                                                 TotpService totpService,
                                                 AesGcmEncryptionService encryptionService,
                                                 LoginAttemptService loginAttemptService,
                                                 AuthAuditService auditService) {
        this.mfaService = mfaService;
        this.totpService = totpService;
        this.encryptionService = encryptionService;
        this.loginAttemptService = loginAttemptService;
        this.auditService = auditService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // 1. Reset failed attempt counter upon successful primary authentication
        loginAttemptService.resetFailedAttempts(user.getEmail());
        auditService.logLoginSuccess(user.getEmail(), request);

        // 2. Clear SecurityContext so protected pages CANNOT be accessed until MFA succeeds
        SecurityContextHolder.clearContext();

        // 3. Cycle session to prevent session fixation while preserving authorized saved request
        HttpSession oldSession = request.getSession(false);
        String savedRedirectUrl = null;
        if (oldSession != null) {
            org.springframework.security.web.savedrequest.SavedRequest savedReq =
                    new org.springframework.security.web.savedrequest.HttpSessionRequestCache().getRequest(request, response);
            if (savedReq != null) {
                savedRedirectUrl = savedReq.getRedirectUrl();
            }
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        if (savedRedirectUrl != null) {
            newSession.setAttribute("SPRING_SECURITY_SAVED_REQUEST_URL", savedRedirectUrl);
        }

        // 4. Evaluate enrollment state consistency
        boolean isEnrolled = user.isMfaEnrolled();
        boolean hasSecret = user.getMfaSecret() != null && !user.getMfaSecret().isBlank();

        // Branch 1: Account claims confirmed enrollment
        if (isEnrolled) {
            if (!hasSecret) {
                // Inconsistency: mfaEnrolled=true but secret is missing
                log.error("DIAGNOSTIC: User {} -> mfaEnrolled=true, hasSecret=false, selectedFlow=INCONSISTENT_STATE, destination=/login?error=true", user.getEmail());
                auditService.record(user.getEmail(), request, "MFA_STATE_INCONSISTENCY", "FAILURE", "MISSING_SECRET",
                        "Account is marked enrolled but stored TOTP secret is missing");
                response.sendRedirect("/login?error=true");
                return;
            }

            // Verify that the stored secret is validly encrypted and decryptable
            boolean secretDecryptable = false;
            try {
                String decrypted = encryptionService.decrypt(user.getMfaSecret());
                secretDecryptable = decrypted != null && !decrypted.isBlank();
            } catch (Exception e) {
                secretDecryptable = false;
            }

            if (!secretDecryptable) {
                // Inconsistency: mfaEnrolled=true but secret cannot be decrypted
                log.error("DIAGNOSTIC: User {} -> mfaEnrolled=true, hasSecret=true, secretDecryptable=false, selectedFlow=INCONSISTENT_STATE, destination=/login?error=true", user.getEmail());
                auditService.record(user.getEmail(), request, "MFA_STATE_INCONSISTENCY", "FAILURE", "CORRUPT_SECRET",
                        "Account is marked enrolled but stored TOTP secret cannot be decrypted");
                response.sendRedirect("/login?error=true");
                return;
            }

            // Consistent enrolled account: issue challenge and route to /mfa-verify
            try {
                MfaChallenge challenge = mfaService.createChallenge(user, user.getMfaType(), request);
                newSession.setAttribute("PENDING_MFA_USER_ID", user.getId());
                newSession.setAttribute("PENDING_MFA_TOKEN", challenge.getChallengeToken());
                newSession.setAttribute("PENDING_MFA_TYPE", challenge.getMfaType());

                log.info("DIAGNOSTIC: User {} -> mfaEnrolled=true, hasSecret=true, selectedFlow=MFA_VERIFY, destination=/mfa-verify", user.getEmail());
                response.sendRedirect("/mfa-verify");
            } catch (Exception e) {
                log.error("Failed to create MFA challenge for user: " + user.getEmail(), e);
                response.sendRedirect("/login?error=true");
            }
            return;
        }

        // Branch 2: Account is NOT enrolled (new, unconfirmed, or reset account)
        if (hasSecret) {
            // Inconsistency: mfaEnrolled=false but secret exists in DB (unconfirmed or stale)
            log.warn("DIAGNOSTIC: User {} -> mfaEnrolled=false, hasSecret=true, selectedFlow=UNCONFIRMED_ENROLL, destination=/mfa-enroll", user.getEmail());
            auditService.record(user.getEmail(), request, "MFA_STATE_INCONSISTENCY", "WARNING", "UNCONFIRMED_SECRET",
                    "Account has unconfirmed secret with mfaEnrolled=false. Directing to enrollment");
        } else {
            log.info("DIAGNOSTIC: User {} -> mfaEnrolled=false, hasSecret=false, selectedFlow=MFA_ENROLL, destination=/mfa-enroll", user.getEmail());
        }

        // Initiate enrollment flow with a unique, cryptographically secure secret
        try {
            String pendingSecret = totpService.generateSecret();
            newSession.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
            newSession.setAttribute("PENDING_MFA_ENROLL_SECRET", pendingSecret);
            newSession.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().plus(Duration.ofMinutes(5)));
            newSession.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 0);

            auditService.logMfaEnrollmentInitiated(user.getEmail(), request);
            response.sendRedirect("/mfa-enroll");
        } catch (Exception e) {
            log.error("Failed to initiate MFA enrollment for user: " + user.getEmail(), e);
            response.sendRedirect("/login?error=true");
        }
    }
}
