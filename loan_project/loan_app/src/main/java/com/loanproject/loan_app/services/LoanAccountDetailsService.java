package com.loanproject.loan_app.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Collateral;
import com.loanproject.loan_app.bean.Guarantor;
import com.loanproject.loan_app.bean.LinkedAccount;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.dto.CollateralGuarantorSectionDTO;
import com.loanproject.loan_app.dto.CustomerProfileSectionDTO;
import com.loanproject.loan_app.dto.DelinquencySectionDTO;
import com.loanproject.loan_app.dto.LinkedAccountSectionDTO;
import com.loanproject.loan_app.dto.LoanAccountDetailsDTO;
import com.loanproject.loan_app.dto.LoanDetailsSectionDTO;
import com.loanproject.loan_app.dto.OutstandingSummarySectionDTO;
import com.loanproject.loan_app.dto.PaymentHistorySectionDTO;
import com.loanproject.loan_app.dto.RepaymentScheduleSectionDTO;
import com.loanproject.loan_app.dto.SectionResult;
import com.loanproject.loan_app.repository.CollateralRepository;
import com.loanproject.loan_app.repository.GuarantorRepository;
import com.loanproject.loan_app.repository.LinkedAccountRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.DataMaskingUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoanAccountDetailsService {

    private static final Logger log = LoggerFactory.getLogger(LoanAccountDetailsService.class);
    private static final DateTimeFormatter REFRESH_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final LoanRepository loanRepository;
    private final RepaymentScheduleService repaymentScheduleService;
    private final CollateralRepository collateralRepository;
    private final GuarantorRepository guarantorRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final CoreBankingService coreBankingService;
    private final AuthAuditService auditService;

    public LoanAccountDetailsService(LoanRepository loanRepository,
                                     RepaymentScheduleService repaymentScheduleService,
                                     CollateralRepository collateralRepository,
                                     GuarantorRepository guarantorRepository,
                                     LinkedAccountRepository linkedAccountRepository,
                                     CoreBankingService coreBankingService,
                                     AuthAuditService auditService) {
        this.loanRepository = loanRepository;
        this.repaymentScheduleService = repaymentScheduleService;
        this.collateralRepository = collateralRepository;
        this.guarantorRepository = guarantorRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.coreBankingService = coreBankingService;
        this.auditService = auditService;
    }

    /**
     * Orchestrates retrieval of the 360 degree loan account details with partial failure resilience.
     */
    @Transactional
    public LoanAccountDetailsDTO getLoanAccount360Details(User officer, Long loanId, HttpServletRequest request) {
        if (officer == null) {
            throw new AccessDeniedException("Authentication required.");
        }

        // Branch and Role Permission check
        String officerBranch = (officer.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(officer.getBranch()))
                ? "ALL"
                : (officer.getBranch() != null ? officer.getBranch() : "MAIN");

        Loan loan = loanRepository.findById(loanId).orElse(null);

        if (loan == null) {
            auditService.logLoan360AccessDenied(officer.getEmail(), request, loanId, "Loan ID not found.");
            throw new IllegalArgumentException("Loan record not found.");
        }

        // Check if officer's branch allows access to this loan
        if (!"ALL".equals(officerBranch) && !officerBranch.equalsIgnoreCase(loan.getBranch())) {
            auditService.logLoan360AccessDenied(officer.getEmail(), request, loanId,
                    "Officer branch " + officerBranch + " not authorized for loan branch " + loan.getBranch());
            throw new AccessDeniedException("Access denied: You are not authorized to view loan accounts outside your branch.");
        }

        // Customer access control: customers cannot view other customers' 360 views
        if (officer.getRole() == AppRole.ROLE_CUSTOMER && !loan.getUser().getId().equals(officer.getId())) {
            auditService.logLoan360AccessDenied(officer.getEmail(), request, loanId, "Customer attempted IDOR.");
            throw new AccessDeniedException("Forbidden: Unauthorized loan account access.");
        }

        // Audit successful 360 profile access
        auditService.logLoan360Accessed(officer.getEmail(), request, loanId, loan.getBranch());

        LoanAccountDetailsDTO dto = new LoanAccountDetailsDTO();
        dto.setLoanId(loan.getLoanId());
        dto.setAccountNumber(loan.getAccountNumber());
        dto.setBranch(loan.getBranch());
        dto.setCorrelationId("CORR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        dto.setLastRefreshed(REFRESH_FORMATTER.format(Instant.now()));
        dto.setOfficerBranch(officerBranch);
        dto.setUserRole(officer.getRole().name());

        // 1. Customer Profile Section (Isolated try-catch for partial failure)
        try {
            User customer = loan.getUser();
            String maskedPhone = DataMaskingUtils.maskPhone(customer.getPhoneNumber());
            String maskedEmail = DataMaskingUtils.maskEmail(customer.getEmail());
            String dob = customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : "Not Provided";
            String kycStatus = customer.getKycStatus() != null ? customer.getKycStatus() : "VERIFIED";
            String address = customer.getAddress() != null ? customer.getAddress() : "Address on file with branch";
            String contactMethod = customer.getPreferredContactMethod() != null ? customer.getPreferredContactMethod() : "EMAIL";

            CustomerProfileSectionDTO profileDto = new CustomerProfileSectionDTO(
                    customer.getName(),
                    customer.getCustomerId() != null ? customer.getCustomerId() : "CUST" + customer.getId(),
                    dob,
                    kycStatus,
                    address,
                    maskedPhone,
                    maskedEmail,
                    contactMethod
            );
            dto.setCustomerProfileSection(SectionResult.success(profileDto));
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Customer Profile section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setCustomerProfileSection(SectionResult.error("Unable to load customer profile information.", cid));
        }

        // 2. Loan Details Section (Isolated try-catch)
        try {
            BigDecimal sanctioned = BigDecimal.valueOf(loan.getAmount()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal disbursed = BigDecimal.valueOf(loan.getDisbursedAmount() > 0 ? loan.getDisbursedAmount() : loan.getAmount())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal rate = BigDecimal.valueOf(loan.getInterest()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal emi = BigDecimal.valueOf(loan.getEmi()).setScale(2, RoundingMode.HALF_UP);
            String disburseDate = loan.getDisbursementDate() != null ? loan.getDisbursementDate().toString() : "N/A";

            LoanDetailsSectionDTO detailsDto = new LoanDetailsSectionDTO(
                    loan.getAccountNumber(),
                    loan.getProductType() != null ? loan.getProductType() : "Personal Loan",
                    sanctioned,
                    disbursed,
                    disburseDate,
                    rate,
                    loan.getInterestType() != null ? loan.getInterestType() : "FIXED",
                    loan.getMonths(),
                    emi
            );
            detailsDto.setLoanStatus(loan.getStatus() != null ? loan.getStatus() : "APPROVED");
            dto.setLoanDetailsSection(SectionResult.success(detailsDto));
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Loan Details section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setLoanDetailsSection(SectionResult.error("Unable to retrieve authoritative loan product details.", cid));
        }

        // 3. Repayment Schedule Section (Isolated try-catch)
        RepaymentScheduleSectionDTO scheduleData = null;
        try {
            scheduleData = repaymentScheduleService.getRepaymentSchedule(loan, null);
            dto.setRepaymentScheduleSection(SectionResult.success(scheduleData));
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Repayment Schedule section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setRepaymentScheduleSection(SectionResult.error("Repayment schedule calculation is temporarily unavailable.", cid));
        }

        // 4. Delinquency Information Section (Isolated try-catch)
        DelinquencySectionDTO delinquencyData = null;
        try {
            delinquencyData = repaymentScheduleService.calculateDelinquency(scheduleData, loan);
            dto.setDelinquencySection(SectionResult.success(delinquencyData));
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Delinquency section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setDelinquencySection(SectionResult.error("Delinquency tracking is temporarily unavailable.", cid));
        }

        // 5. Outstanding Summary Section (Isolated try-catch)
        try {
            OutstandingSummarySectionDTO outstanding = repaymentScheduleService.calculateOutstandingSummary(loan, delinquencyData);
            dto.setOutstandingSummarySection(SectionResult.success(outstanding));
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Outstanding Summary section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setOutstandingSummarySection(SectionResult.error("Authoritative balance summary calculation failed.", cid));
        }

        // 6. Payment History Section (Isolated try-catch)
        try {
            PaymentHistorySectionDTO history = repaymentScheduleService.getPaymentHistory(loan);
            dto.setPaymentHistorySection(SectionResult.success(history));
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Payment History section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setPaymentHistorySection(SectionResult.error("Payment transaction ledger is temporarily unavailable.", cid));
        }

        // 7. Collateral & Guarantor Section (Role-restricted and isolated)
        try {
            // Role restriction check for collateral/guarantor inspection
            if (officer.getRole() == AppRole.ROLE_CUSTOMER) {
                dto.setCollateralGuarantorSection(SectionResult.unavailable("Collateral and guarantor inspection is restricted to authorized bank officers."));
            } else {
                List<Collateral> collaterals = collateralRepository.findByLoan(loan);
                List<Guarantor> guarantors = guarantorRepository.findByLoan(loan);

                List<CollateralGuarantorSectionDTO.CollateralItemDTO> cList = new ArrayList<>();
                for (Collateral c : collaterals) {
                    cList.add(new CollateralGuarantorSectionDTO.CollateralItemDTO(
                            c.getCollateralType(),
                            c.getCollateralValue(),
                            c.getDescription(),
                            c.getStatus()
                    ));
                }

                List<CollateralGuarantorSectionDTO.GuarantorItemDTO> gList = new ArrayList<>();
                for (Guarantor g : guarantors) {
                    gList.add(new CollateralGuarantorSectionDTO.GuarantorItemDTO(
                            g.getName(),
                            g.getRelationship(),
                            DataMaskingUtils.maskPhone(g.getPhone()),
                            DataMaskingUtils.maskEmail(g.getEmail()),
                            DataMaskingUtils.maskPan(g.getPan())
                    ));
                }

                if (cList.isEmpty() && gList.isEmpty()) {
                    dto.setCollateralGuarantorSection(SectionResult.partial(
                            new CollateralGuarantorSectionDTO(cList, gList),
                            "No registered collateral or third-party guarantor associated with this unsecured loan."
                    ));
                } else {
                    dto.setCollateralGuarantorSection(SectionResult.success(new CollateralGuarantorSectionDTO(cList, gList)));
                }
            }
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Collateral/Guarantor section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setCollateralGuarantorSection(SectionResult.error("Unable to load security or guarantor assets.", cid));
        }

        // 8. Linked Accounts Section (with CBS contract)
        try {
            LinkedAccount linked = linkedAccountRepository.findFirstByLoan(loan).orElse(null);
            if (linked == null) {
                // If not present in local DB, create a default auto-debit linked account record
                linked = new LinkedAccount(
                        loan,
                        "SB" + loan.getUser().getId() + "001234",
                        "SAVINGS",
                        "Patil Capitals Auto-Debit Account",
                        "PATL0001001",
                        true
                );
                linked = linkedAccountRepository.save(linked);
            }

            String maskedAcc = DataMaskingUtils.maskPhone(linked.getAccountNumber()); // Masks leading digits
            SectionResult<BigDecimal> cbsResult = coreBankingService.getLiveAccountBalance(linked.getAccountNumber());

            LinkedAccountSectionDTO linkedDto = new LinkedAccountSectionDTO(
                    maskedAcc,
                    linked.getAccountType(),
                    linked.getBankName(),
                    linked.getIfscCode(),
                    linked.isAutoDebitEnabled(),
                    cbsResult.getData(),
                    cbsResult.getStatus().name(),
                    cbsResult.getErrorMessage()
            );

            if (cbsResult.isUnavailable()) {
                dto.setLinkedAccountSection(SectionResult.partial(linkedDto, cbsResult.getErrorMessage()));
            } else {
                dto.setLinkedAccountSection(SectionResult.success(linkedDto));
            }
        } catch (Exception e) {
            String cid = UUID.randomUUID().toString();
            log.error("Failed loading Linked Account section for loanId={} [correlationId={}]", loanId, cid, e);
            dto.setLinkedAccountSection(SectionResult.error("Linked account auto-debit information is temporarily unavailable.", cid));
        }

        return dto;
    }
}
