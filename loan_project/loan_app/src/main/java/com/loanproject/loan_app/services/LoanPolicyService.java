package com.loanproject.loan_app.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.loanproject.loan_app.policy.LoanSlab;

/**
 * Dedicated service for loan policy rules, configurable amount slabs,
 * interest rate determination, tenure assignment, and EMI computation.
 */
@Service
public class LoanPolicyService {

    private static final Logger log = LoggerFactory.getLogger(LoanPolicyService.class);

    @Value("${loan.policy.min-amount:${loan.product.min-amount:100.0}}")
    private double minAmount = 100.0;

    @Value("${loan.policy.max-amount:${loan.product.max-amount:10000000.0}}")
    private double maxAmount = 10000000.0;

    @Value("${loan.policy.slabs:}")
    private String slabsConfig;

    @Value("${loan.product.default.interest-rate:10.5}")
    private double defaultInterestRate = 10.5;

    @Value("${loan.product.default.tenure-months:24}")
    private int defaultTenureMonths = 24;

    private final List<LoanSlab> slabs = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (slabsConfig != null && !slabsConfig.trim().isEmpty()) {
            parseConfiguredSlabs(slabsConfig);
        }

        if (slabs.isEmpty()) {
            loadDefaultSampleSlabs();
        }
    }

    /**
     * Parses slabs configured in property string:
     * Format: min-max:rate:tenure or name:min-max:rate:tenure (comma separated)
     * e.g. "100-50000:12.0:12,50000.01-200000:10.5:24,200000.01-1000000:9.5:36"
     */
    public synchronized void parseConfiguredSlabs(String config) {
        slabs.clear();
        try {
            String[] tokens = config.split(",");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) continue;

                String[] parts = trimmed.split(":");
                if (parts.length == 3) {
                    // range:rate:tenure
                    String[] range = parts[0].split("-");
                    double min = Double.parseDouble(range[0].trim());
                    double max = Double.parseDouble(range[1].trim());
                    double rate = Double.parseDouble(parts[1].trim());
                    int tenure = Integer.parseInt(parts[2].trim());
                    slabs.add(new LoanSlab(min, max, rate, tenure));
                } else if (parts.length == 4) {
                    // name:range:rate:tenure
                    String name = parts[0].trim();
                    String[] range = parts[1].split("-");
                    double min = Double.parseDouble(range[0].trim());
                    double max = Double.parseDouble(range[1].trim());
                    double rate = Double.parseDouble(parts[2].trim());
                    int tenure = Integer.parseInt(parts[3].trim());
                    slabs.add(new LoanSlab(name, min, max, rate, tenure));
                }
            }
            slabs.sort(Comparator.comparingDouble(LoanSlab::getMinAmount));
            log.info("Loaded {} loan slabs from configuration.", slabs.size());
        } catch (Exception e) {
            log.error("Failed to parse loan.policy.slabs configuration: '{}'. Falling back to defaults.", config, e);
            slabs.clear();
            loadDefaultSampleSlabs();
        }
    }

    /**
     * Populates default sample loan slabs spanning from ₹100 to ₹1,00,00,000.
     * These can be updated dynamically via backend configuration or setSlabs().
     */
    public synchronized void loadDefaultSampleSlabs() {
        slabs.clear();
        slabs.add(new LoanSlab("Micro Starter Loan", 100.0, 50000.0, 12.0, 12));
        slabs.add(new LoanSlab("Personal Classic Loan", 50000.01, 200000.0, 10.5, 24));
        slabs.add(new LoanSlab("Prime Growth Loan", 200000.01, 1000000.0, 9.5, 36));
        slabs.add(new LoanSlab("Executive Elite Loan", 1000000.01, 5000000.0, 8.5, 60));
        slabs.add(new LoanSlab("Corporate Premier Loan", 5000000.01, 10000000.0, 7.5, 84));
        slabs.sort(Comparator.comparingDouble(LoanSlab::getMinAmount));
        log.info("Initialized default loan policy with {} sample slabs.", slabs.size());
    }

    /**
     * Validates loan amount against permitted bank policy bounds.
     */
    public void validateAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < minAmount || amount > maxAmount) {
            throw new IllegalArgumentException(String.format("Loan amount must be between ₹%,.2f and ₹%,.2f", minAmount, maxAmount));
        }
    }

    /**
     * Resolves the applicable loan slab for a given loan amount.
     */
    public LoanSlab getSlabForAmount(double amount) {
        validateAmount(amount);

        // Find exact match
        for (LoanSlab slab : slabs) {
            if (slab.matches(amount)) {
                return slab;
            }
        }

        // Handle floating-point boundary continuity
        for (LoanSlab slab : slabs) {
            if (amount <= slab.getMaxAmount()) {
                return slab;
            }
        }

        if (!slabs.isEmpty()) {
            return slabs.get(slabs.size() - 1);
        }

        // Fallback default slab
        return new LoanSlab("Standard Policy", minAmount, maxAmount, defaultInterestRate, defaultTenureMonths);
    }

    /**
     * Calculates the estimated monthly EMI using the standard reducing-balance formula.
     * Formula: EMI = [P * r * (1 + r)^n] / [(1 + r)^n - 1]
     * where r = annualInterestRate / (12 * 100), n = tenureMonths
     */
    public double calculateEmi(double principal, double annualInterestRate, int tenureMonths) {
        if (principal <= 0 || annualInterestRate <= 0 || tenureMonths <= 0) {
            return 0.0;
        }
        double r = annualInterestRate / (12 * 100);
        double emi = (principal * r * Math.pow(1 + r, tenureMonths)) /
                     (Math.pow(1 + r, tenureMonths) - 1);
        return Math.round(emi * 100.0) / 100.0;
    }

    public List<LoanSlab> getSlabs() {
        return Collections.unmodifiableList(slabs);
    }

    public synchronized void setSlabs(List<LoanSlab> newSlabs) {
        this.slabs.clear();
        if (newSlabs != null) {
            this.slabs.addAll(newSlabs);
            this.slabs.sort(Comparator.comparingDouble(LoanSlab::getMinAmount));
        }
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

    public double getDefaultInterestRate() {
        return defaultInterestRate;
    }

    public void setDefaultInterestRate(double defaultInterestRate) {
        this.defaultInterestRate = defaultInterestRate;
    }

    public int getDefaultTenureMonths() {
        return defaultTenureMonths;
    }

    public void setDefaultTenureMonths(int defaultTenureMonths) {
        this.defaultTenureMonths = defaultTenureMonths;
    }
}
