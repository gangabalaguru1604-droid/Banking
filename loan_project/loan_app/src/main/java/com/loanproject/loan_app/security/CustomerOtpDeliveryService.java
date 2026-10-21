package com.loanproject.loan_app.security;

import com.loanproject.loan_app.bean.User;
import jakarta.servlet.http.HttpServletRequest;

public interface CustomerOtpDeliveryService {

    /**
     * Delivers dynamic time-limited OTP to customer via configured verification channel (e.g. Email).
     *
     * @param customer Authenticated customer account
     * @param otpCode Plaintext OTP code to send (never logged)
     * @param challengeToken Challenge identifier
     * @param request HTTP request context
     * @return true if successfully delivered, false otherwise
     */
    boolean deliverOtp(User customer, String otpCode, String challengeToken, HttpServletRequest request);

    /**
     * Test hook to retrieve the last delivered OTP for automated test verification without leaking to logs.
     */
    String getLastDeliveredOtpForTesting(String email);

    /**
     * Test hook to simulate delivery failures to verify resilient error handling and non-bypass.
     */
    void setSimulateDeliveryFailure(boolean simulateFailure);

    /**
     * Returns true if delivery is in simulated mode (e.g. when real SMTP credentials are not configured).
     */
    boolean isSimulatedDelivery();
}
