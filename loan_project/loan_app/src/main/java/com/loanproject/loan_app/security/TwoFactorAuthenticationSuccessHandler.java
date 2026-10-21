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
    private final com.loanproject.loan_app.repository.UserRepository userRepository;

    public TwoFactorAuthenticationSuccessHandler(MfaService mfaService,
                                                 TotpService totpService,
                                                 AesGcmEncryptionService encryptionService,
                                                 LoginAttemptService loginAttemptService,
                                                 AuthAuditService auditService,
                                                 com.loanproject.loan_app.repository.UserRepository userRepository) {
        this.mfaService = mfaService;
        this.totpService = totpService;
        this.encryptionService = encryptionService;
        this.loginAttemptService = loginAttemptService;
        this.auditService = auditService;
        this.userRepository = userRepository;
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

        // 4. Route by trusted backend account role and preferred MFA type
        if (mfaService.isCustomerUser(user) && "DYNAMIC_EMAIL".equalsIgnoreCase(user.getMfaType()) && user.isMfaEnrolled()) {
            // Dynamic email OTP flow for accounts explicitly configured for email OTP
            try {
                MfaChallenge challenge = mfaService.createCustomerChallenge(user, request);
                newSession.setAttribute("PENDING_MFA_USER_ID", user.getId());
                newSession.setAttribute("PENDING_MFA_TOKEN", challenge.getChallengeToken());
                newSession.setAttribute("PENDING_MFA_TYPE", challenge.getMfaType());

                log.info("DIAGNOSTIC: Customer User {} -> dynamicMfa=true, selectedFlow=CUSTOMER_MFA_VERIFY, destination=/mfa-verify", user.getEmail());
                response.sendRedirect("/mfa-verify");
            } catch (OtpDeliveryException e) {
                log.error("Customer dynamic OTP delivery failed for user {}: {}", DataMaskingUtils.maskEmail(user.getEmail()), e.getMessage());
                auditService.record(user.getEmail(), request, "CUSTOMER_MFA_DELIVERY_FAILED", "FAILURE", "DELIVERY_ERROR",
                        "Failed to deliver dynamic OTP to customer verification channel");
                // Do not bypass customer MFA if delivery fails; show error and allow safe retry
                response.sendRedirect("/login?mfaDeliveryError=true");
            } catch (Exception e) {
                log.error("Failed to create customer MFA challenge for user: " + user.getEmail(), e);
                response.sendRedirect("/login?error=true");
            }
            return;
        }

        // Authenticator App (TOTP with QR code scan) & Staff Flow
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
                // Inconsistency: mfaEnrolled=true but secret cannot be decrypted under current cryptographic key
                log.error("DIAGNOSTIC: User {} -> mfaEnrolled=true, hasSecret=true, secretDecryptable=false, selectedFlow=INCONSISTENT_STATE, destination=/login?error=true", user.getEmail());
                auditService.record(user.getEmail(), request, "MFA_STATE_INCONSISTENCY", "FAILURE", "CORRUPT_SECRET",
                        "Stored MFA secret cannot be decrypted under current cryptographic key");
                response.sendRedirect("/login?error=true");
                return;
            }

            // Consistent enrolled account: issue challenge and route to /mfa-verify
            try {
                MfaChallenge challenge = (mfaService.isStaffUser(user) && mfaService.isStaffStaticOtpEnabled())
                        ? mfaService.createStaffChallenge(user, request)
                        : mfaService.createChallenge(user, user.getMfaType() != null ? user.getMfaType() : "TOTP", request);

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

        redirectToEnrollment(user, request, response, newSession);
    }

    private void redirectToEnrollment(User user,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      HttpSession session) throws IOException {
        try {
            String pendingSecret = totpService.generateSecret();
            session.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
            session.setAttribute("PENDING_MFA_ENROLL_SECRET", pendingSecret);
            session.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().plus(Duration.ofMinutes(5)));
            session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 0);

            auditService.logMfaEnrollmentInitiated(user.getEmail(), request);
            response.sendRedirect("/mfa-enroll");
        } catch (Exception e) {
            log.error("Failed to initiate MFA enrollment for user: " + user.getEmail(), e);
            response.sendRedirect("/login?error=true");
        }
    }
}
