package com.loanproject.loan_app.faces.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

@Component("loanPolicySettingsBean")
@SessionScope
public class LoanPolicySettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LoanPolicySettingsBean.class);

    private final LoanService loanService;
    private final UserRepository userRepository;
    private final AuthAuditService auditService;

    // Active Policy Settings
    private double defaultInterestRate;
    private int defaultTenureMonths;
    private double minLoanAmount;
    private double maxLoanAmount;

    // Policy parameters for editing
    private double editInterestRate;
    private int editTenureMonths;
    private double editMinAmount;
    private double editMaxAmount;

    // Interactive Policy EMI Simulator
    private double simAmount = 100000.0;
    private double simInterestRate = 10.5;
    private int simTenureMonths = 24;
    private double simCalculatedEmi = 0.0;
    private double simTotalPayable = 0.0;
    private double simTotalInterest = 0.0;
    private List<SimAmortizationRow> sampleSchedule = new ArrayList<>();

    // Feedback
    private String successMessage;
    private String errorMessage;

    public LoanPolicySettingsBean(LoanService loanService,
                                  UserRepository userRepository,
                                  AuthAuditService auditService) {
        this.loanService = loanService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        loadPolicySettings();
        calculateSimulation();
    }

    public void loadPolicySettings() {
        this.successMessage = null;
        this.errorMessage = null;

        this.defaultInterestRate = loanService.getDefaultInterestRate();
        this.defaultTenureMonths = loanService.getDefaultTenureMonths();
        this.minLoanAmount = loanService.getMinAmount();
        this.maxLoanAmount = loanService.getMaxAmount();

        this.editInterestRate = this.defaultInterestRate;
        this.editTenureMonths = this.defaultTenureMonths;
        this.editMinAmount = this.minLoanAmount;
        this.editMaxAmount = this.maxLoanAmount;

        this.simInterestRate = this.defaultInterestRate;
        this.simTenureMonths = this.defaultTenureMonths;
    }

    public void savePolicySettings() {
        this.successMessage = null;
        this.errorMessage = null;

        User currentUser = getAuthenticatedUser();
        if (currentUser == null) {
            this.errorMessage = "Session expired or user not authenticated.";
            return;
        }

        // Only Administrators can update bank underwriting policy limits
        if (currentUser.getRole() != AppRole.ROLE_ADMIN) {
            this.errorMessage = "Access Denied: Only administrators are authorized to modify bank loan policy limits.";
            throw new AccessDeniedException(this.errorMessage);
        }

        // Server-side policy validation
        if (this.editInterestRate < 0.1 || this.editInterestRate > 100.0) {
            this.errorMessage = "Annual Interest Rate must be between 0.1% and 100.0%.";
            return;
        }

        if (this.editTenureMonths < 1 || this.editTenureMonths > 360) {
            this.errorMessage = "Standard Tenure must be between 1 and 360 months.";
            return;
        }

        if (this.editMinAmount <= 0) {
            this.errorMessage = "Minimum loan amount must be greater than zero.";
            return;
        }

        if (this.editMaxAmount < this.editMinAmount) {
            this.errorMessage = "Maximum loan amount cannot be less than the minimum loan amount.";
            return;
        }

        // Apply changes to loanService
        loanService.setDefaultInterestRate(this.editInterestRate);
        loanService.setDefaultTenureMonths(this.editTenureMonths);
        loanService.setMinAmount(this.editMinAmount);
        loanService.setMaxAmount(this.editMaxAmount);

        this.defaultInterestRate = this.editInterestRate;
        this.defaultTenureMonths = this.editTenureMonths;
        this.minLoanAmount = this.editMinAmount;
        this.maxLoanAmount = this.editMaxAmount;

        // Security Audit Log
        HttpServletRequest req = getHttpServletRequest();
        auditService.logLoanPolicyUpdated(currentUser.getEmail(), req,
                "Rate=" + this.defaultInterestRate + "%, Tenure=" + this.defaultTenureMonths + "mos, Min=₹" + this.minLoanAmount + ", Max=₹" + this.maxLoanAmount);

        this.successMessage = "Loan underwriting policy updated successfully!";
        log.info("Admin {} updated bank loan policy: Rate={}%, Tenure={}mos, Min={}, Max={}",
                currentUser.getEmail(), this.defaultInterestRate, this.defaultTenureMonths, this.minLoanAmount, this.maxLoanAmount);

        // Update simulation defaults
        this.simInterestRate = this.defaultInterestRate;
        this.simTenureMonths = this.defaultTenureMonths;
        calculateSimulation();
    }

    public void calculateSimulation() {
        if (this.simAmount <= 0 || this.simInterestRate <= 0 || this.simTenureMonths <= 0) {
            this.simCalculatedEmi = 0.0;
            this.simTotalPayable = 0.0;
            this.simTotalInterest = 0.0;
            this.sampleSchedule = new ArrayList<>();
            return;
        }

        double r = this.simInterestRate / (12 * 100);
        this.simCalculatedEmi = (this.simAmount * r * Math.pow(1 + r, this.simTenureMonths)) /
                                (Math.pow(1 + r, this.simTenureMonths) - 1);
        this.simTotalPayable = this.simCalculatedEmi * this.simTenureMonths;
        this.simTotalInterest = this.simTotalPayable - this.simAmount;

        // Generate up to 6 months sample amortization preview
        this.sampleSchedule = new ArrayList<>();
        double balance = this.simAmount;
        int previewMonths = Math.min(6, this.simTenureMonths);
        for (int i = 1; i <= previewMonths; i++) {
            double interestPortion = balance * r;
            double principalPortion = this.simCalculatedEmi - interestPortion;
            balance = Math.max(0, balance - principalPortion);
            this.sampleSchedule.add(new SimAmortizationRow(i, this.simCalculatedEmi, principalPortion, interestPortion, balance));
        }
    }

    public boolean isUserAdmin() {
        User user = getAuthenticatedUser();
        return user != null && user.getRole() == AppRole.ROLE_ADMIN;
    }

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        if (auth.getName() != null) {
            return userRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null && ctx.getExternalContext() != null) {
                Object req = ctx.getExternalContext().getRequest();
                if (req instanceof HttpServletRequest httpReq) {
                    return httpReq;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static class SimAmortizationRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int month;
        private final double emi;
        private final double principal;
        private final double interest;
        private final double balance;

        public SimAmortizationRow(int month, double emi, double principal, double interest, double balance) {
            this.month = month;
            this.emi = emi;
            this.principal = principal;
            this.interest = interest;
            this.balance = balance;
        }

        public int getMonth() { return month; }
        public double getEmi() { return emi; }
        public double getPrincipal() { return principal; }
        public double getInterest() { return interest; }
        public double getBalance() { return balance; }
    }

    // Getters and Setters
    public double getDefaultInterestRate() { return defaultInterestRate; }
    public int getDefaultTenureMonths() { return defaultTenureMonths; }
    public double getMinLoanAmount() { return minLoanAmount; }
    public double getMaxLoanAmount() { return maxLoanAmount; }

    public double getEditInterestRate() { return editInterestRate; }
    public void setEditInterestRate(double editInterestRate) { this.editInterestRate = editInterestRate; }

    public int getEditTenureMonths() { return editTenureMonths; }
    public void setEditTenureMonths(int editTenureMonths) { this.editTenureMonths = editTenureMonths; }

    public double getEditMinAmount() { return editMinAmount; }
    public void setEditMinAmount(double editMinAmount) { this.editMinAmount = editMinAmount; }

    public double getEditMaxAmount() { return editMaxAmount; }
    public void setEditMaxAmount(double editMaxAmount) { this.editMaxAmount = editMaxAmount; }

    public double getSimAmount() { return simAmount; }
    public void setSimAmount(double simAmount) { this.simAmount = simAmount; }

    public double getSimInterestRate() { return simInterestRate; }
    public void setSimInterestRate(double simInterestRate) { this.simInterestRate = simInterestRate; }

    public int getSimTenureMonths() { return simTenureMonths; }
    public void setSimTenureMonths(int simTenureMonths) { this.simTenureMonths = simTenureMonths; }

    public double getSimCalculatedEmi() { return simCalculatedEmi; }
    public double getSimTotalPayable() { return simTotalPayable; }
    public double getSimTotalInterest() { return simTotalInterest; }
    public List<SimAmortizationRow> getSampleSchedule() { return sampleSchedule; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
