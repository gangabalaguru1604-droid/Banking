package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.dto.LoanSearchResultDto;
import com.loanproject.loan_app.dto.StaffDashboardLoanDto;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanSearchService;
import com.loanproject.loan_app.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
public class StaffRoleDashboardAndSearchTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanService loanService;

    @Autowired
    private RepaymentInstallmentRepository installmentRepository;

    @Autowired
    private AuthAuditLogRepository auditRepo;

    @Autowired
    private LoanSearchService loanSearchService;

    private User loanOfficer;
    private User collectionsAgent;
    private User collectionsManager;
    private User admin;
    private User customerMain;
    private User customerNorth;
    private Loan activeLoanMain;
    private Loan pendingLoanMain;
    private Loan overdueLoanMain;
    private Loan loanNorth;

    @BeforeEach
    void setUp() {
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
        userRepository.deleteAll();
        auditRepo.deleteAll();

        // 1. Staff users
        loanOfficer = new User("Officer Dan", "officer.dan@bank.com", "Officer@Pass123", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        loanOfficer.setBranch("MAIN");
        loanOfficer = userRepository.save(loanOfficer);

        collectionsAgent = new User("Agent Carol", "agent.carol@bank.com", "Agent@Pass123", AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgent.setBranch("MAIN");
        collectionsAgent = userRepository.save(collectionsAgent);

        collectionsManager = new User("Manager Dave", "manager.dave@bank.com", "Manager@Pass123", AppRole.ROLE_COLLECTIONS_MANAGER, UserType.STAFF);
        collectionsManager.setBranch("MAIN");
        collectionsManager = userRepository.save(collectionsManager);

        admin = new User("Admin Emma", "admin.emma@bank.com", "Admin@Pass123", AppRole.ROLE_ADMIN, UserType.STAFF);
        admin.setBranch("ALL");
        admin = userRepository.save(admin);

        // 2. Customers
        customerMain = new User("Rahul Verma", "rahul.verma@example.com", "Pass@123", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerMain.setBranch("MAIN");
        customerMain.setPhoneNumber("9876543210");
        customerMain.setCustomerId("CUST100001");
        customerMain = userRepository.save(customerMain);

        customerNorth = new User("Priya Sharma", "priya.sharma@example.com", "Pass@123", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerNorth.setBranch("NORTH");
        customerNorth.setPhoneNumber("9876543211");
        customerNorth.setCustomerId("CUST200002");
        customerNorth = userRepository.save(customerNorth);

        // 3. Loans
        // Active Loan in MAIN
        activeLoanMain = loanService.apply(customerMain, 50000.0, 10.5, 12);
        activeLoanMain.setAccountNumber("LN1000000001");
        activeLoanMain.setStatus("ACTIVE");
        activeLoanMain = loanRepository.save(activeLoanMain);

        // Pending Loan in MAIN
        pendingLoanMain = new Loan();
        pendingLoanMain.setUser(customerMain);
        pendingLoanMain.setAmount(75000.0);
        pendingLoanMain.setInterest(11.0);
        pendingLoanMain.setMonths(24);
        pendingLoanMain.setEmi(3500.0);
        pendingLoanMain.setRemaining(75000.0);
        pendingLoanMain.setBranch("MAIN");
        pendingLoanMain.setStatus("PENDING");
        pendingLoanMain.setAccountNumber("LN1000000002");
        pendingLoanMain = loanRepository.save(pendingLoanMain);

        // Overdue Loan in MAIN (with overdue installment)
        overdueLoanMain = loanService.apply(customerMain, 30000.0, 12.0, 12);
        overdueLoanMain.setAccountNumber("LN1000000003");
        overdueLoanMain.setStatus("ACTIVE");
        overdueLoanMain = loanRepository.save(overdueLoanMain);

        // Add overdue installment for overdueLoanMain
        RepaymentInstallment overdueInst = new RepaymentInstallment(
                overdueLoanMain,
                1,
                LocalDate.now().minusDays(15),
                BigDecimal.valueOf(2665.46),
                BigDecimal.valueOf(2365.46),
                BigDecimal.valueOf(300.0),
                BigDecimal.ZERO,
                "OVERDUE",
                15
        );
        installmentRepository.save(overdueInst);

        // Loan in NORTH branch
        loanNorth = loanService.apply(customerNorth, 40000.0, 10.0, 12);
        loanNorth.setAccountNumber("LN2000000001");
        loanNorth.setStatus("PENDING");
        loanNorth.setBranch("NORTH");
        loanNorth = loanRepository.save(loanNorth);
    }

    // ==========================================
    // 1. LOAN OFFICER TESTS
    // ==========================================

    @Test
    @DisplayName("Loan Officer: Dashboard displays branch loans, pending review queue, and active metrics")
    void testLoanOfficerDashboard() throws Exception {
        MvcResult result = mockMvc.perform(get("/dashboard")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isStaff", true))
                .andExpect(model().attribute("officerBranch", "MAIN"))
                .andExpect(model().attributeExists("pendingLoans"))
                .andExpect(model().attributeExists("activeLoans"))
                .andExpect(model().attributeExists("totalPrincipal"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StaffDashboardLoanDto> pendingList = (List<StaffDashboardLoanDto>) result.getModelAndView().getModel().get("pendingLoans");
        assertNotNull(pendingList);
        assertTrue(pendingList.stream().anyMatch(l -> "LN1000000002".equals(l.getAccountNumber())),
                "Pending loan LN1000000002 must be in pending review list");

        @SuppressWarnings("unchecked")
        List<StaffDashboardLoanDto> activeList = (List<StaffDashboardLoanDto>) result.getModelAndView().getModel().get("activeLoans");
        assertNotNull(activeList);
        assertTrue(activeList.stream().anyMatch(l -> "LN1000000001".equals(l.getAccountNumber())));
    }

    @Test
    @DisplayName("Loan Officer: Review and approve pending loan application in authorized branch")
    void testLoanOfficerApproveApplication() throws Exception {
        mockMvc.perform(post("/staff/loans/approve")
                .param("loanId", pendingLoanMain.getLoanId().toString())
                .with(user(new CustomUserDetails(loanOfficer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?approved=true"));

        Loan updated = loanRepository.findById(pendingLoanMain.getLoanId()).orElseThrow();
        assertEquals("ACTIVE", updated.getStatus());
        assertNotNull(updated.getDisbursementDate());

        // Verify schedule was generated
        List<RepaymentInstallment> installments = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(pendingLoanMain.getLoanId());
        assertFalse(installments.isEmpty(), "Amortization schedule must be generated upon approval");

        // Verify audit log
        List<AuthAuditLog> logs = auditRepo.findByUsernameOrderByTimestampDesc(loanOfficer.getEmail());
        assertTrue(logs.stream().anyMatch(l -> "LOAN_APPLICATION_APPROVED".equals(l.getEventType())));
    }

    @Test
    @DisplayName("Loan Officer: Review and reject pending loan application with reason")
    void testLoanOfficerRejectApplication() throws Exception {
        mockMvc.perform(post("/staff/loans/reject")
                .param("loanId", pendingLoanMain.getLoanId().toString())
                .param("reason", "Credit score does not meet minimum bank threshold")
                .with(user(new CustomUserDetails(loanOfficer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?rejected=true"));

        Loan updated = loanRepository.findById(pendingLoanMain.getLoanId()).orElseThrow();
        assertEquals("REJECTED", updated.getStatus());

        // Verify audit log
        List<AuthAuditLog> logs = auditRepo.findByUsernameOrderByTimestampDesc(loanOfficer.getEmail());
        assertTrue(logs.stream().anyMatch(l -> "LOAN_APPLICATION_REJECTED".equals(l.getEventType())));
    }

    @Test
    @DisplayName("Loan Officer: Cannot approve or reject loan belonging to another branch (403 Forbidden)")
    void testLoanOfficerBranchIsolation() throws Exception {
        mockMvc.perform(post("/staff/loans/approve")
                .param("loanId", loanNorth.getLoanId().toString())
                .with(user(new CustomUserDetails(loanOfficer)))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Loan Officer: Forbidden from accessing customer repayment processing (/payPage, /pay)")
    void testLoanOfficerForbiddenRepayment() throws Exception {
        mockMvc.perform(get("/payPage")
                .param("loanId", activeLoanMain.getLoanId().toString())
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/pay")
                .param("loanId", activeLoanMain.getLoanId().toString())
                .param("amount", "1000")
                .with(user(new CustomUserDetails(loanOfficer)))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 2. COLLECTIONS AGENT TESTS
    // ==========================================

    @Test
    @DisplayName("Collections Agent: Dashboard displays overdue accounts, DPD buckets, and collection actions")
    void testCollectionsAgentDashboard() throws Exception {
        MvcResult result = mockMvc.perform(get("/dashboard")
                .with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isStaff", true))
                .andExpect(model().attributeExists("delinquentLoans"))
                .andExpect(model().attributeExists("overdueLoansCount"))
                .andExpect(model().attributeExists("totalOverdueAmount"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StaffDashboardLoanDto> delinquentList = (List<StaffDashboardLoanDto>) result.getModelAndView().getModel().get("delinquentLoans");
        assertNotNull(delinquentList);
        assertTrue(delinquentList.stream().anyMatch(l -> "LN1000000003".equals(l.getAccountNumber())),
                "Overdue loan LN1000000003 must be in delinquent loans list");

        StaffDashboardLoanDto delinquentLoan = delinquentList.stream()
                .filter(l -> "LN1000000003".equals(l.getAccountNumber()))
                .findFirst()
                .orElseThrow();
        assertEquals("OVERDUE", delinquentLoan.getRepaymentStatus());
        assertTrue(delinquentLoan.getDpd() >= 15);
    }

    @Test
    @DisplayName("Collections Agent: Successfully record collection payment for authorized overdue account")
    void testCollectionsAgentRecordCollection() throws Exception {
        double priorRemaining = overdueLoanMain.getRemaining();
        double paymentAmount = 2665.46;

        mockMvc.perform(post("/staff/collections/record")
                .param("loanId", overdueLoanMain.getLoanId().toString())
                .param("amount", String.valueOf(paymentAmount))
                .param("paymentMode", "CASH")
                .param("transactionReference", "REC-COL-001")
                .with(user(new CustomUserDetails(collectionsAgent)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?collectionRecorded=true"));

        Loan updated = loanRepository.findById(overdueLoanMain.getLoanId()).orElseThrow();
        assertEquals(Math.round((priorRemaining - paymentAmount) * 100.0) / 100.0,
                Math.round(updated.getRemaining() * 100.0) / 100.0);

        // Verify audit log
        List<AuthAuditLog> logs = auditRepo.findByUsernameOrderByTimestampDesc(collectionsAgent.getEmail());
        assertTrue(logs.stream().anyMatch(l -> "COLLECTION_RECORDED".equals(l.getEventType())));
    }

    @Test
    @DisplayName("Collections Agent: Record collection validation rejects zero or negative amounts")
    void testCollectionsAgentInvalidAmountRejected() {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/staff/collections/record")
                    .param("loanId", overdueLoanMain.getLoanId().toString())
                    .param("amount", "0")
                    .param("paymentMode", "CASH")
                    .with(user(new CustomUserDetails(collectionsAgent)))
                    .with(csrf()));
        });
    }

    @Test
    @DisplayName("Collections Agent: Record collection validation rejects amount exceeding remaining balance")
    void testCollectionsAgentExcessiveAmountRejected() {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/staff/collections/record")
                    .param("loanId", overdueLoanMain.getLoanId().toString())
                    .param("amount", String.valueOf(overdueLoanMain.getRemaining() + 50000))
                    .param("paymentMode", "CASH")
                    .with(user(new CustomUserDetails(collectionsAgent)))
                    .with(csrf()));
        });
    }

    @Test
    @DisplayName("Collections Agent: Cannot record collection for account in another branch (403 Forbidden)")
    void testCollectionsAgentBranchIsolation() throws Exception {
        mockMvc.perform(post("/staff/collections/record")
                .param("loanId", loanNorth.getLoanId().toString())
                .param("amount", "1000")
                .param("paymentMode", "CASH")
                .with(user(new CustomUserDetails(collectionsAgent)))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. COLLECTIONS MANAGER TESTS
    // ==========================================

    @Test
    @DisplayName("Collections Manager: Dashboard displays portfolio risk PAR, DPD breakdown categories (SMA-0, SMA-1, SMA-2, NPA)")
    void testCollectionsManagerDashboard() throws Exception {
        MvcResult result = mockMvc.perform(get("/dashboard")
                .with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isStaff", true))
                .andExpect(model().attributeExists("parRate"))
                .andExpect(model().attributeExists("dpdCurrentCount"))
                .andExpect(model().attributeExists("dpdSma0Count"))
                .andExpect(model().attributeExists("dpdSma1Count"))
                .andExpect(model().attributeExists("dpdSma2Count"))
                .andExpect(model().attributeExists("dpdNpaCount"))
                .andReturn();

        Long sma0Count = (Long) result.getModelAndView().getModel().get("dpdSma0Count");
        assertNotNull(sma0Count);
        assertTrue(sma0Count >= 1, "Account with 15 DPD must be in SMA-0 (1-30 DPD)");
    }

    // ==========================================
    // 4. LOAN SEARCH ENHANCEMENTS TESTS
    // ==========================================

    @Test
    @DisplayName("Loan Search: Search by loan account number returns status, outstanding balance, repayment status, and DPD")
    void testLoanSearchByAccountNumber() throws Exception {
        MvcResult result = mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", "LN1000000003")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-search"))
                .andExpect(model().attributeExists("results"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> results = (Page<LoanSearchResultDto>) result.getModelAndView().getModel().get("results");
        assertEquals(1, results.getTotalElements());

        LoanSearchResultDto row = results.getContent().get(0);
        assertEquals("LN1000000003", row.getAccountNumber());
        assertEquals("Rahul Verma", row.getCustomerName());
        assertEquals("ACTIVE", row.getStatus());
        assertEquals("OVERDUE", row.getRepaymentStatus());
        assertTrue(row.getDpd() >= 15);
        assertTrue(row.getRemaining() > 0);
    }

    @Test
    @DisplayName("Loan Search: Search by Customer ID returns matching authorized branch records")
    void testLoanSearchByCustomerId() throws Exception {
        MvcResult result = mockMvc.perform(get("/loan-search")
                .param("searchType", "CUSTOMER_ID")
                .param("query", "CUST100001")
                .with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-search"))
                .andExpect(model().attributeExists("results"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> results = (Page<LoanSearchResultDto>) result.getModelAndView().getModel().get("results");
        assertFalse(results.isEmpty());
        assertTrue(results.getContent().stream().anyMatch(l -> "Rahul Verma".equals(l.getCustomerName())));
    }

    @Test
    @DisplayName("Loan Search: Search by Customer Name returns matching authorized branch records")
    void testLoanSearchByCustomerName() throws Exception {
        MvcResult result = mockMvc.perform(get("/loan-search")
                .param("searchType", "NAME")
                .param("query", "Rahul")
                .with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-search"))
                .andExpect(model().attributeExists("results"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> results = (Page<LoanSearchResultDto>) result.getModelAndView().getModel().get("results");
        assertFalse(results.isEmpty());
        assertTrue(results.getContent().stream().anyMatch(l -> "LN1000000001".equals(l.getAccountNumber())));
    }

    @Test
    @DisplayName("Loan Search: Branch isolation restricts officer from seeing loans in other branches")
    void testLoanSearchBranchIsolation() throws Exception {
        // Loan officer in MAIN searching for NORTH branch loan account number
        MvcResult result = mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", "LN2000000001")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> results = (Page<LoanSearchResultDto>) result.getModelAndView().getModel().get("results");
        assertTrue(results == null || results.isEmpty());
    }

    // ==========================================
    // 5. RESTRICTIONS ENFORCEMENT TESTS
    // ==========================================

    @Test
    @DisplayName("Restrictions: Staff navigating to /applyLoan is shown staff policy notice, NOT Originate New Loan or Apply Loan form")
    void testStaffApplyLoanRestrictedUI() throws Exception {
        mockMvc.perform(get("/applyLoan")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andExpect(view().name("applyLoan"));

        Path reactPath = Path.of("../../frontend/my-react-app/src/pages/ApplyLoanPage.tsx");
        if (!Files.exists(reactPath)) {
            reactPath = Path.of("frontend/my-react-app/src/pages/ApplyLoanPage.tsx");
        }
        String reactContent = Files.readString(reactPath);
        assertTrue(reactContent.contains("Staff Policy Restriction"), "ApplyLoanPage.tsx must have staff policy restriction");
        assertFalse(reactContent.contains("Originate New Loan"), "ApplyLoanPage.tsx must NOT have 'Originate New Loan'");
        assertFalse(reactContent.contains("Borrower / Customer Email"), "ApplyLoanPage.tsx must NOT expose staff borrower email input");
    }

    @Test
    @DisplayName("Restrictions: Single Profile button links to 360 view (/loan-details) across staff views; NO separate 360 button")
    void testSingleProfileButtonStructure() throws Exception {
        mockMvc.perform(get("/dashboard")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());

        Path dashboardPath = Path.of("../../frontend/my-react-app/src/pages/StaffDashboard.tsx");
        if (!Files.exists(dashboardPath)) {
            dashboardPath = Path.of("frontend/my-react-app/src/pages/StaffDashboard.tsx");
        }
        String dashboardContent = Files.readString(dashboardPath);
        assertTrue(dashboardContent.contains("btn-profile"), "StaffDashboard.tsx must use single Profile action");
        assertTrue(dashboardContent.contains("/loan-details?loanId="), "Profile action must navigate to 360 view");
        assertFalse(dashboardContent.contains(">360° View<"), "StaffDashboard.tsx must NOT contain separate 360° button");
        assertFalse(dashboardContent.contains(">Customer Profile<"), "StaffDashboard.tsx must NOT contain separate Customer Profile button");
        assertFalse(dashboardContent.contains("Originate New Loan"), "StaffDashboard.tsx must NOT show Originate New Loan to staff");

        Path searchPath = Path.of("../../frontend/my-react-app/src/pages/LoanSearchPage.tsx");
        if (!Files.exists(searchPath)) {
            searchPath = Path.of("frontend/my-react-app/src/pages/LoanSearchPage.tsx");
        }
        String searchContent = Files.readString(searchPath);
        assertTrue(searchContent.contains("Profile"));
        assertTrue(searchContent.contains("/loan-details?loanId="));
        assertFalse(searchContent.contains(">360° View<"));
    }
}
