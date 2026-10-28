package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanApprovalHistory;
import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.faces.LoanApprovalBean;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.LoanApprovalHistoryRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanApprovalService;
import com.loanproject.loan_app.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LoanApprovalWorkflowJsfTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanApprovalService loanApprovalService;

    @Autowired
    private LoanApprovalHistoryRepository approvalHistoryRepository;

    @Autowired
    private RepaymentInstallmentRepository installmentRepository;

    @Autowired
    private AuthAuditLogRepository auditRepo;

    @Autowired
    private LoanApprovalBean loanApprovalBean;

    private User loanOfficer;
    private User admin;
    private User customer;
    private User collectionsAgent;
    private User collectionsManager;
    private User customerNorth;

    private Loan pendingLoan1;
    private Loan pendingLoan2;
    private Loan pendingLoanNorth;

    @BeforeEach
    void setUp() {
        approvalHistoryRepository.deleteAll();
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
        userRepository.deleteAll();
        auditRepo.deleteAll();

        // 1. Staff users
        loanOfficer = new User("Officer Arjun", "officer.arjun@bank.com", "Officer@Pass123", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        loanOfficer.setBranch("MAIN");
        loanOfficer = userRepository.save(loanOfficer);

        admin = new User("Admin Meera", "admin.meera@bank.com", "Admin@Pass123", AppRole.ROLE_ADMIN, UserType.STAFF);
        admin.setBranch("ALL");
        admin = userRepository.save(admin);

        collectionsAgent = new User("Agent Suresh", "agent.suresh@bank.com", "Agent@Pass123", AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgent.setBranch("MAIN");
        collectionsAgent = userRepository.save(collectionsAgent);

        collectionsManager = new User("Manager Kavita", "manager.kavita@bank.com", "Manager@Pass123", AppRole.ROLE_COLLECTIONS_MANAGER, UserType.STAFF);
        collectionsManager.setBranch("MAIN");
        collectionsManager = userRepository.save(collectionsManager);

        // 2. Customers
        customer = new User("Vikas Gupta", "vikas.gupta@example.com", "Cust@Pass123", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer.setBranch("MAIN");
        customer.setCustomerId("CUST880001");
        customer.setPhoneNumber("9811223344");
        customer.setPanNumber("ABCDE1234F");
        customer.setAadhaarNumber("998877665544");
        customer = userRepository.save(customer);

        customerNorth = new User("Ananya Roy", "ananya.roy@example.com", "Cust@Pass123", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerNorth.setBranch("NORTH");
        customerNorth.setCustomerId("CUST880002");
        customerNorth.setPhoneNumber("9822334455");
        customerNorth = userRepository.save(customerNorth);

        // 3. Pending Loans
        pendingLoan1 = new Loan();
        pendingLoan1.setUser(customer);
        pendingLoan1.setAmount(100000.0);
        pendingLoan1.setInterest(10.5);
        pendingLoan1.setMonths(24);
        pendingLoan1.setEmi(4638.0);
        pendingLoan1.setRemaining(100000.0);
        pendingLoan1.setBranch("MAIN");
        pendingLoan1.setStatus("PENDING");
        pendingLoan1 = loanRepository.save(pendingLoan1);

        pendingLoan2 = new Loan();
        pendingLoan2.setUser(customer);
        pendingLoan2.setAmount(50000.0);
        pendingLoan2.setInterest(12.0);
        pendingLoan2.setMonths(12);
        pendingLoan2.setEmi(4442.0);
        pendingLoan2.setRemaining(50000.0);
        pendingLoan2.setBranch("MAIN");
        pendingLoan2.setStatus("SUBMITTED");
        pendingLoan2 = loanRepository.save(pendingLoan2);

        pendingLoanNorth = new Loan();
        pendingLoanNorth.setUser(customerNorth);
        pendingLoanNorth.setAmount(200000.0);
        pendingLoanNorth.setInterest(9.5);
        pendingLoanNorth.setMonths(36);
        pendingLoanNorth.setEmi(6406.0);
        pendingLoanNorth.setRemaining(200000.0);
        pendingLoanNorth.setBranch("NORTH");
        pendingLoanNorth.setStatus("IN_REVIEW");
        pendingLoanNorth = loanRepository.save(pendingLoanNorth);
    }

    // -------------------------------------------------------------
    // Test 1: Authorized loan officer can view pending applications
    // -------------------------------------------------------------
    @Test
    @DisplayName("1. Authorized loan officer can view pending applications in their branch")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testAuthorizedOfficerCanViewPendingApplications() {
        List<Loan> pending = loanApprovalService.getPendingLoans(loanOfficer);
        assertNotNull(pending);
        assertEquals(2, pending.size(), "Loan officer in MAIN branch should see 2 pending loans");
        assertTrue(pending.stream().anyMatch(l -> l.getLoanId().equals(pendingLoan1.getLoanId())));
        assertTrue(pending.stream().anyMatch(l -> l.getLoanId().equals(pendingLoan2.getLoanId())));
        // North branch loan should not be included for MAIN branch officer
        assertFalse(pending.stream().anyMatch(l -> l.getLoanId().equals(pendingLoanNorth.getLoanId())));
    }

    // -------------------------------------------------------------
    // Test 2: Unauthorized customer cannot access approval workflow
    // -------------------------------------------------------------
    @Test
    @DisplayName("2. Unauthorized customer cannot access the approval dashboard (403 Forbidden)")
    void testCustomerForbiddenAccessToApprovalDashboard() throws Exception {
        mockMvc.perform(get("/loan-approval.xhtml")
                .with(user(new CustomUserDetails(customer))))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // Test 3: Collections staff cannot approve or reject applications
    // -------------------------------------------------------------
    @Test
    @DisplayName("3. Collections agents and managers cannot approve or reject applications")
    void testCollectionsStaffForbiddenFromApprovalWorkflow() throws Exception {
        mockMvc.perform(get("/loan-approval.xhtml")
                .with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/loan-approval.xhtml")
                .with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isForbidden());

        org.springframework.security.core.Authentication agentAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new CustomUserDetails(collectionsAgent), null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_COLLECTIONS_AGENT")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(agentAuth);

        try {
            assertThrows(AccessDeniedException.class, () ->
                    loanApprovalService.approveLoan(pendingLoan1.getLoanId(), collectionsAgent, "Attempted by collector", null));

            assertThrows(AccessDeniedException.class, () ->
                    loanApprovalService.rejectLoan(pendingLoan1.getLoanId(), "Valid reason", collectionsAgent, null));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    // -------------------------------------------------------------
    // Test 4: Approval changes status to ACTIVE & generates schedule
    // -------------------------------------------------------------
    @Test
    @DisplayName("4. Approval changes pending loan to ACTIVE, disburses funds, and generates schedule")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testApprovalChangesPendingToActiveAndGeneratesSchedule() {
        Loan approved = loanApprovalService.approveLoan(pendingLoan1.getLoanId(), loanOfficer, "Credit check passed", null);

        assertEquals("ACTIVE", approved.getStatus());
        assertNotNull(approved.getDisbursementDate());
        assertEquals(approved.getAmount(), approved.getDisbursedAmount());
        assertNotNull(approved.getAccountNumber());
        assertTrue(approved.getAccountNumber().startsWith("LN"));

        // Verify amortization schedule was generated
        List<RepaymentInstallment> schedule = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(approved.getLoanId());
        assertFalse(schedule.isEmpty(), "Amortization schedule must be generated upon approval");
        assertEquals(24, schedule.size(), "24 installments must be created for 24-month loan");

        // Verify history record
        List<LoanApprovalHistory> historyList = approvalHistoryRepository.findByLoan_LoanIdOrderByDecisionTimestampDesc(approved.getLoanId());
        assertFalse(historyList.isEmpty());
        LoanApprovalHistory history = historyList.get(0);
        assertEquals("APPROVED", history.getAction());
        assertEquals("ACTIVE", history.getNewStatus());
        assertEquals(loanOfficer.getEmail(), history.getReviewerEmail());
    }

    // -------------------------------------------------------------
    // Test 5: Rejection requires a nonblank reason
    // -------------------------------------------------------------
    @Test
    @DisplayName("5. Rejection requires a nonblank reason between 3 and 500 characters")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testRejectionRequiresNonBlankReason() {
        assertThrows(IllegalArgumentException.class, () ->
                loanApprovalService.rejectLoan(pendingLoan1.getLoanId(), null, loanOfficer, null));

        assertThrows(IllegalArgumentException.class, () ->
                loanApprovalService.rejectLoan(pendingLoan1.getLoanId(), "  ", loanOfficer, null));

        assertThrows(IllegalArgumentException.class, () ->
                loanApprovalService.rejectLoan(pendingLoan1.getLoanId(), "no", loanOfficer, null)); // too short (<3 chars)
    }

    // -------------------------------------------------------------
    // Test 6: Rejection stores reason & reviewer info in history
    // -------------------------------------------------------------
    @Test
    @DisplayName("6. Rejection stores the correct reason and reviewer information in history and audit")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testRejectionStoresReasonAndReviewerInHistory() {
        String reason = "Insufficient debt-to-income ratio based on recent pay stubs";
        Loan rejected = loanApprovalService.rejectLoan(pendingLoan2.getLoanId(), reason, loanOfficer, null);

        assertEquals("REJECTED", rejected.getStatus());

        List<LoanApprovalHistory> historyList = approvalHistoryRepository.findByLoan_LoanIdOrderByDecisionTimestampDesc(rejected.getLoanId());
        assertFalse(historyList.isEmpty());
        LoanApprovalHistory history = historyList.get(0);
        assertEquals("REJECTED", history.getAction());
        assertEquals(reason, history.getComments());
        assertEquals(loanOfficer.getEmail(), history.getReviewerEmail());
        assertEquals("ROLE_LOAN_OFFICER", history.getReviewerRole());

        // Verify audit log
        List<AuthAuditLog> auditLogs = auditRepo.findByUsernameOrderByTimestampDesc(loanOfficer.getEmail());
        assertTrue(auditLogs.stream().anyMatch(l -> "LOAN_APPLICATION_REJECTED".equals(l.getEventType())));
    }

    // -------------------------------------------------------------
    // Test 7: Non-existent application returns safe error
    // -------------------------------------------------------------
    @Test
    @DisplayName("7. Non-existent application returns safe IllegalArgumentException")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testNonExistentLoanThrowsSafeException() {
        assertThrows(IllegalArgumentException.class, () ->
                loanApprovalService.approveLoan(999999L, loanOfficer, "Test", null));

        assertThrows(IllegalArgumentException.class, () ->
                loanApprovalService.rejectLoan(999999L, "Valid reason", loanOfficer, null));
    }

    // -------------------------------------------------------------
    // Test 8: Already approved application cannot be approved again
    // -------------------------------------------------------------
    @Test
    @DisplayName("8. Already approved application cannot be approved again (IllegalStateException)")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testAlreadyApprovedLoanCannotBeApprovedAgain() {
        loanApprovalService.approveLoan(pendingLoan1.getLoanId(), loanOfficer, "First approval", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                loanApprovalService.approveLoan(pendingLoan1.getLoanId(), loanOfficer, "Second approval attempt", null));

        assertTrue(ex.getMessage().contains("cannot be approved because it is currently 'ACTIVE'"));
    }

    // -------------------------------------------------------------
    // Test 9: Already rejected application cannot be approved
    // -------------------------------------------------------------
    @Test
    @DisplayName("9. Already rejected application cannot be approved (IllegalStateException)")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testAlreadyRejectedLoanCannotBeApproved() {
        loanApprovalService.rejectLoan(pendingLoan2.getLoanId(), "Rejected due to credit", loanOfficer, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                loanApprovalService.approveLoan(pendingLoan2.getLoanId(), loanOfficer, "Attempting approval on rejected", null));

        assertTrue(ex.getMessage().contains("cannot be approved because it is currently 'REJECTED'"));
    }

    // -------------------------------------------------------------
    // Test 10: Concurrent approval/rejection attempts handled safely
    // -------------------------------------------------------------
    @Test
    @DisplayName("10. Concurrent approval attempts handle race condition with only one successful decision")
    void testConcurrentApprovalDecisions() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        org.springframework.security.core.Authentication officerAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new CustomUserDetails(loanOfficer), null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_LOAN_OFFICER")));

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(officerAuth);
                    startLatch.await();
                    loanApprovalService.approveLoan(pendingLoan1.getLoanId(), loanOfficer, "Concurrent test", null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one approval transaction must succeed");
        assertEquals(1, failureCount.get(), "The other concurrent decision must fail safely");

        Loan finalLoan = loanRepository.findById(pendingLoan1.getLoanId()).orElseThrow();
        assertEquals("ACTIVE", finalLoan.getStatus());
    }

    // -------------------------------------------------------------
    // Test 11: Approval history & audit persisted transactionally
    // -------------------------------------------------------------
    @Test
    @DisplayName("11. Approval history and audit records are persisted transactionally")
    @WithMockUser(username = "admin.meera@bank.com", roles = {"ADMIN"})
    void testApprovalHistoryAndAuditPersisted() {
        loanApprovalService.approveLoan(pendingLoanNorth.getLoanId(), admin, "Admin global approval", null);

        List<LoanApprovalHistory> histories = approvalHistoryRepository.findByLoan_LoanIdOrderByDecisionTimestampDesc(pendingLoanNorth.getLoanId());
        assertEquals(1, histories.size());
        assertEquals("APPROVED", histories.get(0).getAction());
        assertEquals(admin.getEmail(), histories.get(0).getReviewerEmail());

        List<AuthAuditLog> auditLogs = auditRepo.findByUsernameOrderByTimestampDesc(admin.getEmail());
        assertTrue(auditLogs.stream().anyMatch(l -> "LOAN_APPLICATION_APPROVED".equals(l.getEventType())));
    }

    // -------------------------------------------------------------
    // Test 12: Branch isolation prevents unauthorized cross-branch decision
    // -------------------------------------------------------------
    @Test
    @DisplayName("12. Branch isolation: Loan officer in MAIN cannot approve loan in NORTH branch")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testBranchIsolationEnforced() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                loanApprovalService.approveLoan(pendingLoanNorth.getLoanId(), loanOfficer, "Cross branch attempt", null));

        assertTrue(ex.getMessage().contains("Branch Isolation Violation"));
    }

    // -------------------------------------------------------------
    // Test 13: Authorized loan officer can access JSF page via HTTP
    // -------------------------------------------------------------
    @Test
    @DisplayName("13. Authorized loan officer can access /loan-approval.xhtml with status 200")
    void testAuthorizedOfficerCanAccessJsfPage() throws Exception {
        mockMvc.perform(get("/loan-approval.xhtml")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------
    // Test 14: Existing JSP routes continue working seamlessly
    // -------------------------------------------------------------
    @Test
    @DisplayName("14. Existing JSP routes (/, /login, /dashboard) continue working seamlessly")
    void testExistingJspRoutesContinueWorking() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------
    // Test 15: Data masking methods in LoanApprovalBean
    // -------------------------------------------------------------
    @Test
    @DisplayName("15. Data masking methods correctly protect sensitive customer identifiers")
    void testDataMaskingMethods() {
        assertEquals("CUST***01", loanApprovalBean.maskCustomerId("CUST100001"));
        assertEquals("******3210", loanApprovalBean.maskPhone("9876543210"));
        assertEquals("r***a@example.com", loanApprovalBean.maskEmail("rahul.verma@example.com"));
        assertEquals("XXXX-XXXX-5544", loanApprovalBean.maskAadhaar("998877665544"));
        assertEquals("ABCDE****F", loanApprovalBean.maskPan("ABCDE1234F"));
        assertEquals("LN******0001", loanApprovalBean.maskAccountNumber("LN0000000001"));
    }

    // -------------------------------------------------------------
    // Test 16: Complete Customer Application -> Pending -> Approved
    // -------------------------------------------------------------
    @Test
    @DisplayName("16. Complete End-to-End Flow: Customer submits application -> Pending in JSF dashboard -> Officer approves")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testEndToEndCustomerSubmissionAndApproval() throws Exception {
        // Step A: Customer submits application via customer self-service endpoint
        mockMvc.perform(post("/applyLoan")
                .param("amount", "150000.0")
                .with(user(new CustomUserDetails(customer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Step B: Application is persisted with initial status PENDING
        List<Loan> customerLoans = loanRepository.findByUserOrderByCreatedAtDesc(customer);
        Loan submittedLoan = customerLoans.stream()
                .filter(l -> l.getAmount() == 150000.0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Customer loan was not persisted"));

        assertEquals("PENDING", submittedLoan.getStatus(), "Customer application must have initial status PENDING");
        assertEquals(0.0, submittedLoan.getDisbursedAmount(), 0.001, "Funds must not be disbursed prior to approval");

        var officerAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new CustomUserDetails(loanOfficer), null, new CustomUserDetails(loanOfficer).getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(officerAuth);

        try {
            // Step C: Application is automatically visible in the JSF approval service for loan officer
            List<Loan> pendingLoans = loanApprovalService.getPendingLoans(loanOfficer);
            assertTrue(pendingLoans.stream().anyMatch(l -> l.getLoanId().equals(submittedLoan.getLoanId())),
                    "Submitted application must automatically appear in JSF dashboard pending list");

            // Step D: Loan Officer reviews application details
            Loan reviewLoan = loanApprovalService.getPendingLoanDetails(submittedLoan.getLoanId(), loanOfficer);
            assertNotNull(reviewLoan);
            assertEquals(customer.getName(), reviewLoan.getUser().getName());

            // Step E: Loan Officer approves application
            Loan approvedLoan = loanApprovalService.approveLoan(submittedLoan.getLoanId(), loanOfficer, "Eligible customer with verified KYC", null);
            assertEquals("ACTIVE", approvedLoan.getStatus());
            assertEquals(150000.0, approvedLoan.getDisbursedAmount(), 0.001);
            assertNotNull(approvedLoan.getDisbursementDate());
            assertNotNull(approvedLoan.getAccountNumber());

            // Step F: Repayment schedule and audit history are persisted
            List<RepaymentInstallment> schedule = installmentRepository.findByLoanOrderByInstallmentNumberAsc(approvedLoan);
            assertFalse(schedule.isEmpty(), "Amortization schedule must be generated upon approval");

            List<LoanApprovalHistory> histories = approvalHistoryRepository.findByLoan_LoanIdOrderByDecisionTimestampDesc(approvedLoan.getLoanId());
            assertEquals(1, histories.size());
            assertEquals("APPROVED", histories.get(0).getAction());
            assertEquals("PENDING", histories.get(0).getPreviousStatus());
            assertEquals("ACTIVE", histories.get(0).getNewStatus());

            // Step G: Application is no longer in pending list
            List<Loan> updatedPending = loanApprovalService.getPendingLoans(loanOfficer);
            assertFalse(updatedPending.stream().anyMatch(l -> l.getLoanId().equals(approvedLoan.getLoanId())));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    // -------------------------------------------------------------
    // Test 17: Complete Customer Application -> Pending -> Rejected
    // -------------------------------------------------------------
    @Test
    @DisplayName("17. Complete End-to-End Flow: Customer submits application -> Pending in JSF dashboard -> Officer rejects with reason")
    @WithMockUser(username = "officer.arjun@bank.com", roles = {"LOAN_OFFICER"})
    void testEndToEndCustomerSubmissionAndRejection() throws Exception {
        // Step A: Customer submits application
        mockMvc.perform(post("/applyLoan")
                .param("amount", "80000.0")
                .with(user(new CustomUserDetails(customer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        List<Loan> customerLoans = loanRepository.findByUserOrderByCreatedAtDesc(customer);
        Loan submittedLoan = customerLoans.stream()
                .filter(l -> l.getAmount() == 80000.0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Customer loan was not persisted"));

        assertEquals("PENDING", submittedLoan.getStatus());

        var officerAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new CustomUserDetails(loanOfficer), null, new CustomUserDetails(loanOfficer).getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(officerAuth);

        try {
            // Step B: Rejection with blank reason fails
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    loanApprovalService.rejectLoan(submittedLoan.getLoanId(), "   ", loanOfficer, null));
            assertTrue(ex.getMessage().contains("mandatory and cannot be blank"));

            // Step C: Officer rejects application with valid reason
            Loan rejectedLoan = loanApprovalService.rejectLoan(submittedLoan.getLoanId(), "Credit score does not meet minimum bank criteria", loanOfficer, null);
            assertEquals("REJECTED", rejectedLoan.getStatus());

            // Step D: Decision and audit history are persisted
            List<LoanApprovalHistory> histories = approvalHistoryRepository.findByLoan_LoanIdOrderByDecisionTimestampDesc(rejectedLoan.getLoanId());
            assertEquals(1, histories.size());
            assertEquals("REJECTED", histories.get(0).getAction());
            assertEquals("Credit score does not meet minimum bank criteria", histories.get(0).getComments());

            // Step E: Cannot approve an already rejected application
            IllegalStateException stateEx = assertThrows(IllegalStateException.class, () ->
                    loanApprovalService.approveLoan(rejectedLoan.getLoanId(), loanOfficer, "Late approval attempt", null));
            assertTrue(stateEx.getMessage().contains("Invalid State Transition"));

            // Step F: Application no longer in pending list
            List<Loan> updatedPending = loanApprovalService.getPendingLoans(loanOfficer);
            assertFalse(updatedPending.stream().anyMatch(l -> l.getLoanId().equals(rejectedLoan.getLoanId())));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
