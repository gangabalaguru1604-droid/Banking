package com.loanproject.loan_app.dto;

public class RevealResponseDto {

    private boolean success;
    private String field;
    private String value;
    private String errorMessage;
    private String correlationId;

    public RevealResponseDto() {
    }

    public static RevealResponseDto success(String field, String value, String correlationId) {
        RevealResponseDto dto = new RevealResponseDto();
        dto.setSuccess(true);
        dto.setField(field);
        dto.setValue(value);
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static RevealResponseDto error(String errorMessage) {
        RevealResponseDto dto = new RevealResponseDto();
        dto.setSuccess(false);
        dto.setErrorMessage(errorMessage);
        return dto;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMessage() {
        return errorMessage;
    }

    public void setMessage(String message) {
        this.errorMessage = message;
    }

    public String getUnmaskedValue() {
        return value;
    }

    public void setUnmaskedValue(String unmaskedValue) {
        this.value = unmaskedValue;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
