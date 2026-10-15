package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RepaymentScheduleSectionDTO implements Serializable {

    private List<RepaymentInstallmentDTO> installments = new ArrayList<>();
    private int totalInstallments;
    private int paidCount;
    private int overdueCount;

    public RepaymentScheduleSectionDTO() {
    }

    public RepaymentScheduleSectionDTO(List<RepaymentInstallmentDTO> installments, int totalInstallments, int paidCount, int overdueCount) {
        this.installments = installments != null ? installments : new ArrayList<>();
        this.totalInstallments = totalInstallments;
        this.paidCount = paidCount;
        this.overdueCount = overdueCount;
    }

    public List<RepaymentInstallmentDTO> getInstallments() {
        return installments;
    }

    public void setInstallments(List<RepaymentInstallmentDTO> installments) {
        this.installments = installments;
    }

    public int getTotalInstallments() {
        return totalInstallments;
    }

    public void setTotalInstallments(int totalInstallments) {
        this.totalInstallments = totalInstallments;
    }

    public int getPaidCount() {
        return paidCount;
    }

    public void setPaidCount(int paidCount) {
        this.paidCount = paidCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }
}
