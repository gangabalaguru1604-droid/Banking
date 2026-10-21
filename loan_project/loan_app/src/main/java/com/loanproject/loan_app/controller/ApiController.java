package com.loanproject.loan_app.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import com.loanproject.loan_app.bean.*;
import com.loanproject.loan_app.dto.*;
import com.loanproject.loan_app.repository.*;
import com.loanproject.loan_app.security.*;
import com.loanproject.loan_app.services.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final UserService userService;
    private final LoanService loanService;
    private final MfaService mfaService;
    private final PasswordResetService passwordResetService;
    private final QrCodeService qrCodeService;
    private final TotpService totpService;
    private final AesGcmEncryptionService encryptionService;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RepaymentInstallmentRepository installmentRepository;
    private final AuthAuditLogRepository auditRepo;
    private final AuthAuditService auditService;
    private final LoanSearchService loanSearchService;
    private final LoanAccountDetailsService loanAccountDetailsService;

    @Autowired(required = false)
    private LoanReminderLogRepository reminderLogRepository;

    @Autowired(required = false)
    private DpdReminderScheduler reminderScheduler;

    @Autowired(required = false)
    private DpdReminderService reminderService;

    @Autowired(required = false)
    private DefaultEmailReminderProvider emailReminderProvider;

    @Autowired(required = false)
    private LoginAttemptService loginAttemptService;

    @Autowired(required = false)
    private AuthenticationManager authenticationManager;

    public ApiController(UserService userService,
                         LoanService loanService,
                         MfaService mfaService,
                         PasswordResetService passwordResetService,
                         QrCodeService qrCodeService,
                         TotpService totpService,
                         AesGcmEncryptionService encryptionService,
                         UserRepository userRepository,
                         LoanRepository loanRepository,
                         PaymentTransactionRepository paymentTransactionRepository,
                         RepaymentInstallmentRepository installmentRepository,
                         AuthAuditLogRepository auditRepo,
                         AuthAuditService auditService,
                         LoanSearchService loanSearchService,
                         LoanAccountDetailsService loanAccountDetailsService) {
        this.userService = userService;
        this.loanService = loanService;
        this.mfaService = mfaService;
        this.passwordResetService = passwordResetService;
        this.qrCodeService = qrCodeService;
        this.totpService = totpService;
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.installmentRepository = installmentRepository;
        this.auditRepo = auditRepo;
        this.auditService = auditService;
        this.loanSearchService = loanSearchService;
        this.loanAccountDetailsService = loanAccountDetailsService;
    }

    private User getAuthenticatedUser(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUser().getEmail())
                    .orElse(userDetails.getUser());
        }
        User u = (User) session.getAttribute("user");
        if (u != null) {
            return userRepository.findByEmail(u.getEmail()).orElse(u);
        }
        throw new AccessDeniedException("User not authenticated or MFA not completed.");
    }

    // ==========================================
    // 1. AUTH & MFA REST ENDPOINTS
    // ==========================================

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "email", required = false) String paramEmail,
            @RequestParam(value = "password", required = false) String paramPassword,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {

        String email = body != null && body.containsKey("email") ? body.get("email") : paramEmail;
        String password = body != null && body.containsKey("password") ? body.get("password") : paramPassword;

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required."));
        }

        email = email.trim();

        if (loginAttemptService != null && loginAttemptService.isAccountLocked(email)) {
            auditService.logLoginFailure(email, request, "ACCOUNT_LOCKED");
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("error", "Account is temporarily locked due to multiple failed login attempts. Please contact your administrator."));
        }

        try {
            Authentication auth;
            if (authenticationManager != null) {
                auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AuthenticationManager not available"));
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            User user = userDetails.getUser();

            if (loginAttemptService != null) {
                loginAttemptService.resetFailedAttempts(user.getEmail());
            }
            auditService.logLoginSuccess(user.getEmail(), request);
            SecurityContextHolder.clearContext();

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession newSession = request.getSession(true);

            boolean isEnrolled = user.isMfaEnrolled();
            boolean hasSecret = user.getMfaSecret() != null && !user.getMfaSecret().isBlank();

            if (isEnrolled && hasSecret) {
                boolean secretDecryptable = false;
                try {
                    String decrypted = encryptionService.decrypt(user.getMfaSecret());
                    secretDecryptable = decrypted != null && !decrypted.isBlank();
                } catch (Exception e) {
                    secretDecryptable = false;
                }

                if (secretDecryptable) {
                    MfaChallenge challenge = mfaService.createChallenge(user, user.getMfaType(), request);
                    newSession.setAttribute("PENDING_MFA_USER_ID", user.getId());
                    newSession.setAttribute("PENDING_MFA_TOKEN", challenge.getChallengeToken());
                    newSession.setAttribute("PENDING_MFA_TYPE", challenge.getMfaType());

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "mfaRequired", true,
                            "flow", "mfa-verify",
                            "redirect", "/mfa-verify"
                    ));
                }
            }

            // Unenrolled account -> initiate enrollment
            String pendingSecret = totpService.generateSecret();
            newSession.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
            newSession.setAttribute("PENDING_MFA_ENROLL_SECRET", pendingSecret);
            newSession.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().plus(Duration.ofMinutes(5)));
            newSession.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 0);

            auditService.logMfaEnrollmentInitiated(user.getEmail(), request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mfaRequired", true,
                    "flow", "mfa-enroll",
                    "redirect", "/mfa-enroll"
            ));

        } catch (org.springframework.security.core.AuthenticationException e) {
            if (loginAttemptService != null) {
                loginAttemptService.recordFailedAttempt(email, request);
            } else {
                auditService.logLoginFailure(email, request, "BAD_CREDENTIALS");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password."));
        } catch (Exception e) {
            log.error("Login processing error for " + email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred during login."));
        }
    }

    @GetMapping("/auth/mfa-verify-data")
    public ResponseEntity<Map<String, Object>> getMfaVerifyData(HttpSession session) {
        Long userId = (Long) session.getAttribute("PENDING_MFA_USER_ID");
        String token = (String) session.getAttribute("PENDING_MFA_TOKEN");
        if (userId == null || token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No pending MFA verification session found. Please log in first."));
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found."));
        }
        return ResponseEntity.ok(Map.of(
                "accountEmail", user.getEmail(),
                "mfaType", session.getAttribute("PENDING_MFA_TYPE") != null ? session.getAttribute("PENDING_MFA_TYPE") : "TOTP"
        ));
    }

    @GetMapping("/auth/mfa-enroll-data")
    public ResponseEntity<Map<String, Object>> getMfaEnrollData(HttpSession session, HttpServletRequest request) {
        Long userId = (Long) session.getAttribute("PENDING_MFA_ENROLL_USER_ID");
        String pendingSecret = (String) session.getAttribute("PENDING_MFA_ENROLL_SECRET");
        Instant expiry = (Instant) session.getAttribute("PENDING_MFA_ENROLL_EXPIRY");
        Integer attempts = (Integer) session.getAttribute("PENDING_MFA_ENROLL_ATTEMPTS");

        if (userId == null || pendingSecret == null || expiry == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No pending MFA enrollment session found. Please log in first."));
        }

        if (Instant.now().isAfter(expiry)) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "MFA enrollment session has expired. Please log in again."));
        }

        if (attempts != null && attempts >= 3) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Maximum MFA enrollment attempts exceeded."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found."));
        }

        String issuer = "Bank of India";
        String otpAuthUrl = totpService.getOtpAuthUrl(user.getEmail(), issuer, pendingSecret);
        String qrCodeDataUri = qrCodeService.generateQrCodeDataUri(otpAuthUrl);

        long secondsRemaining = Math.max(0, Duration.between(Instant.now(), expiry).getSeconds());

        Map<String, Object> data = new HashMap<>();
        data.put("qrCodeDataUri", qrCodeDataUri);
        data.put("secretKey", totpService.formatSecretForDisplay(pendingSecret));
        data.put("rawSecret", pendingSecret);
        data.put("accountEmail", user.getEmail());
        data.put("issuer", issuer);
        data.put("attemptsRemaining", attempts == null ? 3 : (3 - attempts));
        data.put("secondsRemaining", secondsRemaining);

        return ResponseEntity.ok(data);
    }

    @PostMapping("/auth/mfa-enroll")
    public ResponseEntity<Map<String, Object>> verifyMfaEnrollment(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "code", required = false) String paramCode,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {

        String code = body != null && body.containsKey("code") ? body.get("code") : paramCode;
        Long userId = (Long) session.getAttribute("PENDING_MFA_ENROLL_USER_ID");
        String pendingSecret = (String) session.getAttribute("PENDING_MFA_ENROLL_SECRET");
        Instant expiry = (Instant) session.getAttribute("PENDING_MFA_ENROLL_EXPIRY");
        Integer attempts = (Integer) session.getAttribute("PENDING_MFA_ENROLL_ATTEMPTS");

        if (userId == null || pendingSecret == null || expiry == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired or invalid. Please log in again."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found."));
        }

        if (Instant.now().isAfter(expiry)) {
            auditService.logMfaEnrollmentFailure(user.getEmail(), request, "ENROLLMENT_EXPIRED");
            session.invalidate();
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "Enrollment window expired. Please log in again."));
        }

        int currentAttempts = attempts == null ? 0 : attempts;
        if (currentAttempts >= 3) {
            auditService.logMfaEnrollmentLocked(user.getEmail(), request);
            session.invalidate();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed attempts. Account locked temporarily."));
        }

        Instant lastAttempt = (Instant) session.getAttribute("PENDING_MFA_ENROLL_LAST_ATTEMPT");
        if (lastAttempt != null && Duration.between(lastAttempt, Instant.now()).toMillis() < 500) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Please wait a moment before submitting another code."));
        }
        session.setAttribute("PENDING_MFA_ENROLL_LAST_ATTEMPT", Instant.now());

        boolean isValid = totpService.verifyCode(pendingSecret, code != null ? code.trim() : "");

        if (isValid) {
            String encryptedSecret = encryptionService.encrypt(pendingSecret);
            user.setMfaSecret(encryptedSecret);
            user.setMfaEnrolled(true);
            userRepository.save(user);

            auditService.logMfaEnrollmentSuccess(user.getEmail(), request);

            session.removeAttribute("PENDING_MFA_ENROLL_USER_ID");
            session.removeAttribute("PENDING_MFA_ENROLL_SECRET");
            session.removeAttribute("PENDING_MFA_ENROLL_EXPIRY");
            session.removeAttribute("PENDING_MFA_ENROLL_ATTEMPTS");
            session.removeAttribute("PENDING_MFA_ENROLL_LAST_ATTEMPT");

            CustomUserDetails userDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            session.setAttribute("user", user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MFA enrollment completed successfully.",
                    "redirect", "/dashboard"
            ));
        } else {
            currentAttempts++;
            session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", currentAttempts);
            if (currentAttempts >= 3) {
                auditService.logMfaEnrollmentLocked(user.getEmail(), request);
                session.invalidate();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Maximum attempts exceeded. Please login again."));
            }
            int remaining = 3 - currentAttempts;
            auditService.logMfaEnrollmentFailure(user.getEmail(), request, "INVALID_OTP");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Invalid 6-digit authentication code.",
                            "attemptsRemaining", remaining
                    ));
        }
    }

    @PostMapping("/auth/mfa-verify")
    public ResponseEntity<Map<String, Object>> verifyMfaChallenge(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "code", required = false) String paramCode,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {

        String code = body != null && body.containsKey("code") ? body.get("code") : paramCode;
        String token = (String) session.getAttribute("PENDING_MFA_TOKEN");
        Long userId = (Long) session.getAttribute("PENDING_MFA_USER_ID");

        if (token == null || userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No pending MFA challenge found. Please log in first."));
        }

        MfaVerificationResult result = mfaService.verifyChallenge(token, code, request);

        if (result.isSuccess()) {
            User user = result.getUser();

            CustomUserDetails userDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);

            session.setAttribute("user", user);
            session.removeAttribute("PENDING_MFA_USER_ID");
            session.removeAttribute("PENDING_MFA_TOKEN");
            session.removeAttribute("PENDING_MFA_TYPE");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Two-factor authentication successful.",
                    "redirect", "/dashboard"
            ));
        } else {
            if ("CHALLENGE_LOCKED".equals(result.getErrorCode())
                    || "CHALLENGE_ALREADY_USED".equals(result.getErrorCode())
                    || "CHALLENGE_EXPIRED".equals(result.getErrorCode())) {
                session.invalidate();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", result.getErrorMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", result.getErrorMessage(),
                            "attemptsRemaining", result.getAttemptsRemaining()
                    ));
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "name", required = false) String paramName,
            @RequestParam(value = "email", required = false) String paramEmail,
            @RequestParam(value = "password", required = false) String paramPassword) {

        String name = body != null && body.containsKey("name") ? body.get("name") : paramName;
        String email = body != null && body.containsKey("email") ? body.get("email") : paramEmail;
        String password = body != null && body.containsKey("password") ? body.get("password") : paramPassword;

        if (name == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email, and password are required."));
        }

        User u = new User();
        u.setName(name.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPassword(password);

        try {
            userService.register(u);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Customer account registered successfully. Please proceed to login."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed. Please verify your details."));
        }
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "email", required = false) String paramEmail,
            HttpServletRequest request) {

        String email = body != null && body.containsKey("email") ? body.get("email") : paramEmail;
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        }

        String message = passwordResetService.requestReset(email.trim(), request);
        return ResponseEntity.ok(Map.of("success", true, "message", message));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "token", required = false) String paramToken,
            @RequestParam(value = "password", required = false) String paramPassword,
            @RequestParam(value = "confirmPassword", required = false) String paramConfirmPassword,
            HttpServletRequest request) {

        String token = body != null && body.containsKey("token") ? body.get("token") : paramToken;
        String password = body != null && body.containsKey("password") ? body.get("password") : paramPassword;
        String confirmPassword = body != null && body.containsKey("confirmPassword") ? body.get("confirmPassword") : paramConfirmPassword;

        if (token == null || password == null || confirmPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token, password, and confirm password are required."));
        }

        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match."));
        }

        try {
            boolean success = passwordResetService.resetPassword(token, password, request);
            if (success) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                return ResponseEntity.ok(Map.of("success", true, "message", "Password has been reset successfully. Please log in with your new password."));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Password reset token is invalid, expired, or already used."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 2. DASHBOARD DATA REST ENDPOINT
    // ==========================================

    @GetMapping("/dashboard/data")
    public ResponseEntity<Map<String, Object>> getDashboardData(HttpSession session) {
        User u = getAuthenticatedUser(session);
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", u.getId());
        userInfo.put("name", u.getName());
        userInfo.put("email", u.getEmail());
        userInfo.put("role", u.getRole() != null ? u.getRole().name() : "");
        userInfo.put("branch", u.getBranch() != null ? u.getBranch() : "MAIN");
        userInfo.put("customerId", u.getCustomerId() != null ? u.getCustomerId() : "CUST" + u.getId());
        userInfo.put("phoneNumber", u.getPhoneNumber());
        data.put("currentUser", userInfo);

        if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            data.put("isCustomer", true);
            data.put("isStaff", false);

            List<Loan> customerLoans = loanService.get(u);
            data.put("loans", customerLoans);

            double totalPrincipal = customerLoans.stream().mapToDouble(Loan::getAmount).sum();
            double totalRemaining = customerLoans.stream().mapToDouble(Loan::getRemaining).sum();
            double totalEmi = customerLoans.stream().mapToDouble(Loan::getEmi).sum();
            int totalLoanCount = customerLoans.size();

            int activeLoanCount = (int) customerLoans.stream()
                    .filter(l -> !"CLOSED".equalsIgnoreCase(l.getStatus())
                            && !"REJECTED".equalsIgnoreCase(l.getStatus())
                            && !"PENDING".equalsIgnoreCase(l.getStatus())
                            && !"SUBMITTED".equalsIgnoreCase(l.getStatus())
                            && !"IN_REVIEW".equalsIgnoreCase(l.getStatus()))
                    .count();

            int pendingLoanCount = (int) customerLoans.stream()
                    .filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())
                            || "SUBMITTED".equalsIgnoreCase(l.getStatus())
                            || "IN_REVIEW".equalsIgnoreCase(l.getStatus())
                            || "APPLIED".equalsIgnoreCase(l.getStatus()))
                    .count();

            BigDecimal outstandingBalance = customerLoans.stream()
                    .map(l -> BigDecimal.valueOf(l.getRemaining()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            data.put("totalPrincipal", totalPrincipal);
            data.put("totalRemaining", totalRemaining);
            data.put("outstandingBalance", outstandingBalance);
            data.put("totalEmi", totalEmi);
            data.put("activeLoanCount", activeLoanCount);
            data.put("pendingLoanCount", pendingLoanCount);
            data.put("totalLoanCount", totalLoanCount);

            List<CustomerActivityDto> activities = getCustomerRecentActivities(customerLoans);
            data.put("recentActivities", activities);

        } else {
            data.put("isCustomer", false);
            data.put("isStaff", true);
            data.put("userRole", u.getRole().name());

            String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                    ? "ALL"
                    : (u.getBranch() != null ? u.getBranch() : "MAIN");
            data.put("officerBranch", branchFilter);

            List<Loan> rawBranchLoans = loanService.getAll().stream()
                    .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                    .toList();

            List<StaffDashboardLoanDto> staffLoans = rawBranchLoans.stream()
                    .map(this::mapToStaffLoanDto)
                    .toList();

            double totalPrincipal = staffLoans.stream().mapToDouble(StaffDashboardLoanDto::getAmount).sum();
            double totalRemaining = staffLoans.stream().mapToDouble(StaffDashboardLoanDto::getRemaining).sum();
            double totalOverdueAmount = staffLoans.stream().mapToDouble(StaffDashboardLoanDto::getOverdueAmount).sum();

            int totalLoansCount = staffLoans.size();
            int activeLoansCount = (int) staffLoans.stream().filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus())).count();
            int closedLoansCount = (int) staffLoans.stream().filter(l -> "CLOSED".equalsIgnoreCase(l.getStatus())).count();
            int pendingLoansCount = (int) staffLoans.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())
                    || "SUBMITTED".equalsIgnoreCase(l.getStatus())
                    || "IN_REVIEW".equalsIgnoreCase(l.getStatus())
                    || "APPLIED".equalsIgnoreCase(l.getStatus())).count();
            int overdueLoansCount = (int) staffLoans.stream().filter(l -> "OVERDUE".equalsIgnoreCase(l.getRepaymentStatus()) || l.getDpd() > 0).count();

            long dpdCurrentCount = staffLoans.stream().filter(l -> l.getDpd() == 0 && !"CLOSED".equalsIgnoreCase(l.getStatus()) && !"REJECTED".equalsIgnoreCase(l.getStatus())).count();
            double dpdCurrentAmount = staffLoans.stream().filter(l -> l.getDpd() == 0 && !"CLOSED".equalsIgnoreCase(l.getStatus()) && !"REJECTED".equalsIgnoreCase(l.getStatus())).mapToDouble(StaffDashboardLoanDto::getRemaining).sum();

            long dpdSma0Count = staffLoans.stream().filter(l -> l.getDpd() >= 1 && l.getDpd() <= 30).count();
            double dpdSma0Amount = staffLoans.stream().filter(l -> l.getDpd() >= 1 && l.getDpd() <= 30).mapToDouble(StaffDashboardLoanDto::getOverdueAmount).sum();

            long dpdSma1Count = staffLoans.stream().filter(l -> l.getDpd() >= 31 && l.getDpd() <= 60).count();
            double dpdSma1Amount = staffLoans.stream().filter(l -> l.getDpd() >= 31 && l.getDpd() <= 60).mapToDouble(StaffDashboardLoanDto::getOverdueAmount).sum();

            long dpdSma2Count = staffLoans.stream().filter(l -> l.getDpd() >= 61 && l.getDpd() <= 90).count();
            double dpdSma2Amount = staffLoans.stream().filter(l -> l.getDpd() >= 61 && l.getDpd() <= 90).mapToDouble(StaffDashboardLoanDto::getOverdueAmount).sum();

            long dpdNpaCount = staffLoans.stream().filter(l -> l.getDpd() > 90).count();
            double dpdNpaAmount = staffLoans.stream().filter(l -> l.getDpd() > 90).mapToDouble(StaffDashboardLoanDto::getOverdueAmount).sum();

            double parRate = totalRemaining > 0 ? (totalOverdueAmount / totalRemaining) * 100.0 : 0.0;

            List<StaffDashboardLoanDto> pendingLoans = staffLoans.stream()
                    .filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())
                            || "SUBMITTED".equalsIgnoreCase(l.getStatus())
                            || "IN_REVIEW".equalsIgnoreCase(l.getStatus())
                            || "APPLIED".equalsIgnoreCase(l.getStatus()))
                    .toList();

            List<StaffDashboardLoanDto> delinquentLoans = staffLoans.stream()
                    .filter(l -> ("OVERDUE".equalsIgnoreCase(l.getRepaymentStatus()) || l.getDpd() > 0) && !"CLOSED".equalsIgnoreCase(l.getStatus()))
                    .toList();

            List<StaffDashboardLoanDto> activeLoans = staffLoans.stream()
                    .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()))
                    .toList();

            List<PaymentTransaction> recentCollections = Collections.emptyList();
            if (!rawBranchLoans.isEmpty()) {
                try {
                    recentCollections = paymentTransactionRepository.findByLoanInOrderByPaymentDateDesc(rawBranchLoans).stream()
                            .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                            .limit(10)
                            .toList();
                } catch (Exception ignored) {}
            }

            data.put("loans", staffLoans);
            data.put("pendingLoans", pendingLoans);
            data.put("delinquentLoans", delinquentLoans);
            data.put("activeLoans", activeLoans);
            data.put("recentCollections", recentCollections);

            data.put("totalPrincipal", totalPrincipal);
            data.put("totalRemaining", totalRemaining);
            data.put("totalOverdueAmount", totalOverdueAmount);
            data.put("totalLoansCount", totalLoansCount);
            data.put("activeLoansCount", activeLoansCount);
            data.put("closedLoansCount", closedLoansCount);
            data.put("pendingLoansCount", pendingLoansCount);
            data.put("overdueLoansCount", overdueLoansCount);

            data.put("dpdCurrentCount", dpdCurrentCount);
            data.put("dpdCurrentAmount", dpdCurrentAmount);
            data.put("dpdSma0Count", dpdSma0Count);
            data.put("dpdSma0Amount", dpdSma0Amount);
            data.put("dpdSma1Count", dpdSma1Count);
            data.put("dpdSma1Amount", dpdSma1Amount);
            data.put("dpdSma2Count", dpdSma2Count);
            data.put("dpdSma2Amount", dpdSma2Amount);
            data.put("dpdNpaCount", dpdNpaCount);
            data.put("dpdNpaAmount", dpdNpaAmount);
            data.put("parRate", Math.round(parRate * 100.0) / 100.0);
        }

        return ResponseEntity.ok(data);
    }

    // ==========================================
    // 3. LOAN ORIGINATION & REPAYMENT REST ENDPOINTS
    // ==========================================

    @GetMapping("/applyLoan/terms")
    public ResponseEntity<Map<String, Object>> getLoanTerms(HttpSession session) {
        User u = getAuthenticatedUser(session);
        return ResponseEntity.ok(Map.of(
                "defaultInterestRate", loanService.getDefaultInterestRate(),
                "defaultTenureMonths", loanService.getDefaultTenureMonths(),
                "minLoanAmount", loanService.getMinAmount(),
                "maxLoanAmount", loanService.getMaxAmount(),
                "userRole", u.getRole().name()
        ));
    }

    @PostMapping("/applyLoan")
    public ResponseEntity<Map<String, Object>> applyLoan(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "amount", required = false) Double paramAmount,
            @RequestParam(value = "interest", required = false) Double paramInterest,
            @RequestParam(value = "months", required = false) Integer paramMonths,
            @RequestParam(value = "customerEmail", required = false) String paramCustomerEmail,
            HttpSession session) {

        User u = getAuthenticatedUser(session);

        if (u.getRole() == AppRole.ROLE_COLLECTIONS_AGENT || u.getRole() == AppRole.ROLE_COLLECTIONS_MANAGER) {
            throw new AccessDeniedException("Collections staff are not authorized to originate loans.");
        }

        double amount = 0;
        if (body != null && body.containsKey("amount")) {
            amount = Double.parseDouble(String.valueOf(body.get("amount")));
        } else if (paramAmount != null) {
            amount = paramAmount;
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Loan amount is required."));
        }

        loanService.validateLoanAmount(amount);

        User targetUser;
        double effectiveInterest = loanService.getDefaultInterestRate();
        int effectiveTenure = loanService.getDefaultTenureMonths();

        if (u.getRole() == AppRole.ROLE_LOAN_OFFICER || u.getRole() == AppRole.ROLE_ADMIN) {
            String customerEmail = body != null && body.containsKey("customerEmail") ? (String) body.get("customerEmail") : paramCustomerEmail;
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Customer email is required for staff loan origination."));
            }
            targetUser = userService.findByEmail(customerEmail.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerEmail));
            if (targetUser.getRole() != AppRole.ROLE_CUSTOMER) {
                return ResponseEntity.badRequest().body(Map.of("error", "Loans can only be originated for customers."));
            }

            Double interest = body != null && body.containsKey("interest") && body.get("interest") != null ? Double.parseDouble(String.valueOf(body.get("interest"))) : paramInterest;
            Integer months = body != null && body.containsKey("months") && body.get("months") != null ? Integer.parseInt(String.valueOf(body.get("months"))) : paramMonths;
            if (interest != null && interest > 0) effectiveInterest = interest;
            if (months != null && months > 0) effectiveTenure = months;
        } else if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            targetUser = u;
            effectiveInterest = loanService.getDefaultInterestRate();
            effectiveTenure = loanService.getDefaultTenureMonths();
        } else {
            throw new AccessDeniedException("Unauthorized to originate loans.");
        }

        Loan newLoan = loanService.apply(targetUser, amount, effectiveInterest, effectiveTenure);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Loan application submitted successfully.",
                "loanId", newLoan.getLoanId(),
                "accountNumber", newLoan.getAccountNumber() != null ? newLoan.getAccountNumber() : "LN" + newLoan.getLoanId()
        ));
    }

    @GetMapping("/pay/details")
    public ResponseEntity<Map<String, Object>> getPayDetails(
            @RequestParam(value = "loanId", required = false) Long loanId,
            HttpSession session) {

        User u = getAuthenticatedUser(session);
        if (u.getRole() != AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Staff members are not authorized to process repayments.");
        }

        Loan loan;
        if (loanId != null) {
            loan = loanService.getById(loanId)
                    .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
            if (!loan.getUser().getId().equals(u.getId())) {
                throw new AccessDeniedException("Forbidden: You cannot access payment information for another customer's loan.");
            }
        } else {
            List<Loan> userLoans = loanService.get(u);
            if (userLoans.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No active loans found for repayment."));
            } else {
                loan = userLoans.get(0);
            }
        }

        String txRef = "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        return ResponseEntity.ok(Map.of(
                "loan", loan,
                "transactionReference", txRef
        ));
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "loanId", required = false) Long paramLoanId,
            @RequestParam(value = "amount", required = false) Double paramAmount,
            @RequestParam(value = "paymentMode", required = false) String paramPaymentMode,
            @RequestParam(value = "transactionReference", required = false) String paramTxRef,
            HttpSession session) {

        User u = getAuthenticatedUser(session);
        if (u.getRole() != AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Staff members are not authorized to process repayments.");
        }

        Long loanId = body != null && body.containsKey("loanId") ? Long.parseLong(String.valueOf(body.get("loanId"))) : paramLoanId;
        double amount = body != null && body.containsKey("amount") ? Double.parseDouble(String.valueOf(body.get("amount"))) : (paramAmount != null ? paramAmount : 0);
        String paymentMode = body != null && body.containsKey("paymentMode") ? (String) body.get("paymentMode") : (paramPaymentMode != null ? paramPaymentMode : "UPI");
        String transactionReference = body != null && body.containsKey("transactionReference") ? (String) body.get("transactionReference") : paramTxRef;

        if (loanId == null || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid loan ID and payment amount greater than zero are required."));
        }

        Loan l = loanService.getById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (!l.getUser().getId().equals(u.getId())) {
            throw new AccessDeniedException("Forbidden: You cannot process payments for another customer's loan.");
        }

        loanService.pay(loanId, amount, paymentMode, transactionReference);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment of ₹" + amount + " processed successfully."
        ));
    }

    // ==========================================
    // 4. STAFF ACTIONS (APPROVE, REJECT, COLLECTION)
    // ==========================================

    @PostMapping("/staff/loans/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "loanId", required = false) Long paramLoanId,
            HttpSession session,
            HttpServletRequest request) {

        User u = getAuthenticatedUser(session);
        if (u.getRole() != AppRole.ROLE_LOAN_OFFICER && u.getRole() != AppRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Unauthorized: Only Loan Officers and Admins can approve loan applications.");
        }

        Long loanId = body != null && body.containsKey("loanId") ? Long.parseLong(String.valueOf(body.get("loanId"))) : paramLoanId;
        if (loanId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Loan ID is required."));
        }

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        Loan approved = loanService.approveLoan(loanId, u.getEmail(), branchFilter);
        if (auditService != null) {
            auditService.logLoanApplicationApproved(u.getEmail(), request, loanId, approved.getBranch());
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Loan application approved successfully."));
    }

    @PostMapping("/staff/loans/reject")
    public ResponseEntity<Map<String, Object>> rejectLoan(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "loanId", required = false) Long paramLoanId,
            @RequestParam(value = "reason", required = false) String paramReason,
            HttpSession session,
            HttpServletRequest request) {

        User u = getAuthenticatedUser(session);
        if (u.getRole() != AppRole.ROLE_LOAN_OFFICER && u.getRole() != AppRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Unauthorized: Only Loan Officers and Admins can reject loan applications.");
        }

        Long loanId = body != null && body.containsKey("loanId") ? Long.parseLong(String.valueOf(body.get("loanId"))) : paramLoanId;
        String reason = body != null && body.containsKey("reason") ? (String) body.get("reason") : (paramReason != null ? paramReason : "Application does not meet eligibility criteria");

        if (loanId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Loan ID is required."));
        }

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        Loan rejected = loanService.rejectLoan(loanId, reason, u.getEmail(), branchFilter);
        if (auditService != null) {
            auditService.logLoanApplicationRejected(u.getEmail(), request, loanId, reason, rejected.getBranch());
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Loan application rejected."));
    }

    @PostMapping("/staff/collections/record")
    public ResponseEntity<Map<String, Object>> recordCollection(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "loanId", required = false) Long paramLoanId,
            @RequestParam(value = "amount", required = false) Double paramAmount,
            @RequestParam(value = "paymentMode", required = false) String paramPaymentMode,
            @RequestParam(value = "transactionReference", required = false) String paramTxRef,
            HttpSession session,
            HttpServletRequest request) {

        User u = getAuthenticatedUser(session);
        if (u.getRole() != AppRole.ROLE_COLLECTIONS_AGENT && u.getRole() != AppRole.ROLE_COLLECTIONS_MANAGER && u.getRole() != AppRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Unauthorized: Role not permitted to record collections.");
        }

        Long loanId = body != null && body.containsKey("loanId") ? Long.parseLong(String.valueOf(body.get("loanId"))) : paramLoanId;
        double amount = body != null && body.containsKey("amount") ? Double.parseDouble(String.valueOf(body.get("amount"))) : (paramAmount != null ? paramAmount : 0);
        String paymentMode = body != null && body.containsKey("paymentMode") ? (String) body.get("paymentMode") : (paramPaymentMode != null ? paramPaymentMode : "CASH");
        String transactionReference = body != null && body.containsKey("transactionReference") ? (String) body.get("transactionReference") : paramTxRef;

        if (loanId == null || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Collection payment amount must be greater than zero."));
        }

        Loan loan = loanService.getById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        if (!"ALL".equalsIgnoreCase(branchFilter) && !branchFilter.equalsIgnoreCase(loan.getBranch())) {
            if (auditService != null) {
                auditService.logCollectionDenied(u.getEmail(), request, loanId, "Officer branch " + branchFilter + " unauthorized for loan branch " + loan.getBranch());
            }
            throw new AccessDeniedException("Unauthorized: Loan belongs to another branch.");
        }

        if (amount > loan.getRemaining()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Collection amount (₹" + amount + ") cannot exceed outstanding balance of ₹" + loan.getRemaining()));
        }

        String txRef = (transactionReference != null && !transactionReference.isBlank())
                ? transactionReference.trim()
                : "COL" + System.currentTimeMillis() + (int) (Math.random() * 1000);

        String mode = (paymentMode != null && !paymentMode.isBlank()) ? paymentMode.trim().toUpperCase() : "CASH";

        loanService.pay(loanId, amount, mode, txRef);

        if (auditService != null) {
            auditService.logCollectionRecorded(u.getEmail(), request, loanId, amount, mode, txRef, loan.getBranch());
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Collection of ₹" + amount + " successfully recorded."));
    }

    // ==========================================
    // 5. SEARCH, CUSTOMER PROFILE & 360 DETAILS
    // ==========================================

    @GetMapping("/loan-search")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COLLECTIONS_AGENT', 'COLLECTIONS_MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> searchLoans(
            @RequestParam(value = "searchType", required = false, defaultValue = "ACCOUNT_NUMBER") String searchType,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session,
            HttpServletRequest request) {

        User officer = getAuthenticatedUser(session);
        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customer accounts cannot access loan account searches.");
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("officerBranch", loanSearchService.getAuthorizedBranch(officer));
        resp.put("searchType", searchType);
        resp.put("query", query != null ? query.trim() : "");
        resp.put("page", page);
        resp.put("size", size);

        if (query == null || query.trim().isBlank()) {
            resp.put("results", Page.empty());
            return ResponseEntity.ok(resp);
        }

        Page<LoanSearchResultDto> results = loanSearchService.searchLoans(officer, searchType, query.trim(), page, size, request);
        resp.put("results", results);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/customer-profile")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COLLECTIONS_AGENT', 'COLLECTIONS_MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCustomerProfile(
            @RequestParam("loanId") Long loanId,
            HttpSession session,
            HttpServletRequest request) {

        User officer = getAuthenticatedUser(session);
        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customer accounts cannot access customer profiles.");
        }

        CustomerProfileDto profile = loanSearchService.getCustomerProfile(officer, loanId, request)
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found for loanId: " + loanId));

        return ResponseEntity.ok(Map.of(
                "profile", profile,
                "officerBranch", loanSearchService.getAuthorizedBranch(officer)
        ));
    }

    @GetMapping("/loan-details")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COLLECTIONS_AGENT', 'COLLECTIONS_MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> get360Details(
            @RequestParam("loanId") Long loanId,
            @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
            HttpSession session,
            HttpServletRequest request) {

        User officer = getAuthenticatedUser(session);
        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customers cannot access officer 360 degree dashboards.");
        }

        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officer, loanId, request);
        return ResponseEntity.ok(Map.of(
                "loanDetails", details,
                "officerBranch", loanSearchService.getAuthorizedBranch(officer),
                "isRefreshed", refresh
        ));
    }

    // ==========================================
    // 6. ADMIN MANAGEMENT (STAFF, AUDITS, REMINDERS)
    // ==========================================

    @GetMapping("/admin/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminStaffList() {
        List<User> staffList = userService.findAllStaff();
        return ResponseEntity.ok(Map.of(
                "staffList", staffList,
                "roles", List.of(
                        AppRole.ROLE_LOAN_OFFICER,
                        AppRole.ROLE_COLLECTIONS_AGENT,
                        AppRole.ROLE_COLLECTIONS_MANAGER,
                        AppRole.ROLE_ADMIN
                )
        ));
    }

    @PostMapping("/admin/staff/provision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> provisionStaff(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "name", required = false) String paramName,
            @RequestParam(value = "email", required = false) String paramEmail,
            @RequestParam(value = "password", required = false) String paramPassword,
            @RequestParam(value = "role", required = false) String paramRole,
            HttpServletRequest request,
            Authentication authentication) {

        String name = body != null && body.containsKey("name") ? body.get("name") : paramName;
        String email = body != null && body.containsKey("email") ? body.get("email") : paramEmail;
        String password = body != null && body.containsKey("password") ? body.get("password") : paramPassword;
        String roleStr = body != null && body.containsKey("role") ? body.get("role") : paramRole;

        if (name == null || email == null || password == null || roleStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email, password, and role are required."));
        }

        try {
            AppRole role = AppRole.valueOf(roleStr);
            userService.provisionStaff(name, email, password, role, "TOTP");
            auditService.logStaffProvisioned(authentication.getName(), request, email, role.name());
            return ResponseEntity.ok(Map.of("success", true, "message", "Staff member successfully provisioned with mandatory MFA enrollment."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/staff/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unlockStaff(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "email", required = false) String paramEmail,
            Authentication authentication,
            HttpServletRequest request) {

        String email = body != null && body.containsKey("email") ? body.get("email") : paramEmail;
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        }

        if (loginAttemptService != null) {
            loginAttemptService.unlockAccount(email, authentication.getName(), request);
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Account successfully unlocked."));
    }

    @PostMapping("/admin/staff/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStaffRole(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "staffId", required = false) Long paramStaffId,
            @RequestParam(value = "newRole", required = false) String paramNewRole,
            Authentication authentication,
            HttpServletRequest request) {

        Long staffId = body != null && body.containsKey("staffId") ? Long.parseLong(String.valueOf(body.get("staffId"))) : paramStaffId;
        String newRoleStr = body != null && body.containsKey("newRole") ? (String) body.get("newRole") : paramNewRole;

        if (staffId == null || newRoleStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Staff ID and new role are required."));
        }

        AppRole newRole = AppRole.valueOf(newRoleStr);
        userService.findById(staffId).ifPresent(staff -> {
            userService.updateStaffRole(staffId, newRole);
            auditService.logRoleChanged(authentication.getName(), request, staff.getEmail(), newRole.name());
        });

        return ResponseEntity.ok(Map.of("success", true, "message", "Role updated successfully."));
    }

    @PostMapping("/admin/staff/reset-mfa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetStaffMfa(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "staffId", required = false) Long paramStaffId) {

        Long staffId = body != null && body.containsKey("staffId") ? Long.parseLong(String.valueOf(body.get("staffId"))) : paramStaffId;
        if (staffId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Staff ID is required."));
        }

        userService.resetMfaSecret(staffId);
        return ResponseEntity.ok(Map.of("success", true, "message", "MFA secret has been reset for staff member."));
    }

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthAuditLog>> getAuditLogs() {
        List<AuthAuditLog> logs = auditRepo.findTop100ByOrderByTimestampDesc();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/admin/reminders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRemindersDemoData() {
        LocalDate today = LocalDate.now();
        LocalDate minDue = today.minusDays(3);
        LocalDate maxDue = today.minusDays(1);

        List<RepaymentInstallment> overdueList = Collections.emptyList();
        if (installmentRepository != null) {
            overdueList = installmentRepository.findEligibleOverdueInstallmentsList(minDue, maxDue);
        }

        List<LoanReminderLog> logs = Collections.emptyList();
        if (reminderLogRepository != null) {
            logs = reminderLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return ResponseEntity.ok(Map.of(
                "today", today.toString(),
                "overdueInstallments", overdueList,
                "reminderLogs", logs
        ));
    }

    @PostMapping("/admin/reminders/create-overdue-loan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createOverdueLoan(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "customerName", required = false) String paramName,
            @RequestParam(value = "customerEmail", required = false) String paramEmail,
            @RequestParam(value = "customerPhone", defaultValue = "9876543210") String paramPhone,
            @RequestParam(value = "emiAmount", defaultValue = "4500.00") BigDecimal paramEmi,
            @RequestParam(value = "dpdDays", defaultValue = "1") int paramDpd,
            @RequestParam(value = "sendImmediately", defaultValue = "true") boolean paramSend) {

        String customerName = body != null && body.containsKey("customerName") ? (String) body.get("customerName") : (paramName != null ? paramName : "Demo Customer");
        String customerEmail = body != null && body.containsKey("customerEmail") ? (String) body.get("customerEmail") : (paramEmail != null ? paramEmail : "demo@bank.com");
        String customerPhone = body != null && body.containsKey("customerPhone") ? (String) body.get("customerPhone") : paramPhone;
        BigDecimal emiAmount = body != null && body.containsKey("emiAmount") ? new BigDecimal(String.valueOf(body.get("emiAmount"))) : paramEmi;
        int dpdDays = body != null && body.containsKey("dpdDays") ? Integer.parseInt(String.valueOf(body.get("dpdDays"))) : paramDpd;
        boolean sendImmediately = body != null && body.containsKey("sendImmediately") ? Boolean.parseBoolean(String.valueOf(body.get("sendImmediately"))) : paramSend;

        String emailClean = customerEmail.trim().toLowerCase();
        User customer = userRepository.findByEmail(emailClean).orElseGet(() -> {
            User u = new User();
            u.setName(customerName.trim());
            u.setEmail(emailClean);
            u.setPassword("Password123!");
            u.setRole(AppRole.ROLE_CUSTOMER);
            u.setUserType(UserType.CUSTOMER);
            u.setPhoneNumber(customerPhone);
            u.setBranch("MAIN");
            u.setCustomerId("CUST" + (System.currentTimeMillis() % 1000000));
            return userRepository.save(u);
        });

        Loan loan = new Loan();
        loan.setUser(customer);
        loan.setAmount(emiAmount.doubleValue() * 12);
        loan.setInterest(12.0);
        loan.setMonths(12);
        loan.setEmi(emiAmount.doubleValue());
        loan.setRemaining(emiAmount.doubleValue() * 12);
        loan.setBranch("MAIN");
        loan.setStatus("ACTIVE");
        loan.setProductType("Personal Loan");
        loan.setInterestType("FIXED");
        loan.setDisbursedAmount(emiAmount.doubleValue() * 12);
        loan.setDisbursementDate(LocalDate.now().minusMonths(1));
        loan.setAccountNumber("LN" + System.currentTimeMillis());
        Loan savedLoan = loanRepository.save(loan);

        LocalDate dueDate = LocalDate.now().minusDays(dpdDays);
        RepaymentInstallment installment = new RepaymentInstallment(
                savedLoan,
                1,
                dueDate,
                emiAmount,
                emiAmount.multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP),
                emiAmount.multiply(new BigDecimal("0.2")).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO,
                "DUE",
                dpdDays
        );
        installmentRepository.save(installment);

        if (sendImmediately && reminderScheduler != null) {
            reminderScheduler.runDailyReminderJob(LocalDate.now(ZoneId.of("Asia/Kolkata")));
            return ResponseEntity.ok(Map.of("success", true, "message", "Overdue loan created and reminder notice dispatched immediately."));
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Overdue loan created successfully."));
    }

    @PostMapping("/admin/reminders/trigger-now")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerRemindersNow() {
        int processed = 0;
        if (reminderScheduler != null) {
            processed = reminderScheduler.runDailyReminderJob(LocalDate.now(ZoneId.of("Asia/Kolkata")));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daily reminder engine executed.",
                "processedInstallments", processed
        ));
    }

    @PostMapping("/admin/reminders/simulate-webhook")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> simulateWebhook(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "providerMessageId", required = false) String paramMsgId,
            @RequestParam(value = "eventType", required = false) String paramEventType) {

        String providerMessageId = body != null && body.containsKey("providerMessageId") ? body.get("providerMessageId") : paramMsgId;
        String eventType = body != null && body.containsKey("eventType") ? body.get("eventType") : paramEventType;

        if (providerMessageId == null || eventType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "providerMessageId and eventType are required."));
        }

        if (reminderService != null) {
            reminderService.processProviderWebhook(providerMessageId, eventType, "Simulated via Admin REST API");
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Webhook status simulated: " + eventType));
    }

    // Helper mapping for customer activities
    private List<CustomerActivityDto> getCustomerRecentActivities(List<Loan> customerLoans) {
        if (customerLoans == null || customerLoans.isEmpty()) {
            return Collections.emptyList();
        }

        List<CustomerActivityDto> activityList = new ArrayList<>();

        for (Loan l : customerLoans) {
            String accNum = l.getAccountNumber() != null && !l.getAccountNumber().isBlank()
                    ? l.getAccountNumber()
                    : String.format("LN%010d", l.getLoanId());
            String prodType = l.getProductType() != null ? l.getProductType() : "Personal Loan";
            String st = l.getStatus() != null ? l.getStatus() : "ACTIVE";

            String title;
            String badge;
            String icon;
            if ("ACTIVE".equalsIgnoreCase(st) || "APPROVED".equalsIgnoreCase(st)) {
                title = "Loan Approved & Active";
                badge = "badge-success";
                icon = "✅";
            } else if ("CLOSED".equalsIgnoreCase(st)) {
                title = "Loan Closed & Settled";
                badge = "badge-secondary";
                icon = "🎉";
            } else if ("REJECTED".equalsIgnoreCase(st)) {
                title = "Application Rejected";
                badge = "badge-danger";
                icon = "❌";
            } else {
                title = "Loan Application Submitted";
                badge = "badge-warning";
                icon = "📝";
            }

            String desc = String.format("%s for ₹%,.2f (Account %s)", prodType, l.getAmount(), accNum);
            BigDecimal amt = BigDecimal.valueOf(l.getAmount()).setScale(2, RoundingMode.HALF_UP);
            Instant ts = l.getCreatedAt() != null ? l.getCreatedAt() : Instant.now();

            activityList.add(new CustomerActivityDto("LOAN_EVENT", title, desc, ts, st, amt, badge, icon));
        }

        try {
            List<PaymentTransaction> txns = paymentTransactionRepository.findTop5ByLoanInOrderByPaymentDateDesc(customerLoans);
            if (txns != null) {
                for (PaymentTransaction t : txns) {
                    String accNum = t.getLoan() != null && t.getLoan().getAccountNumber() != null
                            ? t.getLoan().getAccountNumber()
                            : (t.getLoan() != null ? "#" + t.getLoan().getLoanId() : "");
                    String title = "Repayment Completed";
                    String desc = String.format("EMI payment of ₹%,.2f processed via %s for Account %s",
                            t.getAmount() != null ? t.getAmount().doubleValue() : 0.0,
                            t.getPaymentMode() != null ? t.getPaymentMode() : "UPI",
                            accNum);
                    activityList.add(new CustomerActivityDto(
                            "REPAYMENT", title, desc, t.getPaymentDate(),
                            t.getStatus() != null ? t.getStatus() : "SUCCESS",
                            t.getAmount(), "badge-success", "💳"
                    ));
                }
            }
        } catch (Exception ignored) {}

        return activityList.stream()
                .sorted(Comparator.comparing(CustomerActivityDto::getTimestamp).reversed())
                .limit(5)
                .toList();
    }

    // Helper mapping for staff loan DTO
    private StaffDashboardLoanDto mapToStaffLoanDto(Loan l) {
        String borrowerName = l.getUser() != null ? l.getUser().getName() : "Unknown";
        String rawEmail = l.getUser() != null ? l.getUser().getEmail() : "";
        String maskedEmail = DataMaskingUtils.maskEmail(rawEmail);
        String customerId = l.getUser() != null && l.getUser().getCustomerId() != null
                ? l.getUser().getCustomerId()
                : (l.getUser() != null ? "CUST" + l.getUser().getId() : "N/A");
        String maskedPhone = l.getUser() != null ? DataMaskingUtils.maskPhone(l.getUser().getPhoneNumber()) : "N/A";
        String createdAt = l.getCreatedAt() != null
                ? DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault()).format(l.getCreatedAt())
                : "N/A";

        String repaymentStatus = "CURRENT";
        int dpd = 0;
        double overdueAmount = 0.0;

        String loanStatus = l.getStatus() != null ? l.getStatus() : "ACTIVE";
        if ("CLOSED".equalsIgnoreCase(loanStatus)) {
            repaymentStatus = "CLOSED";
        } else if ("REJECTED".equalsIgnoreCase(loanStatus)) {
            repaymentStatus = "REJECTED";
        } else if ("PENDING".equalsIgnoreCase(loanStatus) || "SUBMITTED".equalsIgnoreCase(loanStatus)
                || "IN_REVIEW".equalsIgnoreCase(loanStatus) || "APPLIED".equalsIgnoreCase(loanStatus)) {
            repaymentStatus = "PENDING_APPROVAL";
        } else {
            if (l.getRemaining() <= 0) {
                repaymentStatus = "CLOSED";
            } else if (installmentRepository != null) {
                LocalDate today = LocalDate.now(ZoneId.systemDefault());
                List<RepaymentInstallment> installments =
                        installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(l.getLoanId());
                int maxDpd = 0;
                double totalOverdue = 0.0;
                for (RepaymentInstallment inst : installments) {
                    if ("OVERDUE".equalsIgnoreCase(inst.getStatus()) ||
                            ("PARTIALLY_PAID".equalsIgnoreCase(inst.getStatus()) && inst.getDpd() > 0) ||
                            (inst.getDueDate() != null && inst.getDueDate().isBefore(today) &&
                                    (inst.getPaidAmount() == null || inst.getPaidAmount().compareTo(inst.getEmiAmount()) < 0))) {
                        int calculatedDpd = inst.getDpd() > 0 ? inst.getDpd() :
                                (inst.getDueDate() != null ? Math.max(0, (int) ChronoUnit.DAYS.between(inst.getDueDate(), today)) : 0);
                        if (calculatedDpd > maxDpd) {
                            maxDpd = calculatedDpd;
                        }
                        BigDecimal emi = inst.getEmiAmount() != null ? inst.getEmiAmount() : BigDecimal.ZERO;
                        BigDecimal paid = inst.getPaidAmount() != null ? inst.getPaidAmount() : BigDecimal.ZERO;
                        BigDecimal unpaid = emi.subtract(paid).max(BigDecimal.ZERO);
                        totalOverdue += unpaid.doubleValue();
                    }
                }
                if (maxDpd > 0 || totalOverdue > 0) {
                    repaymentStatus = "OVERDUE";
                    dpd = maxDpd;
                    overdueAmount = Math.round(totalOverdue * 100.0) / 100.0;
                }
            }
        }

        return new StaffDashboardLoanDto(
                l.getLoanId(),
                l.getAccountNumber() != null ? l.getAccountNumber() : String.format("LN%010d", l.getLoanId()),
                borrowerName,
                maskedEmail,
                l.getAmount(),
                l.getInterest(),
                l.getMonths(),
                l.getEmi(),
                l.getRemaining(),
                loanStatus,
                l.getBranch() != null ? l.getBranch() : "MAIN",
                repaymentStatus,
                dpd,
                overdueAmount,
                customerId,
                maskedPhone,
                createdAt
        );
    }
}
