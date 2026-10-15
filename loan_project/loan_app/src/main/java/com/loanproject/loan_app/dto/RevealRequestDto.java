package com.loanproject.loan_app.dto;

public class RevealRequestDto {

    private Long loanId;
    private String field;
    private String reason;

    public RevealRequestDto() {
    }

    public RevealRequestDto(Long loanId, String field, String reason) {
        this.loanId = loanId;
        this.field = field;
        this.reason = reason;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
