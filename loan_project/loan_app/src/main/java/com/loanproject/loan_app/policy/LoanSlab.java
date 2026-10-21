package com.loanproject.loan_app.policy;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a configurable loan amount slab defining permitted loan limits,
 * applicable annual interest rate, and standard repayment tenure in months.
 */
public class LoanSlab implements Serializable {

    private static final long serialVersionUID = 1L;

    private String slabName;
    private double minAmount;
    private double maxAmount;
    private double interestRate; // Annual interest percentage (e.g. 10.5 for 10.5% p.a.)
    private int tenureMonths;    // Standard repayment tenure in months (e.g. 24)

    public LoanSlab() {
    }

    public LoanSlab(double minAmount, double maxAmount, double interestRate, int tenureMonths) {
        this("Slab ₹" + Math.round(minAmount) + " - ₹" + Math.round(maxAmount), minAmount, maxAmount, interestRate, tenureMonths);
    }

    public LoanSlab(String slabName, double minAmount, double maxAmount, double interestRate, int tenureMonths) {
        this.slabName = slabName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
    }

    public boolean matches(double amount) {
        return amount >= (minAmount - 0.0001) && amount <= (maxAmount + 0.0001);
    }

    public String getSlabName() {
        return slabName;
    }

    public void setSlabName(String slabName) {
        this.slabName = slabName;
    }

    public double getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(double minAmount) {
        this.minAmount = minAmount;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanSlab loanSlab = (LoanSlab) o;
        return Double.compare(loanSlab.minAmount, minAmount) == 0 &&
                Double.compare(loanSlab.maxAmount, maxAmount) == 0 &&
                Double.compare(loanSlab.interestRate, interestRate) == 0 &&
                tenureMonths == loanSlab.tenureMonths;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minAmount, maxAmount, interestRate, tenureMonths);
    }

    @Override
    public String toString() {
        return "LoanSlab{" +
                "slabName='" + slabName + '\'' +
                ", minAmount=" + minAmount +
                ", maxAmount=" + maxAmount +
                ", interestRate=" + interestRate +
                ", tenureMonths=" + tenureMonths +
                '}';
    }
}
