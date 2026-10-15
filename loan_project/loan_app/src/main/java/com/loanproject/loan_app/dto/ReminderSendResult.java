package com.loanproject.loan_app.dto;

/**
 * Result returned by an external communication provider (Email or SMS).
 */
public class ReminderSendResult {

    private boolean success;
    private String providerMessageId;
    private boolean transientFailure;
    private String errorMessage;

    public ReminderSendResult() {
    }

    public static ReminderSendResult success(String providerMessageId) {
        ReminderSendResult result = new ReminderSendResult();
        result.setSuccess(true);
        result.setProviderMessageId(providerMessageId);
        return result;
    }

    public static ReminderSendResult failure(String errorMessage, boolean transientFailure) {
        ReminderSendResult result = new ReminderSendResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setTransientFailure(transientFailure);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public boolean isTransientFailure() {
        return transientFailure;
    }

    public void setTransientFailure(boolean transientFailure) {
        this.transientFailure = transientFailure;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
