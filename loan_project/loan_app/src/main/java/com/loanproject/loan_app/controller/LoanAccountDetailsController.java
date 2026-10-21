package com.loanproject.loan_app.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.dto.LoanAccountDetailsDTO;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanAccountDetailsService;
import com.loanproject.loan_app.services.LoanSearchService;
import com.loanproject.loan_app.services.LoanStatementPdfService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COLLECTIONS_AGENT', 'COLLECTIONS_MANAGER', 'ADMIN')")
public class LoanAccountDetailsController {

    private static final Logger log = LoggerFactory.getLogger(LoanAccountDetailsController.class);

    private final LoanAccountDetailsService loanAccountDetailsService;
    private final LoanStatementPdfService loanStatementPdfService;
    private final LoanSearchService loanSearchService;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final AuthAuditService authAuditService;

    public LoanAccountDetailsController(LoanAccountDetailsService loanAccountDetailsService,
                                        LoanStatementPdfService loanStatementPdfService,
                                        LoanSearchService loanSearchService,
                                        UserRepository userRepository,
                                        LoanRepository loanRepository,
                                        AuthAuditService authAuditService) {
        this.loanAccountDetailsService = loanAccountDetailsService;
        this.loanStatementPdfService = loanStatementPdfService;
        this.loanSearchService = loanSearchService;
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.authAuditService = authAuditService;
    }

    private User getAuthenticatedOfficer(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userRepository.findByEmail(userDetails.getUser().getEmail())
                        .orElse(userDetails.getUser());
            } else if (auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                Optional<User> userOpt = userRepository.findByEmail(auth.getName());
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }
        }
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            return userRepository.findByEmail(sessionUser.getEmail()).orElse(sessionUser);
        }
        throw new AccessDeniedException("User is not authenticated or MFA not completed.");
    }

    private void addNoCacheHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
    }

    /**
     * Unified 360 Degree Loan Account View
     */
    @GetMapping("/loan-details")
    public String getLoanAccountDetails(@RequestParam("loanId") Long loanId,
                                        @RequestParam(value = "refresh", defaultValue = "false") boolean refresh,
                                        HttpSession session,
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        ModelMap model) {
        addNoCacheHeaders(response);
        User officer = getAuthenticatedOfficer(session);

        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customers cannot access officer 360 degree dashboards.");
        }

        if (loanId == null) {
            model.addAttribute("errorMessage", "Invalid loan ID.");
            return "redirect:/loan-search?error=invalid_id";
        }

        try {
            LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officer, loanId, request);
            model.addAttribute("loanDetails", details);
            model.addAttribute("officer", officer);
            model.addAttribute("officerBranch", loanSearchService.getAuthorizedBranch(officer));
            model.addAttribute("isRefreshed", refresh);
            return "loan-account-details";

        } catch (AccessDeniedException e) {
            log.warn("Access denied to 360 view for officer {} on loan {}", officer.getEmail(), loanId);
            model.addAttribute("errorMessage", "Access Denied: You are not authorized to view loan accounts outside your branch.");
            return "redirect:/loan-search?error=unauthorized_branch";

        } catch (IllegalArgumentException e) {
            log.warn("Loan not found for 360 view: loanId={}", loanId);
            model.addAttribute("errorMessage", "Account not found");
            return "redirect:/loan-search?error=not_found";

        } catch (Exception e) {
            log.error("Unexpected error retrieving 360 details for loanId: {}", loanId, e);
            model.addAttribute("errorMessage", "An error occurred loading loan account details. Please try again.");
            return "redirect:/loan-search?error=system_error";
        }
    }

    /**
     * PDF Loan Statement Download
     */
    @GetMapping({"/loan-details/{loanId}/statement.pdf", "/api/staff/loans/{loanId}/statement.pdf"})
    public ResponseEntity<byte[]> downloadLoanStatement(@PathVariable("loanId") Long loanId,
                                                        HttpSession session,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        addNoCacheHeaders(response);
        User officer = getAuthenticatedOfficer(session);

        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            authAuditService.logStatementDownloadDenied(officer.getEmail(), request, loanId, "Customer account denied statement download");
            throw new AccessDeniedException("Forbidden");
        }

        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (loanOpt.isEmpty()) {
            authAuditService.logStatementDownloadDenied(officer.getEmail(), request, loanId, "Loan ID not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"NotFound\",\"message\":\"Loan account ID " + loanId + " not found.\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        Loan loan = loanOpt.get();
        String authorizedBranch = loanSearchService.getAuthorizedBranch(officer);

        if (!"ALL".equals(authorizedBranch) && !authorizedBranch.equalsIgnoreCase(loan.getBranch())) {
            authAuditService.logStatementDownloadDenied(officer.getEmail(), request, loanId,
                    "Branch mismatch: officer branch " + authorizedBranch + " not authorized for loan branch " + loan.getBranch());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"AccessDenied\",\"message\":\"Access denied: Branch mismatch for loan ID " + loanId + ".\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try {
            LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officer, loanId, request);
            byte[] pdfBytes = loanStatementPdfService.generateLoanStatementPdf(details);

            authAuditService.logStatementDownloaded(officer.getEmail(), request, loanId, loan.getBranch());

            String accNum = (loan.getAccountNumber() != null && !loan.getAccountNumber().isBlank())
                    ? loan.getAccountNumber()
                    : "LN" + String.format("%010d", loanId);
            String filename = "loan_statement_" + accNum + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Failed to generate PDF loan statement for loanId: {}", loanId, e);
            authAuditService.logStatementDownloadDenied(officer.getEmail(), request, loanId, "Generation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"GenerationFailed\",\"message\":\"Failed to generate loan statement: " + e.getMessage() + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
