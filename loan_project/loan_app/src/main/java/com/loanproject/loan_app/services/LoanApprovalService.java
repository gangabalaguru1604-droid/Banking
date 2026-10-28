package com.loanproject.loan_app.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanApprovalHistory;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.LoanApprovalHistoryRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.security.AuthAuditService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoanApprovalService {

    private static final Logger log = LoggerFactory.getLogger(LoanApprovalService.class);

    private static final Set<String> PENDING_STATUSES = Set.of(
            "PENDING", "SUBMITTED", "IN_REVIEW", "APPLIED"
    );

    private final LoanRepository loanRepository;
    private final LoanApprovalHistoryRepository approvalHistoryRepository;
    private final RepaymentScheduleService repaymentScheduleService;
    private final AuthAuditService auditService;

    public LoanApprovalService(LoanRepository loanRepository,
                               LoanApprovalHistoryRepository approvalHistoryRepository,
                               RepaymentScheduleService repaymentScheduleService,
                               AuthAuditService auditService) {
        this.loanRepository = loanRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.repaymentScheduleService = repaymentScheduleService;
        this.auditService = auditService;
    }

    /**
     * Resolves the authorized branch filter for the reviewer.
     */
    public String getAuthorizedBranch(User officer) {
        if (officer == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
        if (officer.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(officer.getBranch())) {
            return "ALL";
        }
        return officer.getBranch() != null ? officer.getBranch() : "MAIN";
    }

    /**
     * Validates that the reviewer has permission to decision loans.
     */
    public void validateReviewerAuthorization(User officer) {
        if (officer == null) {
            throw new AccessDeniedException("Unauthenticated access denied.");
        }
        if (officer.getRole() != AppRole.ROLE_LOAN_OFFICER && officer.getRole() != AppRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Access Denied: Only Loan Officers and Administrators are authorized for loan approval workflows. Role: " + officer.getRole());
        }
    }

    /**
     * Validates branch isolation for a specific loan.
     */
    public void validateBranchAccess(Loan loan, User officer) {
        validateReviewerAuthorization(officer);
        String authorizedBranch = getAuthorizedBranch(officer);
        if (!"ALL".equalsIgnoreCase(authorizedBranch)) {
            String loanBranch = loan.getBranch() != null ? loan.getBranch() : "MAIN";
            if (!authorizedBranch.equalsIgnoreCase(loanBranch)) {
                throw new AccessDeniedException(String.format(
                        "Branch Isolation Violation: Loan #%d belongs to branch '%s', but officer is restricted to '%s'",
                        loan.getLoanId(), loanBranch, authorizedBranch
                ));
            }
        }
    }

    /**
     * Retrieves all pending applications for the reviewer's authorized branch.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public List<Loan> getPendingLoans(User officer) {
        validateReviewerAuthorization(officer);
        String branch = getAuthorizedBranch(officer);
        return loanRepository.findPendingLoansByBranch(branch);
    }

    /**
     * Retrieves detailed application info for review.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public Loan getPendingLoanDetails(Long loanId, User officer) {
        validateReviewerAuthorization(officer);
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null.");
        }
        Loan loan = loanRepository.findWithUserById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanId));
        validateBranchAccess(loan, officer);
        return loan;
    }

    /**
     * Approves a pending loan application.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public Loan approveLoan(Long loanId, User officer, String comments, HttpServletRequest request) {
        validateReviewerAuthorization(officer);
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null.");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanId));

        validateBranchAccess(loan, officer);

        // Server-side state transition revalidation
        String currentStatus = loan.getStatus() != null ? loan.getStatus().toUpperCase() : "UNKNOWN";
        if (!PENDING_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException(String.format(
                    "Invalid State Transition: Loan application #%d cannot be approved because it is currently '%s'. Only pending applications can be approved.",
                    loanId, currentStatus
            ));
        }

        String previousStatus = loan.getStatus();
        loan.setStatus("ACTIVE");
        loan.setDisbursementDate(LocalDate.now());
        loan.setDisbursedAmount(loan.getAmount());
        if (loan.getRemaining() <= 0) {
            loan.setRemaining(loan.getAmount());
        }

        // Assign formal loan account number if not present
        if (loan.getAccountNumber() == null || loan.getAccountNumber().isBlank()) {
            String accNum = String.format("LN%010d", Math.abs(loan.getLoanId()));
            if (loanRepository.findByAccountNumber(accNum).isPresent()) {
                accNum = String.format("LN%08d%02d", Math.abs(loan.getLoanId()), (int) (Math.random() * 90 + 10));
            }
            loan.setAccountNumber(accNum);
        }

        Loan savedLoan = loanRepository.saveAndFlush(loan);

        // Generate and persist amortization schedule
        repaymentScheduleService.generateAndSaveAmortizationSchedule(savedLoan);

        // Persist decision in LoanApprovalHistory
        String effectiveComments = (comments != null && !comments.isBlank()) ? comments.trim() : "Approved by " + officer.getName();
        LoanApprovalHistory history = new LoanApprovalHistory(
                savedLoan,
                "APPROVED",
                previousStatus,
                "ACTIVE",
                officer.getEmail(),
                officer.getName(),
                officer.getRole().name(),
                effectiveComments,
                savedLoan.getBranch()
        );
        approvalHistoryRepository.save(history);

        // Commit audit event
        if (auditService != null) {
            auditService.logLoanApplicationApproved(officer.getEmail(), request, loanId, savedLoan.getBranch());
        }

        log.info("LOAN APPROVED: LoanId={}, Account={}, Approver={}, Branch={}",
                loanId, savedLoan.getAccountNumber(), officer.getEmail(), savedLoan.getBranch());

        return savedLoan;
    }

    /**
     * Rejects a pending loan application.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public Loan rejectLoan(Long loanId, String reason, User officer, HttpServletRequest request) {
        validateReviewerAuthorization(officer);
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null.");
        }

        // Strict rejection reason validation
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("A valid rejection reason is mandatory and cannot be blank.");
        }
        String cleanReason = reason.trim();
        if (cleanReason.length() < 3 || cleanReason.length() > 500) {
            throw new IllegalArgumentException("Rejection reason must be between 3 and 500 characters in length.");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanId));

        validateBranchAccess(loan, officer);

        // Server-side state transition revalidation
        String currentStatus = loan.getStatus() != null ? loan.getStatus().toUpperCase() : "UNKNOWN";
        if (!PENDING_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException(String.format(
                    "Invalid State Transition: Loan application #%d cannot be rejected because it is currently '%s'. Only pending applications can be rejected.",
                    loanId, currentStatus
            ));
        }

        String previousStatus = loan.getStatus();
        loan.setStatus("REJECTED");

        Loan savedLoan = loanRepository.saveAndFlush(loan);

        // Persist decision in LoanApprovalHistory
        LoanApprovalHistory history = new LoanApprovalHistory(
                savedLoan,
                "REJECTED",
                previousStatus,
                "REJECTED",
                officer.getEmail(),
                officer.getName(),
                officer.getRole().name(),
                cleanReason,
                savedLoan.getBranch()
        );
        approvalHistoryRepository.save(history);

        // Commit audit event
        if (auditService != null) {
            auditService.logLoanApplicationRejected(officer.getEmail(), request, loanId, cleanReason, savedLoan.getBranch());
        }

        log.info("LOAN REJECTED: LoanId={}, Reviewer={}, Branch={}, Reason={}",
                loanId, officer.getEmail(), savedLoan.getBranch(), cleanReason);

        return savedLoan;
    }

    /**
     * Fetches approval history for a given loan application.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public List<LoanApprovalHistory> getApprovalHistory(Long loanId, User officer) {
        validateReviewerAuthorization(officer);
        return approvalHistoryRepository.findByLoan_LoanIdOrderByDecisionTimestampDesc(loanId);
    }
}
