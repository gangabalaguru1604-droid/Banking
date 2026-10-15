package com.loanproject.loan_app.services;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.dto.CustomerProfileDto;
import com.loanproject.loan_app.dto.LoanDetailDto;
import com.loanproject.loan_app.dto.LoanSearchResultDto;
import com.loanproject.loan_app.dto.RevealResponseDto;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.DataMaskingUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoanSearchService {

    private static final Logger log = LoggerFactory.getLogger(LoanSearchService.class);
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[A-Za-z]{2}[0-9]{10}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final LoanRepository loanRepository;
    private final AuthAuditService auditService;

    public LoanSearchService(LoanRepository loanRepository, AuthAuditService auditService) {
        this.loanRepository = loanRepository;
        this.auditService = auditService;
    }

    /**
     * Resolves the officer's authorized branch filter. Admins have access to ALL branches.
     */
    public String getAuthorizedBranch(User officer) {
        if (officer.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(officer.getBranch())) {
            return "ALL";
        }
        return officer.getBranch() != null ? officer.getBranch() : "MAIN";
    }

    /**
     * Searches for loans using primary (account number) or secondary (customerId, phone, name) parameters.
     */
    @Transactional(readOnly = true)
    public Page<LoanSearchResultDto> searchLoans(User officer, String searchType, String query,
                                                 int page, int size, HttpServletRequest request) {
        if (officer == null || officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Unauthorized: Customer accounts cannot perform loan account searches.");
        }

        String branchFilter = getAuthorizedBranch(officer);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));

        if (query == null || query.trim().isBlank()) {
            return Page.empty(pageable);
        }

        String cleanQuery = query.trim();
        Page<Loan> loanPage;

        if ("ACCOUNT_NUMBER".equalsIgnoreCase(searchType)) {
            // Validate exact format: 2 letters + 10 digits (Length 12)
            if (!ACCOUNT_NUMBER_PATTERN.matcher(cleanQuery).matches()) {
                log.info("Invalid loan account number format rejected: {}", cleanQuery.length());
                auditService.logLoanSearchNotFound(officer.getEmail(), request, "ACCOUNT_NUMBER_INVALID_FORMAT");
                return Page.empty(pageable);
            }

            Optional<Loan> loanOpt = loanRepository.findByAccountNumberAndBranchFilter(cleanQuery.toUpperCase(), branchFilter);
            if (loanOpt.isPresent()) {
                loanPage = new PageImpl<>(List.of(loanOpt.get()), pageable, 1);
            } else {
                auditService.logLoanSearchNotFound(officer.getEmail(), request, "ACCOUNT_NUMBER");
                return Page.empty(pageable);
            }
        } else if ("CUSTOMER_ID".equalsIgnoreCase(searchType)) {
            loanPage = loanRepository.searchByCustomerId(cleanQuery, branchFilter, pageable);
        } else if ("PHONE".equalsIgnoreCase(searchType)) {
            String digitsOnly = cleanQuery.replaceAll("[^0-9]", "");
            loanPage = loanRepository.searchByPhone(digitsOnly, branchFilter, pageable);
        } else if ("NAME".equalsIgnoreCase(searchType)) {
            // Length restriction on name queries to prevent DOS
            String safeName = cleanQuery.length() > 50 ? cleanQuery.substring(0, 50) : cleanQuery;
            loanPage = loanRepository.searchByName(safeName, branchFilter, pageable);
        } else {
            return Page.empty(pageable);
        }

        if (loanPage.isEmpty()) {
            auditService.logLoanSearchNotFound(officer.getEmail(), request, searchType);
        } else {
            auditService.logLoanSearch(officer.getEmail(), request, searchType, (int) loanPage.getTotalElements(), branchFilter);
        }

        return loanPage.map(this::mapToSearchResultDto);
    }

    /**
     * Loads a customer profile and associated loan details, enforcing branch authorization.
     */
    @Transactional(readOnly = true)
    public Optional<CustomerProfileDto> getCustomerProfile(User officer, Long loanId, HttpServletRequest request) {
        if (officer == null || officer.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Unauthorized: Customer accounts cannot access loan profiles.");
        }

        String branchFilter = getAuthorizedBranch(officer);
        Optional<Loan> primaryLoanOpt = loanRepository.findByLoanIdAndBranchFilter(loanId, branchFilter);

        if (primaryLoanOpt.isEmpty()) {
            // Check if loan exists in another branch to record unauthorized access
            Optional<Loan> anyBranchLoan = loanRepository.findById(loanId);
            if (anyBranchLoan.isPresent()) {
                auditService.logProfileAccessDenied(officer.getEmail(), request, loanId,
                        "Officer branch " + officer.getBranch() + " not authorized for loan branch " + anyBranchLoan.get().getBranch());
                throw new AccessDeniedException("Access denied: Loan belongs to another branch.");
            }
            auditService.logLoanSearchNotFound(officer.getEmail(), request, "PROFILE_ID_NOT_FOUND");
            return Optional.empty();
        }

        Loan primaryLoan = primaryLoanOpt.get();
        User customer = primaryLoan.getUser();

        // Audit successful profile access
        auditService.logProfileAccessed(officer.getEmail(), request, loanId, primaryLoan.getBranch());

        // Fetch all loans for this customer within the officer's permitted branch filter
        List<Loan> customerLoans = loanRepository.findByUserOrderByCreatedAtDesc(customer).stream()
                .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                .collect(Collectors.toList());

        CustomerProfileDto profile = new CustomerProfileDto();
        profile.setUserId(customer.getId());
        profile.setCustomerId(customer.getCustomerId() != null ? customer.getCustomerId() : "CUST" + customer.getId());
        profile.setCustomerName(customer.getName());
        profile.setBranch(customer.getBranch());
        profile.setPrimaryLoanId(primaryLoan.getLoanId());
        profile.setPrimaryAccountNumber(primaryLoan.getAccountNumber());

        // Mask all sensitive fields by default
        profile.setMaskedPhone(DataMaskingUtils.maskPhone(customer.getPhoneNumber()));
        profile.setMaskedEmail(DataMaskingUtils.maskEmail(customer.getEmail()));
        profile.setMaskedPan(DataMaskingUtils.maskPan(customer.getPanNumber()));
        profile.setMaskedAadhaar(DataMaskingUtils.maskAadhaar(customer.getAadhaarNumber()));
        profile.setContactDetailUpdateRequired(customer.isContactDetailUpdateRequired());

        List<LoanDetailDto> loanDtos = customerLoans.stream()
                .map(this::mapToLoanDetailDto)
                .collect(Collectors.toList());
        profile.setLoans(loanDtos);

        return Optional.of(profile);
    }

    /**
     * Server-side reveal endpoint: authorizes officer, records required purpose, audits access, and returns single unmasked field.
     */
    @Transactional
    public RevealResponseDto revealSensitiveField(User officer, Long loanId, String field, String reason, HttpServletRequest request) {
        if (officer == null || officer.getRole() == AppRole.ROLE_CUSTOMER) {
            return RevealResponseDto.error("Forbidden: Unauthorized role.");
        }

        if (reason == null || reason.trim().length() < 8) {
            return RevealResponseDto.error("Invalid request: A valid business justification (minimum 8 characters) is required.");
        }

        String branchFilter = getAuthorizedBranch(officer);
        Optional<Loan> loanOpt = loanRepository.findByLoanIdAndBranchFilter(loanId, branchFilter);

        if (loanOpt.isEmpty()) {
            auditService.logSensitiveDataRevealDenied(officer.getEmail(), request, loanId, field, "Loan not found or branch unauthorized");
            return RevealResponseDto.error("Unauthorized: Loan not found in your assigned branch.");
        }

        Loan loan = loanOpt.get();
        User customer = loan.getUser();
        String correlationId = UUID.randomUUID().toString();
        String unmaskedValue;

        if ("AADHAAR".equalsIgnoreCase(field)) {
            unmaskedValue = customer.getAadhaarNumber() != null ? customer.getAadhaarNumber() : "Not Provided";
        } else if ("PAN".equalsIgnoreCase(field)) {
            unmaskedValue = customer.getPanNumber() != null ? customer.getPanNumber() : "Not Provided";
        } else if ("PHONE".equalsIgnoreCase(field)) {
            unmaskedValue = customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "Not Provided";
        } else if ("EMAIL".equalsIgnoreCase(field)) {
            unmaskedValue = customer.getEmail() != null ? customer.getEmail() : "Not Provided";
        } else {
            return RevealResponseDto.error("Invalid field requested.");
        }

        // Audit the reveal WITHOUT logging the actual sensitive plaintext value
        auditService.logSensitiveDataRevealed(officer.getEmail(), request, loanId, field.toUpperCase(), reason.trim(), correlationId);

        return RevealResponseDto.success(field.toUpperCase(), unmaskedValue, correlationId);
    }

    private LoanSearchResultDto mapToSearchResultDto(Loan loan) {
        User u = loan.getUser();
        String customerId = u != null && u.getCustomerId() != null ? u.getCustomerId() : (u != null ? "CUST" + u.getId() : "N/A");
        String customerName = u != null ? u.getName() : "Unknown";
        String maskedPhone = u != null ? DataMaskingUtils.maskPhone(u.getPhoneNumber()) : "N/A";

        return new LoanSearchResultDto(
                loan.getLoanId(),
                loan.getAccountNumber(),
                customerId,
                customerName,
                maskedPhone,
                loan.getAmount(),
                loan.getRemaining(),
                loan.getEmi(),
                loan.getStatus(),
                loan.getBranch()
        );
    }

    private LoanDetailDto mapToLoanDetailDto(Loan loan) {
        String created = loan.getCreatedAt() != null ? DATE_FORMATTER.format(loan.getCreatedAt()) : "N/A";
        return new LoanDetailDto(
                loan.getLoanId(),
                loan.getAccountNumber(),
                loan.getAmount(),
                loan.getInterest(),
                loan.getMonths(),
                loan.getEmi(),
                loan.getRemaining(),
                loan.getStatus(),
                loan.getBranch(),
                created
        );
    }
}
