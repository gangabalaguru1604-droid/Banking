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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.services.GmailRestService gmailRestService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${app.base-url:}")
    private String configuredBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${reminder.mail.from:security@bankofindia.co.in}")
    private String fromEmail;

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
        Instant now = Instant.now();
        // Memory safety: Prune entries older than 60 seconds when map grows to prevent unbounded memory retention
        if (rateLimitMap.size() > 500) {
            rateLimitMap.entrySet().removeIf(e -> Duration.between(e.getValue(), now).getSeconds() > 60);
        }

        Instant lastRequest = rateLimitMap.get(cleanEmail);
        if (lastRequest != null && Duration.between(lastRequest, now).getSeconds() < 10) {
            log.warn("Rate limited password reset request for {}", cleanEmail);
            return genericMessage;
        }
        rateLimitMap.put(cleanEmail, now);

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

            // Dispatch password reset email with reset link
            dispatchResetEmail(user, rawToken, request);
        } else {
            auditService.logPasswordResetRequested("UNKNOWN (" + cleanEmail + ")", request);
        }

        return genericMessage;
    }

    private void dispatchResetEmail(User user, String rawToken, HttpServletRequest request) {
        try {
            String baseUrl = "http://localhost:8081";
            if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
                baseUrl = configuredBaseUrl.trim().replaceAll("/+$", "");
            } else if (request != null) {
                String forwardedProto = request.getHeader("X-Forwarded-Proto");
                String forwardedHost = request.getHeader("X-Forwarded-Host");
                String scheme = (forwardedProto != null && !forwardedProto.isBlank()) ? forwardedProto : request.getScheme();
                String serverName = (forwardedHost != null && !forwardedHost.isBlank()) ? forwardedHost : request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = request.getContextPath();

                if (serverName.contains(":") || (scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
                    baseUrl = scheme + "://" + serverName + contextPath;
                } else {
                    baseUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;
                }
            }

            String resetUrl = baseUrl + "/reset-password?token=" + rawToken;
            String subject = "Bank of India - Password Reset Request";
            String recipientEmail = user.getEmail().trim();
            String userName = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "Customer";

            String htmlBody = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;'>"
                    + "<h2 style='color: #1a365d; margin-top: 0;'>Bank of India Loan Portal</h2>"
                    + "<p>Dear " + userName + ",</p>"
                    + "<p>We received a request to reset the password for your account.</p>"
                    + "<p>Click the button below to choose a new password. This link is valid for <strong>15 minutes</strong> and can only be used once:</p>"
                    + "<div style='text-align: center; margin: 28px 0;'>"
                    + "<a href='" + resetUrl + "' style='background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 15px; display: inline-block;'>Reset Password</a>"
                    + "</div>"
                    + "<p style='color: #64748b; font-size: 13px;'>If the button does not work, copy and paste this link into your browser:</p>"
                    + "<p style='font-size: 12px; color: #2563eb; word-break: break-all;'>" + resetUrl + "</p>"
                    + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;'/>"
                    + "<p style='color: #94a3b8; font-size: 12px;'>If you did not request this password reset, please ignore this email or contact support immediately if you suspect unauthorized access.</p>"
                    + "</div>";

            // 1. Prioritize Gmail API over HTTPS
            if (gmailRestService != null && gmailRestService.isConfigured()) {
                String messageId = gmailRestService.sendEmail(recipientEmail, subject, htmlBody);
                if (messageId != null) {
                    log.info("Dispatched password reset email via Gmail REST API to {}", recipientEmail);
                    return;
                }
                log.warn("Failed to send password reset via Gmail API, attempting fallback");
            }

            // 2. Fallback to JavaMailSender
            if (mailSender != null) {
                jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom((fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "security@bankofindia.co.in");
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                log.info("Dispatched password reset email via SMTP to {}", recipientEmail);
            }
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), ex.getMessage(), ex);
        }
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

        User tokenUser = token.getUser();
        if (tokenUser == null) {
            log.error("Password reset failed: token has no associated user");
            return false;
        }

        // Fetch fresh user entity from database to ensure proper MongoDB document mapping
        Optional<User> freshUserOpt = (tokenUser.getId() != null)
                ? userRepository.findById(tokenUser.getId())
                : userRepository.findByEmail(tokenUser.getEmail());

        User user = freshUserOpt.orElse(tokenUser);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedAttemptCount(0);
        user.setAccountNonLocked(true);
        user.setLockTime(null);
        userRepository.save(user);

        // Mark token as single-use consumed
        token.setUsed(true);
        tokenRepository.save(token);

        auditService.logPasswordResetSuccess(user.getEmail(), request);
        log.info("Password successfully updated in database for user: {}", user.getEmail());
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
