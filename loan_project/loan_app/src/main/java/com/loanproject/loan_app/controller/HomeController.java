package com.loanproject.loan_app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Duration;
import java.time.Instant;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.MfaChallenge;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AesGcmEncryptionService;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.security.MfaService;
import com.loanproject.loan_app.security.MfaVerificationResult;
import com.loanproject.loan_app.security.PasswordResetService;
import com.loanproject.loan_app.security.QrCodeService;
import com.loanproject.loan_app.security.TotpService;
import com.loanproject.loan_app.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final UserService userService;
    private final MfaService mfaService;
    private final PasswordResetService passwordResetService;
    private final QrCodeService qrCodeService;
    private final TotpService totpService;
    private final AesGcmEncryptionService encryptionService;
    private final UserRepository userRepository;
    private final AuthAuditService auditService;

    public HomeController(UserService userService,
                          MfaService mfaService,
                          PasswordResetService passwordResetService,
                          QrCodeService qrCodeService,
                          TotpService totpService,
                          AesGcmEncryptionService encryptionService,
                          UserRepository userRepository,
                          AuthAuditService auditService) {
        this.userService = userService;
        this.mfaService = mfaService;
        this.passwordResetService = passwordResetService;
        this.qrCodeService = qrCodeService;
        this.totpService = totpService;
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/jsp-home")
    public String jspHome() {
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "expired", required = false) String expired,
                        @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                        ModelMap model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid credentials or account unavailable.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out securely.");
        }
        if (expired != null) {
            model.addAttribute("expiredMessage", "Your session has expired due to 10 minutes of inactivity. Please log in again.");
        }
        if (resetSuccess != null) {
            model.addAttribute("resetSuccessMessage", "Password has been reset successfully. Please log in with your new password.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String save(User u, ModelMap model) {
        try {
            userService.register(u);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Registration failed. Please verify your details.");
            return "register";
        }
    }

    @GetMapping("/mfa-enroll")
    public String mfaEnrollPage(HttpSession session,
                                HttpServletRequest request,
                                ModelMap model) {
        Long userId = (Long) session.getAttribute("PENDING_MFA_ENROLL_USER_ID");
        String pendingSecret = (String) session.getAttribute("PENDING_MFA_ENROLL_SECRET");
        Instant expiry = (Instant) session.getAttribute("PENDING_MFA_ENROLL_EXPIRY");
        Integer attempts = (Integer) session.getAttribute("PENDING_MFA_ENROLL_ATTEMPTS");

        // 1. Ensure unauthenticated access is strictly blocked
        if (userId == null || pendingSecret == null || expiry == null) {
            return "redirect:/login";
        }

        // 2. Enforce 5-minute challenge expiry
        if (Instant.now().isAfter(expiry)) {
            session.invalidate();
            return "redirect:/login?expired=true";
        }

        // 3. Enforce maximum 3 attempts
        if (attempts != null && attempts >= 3) {
            session.invalidate();
            return "redirect:/login?error=true";
        }

        // 4. Load user account
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.invalidate();
            return "redirect:/login";
        }

        // 5. If account is already enrolled, continue to /mfa-verify without replacing existing enrollment
        if (user.isMfaEnrolled() && user.getMfaSecret() != null && !user.getMfaSecret().isBlank()) {
            session.removeAttribute("PENDING_MFA_ENROLL_USER_ID");
            session.removeAttribute("PENDING_MFA_ENROLL_SECRET");
            session.removeAttribute("PENDING_MFA_ENROLL_EXPIRY");
            session.removeAttribute("PENDING_MFA_ENROLL_ATTEMPTS");

            MfaChallenge challenge = mfaService.createChallenge(user, user.getMfaType(), request);
            session.setAttribute("PENDING_MFA_USER_ID", user.getId());
            session.setAttribute("PENDING_MFA_TOKEN", challenge.getChallengeToken());
            session.setAttribute("PENDING_MFA_TYPE", challenge.getMfaType());
            return "redirect:/mfa-verify";
        }

        // 6. Generate otpauth URI and QR code
        String issuer = "Indian Bank";
        String otpAuthUrl = totpService.getOtpAuthUrl(user.getEmail(), issuer, pendingSecret);
        String qrCodeDataUri = qrCodeService.generateQrCodeDataUri(otpAuthUrl);

        model.addAttribute("qrCodeDataUri", qrCodeDataUri);
        model.addAttribute("secretKey", totpService.formatSecretForDisplay(pendingSecret));
        model.addAttribute("rawSecret", pendingSecret);
        model.addAttribute("accountEmail", user.getEmail());
        model.addAttribute("issuer", issuer);
        model.addAttribute("attemptsRemaining", attempts == null ? 3 : (3 - attempts));

        return "mfa-enroll";
    }

    @PostMapping("/mfa-enroll")
    public String verifyEnrollment(@RequestParam(value = "code", required = false) String code,
                                   HttpSession session,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   ModelMap model) {
        Long userId = (Long) session.getAttribute("PENDING_MFA_ENROLL_USER_ID");
        String pendingSecret = (String) session.getAttribute("PENDING_MFA_ENROLL_SECRET");
        Instant expiry = (Instant) session.getAttribute("PENDING_MFA_ENROLL_EXPIRY");
        Integer attempts = (Integer) session.getAttribute("PENDING_MFA_ENROLL_ATTEMPTS");

        // 1. Ensure valid pending enrollment session exists
        if (userId == null || pendingSecret == null || expiry == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.invalidate();
            return "redirect:/login";
        }

        // 2. Enforce 5-minute challenge expiry
        if (Instant.now().isAfter(expiry)) {
            auditService.logMfaEnrollmentFailure(user.getEmail(), request, "ENROLLMENT_EXPIRED");
            session.invalidate();
            return "redirect:/login?expired=true";
        }

        // 3. Enforce maximum 3 attempts
        int currentAttempts = attempts == null ? 0 : attempts;
        if (currentAttempts >= 3) {
            auditService.logMfaEnrollmentLocked(user.getEmail(), request);
            session.invalidate();
            return "redirect:/login?error=true";
        }

        // 4. Rate-limit rapid submission spam (min 500ms between attempts)
        Instant lastAttempt = (Instant) session.getAttribute("PENDING_MFA_ENROLL_LAST_ATTEMPT");
        if (lastAttempt != null && Duration.between(lastAttempt, Instant.now()).toMillis() < 500) {
            String issuer = "Indian Bank";
            String otpAuthUrl = totpService.getOtpAuthUrl(user.getEmail(), issuer, pendingSecret);
            String qrCodeDataUri = qrCodeService.generateQrCodeDataUri(otpAuthUrl);

            model.addAttribute("errorMessage", "Please wait a moment before submitting another code.");
            model.addAttribute("attemptsRemaining", 3 - currentAttempts);
            model.addAttribute("qrCodeDataUri", qrCodeDataUri);
            model.addAttribute("secretKey", totpService.formatSecretForDisplay(pendingSecret));
            model.addAttribute("rawSecret", pendingSecret);
            model.addAttribute("accountEmail", user.getEmail());
            model.addAttribute("issuer", issuer);
            return "mfa-enroll";
        }
        session.setAttribute("PENDING_MFA_ENROLL_LAST_ATTEMPT", Instant.now());

        // 5. Verify code with TotpService
        boolean isValid = totpService.verifyCode(pendingSecret, code != null ? code.trim() : "");

        if (isValid) {
            // Success: Encrypt TOTP secret with AES-GCM and persist with mfaEnrolled = true
            String encryptedSecret = encryptionService.encrypt(pendingSecret);
            user.setMfaSecret(encryptedSecret);
            user.setMfaEnrolled(true);
            userRepository.save(user);

            auditService.logMfaEnrollmentSuccess(user.getEmail(), request);

            // Clean up temporary enrollment attributes
            session.removeAttribute("PENDING_MFA_ENROLL_USER_ID");
            session.removeAttribute("PENDING_MFA_ENROLL_SECRET");
            session.removeAttribute("PENDING_MFA_ENROLL_EXPIRY");
            session.removeAttribute("PENDING_MFA_ENROLL_ATTEMPTS");
            session.removeAttribute("PENDING_MFA_ENROLL_LAST_ATTEMPT");

            // Establish fully authenticated Spring Security context
            CustomUserDetails userDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            new HttpSessionSecurityContextRepository().saveContext(context, request, response);

            // Backward compatibility for existing JSP views
            session.setAttribute("user", user);

            log.info("MFA enrollment completed successfully for user {}. SecurityContext established.", user.getEmail());

            return getPostLoginRedirect(session, user);
        } else {
            // Failed verification: increment attempts
            currentAttempts++;
            session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", currentAttempts);

            if (currentAttempts >= 3) {
                auditService.logMfaEnrollmentLocked(user.getEmail(), request);
                session.invalidate();
                return "redirect:/login?error=true";
            }

            int remaining = 3 - currentAttempts;
            auditService.logMfaEnrollmentFailure(user.getEmail(), request, "INVALID_OTP");

            // Re-populate view with QR code and setup instructions
            String issuer = "Indian Bank";
            String otpAuthUrl = totpService.getOtpAuthUrl(user.getEmail(), issuer, pendingSecret);
            String qrCodeDataUri = qrCodeService.generateQrCodeDataUri(otpAuthUrl);

            model.addAttribute("errorMessage", "Invalid 6-digit authentication code. " + remaining + " attempts remaining.");
            model.addAttribute("attemptsRemaining", remaining);
            model.addAttribute("qrCodeDataUri", qrCodeDataUri);
            model.addAttribute("secretKey", totpService.formatSecretForDisplay(pendingSecret));
            model.addAttribute("rawSecret", pendingSecret);
            model.addAttribute("accountEmail", user.getEmail());
            model.addAttribute("issuer", issuer);

            return "mfa-enroll";
        }
    }

    @GetMapping("/mfa-verify")
    public String mfaPage(HttpSession session, ModelMap model) {
        String token = (String) session.getAttribute("PENDING_MFA_TOKEN");
        Long userId = (Long) session.getAttribute("PENDING_MFA_USER_ID");

        if (token == null || userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("mfaType", session.getAttribute("PENDING_MFA_TYPE"));
        return "mfa";
    }

    @PostMapping("/mfa-verify")
    public String verifyMfa(@RequestParam(value = "code", required = false) String code,
                            HttpSession session,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            ModelMap model) {
        String token = (String) session.getAttribute("PENDING_MFA_TOKEN");
        Long userId = (Long) session.getAttribute("PENDING_MFA_USER_ID");

        if (token == null || userId == null) {
            return "redirect:/login";
        }

        MfaVerificationResult result = mfaService.verifyChallenge(token, code, request);

        if (result.isSuccess()) {
            User user = result.getUser();

            // Establish fully authenticated Spring Security context
            CustomUserDetails userDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            new HttpSessionSecurityContextRepository().saveContext(context, request, response);

            // Backward compatibility for existing JSP pages
            session.setAttribute("user", user);

            // Clean up temporary MFA session attributes
            session.removeAttribute("PENDING_MFA_USER_ID");
            session.removeAttribute("PENDING_MFA_TOKEN");
            session.removeAttribute("PENDING_MFA_TYPE");

            log.info("MFA completed successfully for user {}. SecurityContext established.", user.getEmail());

            return getPostLoginRedirect(session, user);
        } else {
            if ("CHALLENGE_LOCKED".equals(result.getErrorCode()) || "CHALLENGE_ALREADY_USED".equals(result.getErrorCode()) || "CHALLENGE_EXPIRED".equals(result.getErrorCode())) {
                session.invalidate();
                return "redirect:/login?error=true";
            }

            model.addAttribute("errorMessage", result.getErrorMessage());
            model.addAttribute("attemptsRemaining", result.getAttemptsRemaining());
            model.addAttribute("mfaType", session.getAttribute("PENDING_MFA_TYPE"));
            return "mfa";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       HttpServletRequest request,
                                       ModelMap model) {
        String message = passwordResetService.requestReset(email, request);
        model.addAttribute("infoMessage", message);
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                    ModelMap model) {
        if (token == null || !passwordResetService.validateToken(token)) {
            model.addAttribute("errorMessage", "The password reset link is invalid or has expired.");
            return "forgot-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      HttpServletRequest request,
                                      ModelMap model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "reset-password";
        }

        try {
            boolean success = passwordResetService.resetPassword(token, password, request);
            if (success) {
                // Invalidate session after password reset
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                return "redirect:/login?resetSuccess=true";
            } else {
                model.addAttribute("errorMessage", "Password reset token is invalid, expired, or already used.");
                return "forgot-password";
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", e.getMessage());
            return "reset-password";
        }
    }

    /**
     * Determines the authoritative post-login / post-MFA redirect URL.
     * Preserves deep-linked destinations when authorized, and routes all roles to their designated dashboard.
     */
    private String getPostLoginRedirect(HttpSession session, User user) {
        String savedUrl = (String) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST_URL");
        session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST_URL");

        if (savedUrl != null && !savedUrl.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(savedUrl);
                String path = uri.getPath();
                if (path != null && !path.startsWith("/login") && !path.startsWith("/mfa")
                        && !path.startsWith("/logout") && !path.equals("/")) {
                    // RBAC check on deep-link destination
                    if (path.startsWith("/admin") && user.getRole() != AppRole.ROLE_ADMIN) {
                        return "redirect:/dashboard";
                    }
                    if (path.startsWith("/pay") && user.getRole() != AppRole.ROLE_CUSTOMER) {
                        return "redirect:/dashboard";
                    }
                    return "redirect:" + path + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
                }
            } catch (Exception e) {
                log.warn("Invalid saved request URL format: {}", savedUrl);
            }
        }

        // Standard landing page for all authenticated roles:
        // ROLE_ADMIN -> Admin Dashboard / Staff Loan Portfolio
        // ROLE_LOAN_OFFICER -> Loan Officer Dashboard
        // ROLE_COLLECTIONS_AGENT -> Collections Agent Dashboard
        // ROLE_COLLECTIONS_MANAGER -> Collections Manager Dashboard
        // ROLE_CUSTOMER -> Customer Dashboard
        return "redirect:/dashboard";
    }
}
