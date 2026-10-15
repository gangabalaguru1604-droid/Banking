package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class DelinquencySectionDTO implements Serializable {

    private int dpd;
    private BigDecimal overdueAmount;
    private String lastReminderSent;
    private String delinquencyBucket; // CURRENT, SMA-0, SMA-1, SMA-2, NPA
    private String accountStatus; // STANDARD, SPECIAL_MENTION, NON_PERFORMING

    public DelinquencySectionDTO() {
    }

    public DelinquencySectionDTO(int dpd, BigDecimal overdueAmount, String lastReminderSent,
                                String delinquencyBucket, String accountStatus) {
        this.dpd = dpd;
        this.overdueAmount = overdueAmount != null ? overdueAmount : BigDecimal.ZERO;
        this.lastReminderSent = lastReminderSent;
        this.delinquencyBucket = delinquencyBucket;
        this.accountStatus = accountStatus;
    }

    public int getDpd() {
        return dpd;
    }

    public void setDpd(int dpd) {
        this.dpd = dpd;
    }

    public BigDecimal getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(BigDecimal overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public String getLastReminderSent() {
        return lastReminderSent;
    }

    public void setLastReminderSent(String lastReminderSent) {
        this.lastReminderSent = lastReminderSent;
    }

    public String getDelinquencyBucket() {
        return delinquencyBucket;
    }

    public void setDelinquencyBucket(String delinquencyBucket) {
        this.delinquencyBucket = delinquencyBucket;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
}
