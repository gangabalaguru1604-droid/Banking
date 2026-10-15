package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class OutstandingSummarySectionDTO implements Serializable {

    private BigDecimal principalOutstanding;
    private BigDecimal accruedInterest;
    private BigDecimal penaltiesAndLateFees;
    private BigDecimal totalPayable;
    private BigDecimal foreclosureAmount;
    private boolean foreclosureAvailable;
    private String foreclosureExplanation;

    public OutstandingSummarySectionDTO() {
    }

    public OutstandingSummarySectionDTO(BigDecimal principalOutstanding, BigDecimal accruedInterest,
                                       BigDecimal penaltiesAndLateFees, BigDecimal totalPayable,
                                       BigDecimal foreclosureAmount, boolean foreclosureAvailable,
                                       String foreclosureExplanation) {
        this.principalOutstanding = principalOutstanding;
        this.accruedInterest = accruedInterest;
        this.penaltiesAndLateFees = penaltiesAndLateFees;
        this.totalPayable = totalPayable;
        this.foreclosureAmount = foreclosureAmount;
        this.foreclosureAvailable = foreclosureAvailable;
        this.foreclosureExplanation = foreclosureExplanation;
    }

    public BigDecimal getPrincipalOutstanding() {
        return principalOutstanding;
    }

    public void setPrincipalOutstanding(BigDecimal principalOutstanding) {
        this.principalOutstanding = principalOutstanding;
    }

    public BigDecimal getAccruedInterest() {
        return accruedInterest;
    }

    public void setAccruedInterest(BigDecimal accruedInterest) {
        this.accruedInterest = accruedInterest;
    }

    public BigDecimal getPenaltiesAndLateFees() {
        return penaltiesAndLateFees;
    }

    public void setPenaltiesAndLateFees(BigDecimal penaltiesAndLateFees) {
        this.penaltiesAndLateFees = penaltiesAndLateFees;
    }

    public BigDecimal getTotalPayable() {
        return totalPayable;
    }

    public void setTotalPayable(BigDecimal totalPayable) {
        this.totalPayable = totalPayable;
    }

    public BigDecimal getForeclosureAmount() {
        return foreclosureAmount;
    }

    public void setForeclosureAmount(BigDecimal foreclosureAmount) {
        this.foreclosureAmount = foreclosureAmount;
    }

    public boolean isForeclosureAvailable() {
        return foreclosureAvailable;
    }

    public void setForeclosureAvailable(boolean foreclosureAvailable) {
        this.foreclosureAvailable = foreclosureAvailable;
    }

    public String getForeclosureExplanation() {
        return foreclosureExplanation;
    }

    public void setForeclosureExplanation(String foreclosureExplanation) {
        this.foreclosureExplanation = foreclosureExplanation;
    }
}
