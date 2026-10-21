package com.loanproject.loan_app.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.MfaChallenge;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.MfaChallengeRepository;
import com.loanproject.loan_app.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final int MAX_MFA_ATTEMPTS = 3;
    private static final Duration CHALLENGE_VALIDITY = Duration.ofMinutes(5);

    private final MfaChallengeRepository challengeRepo;
    private final UserRepository userRepository;
    private final AuthAuditService auditService;
    private final Map<String, MfaProvider> providers;
    private final CustomerOtpDeliveryService customerOtpDeliveryService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${mfa.staff.static-otp-enabled:true}")
    private boolean staffStaticOtpEnabled = true;

    @Value("${mfa.staff.test-otp:123456}")
    private String staffTestOtp = "123456";

    @Value("${mfa.customer.otp-validity-minutes:5}")
    private int customerOtpValidityMinutes = 5;

    @Value("${mfa.customer.max-attempts:3}")
    private int customerMaxAttempts = 3;

    // Rate-limiting challenge creation per user ID (timestamp of last creation)
    private final Map<Long, Instant> lastCreationTimeByUser = new ConcurrentHashMap<>();

    public MfaService(MfaChallengeRepository challengeRepo,
                      UserRepository userRepository,
                      AuthAuditService auditService,
                      List<MfaProvider> providerList,
                      CustomerOtpDeliveryService customerOtpDeliveryService) {
        this.challengeRepo = challengeRepo;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.customerOtpDeliveryService = customerOtpDeliveryService;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(MfaProvider::getType, Function.identity()));
    }

    public boolean isStaffStaticOtpEnabled() {
        return staffStaticOtpEnabled;
    }

    public void setStaffStaticOtpEnabled(boolean staffStaticOtpEnabled) {
        this.staffStaticOtpEnabled = staffStaticOtpEnabled;
    }

    public String getStaffTestOtp() {
        return staffTestOtp;
    }

    public void setStaffTestOtp(String staffTestOtp) {
        this.staffTestOtp = staffTestOtp;
    }

    public int getCustomerOtpValidityMinutes() {
        return customerOtpValidityMinutes;
    }

    public void setCustomerOtpValidityMinutes(int customerOtpValidityMinutes) {
        this.customerOtpValidityMinutes = customerOtpValidityMinutes;
    }

    public int getCustomerMaxAttempts() {
        return customerMaxAttempts;
    }

    public void setCustomerMaxAttempts(int customerMaxAttempts) {
        this.customerMaxAttempts = customerMaxAttempts;
    }

    public boolean isStaffUser(User user) {
        if (user == null) return false;
        return user.isStaff();
    }

    public boolean isCustomerUser(User user) {
        if (user == null) return false;
        return user.isCustomer();
    }

    private void enforceRateLimit(Long userId) {
        Instant now = Instant.now();
        if (lastCreationTimeByUser.size() > 500) {
            lastCreationTimeByUser.entrySet().removeIf(e -> Duration.between(e.getValue(), now).getSeconds() > 60);
        }

        Instant lastCreation = lastCreationTimeByUser.get(userId);
        if (lastCreation != null && Duration.between(lastCreation, now).getSeconds() < 5) {
            throw new IllegalStateException("Please wait before requesting another authentication challenge");
        }
        lastCreationTimeByUser.put(userId, now);
    }

    public String hashOtp(String otp) {
        if (otp == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public boolean verifyHashedOtp(String inputOtp, String storedHash) {
        if (inputOtp == null || storedHash == null) {
            return false;
        }
        String inputHash = hashOtp(inputOtp.trim());
        return MessageDigest.isEqual(inputHash.getBytes(StandardCharsets.UTF_8),
                                     storedHash.getBytes(StandardCharsets.UTF_8));
    }

    public String generateCustomerDynamicOtp() {
        String otp;
        do {
            int val = secureRandom.nextInt(1_000_000);
            otp = String.format("%06d", val);
        } while (staffTestOtp != null && otp.equals(staffTestOtp.trim()));
        return otp;
    }

    @Transactional
    public MfaChallenge createStaffChallenge(User user, HttpServletRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for MFA challenge");
        }
        enforceRateLimit(user.getId());

        String type = staffStaticOtpEnabled ? "STAFF_STATIC" : "TOTP";
        String challengeToken = UUID.randomUUID().toString();
        Instant expiryTime = Instant.now().plus(CHALLENGE_VALIDITY);

        MfaChallenge challenge = new MfaChallenge(user, challengeToken, type, expiryTime);
        challengeRepo.save(challenge);

        // For staff test OTP verification, do NOT send email or SMS
        auditService.logMfaChallengeCreated(user.getEmail(), request);
        return challenge;
    }

    @Transactional
    public MfaChallenge createCustomerChallenge(User user, HttpServletRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for MFA challenge");
        }
        enforceRateLimit(user.getId());

        String challengeToken = UUID.randomUUID().toString();
        Instant expiryTime = Instant.now().plus(Duration.ofMinutes(customerOtpValidityMinutes));

        String rawOtp = generateCustomerDynamicOtp();
        String hashedOtp = hashOtp(rawOtp);

        MfaChallenge challenge = new MfaChallenge(user, challengeToken, "DYNAMIC_EMAIL", expiryTime);
        challenge.setHashedOtpCode(hashedOtp);
        challengeRepo.save(challenge);

        // Deliver dynamic OTP via customer verification channel
        boolean delivered = customerOtpDeliveryService.deliverOtp(user, rawOtp, challengeToken, request);
        if (!delivered) {
            // Do NOT bypass customer MFA if OTP delivery fails!
            throw new OtpDeliveryException("Failed to deliver dynamic OTP to customer verification channel");
        }

        auditService.logMfaChallengeCreated(user.getEmail(), request);
        return challenge;
    }

    @Transactional
    public MfaChallenge createChallenge(User user, String preferredType, HttpServletRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for MFA challenge");
        }

        // Intelligently route by trusted backend user role
        if (isCustomerUser(user)) {
            if ("DYNAMIC_EMAIL".equalsIgnoreCase(preferredType) || "DYNAMIC_EMAIL".equalsIgnoreCase(user.getMfaType())) {
                return createCustomerChallenge(user, request);
            }
            // Customer using TOTP Authenticator app (fall through to TOTP challenge generation below)
        }

        if (isStaffUser(user)) {
            if (staffStaticOtpEnabled) {
                return createStaffChallenge(user, request);
            }
        }

        // Standard TOTP / SMS fallback
        enforceRateLimit(user.getId());

        String type = preferredType != null ? preferredType : user.getMfaType();
        if (type == null || !providers.containsKey(type)) {
            type = "TOTP";
        }

        MfaProvider provider = providers.get(type);
        if (provider == null || !provider.isAvailable()) {
            type = "TOTP";
            provider = providers.get(type);
        }

        String challengeToken = UUID.randomUUID().toString();
        Instant expiryTime = Instant.now().plus(CHALLENGE_VALIDITY);

        MfaChallenge challenge = new MfaChallenge(user, challengeToken, type, expiryTime);
        challengeRepo.save(challenge);

        if (provider != null) {
            provider.initiate(user, challengeToken, null);
        }

        auditService.logMfaChallengeCreated(user.getEmail(), request);
        return challenge;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public MfaVerificationResult verifyChallenge(String challengeToken, String inputCode, HttpServletRequest request) {
        if (challengeToken == null || challengeToken.isBlank()) {
            return MfaVerificationResult.failure("INVALID_CHALLENGE", "Invalid or missing challenge", 0);
        }

        Optional<MfaChallenge> optionalChallenge = challengeRepo.findByChallengeToken(challengeToken);
        if (optionalChallenge.isEmpty()) {
            return MfaVerificationResult.failure("CHALLENGE_NOT_FOUND", "Authentication challenge not found", 0);
        }

        MfaChallenge challenge = optionalChallenge.get();
        User user = challenge.getUser();
        if (user != null && user.getId() != null) {
            user = userRepository.findById(user.getId()).orElse(user);
        }
        String username = user != null ? user.getEmail() : "UNKNOWN";

        int maxAttempts = isCustomerUser(user) ? customerMaxAttempts : MAX_MFA_ATTEMPTS;

        // 1. Check if already locked
        if (challenge.isLocked() || challenge.getAttemptCount() >= maxAttempts) {
            challenge.setLocked(true);
            challengeRepo.save(challenge);
            auditService.logMfaLocked(username, request);
            return MfaVerificationResult.failure("CHALLENGE_LOCKED", "Challenge locked due to excessive failed attempts. Please login again.", 0);
        }

        // 2. Check if already verified (prevent double consumption / replay)
        if (challenge.isVerified()) {
            auditService.logMfaFailure(username, request, "CHALLENGE_ALREADY_USED");
            return MfaVerificationResult.failure("CHALLENGE_ALREADY_USED", "Challenge has already been used. Please login again.", 0);
        }

        // 3. Check expiry
        if (challenge.isExpired()) {
            auditService.logMfaFailure(username, request, "CHALLENGE_EXPIRED");
            return MfaVerificationResult.failure("CHALLENGE_EXPIRED", "Authentication challenge has expired. Please login again.", 0);
        }

        String trimmedCode = inputCode != null ? inputCode.trim() : "";
        boolean codeValid = false;

        boolean isStaff = isStaffUser(user);
        boolean isCustomer = isCustomerUser(user);

        if (isCustomer) {
            // Customer Verification Flow:
            // CRITICAL: Reject static staff OTP for customer accounts!
            if (staffTestOtp != null && !staffTestOtp.isBlank() && staffTestOtp.equals(trimmedCode)) {
                log.warn("Customer account {} attempted to use staff static test OTP. Rejected.", DataMaskingUtils.maskEmail(username));
                codeValid = false;
            } else if ("DYNAMIC_EMAIL".equals(challenge.getMfaType()) && challenge.getHashedOtpCode() != null && !trimmedCode.isEmpty()) {
                codeValid = verifyHashedOtp(trimmedCode, challenge.getHashedOtpCode());
            } else {
                // TOTP Authenticator Provider (Google Authenticator, Microsoft Authenticator)
                MfaProvider totpProvider = providers.get("TOTP");
                if (totpProvider != null && !trimmedCode.isEmpty()) {
                    codeValid = totpProvider.verify(user, trimmedCode);
                }
            }
        } else if (isStaff) {
            // Staff Verification Flow:
            // Check static test OTP if enabled
            if (staffStaticOtpEnabled && staffTestOtp != null && !staffTestOtp.isBlank() && staffTestOtp.equals(trimmedCode)) {
                codeValid = true;
            } else {
                // Check TOTP provider if enrolled
                MfaProvider totpProvider = providers.get("TOTP");
                if (totpProvider != null && !trimmedCode.isEmpty()) {
                    codeValid = totpProvider.verify(user, trimmedCode);
                }
            }
        } else {
            // Fallback for any other provider type
            MfaProvider provider = providers.get(challenge.getMfaType());
            if (provider != null && !trimmedCode.isEmpty()) {
                codeValid = provider.verify(user, trimmedCode);
            }
        }

        if (codeValid) {
            // Atomically mark verified and consumed
            challenge.setVerified(true);
            challengeRepo.save(challenge);
            auditService.logMfaSuccess(username, request);
            return MfaVerificationResult.success(user);
        } else {
            int newAttempts = challenge.getAttemptCount() + 1;
            challenge.setAttemptCount(newAttempts);

            if (newAttempts >= maxAttempts) {
                challenge.setLocked(true);
                challengeRepo.save(challenge);
                auditService.logMfaLocked(username, request);
                return MfaVerificationResult.failure("CHALLENGE_LOCKED", "Challenge locked due to " + maxAttempts + " incorrect attempts. Please login again.", 0);
            }

            challengeRepo.save(challenge);
            int remaining = maxAttempts - newAttempts;
            auditService.logMfaFailure(username, request, "INVALID_CODE");
            return MfaVerificationResult.failure("INVALID_CODE", "Invalid authentication code. " + remaining + " attempts remaining.", remaining);
        }
    }
}
