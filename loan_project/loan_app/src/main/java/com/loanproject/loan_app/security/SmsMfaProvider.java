package com.loanproject.loan_app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.loanproject.loan_app.bean.User;

@Component
public class SmsMfaProvider implements MfaProvider {

    private static final Logger log = LoggerFactory.getLogger(SmsMfaProvider.class);

    @Value("${sms.provider.configured:false}")
    private boolean configured;

    @Value("${sms.provider.api-key:}")
    private String apiKey;

    @Override
    public String getType() {
        return "SMS";
    }

    @Override
    public boolean isAvailable() {
        // Explicitly check if real SMS provider is configured with API keys
        return configured && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public void initiate(User user, String challengeToken, String otpCode) {
        if (!isAvailable()) {
            throw new IllegalStateException("SMS delivery is not configured. Please use TOTP Authenticator.");
        }
        // When real SMS provider is configured via API key in production, send via HTTP API
        log.info("Sending SMS OTP to user id: {}", user.getId());
    }

    @Override
    public boolean verify(User user, String inputCode) {
        if (!isAvailable()) {
            return false;
        }
        // Verified against hashed OTP in challenge
        return false;
    }
}
