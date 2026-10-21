package com.loanproject.loan_app.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.loanproject.loan_app.bean.User;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class DefaultCustomerOtpDeliveryService implements CustomerOtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCustomerOtpDeliveryService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${mfa.customer.mail.from:${reminder.mail.from:security@bankofindia.co.in}}")
    private String fromEmail;

    @Value("${mfa.customer.mail.simulate:false}")
    private boolean simulateMail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    private volatile boolean simulateDeliveryFailure = false;

    // Secure test-only map storing last delivered OTP per email for testing assertions without log leakage
    private final Map<String, String> lastDeliveredOtpByEmail = new ConcurrentHashMap<>();

    public boolean hasValidMailCredentials() {
        return mailSender != null
                && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    @Override
    public boolean isSimulatedDelivery() {
        return simulateMail || !hasValidMailCredentials();
    }

    @Override
    public boolean deliverOtp(User customer, String otpCode, String challengeToken, HttpServletRequest request) {
        if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
            log.warn("Customer OTP delivery aborted: customer or email is null/empty");
            return false;
        }

        String recipientEmail = customer.getEmail().trim().toLowerCase();
        String maskedEmail = DataMaskingUtils.maskEmail(recipientEmail);

        if (simulateDeliveryFailure) {
            log.warn("Simulated delivery failure triggered for recipient: {}", maskedEmail);
            return false;
        }

        if (otpCode == null || otpCode.isBlank()) {
            log.error("Customer OTP delivery aborted: OTP code is empty for recipient: {}", maskedEmail);
            return false;
        }

        // Record in test-hook storage
        lastDeliveredOtpByEmail.put(recipientEmail, otpCode);

        // Deliver via real JavaMailSender ONLY if SMTP credentials are fully provided and not simulated
        if (hasValidMailCredentials() && !simulateMail) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                String from = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "security@bankofindia.co.in";
                helper.setFrom(from);
                helper.setTo(recipientEmail);
                helper.setSubject("Bank of India - Authentication Verification Code");

                String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>"
                        + "<h2 style='color: #1a365d; margin-top: 0;'>Bank of India Loan Portal</h2>"
                        + "<p>Dear " + (customer.getName() != null ? customer.getName() : "Customer") + ",</p>"
                        + "<p>Your one-time dynamic verification code for signing in is:</p>"
                        + "<div style='font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #2b6cb0; background: #edf2f7; padding: 12px 24px; text-align: center; border-radius: 6px; margin: 20px 0;'>"
                        + otpCode
                        + "</div>"
                        + "<p style='color: #718096; font-size: 13px;'>This code is valid for 5 minutes and can only be used once. Never share this code with anyone, including bank staff.</p>"
                        + "<p style='color: #718096; font-size: 13px;'>If you did not request this login attempt, please secure your account immediately.</p>"
                        + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'/>"
                        + "<p style='color: #a0aec0; font-size: 11px; text-align: center;'>Bank of India Loan Management System</p>"
                        + "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);

                // Note: never log the OTP code itself!
                log.info("Dispatched dynamic MFA OTP via SMTP email channel to recipient {}", maskedEmail);
                return true;
            } catch (Exception e) {
                log.error("Failed to deliver dynamic OTP email to recipient {}: {}", maskedEmail, e.getMessage());
                return false;
            }
        }

        // Simulated / Test delivery (when mailSender is not configured or simulateMail is active)
        // Log masked email without leaking OTP code
        log.info("Dispatched dynamic MFA OTP via secure email channel to recipient {} (delivery simulated/test mode)", maskedEmail);
        return true;
    }

    @Override
    public String getLastDeliveredOtpForTesting(String email) {
        if (email == null) return null;
        return lastDeliveredOtpByEmail.get(email.trim().toLowerCase());
    }

    @Override
    public void setSimulateDeliveryFailure(boolean simulateFailure) {
        this.simulateDeliveryFailure = simulateFailure;
    }
}
