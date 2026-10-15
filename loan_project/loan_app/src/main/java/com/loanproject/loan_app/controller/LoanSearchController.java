package com.loanproject.loan_app.controller;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.dto.CustomerProfileDto;
import com.loanproject.loan_app.dto.LoanSearchResultDto;
import com.loanproject.loan_app.dto.RevealRequestDto;
import com.loanproject.loan_app.dto.RevealResponseDto;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanSearchService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COLLECTIONS_AGENT', 'COLLECTIONS_MANAGER', 'ADMIN')")
public class LoanSearchController {

    private static final Logger log = LoggerFactory.getLogger(LoanSearchController.class);

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[A-Za-z]{2}[0-9]{10}$");
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,25}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{1,20}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z\\s.'\\-]{1,50}$");

    private final LoanSearchService loanSearchService;
    private final UserRepository userRepository;

    public LoanSearchController(LoanSearchService loanSearchService, UserRepository userRepository) {
        this.loanSearchService = loanSearchService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedOfficer(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                // Refresh officer from repository to ensure fresh branch data
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

    @GetMapping("/loan-search")
    public String searchPage(@RequestParam(value = "searchType", required = false, defaultValue = "ACCOUNT_NUMBER") String searchType,
                             @RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size,
                             HttpSession session,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             ModelMap model) {
        addNoCacheHeaders(response);
        User officer = getAuthenticatedOfficer(session);

        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customer accounts cannot access loan account searches.");
        }

        model.addAttribute("officer", officer);
        model.addAttribute("officerBranch", loanSearchService.getAuthorizedBranch(officer));
        model.addAttribute("searchType", searchType);
        model.addAttribute("page", page);
        model.addAttribute("size", size);

        if (query == null || query.trim().isBlank()) {
            model.addAttribute("query", "");
            model.addAttribute("hasSearched", false);
            return "loan-search";
        }

        String cleanQuery = query.trim();
        model.addAttribute("query", cleanQuery);
        model.addAttribute("hasSearched", true);

        // Input validation according to searchType
        if ("CUSTOMER_ID".equalsIgnoreCase(searchType)) {
            if (!CUSTOMER_ID_PATTERN.matcher(cleanQuery).matches()) {
                model.addAttribute("errorMessage", "Invalid Customer ID format. Allowed characters: alphanumeric up to 25 characters.");
                model.addAttribute("results", Page.empty());
                return "loan-search";
            }
        } else if ("PHONE".equalsIgnoreCase(searchType)) {
            if (!PHONE_PATTERN.matcher(cleanQuery).matches()) {
                model.addAttribute("errorMessage", "Invalid phone number format.");
                model.addAttribute("results", Page.empty());
                return "loan-search";
            }
        } else if ("NAME".equalsIgnoreCase(searchType)) {
            if (!NAME_PATTERN.matcher(cleanQuery).matches()) {
                model.addAttribute("errorMessage", "Invalid customer name. Allowed characters: letters, spaces, dots, hyphens (max 50 chars).");
                model.addAttribute("results", Page.empty());
                return "loan-search";
            }
        }

        Page<LoanSearchResultDto> results = loanSearchService.searchLoans(officer, searchType, cleanQuery, page, size, request);
        model.addAttribute("results", results);

        if (results.isEmpty()) {
            if ("ACCOUNT_NUMBER".equalsIgnoreCase(searchType)) {
                model.addAttribute("errorMessage", "Account not found");
            } else {
                model.addAttribute("infoMessage", "No matching loan accounts found in your authorized branch.");
            }
        }

        return "loan-search";
    }

    @GetMapping("/customer-profile")
    public String customerProfile(@RequestParam("loanId") Long loanId,
                                  HttpSession session,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  ModelMap model) {
        addNoCacheHeaders(response);
        User officer = getAuthenticatedOfficer(session);

        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customer accounts cannot access customer profiles.");
        }

        if (loanId == null) {
            model.addAttribute("errorMessage", "Account not found");
            return "redirect:/loan-search?searchType=ACCOUNT_NUMBER&error=invalid_id";
        }

        Optional<CustomerProfileDto> profileOpt;
        try {
            profileOpt = loanSearchService.getCustomerProfile(officer, loanId, request);
        } catch (AccessDeniedException e) {
            // Prevent unauthorized branch enumeration: generic error or redirect
            model.addAttribute("errorMessage", "Access Denied: You are not authorized to view loan records outside your branch.");
            return "redirect:/loan-search?error=unauthorized_branch";
        }

        if (profileOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Account not found");
            return "redirect:/loan-search?error=not_found";
        }

        model.addAttribute("profile", profileOpt.get());
        model.addAttribute("officer", officer);
        model.addAttribute("officerBranch", loanSearchService.getAuthorizedBranch(officer));
        return "customer-profile";
    }

    @PostMapping(value = "/loan-search/reveal", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<RevealResponseDto> revealSensitiveField(
            @RequestBody(required = false) RevealRequestDto requestDto,
            @RequestParam(value = "loanId", required = false) Long paramLoanId,
            @RequestParam(value = "field", required = false) String paramField,
            @RequestParam(value = "reason", required = false) String paramReason,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {
        addNoCacheHeaders(response);

        User officer;
        try {
            officer = getAuthenticatedOfficer(session);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RevealResponseDto.error("Authentication required."));
        }

        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(RevealResponseDto.error("Access Denied: Customer accounts cannot reveal sensitive data."));
        }

        Long loanId = requestDto != null && requestDto.getLoanId() != null ? requestDto.getLoanId() : paramLoanId;
        String field = requestDto != null && requestDto.getField() != null ? requestDto.getField() : paramField;
        String reason = requestDto != null && requestDto.getReason() != null ? requestDto.getReason() : paramReason;

        if (loanId == null || field == null || reason == null || reason.trim().length() < 8) {
            return ResponseEntity.badRequest().body(
                    RevealResponseDto.error("Invalid request: Loan ID, field, and justification (minimum 8 characters) are required."));
        }

        RevealResponseDto result = loanSearchService.revealSensitiveField(officer, loanId, field, reason, request);

        if (!result.isSuccess()) {
            if (result.getMessage() != null && result.getMessage().toLowerCase().contains("unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
            }
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}
