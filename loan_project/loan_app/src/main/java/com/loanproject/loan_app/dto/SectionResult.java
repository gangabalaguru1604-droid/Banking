package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.util.UUID;

public class SectionResult<T> implements Serializable {

    private SectionStatus status;
    private T data;
    private String errorMessage;
    private String correlationId;

    public SectionResult() {
    }

    public SectionResult(SectionStatus status, T data, String errorMessage, String correlationId) {
        this.status = status;
        this.data = data;
        this.errorMessage = errorMessage;
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    public static <T> SectionResult<T> success(T data) {
        return new SectionResult<>(SectionStatus.SUCCESS, data, null, UUID.randomUUID().toString());
    }

    public static <T> SectionResult<T> partial(T data, String message) {
        return new SectionResult<>(SectionStatus.PARTIAL, data, message, UUID.randomUUID().toString());
    }

    public static <T> SectionResult<T> unavailable(String explanation) {
        return new SectionResult<>(SectionStatus.UNAVAILABLE, null, explanation, UUID.randomUUID().toString());
    }

    public static <T> SectionResult<T> error(String errorMessage, String correlationId) {
        return new SectionResult<>(SectionStatus.ERROR, null, errorMessage, correlationId != null ? correlationId : UUID.randomUUID().toString());
    }

    public boolean isSuccess() {
        return status == SectionStatus.SUCCESS;
    }

    public boolean isPartial() {
        return status == SectionStatus.PARTIAL;
    }

    public boolean isUnavailable() {
        return status == SectionStatus.UNAVAILABLE;
    }

    public boolean isError() {
        return status == SectionStatus.ERROR;
    }

    public SectionStatus getStatus() {
        return status;
    }

    public void setStatus(SectionStatus status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
