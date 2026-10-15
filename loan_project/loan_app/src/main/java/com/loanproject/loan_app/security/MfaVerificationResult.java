package com.loanproject.loan_app.security;

import com.loanproject.loan_app.bean.User;

public class MfaVerificationResult {

    private final boolean success;
    private final User user;
    private final String errorCode;
    private final String errorMessage;
    private final int attemptsRemaining;

    private MfaVerificationResult(boolean success, User user, String errorCode, String errorMessage, int attemptsRemaining) {
        this.success = success;
        this.user = user;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.attemptsRemaining = attemptsRemaining;
    }

    public static MfaVerificationResult success(User user) {
        return new MfaVerificationResult(true, user, null, null, 0);
    }

    public static MfaVerificationResult failure(String errorCode, String errorMessage, int attemptsRemaining) {
        return new MfaVerificationResult(false, null, errorCode, errorMessage, attemptsRemaining);
    }

    public boolean isSuccess() {
        return success;
    }

    public User getUser() {
        return user;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }
}
