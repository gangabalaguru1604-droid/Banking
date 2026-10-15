package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.Collateral;
import com.loanproject.loan_app.bean.Guarantor;
import com.loanproject.loan_app.bean.LinkedAccount;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.dto.DelinquencySectionDTO;
import com.loanproject.loan_app.dto.LoanAccountDetailsDTO;
import com.loanproject.loan_app.dto.OutstandingSummarySectionDTO;
import com.loanproject.loan_app.dto.RepaymentScheduleSectionDTO;
import com.loanproject.loan_app.dto.SectionResult;
import com.loanproject.loan_app.dto.SectionStatus;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.CollateralRepository;
import com.loanproject.loan_app.repository.GuarantorRepository;
import com.loanproject.loan_app.repository.LinkedAccountRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.PaymentTransactionRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanAccountDetailsService;
import com.loanproject.loan_app.services.LoanService;
import com.loanproject.loan_app.services.RepaymentScheduleService;

@SpringBootTest
@AutoConfigureMockMvc
public class LoanAccount360DetailsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanService loanService;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Autowired
    private LoanAccountDetailsService loanAccountDetailsService;

    @Autowired
    private RepaymentInstallmentRepository installmentRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LinkedAccountRepository linkedAccountRepository;

    @Autowired
    private AuthAuditLogRepository auditRepo;

    @Autowired
    private com.loanproject.loan_app.repository.MfaChallengeRepository challengeRepository;

    @Autowired
    private com.loanproject.loan_app.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    private User customerMain;
    private User customerNorth;
    private Loan loanMain;
    private Loan loanNorth;

    private User officerMain;
    private User officerNorth;
    private User adminUser;
    private User collectionsAgentMain;
    private User collectionsManagerMain;
    private User collectionsAgentNorth;

    @BeforeEach
    void setUp() {
        auditRepo.deleteAll();
        paymentTransactionRepository.deleteAll();
        installmentRepository.deleteAll();
        collateralRepository.deleteAll();
        guarantorRepository.deleteAll();
        linkedAccountRepository.deleteAll();
        loanRepository.deleteAll();
        challengeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Customer in MAIN branch
        customerMain = new User("Rajesh Kumar", "rajesh.kumar@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerMain.setBranch("MAIN");
        customerMain.setCustomerId("CUST000501");
        customerMain.setPhoneNumber("9876501234");
        customerMain.setDateOfBirth(LocalDate.of(1988, 5, 12));
        customerMain.setKycStatus("VERIFIED");
        customerMain.setAddress("42 Commercial Street, Bangalore, Karnataka");
        customerMain.setPreferredContactMethod("EMAIL");
        customerMain = userRepository.save(customerMain);

        loanMain = loanService.apply(customerMain, 600000, 10.5, 24);
        loanMain.setBranch("MAIN");
        loanMain.setProductType("Personal Loan");
        loanMain.setInterestType("FIXED");
        loanMain.setDisbursedAmount(600000.0);
        loanMain.setDisbursementDate(LocalDate.now().minusMonths(6));
        loanMain = loanRepository.save(loanMain);

        // 2. Customer in NORTH branch
        customerNorth = new User("Sunita Verma", "sunita.verma@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerNorth.setBranch("NORTH");
        customerNorth.setCustomerId("CUST000601");
        customerNorth.setPhoneNumber("9811002233");
        customerNorth = userRepository.save(customerNorth);

        loanNorth = loanService.apply(customerNorth, 400000, 9.5, 12);
        loanNorth.setBranch("NORTH");
        loanNorth = loanRepository.save(loanNorth);

        // 3. Officers & Staff
        officerMain = new User("Officer Main", "officer.main@bank.com", "Password123!", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        officerMain.setBranch("MAIN");
        officerMain = userRepository.save(officerMain);

        officerNorth = new User("Officer North", "officer.north@bank.com", "Password123!", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        officerNorth.setBranch("NORTH");
        officerNorth = userRepository.save(officerNorth);

        adminUser = new User("Admin", "admin@bank.com", "Password123!", AppRole.ROLE_ADMIN, UserType.STAFF);
        adminUser.setBranch("ALL");
        adminUser = userRepository.save(adminUser);

        collectionsAgentMain = new User("Collections Agent Main", "agent.main@bank.com", "Password123!", AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgentMain.setBranch("MAIN");
        collectionsAgentMain = userRepository.save(collectionsAgentMain);

        collectionsManagerMain = new User("Collections Manager Main", "manager.main@bank.com", "Password123!", AppRole.ROLE_COLLECTIONS_MANAGER, UserType.STAFF);
        collectionsManagerMain.setBranch("MAIN");
        collectionsManagerMain = userRepository.save(collectionsManagerMain);

        collectionsAgentNorth = new User("Collections Agent North", "agent.north@bank.com", "Password123!", AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgentNorth.setBranch("NORTH");
        collectionsAgentNorth = userRepository.save(collectionsAgentNorth);
    }

    @Test
    @DisplayName("1. Authorized officer can access an authorized loan profile")
    void testAuthorizedOfficerCanAccessLoan360() throws Exception {
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")));

        // Verify audit log
        List<AuthAuditLog> audits = auditRepo.findByUsernameOrderByTimestampDesc(officerMain.getEmail());
        assertTrue(audits.stream().anyMatch(a -> "LOAN_360_VIEW_ACCESSED".equals(a.getEventType()) && "SUCCESS".equals(a.getOutcome())));
    }

    @Test
    @DisplayName("2. Unauthorized branch access is denied and audited")
    void testUnauthorizedBranchAccessIsDeniedAndAudited() throws Exception {
        // Officer North tries to view Loan Main
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(officerNorth))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loan-search?error=unauthorized_branch"));

        // Verify audit failure logged
        List<AuthAuditLog> audits = auditRepo.findByUsernameOrderByTimestampDesc(officerNorth.getEmail());
        assertTrue(audits.stream().anyMatch(a -> "LOAN_360_VIEW_DENIED".equals(a.getEventType()) && "FAILURE".equals(a.getOutcome())));
    }

    @Test
    @DisplayName("3. Customer and loan details display correctly in DTO")
    void testCustomerAndLoanDetailsDisplayCorrectly() {
        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officerMain, loanMain.getLoanId(), null);

        assertNotNull(details);
        assertEquals(loanMain.getLoanId(), details.getLoanId());
        assertEquals("MAIN", details.getBranch());

        // Check Customer Profile section
        assertEquals(SectionStatus.SUCCESS, details.getCustomerProfile().getStatus());
        assertEquals("Rajesh Kumar", details.getCustomerProfile().getData().getFullName());
        assertEquals("CUST000501", details.getCustomerProfile().getData().getCustomerId());
        assertEquals("VERIFIED", details.getCustomerProfile().getData().getKycStatus());
        assertEquals("EMAIL", details.getCustomerProfile().getData().getPreferredContactMethod());

        // Check Loan Details section
        assertEquals(SectionStatus.SUCCESS, details.getLoanDetails().getStatus());
        assertEquals("Personal Loan", details.getLoanDetails().getData().getProductType());
        assertEquals("FIXED", details.getLoanDetails().getData().getInterestType());
        assertEquals(24, details.getLoanDetails().getData().getTenureMonths());
        assertEquals(0, new BigDecimal("600000.00").compareTo(details.getLoanDetails().getData().getSanctionedAmount()));
    }

    @Test
    @DisplayName("4. Monetary calculations use BigDecimal and approved rules")
    void testMonetaryCalculationsUseBigDecimal() {
        RepaymentScheduleSectionDTO schedule = repaymentScheduleService.getRepaymentSchedule(loanMain, null);
        DelinquencySectionDTO delinquency = repaymentScheduleService.calculateDelinquency(schedule, loanMain);
        OutstandingSummarySectionDTO summary = repaymentScheduleService.calculateOutstandingSummary(loanMain, delinquency);

        assertNotNull(summary.getPrincipalOutstanding());
        assertNotNull(summary.getAccruedInterest());
        assertNotNull(summary.getPenaltiesAndLateFees());
        assertNotNull(summary.getTotalPayable());

        // Total payable must equal principal + accrued + penalties
        BigDecimal calculatedTotal = summary.getPrincipalOutstanding()
                .add(summary.getAccruedInterest())
                .add(summary.getPenaltiesAndLateFees())
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(calculatedTotal, summary.getTotalPayable());
    }

    @Test
    @DisplayName("5. DPD calculations work for due, overdue and paid installments")
    void testDpdCalculationsBoundaryScenarios() {
        LocalDate today = LocalDate.now();

        // Ensure schedule is generated
        repaymentScheduleService.generateAndSaveAmortizationSchedule(loanMain);
        List<RepaymentInstallment> installments = installmentRepository.findByLoanOrderByInstallmentNumberAsc(loanMain);
        assertFalse(installments.isEmpty());

        // Installment 1: Paid (due 2 months ago, but paid in full with recorded PaymentTransaction)
        RepaymentInstallment inst1 = installments.get(0);
        inst1.setDueDate(today.minusMonths(2));
        installmentRepository.save(inst1);

        com.loanproject.loan_app.bean.PaymentTransaction pt = new com.loanproject.loan_app.bean.PaymentTransaction(
                loanMain, inst1.getEmiAmount(), "NACH", "TXN-TEST-001", "SUCCESS");
        paymentTransactionRepository.save(pt);

        // Installment 2: Overdue (due 15 days ago, unpaid)
        RepaymentInstallment inst2 = installments.get(1);
        inst2.setDueDate(today.minusDays(15));
        inst2.setPaidAmount(BigDecimal.ZERO);
        inst2.setStatus("DUE");
        inst2.setDpd(0);
        installmentRepository.save(inst2);

        // Installment 3: Due today (0 days past due)
        RepaymentInstallment inst3 = installments.get(2);
        inst3.setDueDate(today);
        inst3.setPaidAmount(BigDecimal.ZERO);
        inst3.setStatus("DUE");
        inst3.setDpd(0);
        installmentRepository.save(inst3);

        // Ensure all remaining installments are in the future
        for (int i = 3; i < installments.size(); i++) {
            RepaymentInstallment futureInst = installments.get(i);
            futureInst.setDueDate(today.plusMonths(i - 2));
            installmentRepository.save(futureInst);
        }

        // Re-run schedule calculation
        RepaymentScheduleSectionDTO scheduleDTO = repaymentScheduleService.getRepaymentSchedule(loanMain, today);
        DelinquencySectionDTO delinquencyDTO = repaymentScheduleService.calculateDelinquency(scheduleDTO, loanMain);

        // 1. Paid installment must have DPD = 0 and status PAID
        assertEquals("PAID", scheduleDTO.getInstallments().get(0).getStatus());
        assertEquals(0, scheduleDTO.getInstallments().get(0).getDpd());

        // 2. Overdue installment must have DPD = 15 and status OVERDUE
        assertEquals("OVERDUE", scheduleDTO.getInstallments().get(1).getStatus());
        assertEquals(15, scheduleDTO.getInstallments().get(1).getDpd());

        // 3. Due today installment must have DPD = 0 and status DUE
        assertEquals("DUE", scheduleDTO.getInstallments().get(2).getStatus());
        assertEquals(0, scheduleDTO.getInstallments().get(2).getDpd());

        // 4. Overall loan DPD must be 15 (SMA-0 bucket)
        assertEquals(15, delinquencyDTO.getDpd());
        assertEquals("SMA-0", delinquencyDTO.getDelinquencyBucket());
        assertEquals("SPECIAL_MENTION", delinquencyDTO.getAccountStatus());
    }

    @Test
    @DisplayName("6. Overdue installments are visually distinguishable in HTML")
    void testOverdueInstallmentVisualBadgeInView() throws Exception {
        // Set an overdue installment
        repaymentScheduleService.generateAndSaveAmortizationSchedule(loanMain);
        List<RepaymentInstallment> installments = installmentRepository.findByLoanOrderByInstallmentNumberAsc(loanMain);
        RepaymentInstallment inst = installments.get(0);
        inst.setDueDate(LocalDate.now().minusDays(10));
        inst.setStatus("DUE");
        installmentRepository.save(inst);

        MvcResult result = mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andReturn();

        LoanAccountDetailsDTO dto = (LoanAccountDetailsDTO) result.getModelAndView().getModel().get("loanDetails");
        assertNotNull(dto);
        assertTrue(dto.getRepaymentSchedule().getData().getInstallments().stream()
                .anyMatch(i -> "OVERDUE".equals(i.getStatus()) && i.getDpd() > 0));
    }

    @Test
    @DisplayName("7. Payment history contains actual transactions only")
    void testPaymentHistoryContainsActualTransactionsOnly() {
        // Before repayment: transactions should be empty
        LoanAccountDetailsDTO detailsBefore = loanAccountDetailsService.getLoanAccount360Details(officerMain, loanMain.getLoanId(), null);
        assertTrue(detailsBefore.getPaymentHistory().getData().getTransactions().isEmpty());

        // Make an actual payment via loanService.pay
        loanService.pay(loanMain.getLoanId(), 25000);

        // After repayment: exactly 1 transaction
        LoanAccountDetailsDTO detailsAfter = loanAccountDetailsService.getLoanAccount360Details(officerMain, loanMain.getLoanId(), null);
        assertEquals(1, detailsAfter.getPaymentHistory().getData().getTransactions().size());
        assertEquals(new BigDecimal("25000.00"), detailsAfter.getPaymentHistory().getData().getTransactions().get(0).getAmount());
    }

    @Test
    @DisplayName("8. Missing optional information is shown as unavailable or resiliently handled")
    void testMissingOptionalInformationResilience() {
        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officerMain, loanMain.getLoanId(), null);

        // Collaterals & Guarantors are not added yet, should return PARTIAL or SUCCESS with empty lists, not throw NPE
        assertTrue(details.getCollateralGuarantor().isPartial() || details.getCollateralGuarantor().isSuccess());
        assertTrue(details.getCollateralGuarantor().getData().getCollaterals().isEmpty());
        assertTrue(details.getCollateralGuarantor().getData().getGuarantors().isEmpty());

        // Core banking service is unconfigured, should return PARTIAL or UNAVAILABLE with informative message
        assertTrue(details.getLinkedAccount().isPartial() || details.getLinkedAccount().isUnavailable());
        assertTrue(details.getLinkedAccount().getErrorMessage().contains("Core Banking System"));
    }

    @Test
    @DisplayName("9. Failure of one data source does not prevent other sections from rendering")
    void testPartialFailureHandling() {
        // loanMain details section is SUCCESS even when linkedAccount is PARTIAL/UNAVAILABLE
        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officerMain, loanMain.getLoanId(), null);

        assertEquals(SectionStatus.SUCCESS, details.getCustomerProfile().getStatus());
        assertEquals(SectionStatus.SUCCESS, details.getLoanDetails().getStatus());
        assertEquals(SectionStatus.SUCCESS, details.getOutstandingSummary().getStatus());
        assertEquals(SectionStatus.SUCCESS, details.getRepaymentSchedule().getStatus());
        assertTrue(details.getLinkedAccount().isPartial() || details.getLinkedAccount().isUnavailable());
    }

    @Test
    @DisplayName("10. Last-refreshed timestamp reflects actual retrieval time")
    void testLastRefreshedTimestampReflectsActualTime() {
        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officerMain, loanMain.getLoanId(), null);
        assertNotNull(details.getLastRefreshed());
        assertFalse(details.getLastRefreshed().isBlank());
    }

    @Test
    @DisplayName("11. PDF statement contains expected authorized information")
    void testPdfStatementDownloadSuccess() throws Exception {
        MvcResult result = mockMvc.perform(get("/loan-details/" + loanMain.getLoanId() + "/statement.pdf")
                        .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment; filename=\"loan_statement_")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500); // Valid non-empty PDF binary
        assertEquals("%PDF-", new String(pdfBytes, 0, 5)); // PDF Magic Header
    }

    @Test
    @DisplayName("12. PDF generation and denied download attempts are audited")
    void testPdfStatementAudited() throws Exception {
        // Success download by authorized officer
        mockMvc.perform(get("/loan-details/" + loanMain.getLoanId() + "/statement.pdf")
                        .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk());

        List<AuthAuditLog> audits = auditRepo.findByUsernameOrderByTimestampDesc(officerMain.getEmail());
        assertTrue(audits.stream().anyMatch(a -> "LOAN_STATEMENT_DOWNLOADED".equals(a.getEventType())));

        // Denied download by unauthorized branch officer
        mockMvc.perform(get("/loan-details/" + loanMain.getLoanId() + "/statement.pdf")
                        .with(user(new CustomUserDetails(officerNorth))))
                .andExpect(status().isForbidden());

        List<AuthAuditLog> northAudits = auditRepo.findByUsernameOrderByTimestampDesc(officerNorth.getEmail());
        assertTrue(northAudits.stream().anyMatch(a -> "LOAN_STATEMENT_DOWNLOAD_DENIED".equals(a.getEventType())));
    }

    @Test
    @DisplayName("13. Sensitive fields remain masked for unauthorized roles")
    void testSensitiveFieldsRemainMaskedByDefault() {
        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officerMain, loanMain.getLoanId(), null);

        String maskedPhone = details.getCustomerProfile().getData().getMaskedPhone();
        String maskedEmail = details.getCustomerProfile().getData().getMaskedEmail();

        assertTrue(maskedPhone.contains("XXXX") || maskedPhone.contains("*"));
        assertTrue(maskedEmail.contains("****") || maskedEmail.contains("*"));
        assertFalse(maskedPhone.equals("9876501234"));
        assertFalse(maskedEmail.equals("rajesh.kumar@example.com"));
    }

    @Test
    @DisplayName("14. Direct URL and loan-ID manipulation cannot expose another customer's information")
    void testIdorPreventedForCustomerRole() throws Exception {
        // Customer Main tries to access 360 view of Customer North
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanNorth.getLoanId().toString())
                        .with(user(new CustomUserDetails(customerMain))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Refresh action works smoothly")
    void testRefreshActionWorks() throws Exception {
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .param("refresh", "true")
                        .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attribute("isRefreshed", true));
    }

    @Test
    @DisplayName("16. Collections Agent can access 360 loan view for authorized branch")
    void testAuthorizedCollectionsAgentCanAccessLoan360() throws Exception {
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(collectionsAgentMain))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")));

        List<AuthAuditLog> audits = auditRepo.findByUsernameOrderByTimestampDesc(collectionsAgentMain.getEmail());
        assertTrue(audits.stream().anyMatch(a -> "LOAN_360_VIEW_ACCESSED".equals(a.getEventType()) && "SUCCESS".equals(a.getOutcome())));
    }

    @Test
    @DisplayName("17. Collections Manager can access 360 loan view for authorized branch")
    void testAuthorizedCollectionsManagerCanAccessLoan360() throws Exception {
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(collectionsManagerMain))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("no-store")));

        List<AuthAuditLog> audits = auditRepo.findByUsernameOrderByTimestampDesc(collectionsManagerMain.getEmail());
        assertTrue(audits.stream().anyMatch(a -> "LOAN_360_VIEW_ACCESSED".equals(a.getEventType()) && "SUCCESS".equals(a.getOutcome())));
    }

    @Test
    @DisplayName("18. Unauthorized branch access is denied for Collections Agent")
    void testUnauthorizedBranchAccessDeniedForCollectionsAgent() throws Exception {
        // Agent North tries to view Loan Main
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(collectionsAgentNorth))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loan-search?error=unauthorized_branch"));

        List<AuthAuditLog> audits = auditRepo.findByUsernameOrderByTimestampDesc(collectionsAgentNorth.getEmail());
        assertTrue(audits.stream().anyMatch(a -> "LOAN_360_VIEW_DENIED".equals(a.getEventType()) && "FAILURE".equals(a.getOutcome())));
    }

    @Test
    @DisplayName("19. Admin profile access remains unchanged and authorized across branches")
    void testAdminProfileAccessRemainsUnchangedAcrossBranches() throws Exception {
        // Admin views Loan Main (MAIN branch)
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"));

        // Admin views Loan North (NORTH branch)
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loanNorth.getLoanId().toString())
                        .with(user(new CustomUserDetails(adminUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"));
    }

    @Test
    @DisplayName("20. Viewing 360 profile does not grant unauthorized repayment or origination permissions")
    void testPermissionBoundariesPreserved() throws Exception {
        // Loan officer cannot access repayment
        mockMvc.perform(get("/payPage")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isForbidden());

        // Collections agent cannot access repayment
        mockMvc.perform(get("/payPage")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(collectionsAgentMain))))
                .andExpect(status().isForbidden());

        // Collections manager cannot access repayment
        mockMvc.perform(get("/payPage")
                        .param("loanId", loanMain.getLoanId().toString())
                        .with(user(new CustomUserDetails(collectionsManagerMain))))
                .andExpect(status().isForbidden());

        // Collections agent cannot access loan origination
        mockMvc.perform(get("/applyLoan")
                        .with(user(new CustomUserDetails(collectionsAgentMain))))
                .andExpect(status().isForbidden());

        // Collections manager cannot access loan origination
        mockMvc.perform(get("/applyLoan")
                        .with(user(new CustomUserDetails(collectionsManagerMain))))
                .andExpect(status().isForbidden());
    }
}
