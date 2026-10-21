package com.loanproject.loan_app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import com.loanproject.loan_app.dto.LoanSearchRequestDto;
import com.loanproject.loan_app.dto.LoanSearchResponseDto;
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
                if (userDetails.getUser() != null && userDetails.getUser().getBranch() != null) {
                    return userDetails.getUser();
                }
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
            return sessionUser;
        }
        throw new AccessDeniedException("User is not authenticated or MFA not completed.");
    }

    private void addNoCacheHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
    }

    private LoanSearchResponseDto executeSearch(User officer, String searchType, String query,
                                               int page, int size, HttpServletRequest request) {
        String effectiveType = (searchType != null && !searchType.isBlank()) ? searchType.trim().toUpperCase() : "ACCOUNT_NUMBER";
        String authorizedBranch = loanSearchService.getAuthorizedBranch(officer);
        LoanSearchResponseDto response = new LoanSearchResponseDto();
        response.setSearchType(effectiveType);
        response.setPage(page);
        response.setSize(size);
        response.setAuthorizedBranch(authorizedBranch);

        if (query == null || query.trim().isBlank()) {
            return response;
        }

        String cleanQuery = query.trim();

        // Backend input validation without echoing sensitive values in responses or logs
        if ("CUSTOMER_ID".equalsIgnoreCase(effectiveType)) {
            if (!CUSTOMER_ID_PATTERN.matcher(cleanQuery).matches()) {
                response.setErrorMessage("Invalid Customer ID format. Allowed characters: alphanumeric up to 25 characters.");
                return response;
            }
        } else if ("PHONE".equalsIgnoreCase(effectiveType)) {
            if (!PHONE_PATTERN.matcher(cleanQuery).matches()) {
                response.setErrorMessage("Invalid phone number format.");
                return response;
            }
        } else if ("NAME".equalsIgnoreCase(effectiveType)) {
            if (!NAME_PATTERN.matcher(cleanQuery).matches()) {
                response.setErrorMessage("Invalid customer name. Allowed characters: letters, spaces, dots, hyphens (max 50 chars).");
                return response;
            }
        }

        Page<LoanSearchResultDto> results = loanSearchService.searchLoans(officer, effectiveType, cleanQuery, page, size, request);
        response.setContent(results.getContent());
        response.setTotalElements(results.getTotalElements());
        response.setTotalPages(results.getTotalPages());
        response.setFirst(results.isFirst());
        response.setLast(results.isLast());

        if (results.isEmpty()) {
            if ("ACCOUNT_NUMBER".equalsIgnoreCase(effectiveType)) {
                response.setErrorMessage("Account not found");
            } else {
                response.setInfoMessage("No matching loan accounts found in your authorized branch.");
            }
        }

        return response;
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

        LoanSearchResponseDto searchResponse = executeSearch(officer, searchType, query, page, size, request);
        model.addAttribute("query", query.trim());
        model.addAttribute("hasSearched", true);

        if (searchResponse.getErrorMessage() != null) {
            model.addAttribute("errorMessage", searchResponse.getErrorMessage());
            model.addAttribute("results", Page.empty(PageRequest.of(Math.max(0, page), Math.max(1, size))));
            return "loan-search";
        }

        if (searchResponse.getInfoMessage() != null) {
            model.addAttribute("infoMessage", searchResponse.getInfoMessage());
        }

        Page<LoanSearchResultDto> pageResult = new PageImpl<>(
                searchResponse.getContent(),
                PageRequest.of(searchResponse.getPage(), Math.max(1, searchResponse.getSize())),
                searchResponse.getTotalElements()
        );
        model.addAttribute("results", pageResult);
        return "loan-search";
    }

    @PostMapping("/loan-search")
    public String searchPagePost(@RequestParam(value = "searchType", required = false, defaultValue = "ACCOUNT_NUMBER") String searchType,
                                 @RequestParam(value = "query", required = false) String query,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 ModelMap model) {
        return searchPage(searchType, query, page, size, session, request, response, model);
    }

    @PostMapping(value = "/api/loans/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<LoanSearchResponseDto> searchLoansApi(
            @RequestBody(required = false) LoanSearchRequestDto searchRequest,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {
        addNoCacheHeaders(response);
        User officer = getAuthenticatedOfficer(session);

        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customer accounts cannot access loan account searches.");
        }

        String type = (searchRequest != null && searchRequest.getSearchType() != null) ? searchRequest.getSearchType() : "ACCOUNT_NUMBER";
        String q = searchRequest != null ? searchRequest.getQuery() : null;
        int page = searchRequest != null ? searchRequest.getPage() : 0;
        int size = searchRequest != null && searchRequest.getSize() > 0 ? searchRequest.getSize() : 10;

        LoanSearchResponseDto result = executeSearch(officer, type, q, page, size, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/api/loans/search/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSearchConfig(HttpSession session, HttpServletResponse response) {
        addNoCacheHeaders(response);
        User officer = getAuthenticatedOfficer(session);

        if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Customer accounts cannot access loan account searches.");
        }

        Map<String, Object> config = new HashMap<>();
        config.put("authorizedBranch", loanSearchService.getAuthorizedBranch(officer));
        config.put("officerName", officer.getName());
        config.put("role", officer.getRole() != null ? officer.getRole().name() : "");
        return ResponseEntity.ok(config);
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
