package com.loanproject.loan_app.faces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanApprovalHistory;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.security.DataMaskingUtils;
import com.loanproject.loan_app.services.LoanApprovalService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

@Component("loanApprovalBean")
@SessionScope
public class LoanApprovalBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LoanApprovalBean.class);

    private final LoanApprovalService loanApprovalService;
    private final UserRepository userRepository;

    // View Model Properties
    private List<Loan> pendingLoans = new ArrayList<>();
    private Loan selectedLoan;
    private Long selectedLoanId;
    private List<LoanApprovalHistory> selectedLoanHistory = new ArrayList<>();

    private String searchQuery = "";
    private String statusFilter = "ALL";

    private String approvalComments = "";
    private String rejectionReason = "";

    private String successMessage;
    private String errorMessage;

    private boolean processing = false;

    public LoanApprovalBean(LoanApprovalService loanApprovalService, UserRepository userRepository) {
        this.loanApprovalService = loanApprovalService;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        loadPendingApplications();
    }

    /**
     * Loads pending applications applying current search and status filters.
     */
    public void loadPendingApplications() {
        try {
            User officer = getAuthenticatedOfficer();
            if (officer == null) {
                this.pendingLoans = new ArrayList<>();
                return;
            }

            List<Loan> allPending = loanApprovalService.getPendingLoans(officer);
            if (allPending == null) {
                allPending = new ArrayList<>();
            }

            // Apply search filter and status filter
            this.pendingLoans = allPending.stream()
                    .filter(loan -> {
                        if (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter)) {
                            if (!statusFilter.equalsIgnoreCase(loan.getStatus())) {
                                return false;
                            }
                        }
                        if (searchQuery != null && !searchQuery.trim().isBlank()) {
                            String q = searchQuery.trim().toLowerCase();
                            String loanIdStr = String.valueOf(loan.getLoanId());
                            String accNum = loan.getAccountNumber() != null ? loan.getAccountNumber().toLowerCase() : "";
                            String custName = loan.getUser() != null && loan.getUser().getName() != null
                                    ? loan.getUser().getName().toLowerCase() : "";
                            String custId = loan.getUser() != null && loan.getUser().getCustomerId() != null
                                    ? loan.getUser().getCustomerId().toLowerCase() : "";

                            return loanIdStr.contains(q) || accNum.contains(q) || custName.contains(q) || custId.contains(q);
                        }
                        return true;
                    })
                    .toList();

        } catch (Exception e) {
            log.error("Error loading pending loan applications", e);
            this.errorMessage = "Unable to load pending loan applications: " + e.getMessage();
        }
    }

    public void search() {
        clearMessages();
        loadPendingApplications();
    }

    public void resetSearch() {
        this.searchQuery = "";
        this.statusFilter = "ALL";
        clearMessages();
        loadPendingApplications();
    }

    /**
     * Selects a loan application for full details review.
     */
    public void selectApplication(Long loanId) {
        clearMessages();
        this.selectedLoanId = loanId;
        this.approvalComments = "";
        this.rejectionReason = "";

        if (loanId == null) {
            this.selectedLoan = null;
            this.selectedLoanHistory = new ArrayList<>();
            return;
        }

        try {
            User officer = getAuthenticatedOfficer();
            this.selectedLoan = loanApprovalService.getPendingLoanDetails(loanId, officer);
            this.selectedLoanHistory = loanApprovalService.getApprovalHistory(loanId, officer);
        } catch (Exception e) {
            log.error("Failed to load details for loan application #" + loanId, e);
            this.errorMessage = "Could not load loan details: " + e.getMessage();
            this.selectedLoan = null;
        }
    }

    /**
     * Closes the review view.
     */
    public void closeDetails() {
        this.selectedLoan = null;
        this.selectedLoanId = null;
        this.selectedLoanHistory = new ArrayList<>();
        this.approvalComments = "";
        this.rejectionReason = "";
        clearMessages();
    }

    /**
     * Approves the currently selected loan.
     */
    public String approve() {
        clearMessages();
        if (selectedLoan == null && selectedLoanId == null) {
            this.errorMessage = "No loan application selected for approval.";
            return null;
        }

        Long targetLoanId = selectedLoan != null ? selectedLoan.getLoanId() : selectedLoanId;

        if (processing) {
            this.errorMessage = "A decision is already being processed. Please wait.";
            return null;
        }

        try {
            this.processing = true;
            User officer = getAuthenticatedOfficer();
            HttpServletRequest request = getHttpServletRequest();

            Loan approved = loanApprovalService.approveLoan(targetLoanId, officer, approvalComments, request);

            this.successMessage = String.format(
                    "Loan Application #%d has been successfully APPROVED! Assigned Account Number: %s",
                    approved.getLoanId(), approved.getAccountNumber()
            );
            closeDetails();
            loadPendingApplications();

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Business rule rejection during approval of loan {}: {}", targetLoanId, e.getMessage());
            this.errorMessage = e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error approving loan #" + targetLoanId, e);
            this.errorMessage = "Approval failed: " + e.getMessage();
        } finally {
            this.processing = false;
        }

        return null;
    }

    /**
     * Rejects the currently selected loan with a validated reason.
     */
    public String reject() {
        clearMessages();
        if (selectedLoan == null && selectedLoanId == null) {
            this.errorMessage = "No loan application selected for rejection.";
            return null;
        }

        Long targetLoanId = selectedLoan != null ? selectedLoan.getLoanId() : selectedLoanId;

        if (rejectionReason == null || rejectionReason.trim().isBlank()) {
            this.errorMessage = "A specific rejection reason is mandatory before rejecting an application.";
            return null;
        }

        if (rejectionReason.trim().length() < 3 || rejectionReason.trim().length() > 500) {
            this.errorMessage = "Rejection reason must be between 3 and 500 characters.";
            return null;
        }

        if (processing) {
            this.errorMessage = "A decision is already being processed. Please wait.";
            return null;
        }

        try {
            this.processing = true;
            User officer = getAuthenticatedOfficer();
            HttpServletRequest request = getHttpServletRequest();

            Loan rejected = loanApprovalService.rejectLoan(targetLoanId, rejectionReason, officer, request);

            this.successMessage = String.format(
                    "Loan Application #%d has been REJECTED. Reason recorded: %s",
                    rejected.getLoanId(), rejectionReason.trim()
            );
            closeDetails();
            loadPendingApplications();

        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Business rule rejection during rejection of loan {}: {}", targetLoanId, e.getMessage());
            this.errorMessage = e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error rejecting loan #" + targetLoanId, e);
            this.errorMessage = "Rejection failed: " + e.getMessage();
        } finally {
            this.processing = false;
        }

        return null;
    }

    public void clearMessages() {
        this.successMessage = null;
        this.errorMessage = null;
    }

    // Security & Request Helpers
    public User getAuthenticatedOfficer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            User principalUser = userDetails.getUser();
            return userRepository.findByEmail(principalUser.getEmail()).orElse(principalUser);
        }
        return null;
    }

    public boolean isOfficerAuthorized() {
        User u = getAuthenticatedOfficer();
        return u != null && (u.getRole() == AppRole.ROLE_LOAN_OFFICER || u.getRole() == AppRole.ROLE_ADMIN);
    }

    public String getCurrentOfficerName() {
        User u = getAuthenticatedOfficer();
        return u != null ? u.getName() : "Bank Reviewer";
    }

    public String getCurrentOfficerRole() {
        User u = getAuthenticatedOfficer();
        return u != null ? u.getRole().getDisplayName() : "";
    }

    public String getCurrentOfficerBranch() {
        User u = getAuthenticatedOfficer();
        return u != null ? loanApprovalService.getAuthorizedBranch(u) : "MAIN";
    }

    private HttpServletRequest getHttpServletRequest() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            Object req = context.getExternalContext().getRequest();
            if (req instanceof HttpServletRequest httpReq) {
                return httpReq;
            }
        }
        return null;
    }

    // Masking & Presentation Utility Methods for JSF Facelets
    public String maskCustomerId(String id) {
        if (id == null || id.isBlank()) return "N/A";
        String clean = id.trim();
        if (clean.length() <= 4) return "CUST***" + clean;
        return clean.substring(0, 4) + "***" + clean.substring(clean.length() - 2);
    }

    public String maskPhone(String phone) {
        return DataMaskingUtils.maskPhone(phone);
    }

    public String maskEmail(String email) {
        return DataMaskingUtils.maskEmail(email);
    }

    public String maskAadhaar(String aadhaar) {
        return DataMaskingUtils.maskAadhaar(aadhaar);
    }

    public String maskPan(String pan) {
        return DataMaskingUtils.maskPan(pan);
    }

    public String maskAccountNumber(String acc) {
        return DataMaskingUtils.maskAccountNumber(acc);
    }

    public String formatCurrency(double amount) {
        return String.format("₹%,.2f", amount);
    }

    // Getters and Setters
    public List<Loan> getPendingLoans() {
        return pendingLoans;
    }

    public int getPendingLoansCount() {
        return pendingLoans != null ? pendingLoans.size() : 0;
    }

    public Loan getSelectedLoan() {
        return selectedLoan;
    }

    public void setSelectedLoan(Loan selectedLoan) {
        this.selectedLoan = selectedLoan;
    }

    public Long getSelectedLoanId() {
        return selectedLoanId;
    }

    public void setSelectedLoanId(Long selectedLoanId) {
        this.selectedLoanId = selectedLoanId;
    }

    public List<LoanApprovalHistory> getSelectedLoanHistory() {
        return selectedLoanHistory;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public String getApprovalComments() {
        return approvalComments;
    }

    public void setApprovalComments(String approvalComments) {
        this.approvalComments = approvalComments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isProcessing() {
        return processing;
    }
}
