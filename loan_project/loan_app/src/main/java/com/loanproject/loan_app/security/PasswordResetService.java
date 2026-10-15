package com.loanproject.loan_app.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.PasswordResetToken;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.PasswordResetTokenRepository;
import com.loanproject.loan_app.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);

    // Password strength regex: min 8 characters, at least 1 uppercase, 1 lowercase, 1 number, 1 special char
    private static final Pattern PASSWORD_POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-={}\\[\\]|:;\"'<>,.~`]).{8,64}$");

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory rate limiting: email/ip -> count and window
    private final Map<String, Instant> rateLimitMap = new ConcurrentHashMap<>();

    // Test hook to inspect last generated token in automated tests without leaking to users
    private volatile String lastGeneratedTokenForTesting;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                AuthAuditService auditService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public String getLastGeneratedTokenForTesting() {
        return lastGeneratedTokenForTesting;
    }

    @Transactional
    public String requestReset(String email, HttpServletRequest request) {
        // Generic response returned regardless of user existence to prevent account enumeration
        String genericMessage = "If an account with that email exists, password reset instructions have been sent.";

        if (email == null || email.isBlank()) {
            return genericMessage;
        }

        String cleanEmail = email.trim().toLowerCase();

        // Rate limiting check: 1 request every 10 seconds per email
        Instant lastRequest = rateLimitMap.get(cleanEmail);
        if (lastRequest != null && Duration.between(lastRequest, Instant.now()).getSeconds() < 10) {
            log.warn("Rate limited password reset request for {}", cleanEmail);
            return genericMessage;
        }
        rateLimitMap.put(cleanEmail, Instant.now());

        Optional<User> optionalUser = userRepository.findByEmail(cleanEmail);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Generate cryptographically random 32-byte token
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String rawToken = HexFormat.of().formatHex(randomBytes);
            this.lastGeneratedTokenForTesting = rawToken;

            // Hash the token with SHA-256 for protected database storage
            String tokenHash = hashToken(rawToken);
            Instant expiryTime = Instant.now().plus(TOKEN_VALIDITY);

            PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, expiryTime);
            tokenRepository.save(resetToken);

            auditService.logPasswordResetRequested(user.getEmail(), request);
            log.info("Password reset initiated for user: {}", user.getEmail());
        } else {
            auditService.logPasswordResetRequested("UNKNOWN (" + cleanEmail + ")", request);
        }

        return genericMessage;
    }

    public boolean validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String tokenHash = hashToken(rawToken.trim());
        Optional<PasswordResetToken> optionalToken = tokenRepository.findByTokenHash(tokenHash);
        if (optionalToken.isEmpty()) {
            return false;
        }
        PasswordResetToken token = optionalToken.get();
        return !token.isUsed() && !token.isExpired();
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword, HttpServletRequest request) {
        if (!validatePasswordStrength(newPassword)) {
            throw new IllegalArgumentException(
                    "Password must be 8-64 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }

        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String tokenHash = hashToken(rawToken.trim());
        Optional<PasswordResetToken> optionalToken = tokenRepository.findByTokenHash(tokenHash);
        if (optionalToken.isEmpty()) {
            return false;
        }

        PasswordResetToken token = optionalToken.get();
        if (token.isUsed() || token.isExpired()) {
            return false;
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedAttemptCount(0);
        user.setAccountNonLocked(true);
        user.setLockTime(null);
        userRepository.save(user);

        // Mark token as single-use consumed
        token.setUsed(true);
        tokenRepository.save(token);

        auditService.logPasswordResetSuccess(user.getEmail(), request);
        return true;
    }

    public boolean validatePasswordStrength(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_POLICY.matcher(password).matches();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
