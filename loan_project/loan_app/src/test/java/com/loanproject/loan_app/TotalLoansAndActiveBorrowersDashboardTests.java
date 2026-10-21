package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.dto.ActiveBorrowerDto;
import com.loanproject.loan_app.dto.StaffDashboardLoanDto;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
public class TotalLoansAndActiveBorrowersDashboardTests {

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

    private User admin;
    private User loanOfficer;
    private User collectionsAgent;
    private User collectionsManager;
    private User customer1;
    private User customer2;
    private User customer3;

    private Loan loanMain1;
    private Loan loanMain2Multi;
    private Loan loanNorth1;
    private Loan loanPendingMain;

    @BeforeEach
    void setUp() {
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
        userRepository.deleteAll();
        auditRepo.deleteAll();

        // 1. Staff users
        admin = new User("Admin Emma", "admin.emma@bank.com", "Admin@Pass123", AppRole.ROLE_ADMIN, UserType.STAFF);
        admin.setBranch("ALL");
        admin = userRepository.save(admin);

        loanOfficer = new User("Officer Dan", "officer.dan@bank.com", "Officer@Pass123", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        loanOfficer.setBranch("MAIN");
        loanOfficer = userRepository.save(loanOfficer);

        collectionsAgent = new User("Agent Carol", "agent.carol@bank.com", "Agent@Pass123", AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgent.setBranch("MAIN");
        collectionsAgent = userRepository.save(collectionsAgent);

        collectionsManager = new User("Manager Dave", "manager.dave@bank.com", "Manager@Pass123", AppRole.ROLE_COLLECTIONS_MANAGER, UserType.STAFF);
        collectionsManager.setBranch("MAIN");
        collectionsManager = userRepository.save(collectionsManager);

        // 2. Customers
        customer1 = new User("Rahul Verma", "rahul.verma@example.com", "Pass@123", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer1.setBranch("MAIN");
        customer1.setPhoneNumber("9876543210");
        customer1.setCustomerId("CUST100001");
        customer1 = userRepository.save(customer1);

        customer2 = new User("Priya Sharma", "priya.sharma@example.com", "Pass@123", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer2.setBranch("NORTH");
        customer2.setPhoneNumber("9876543211");
        customer2.setCustomerId("CUST200002");
        customer2 = userRepository.save(customer2);

        customer3 = new User("Amit Patel", "amit.patel@example.com", "Pass@123", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer3.setBranch("MAIN");
        customer3.setPhoneNumber("9876543212");
        customer3.setCustomerId("CUST100003");
        customer3 = userRepository.save(customer3);

        // 3. Loans:
        // Loan 1: Customer 1 active loan in MAIN (50,000)
        loanMain1 = loanService.apply(customer1, 50000.0, 10.5, 12);
        loanMain1.setAccountNumber("LN1000000001");
        loanMain1.setStatus("ACTIVE");
        loanMain1.setBranch("MAIN");
        loanMain1.setDisbursementDate(java.time.LocalDate.of(2026, 1, 15));
        loanMain1 = loanRepository.save(loanMain1);

        // Loan 2: Customer 1 SECOND active loan in MAIN (30,000) - for testing deduplication
        loanMain2Multi = loanService.apply(customer1, 30000.0, 11.0, 24);
        loanMain2Multi.setAccountNumber("LN1000000002");
        loanMain2Multi.setStatus("ACTIVE");
        loanMain2Multi.setBranch("MAIN");
        loanMain2Multi.setDisbursementDate(java.time.LocalDate.of(2026, 2, 10));
        loanMain2Multi = loanRepository.save(loanMain2Multi);

        // Loan 3: Customer 2 active loan in NORTH branch (100,000) - for testing branch isolation
        loanNorth1 = loanService.apply(customer2, 100000.0, 9.5, 36);
        loanNorth1.setAccountNumber("LN2000000001");
        loanNorth1.setStatus("ACTIVE");
        loanNorth1.setBranch("NORTH");
        loanNorth1.setDisbursementDate(java.time.LocalDate.of(2026, 3, 1));
        loanNorth1 = loanRepository.save(loanNorth1);

        // Loan 4: Customer 3 pending application in MAIN (40,000) - not active
        loanPendingMain = new Loan();
        loanPendingMain.setUser(customer3);
        loanPendingMain.setAmount(40000.0);
        loanPendingMain.setRemaining(40000.0);
        loanPendingMain.setInterest(12.0);
        loanPendingMain.setMonths(18);
        loanPendingMain.setEmi(2438.0);
        loanPendingMain.setAccountNumber("LN1000000004");
        loanPendingMain.setStatus("PENDING");
        loanPendingMain.setBranch("MAIN");
        loanPendingMain = loanRepository.save(loanPendingMain);
    }

    @Test
    @DisplayName("1. Total Loans Page: Admin accesses /total-loans and views all 13 required fields across authorized branches")
    void testTotalLoansPageDisplaysAllFieldsAdmin() throws Exception {
        MvcResult result = mockMvc.perform(get("/total-loans")
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("total-loans"))
                .andExpect(model().attributeExists("loans"))
                .andExpect(model().attribute("totalLoansCount", 4))
                .andExpect(model().attributeExists("allBranches"))
                .andExpect(model().attributeExists("allStatuses"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StaffDashboardLoanDto> loans = (List<StaffDashboardLoanDto>) result.getModelAndView().getModel().get("loans");
        assertEquals(4, loans.size());

        // Verify all 13 required fields are present on the DTO:
        StaffDashboardLoanDto sample = loans.stream()
                .filter(l -> "LN1000000001".equals(l.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        // 1. Loan account number
        assertEquals("LN1000000001", sample.getAccountNumber());
        // 2. Application ID / Loan ID
        assertNotNull(sample.getLoanId());
        // 3. Borrower name
        assertEquals("Rahul Verma", sample.getBorrowerName());
        // 4. Customer ID
        assertEquals("CUST100001", sample.getCustomerId());
        // 5. Loan type
        assertNotNull(sample.getProductType());
        // 6. Principal / Sanctioned amount
        assertEquals(50000.0, sample.getAmount());
        // 7. Interest rate
        assertEquals(10.5, sample.getInterest());
        // 8. Loan tenure in months
        assertEquals(12, sample.getMonths());
        // 9. Monthly EMI
        assertTrue(sample.getEmi() > 0);
        // 10. Outstanding balance
        assertTrue(sample.getRemaining() >= 0);
        // 11. Branch
        assertEquals("MAIN", sample.getBranch());
        // 12. Loan status
        assertEquals("ACTIVE", sample.getStatus());
        // 13. Loan start date
        assertEquals("2026-01-15", sample.getStartDate());
    }

    @Test
    @DisplayName("2. Total Loans Page: Branch isolation restricts Loan Officer to assigned branch")
    void testTotalLoansPageBranchIsolationLoanOfficer() throws Exception {
        MvcResult result = mockMvc.perform(get("/total-loans")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andExpect(view().name("total-loans"))
                .andExpect(model().attribute("totalLoansCount", 3))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StaffDashboardLoanDto> loans = (List<StaffDashboardLoanDto>) result.getModelAndView().getModel().get("loans");
        assertEquals(3, loans.size());
        assertTrue(loans.stream().allMatch(l -> "MAIN".equalsIgnoreCase(l.getBranch())));
        assertFalse(loans.stream().anyMatch(l -> "LN2000000001".equals(l.getAccountNumber())),
                "Officer in MAIN branch must NOT see NORTH branch loan");
    }

    @Test
    @DisplayName("3. Active Borrowers Page: Avoids duplicate borrower records when customer has multiple active loans")
    void testActiveBorrowersDeduplicatesCustomersWithMultipleLoans() throws Exception {
        MvcResult result = mockMvc.perform(get("/active-borrowers")
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("active-borrowers"))
                .andExpect(model().attributeExists("activeBorrowers"))
                .andExpect(model().attribute("activeBorrowersCount", 2)) // Rahul Verma (2 loans) and Priya Sharma (1 loan)
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ActiveBorrowerDto> borrowers = (List<ActiveBorrowerDto>) result.getModelAndView().getModel().get("activeBorrowers");
        assertEquals(2, borrowers.size(), "Should have exactly 2 distinct active borrowers (Rahul and Priya)");

        // Find Rahul Verma
        ActiveBorrowerDto rahul = borrowers.stream()
                .filter(b -> "Rahul Verma".equals(b.getBorrowerName()))
                .findFirst()
                .orElseThrow();

        // 1. Customer ID
        assertEquals("CUST100001", rahul.getCustomerId());
        // 2. Borrower name
        assertEquals("Rahul Verma", rahul.getBorrowerName());
        // 3. Loan account number(s)
        assertEquals(2, rahul.getAccountNumbers().size());
        assertTrue(rahul.getAccountNumbers().contains("LN1000000001"));
        assertTrue(rahul.getAccountNumbers().contains("LN1000000002"));
        // 4. Active loan count
        assertEquals(2, rahul.getActiveLoanCount());
        // 5. Total sanctioned amount (50,000 + 30,000 = 80,000)
        assertEquals(80000.0, rahul.getTotalSanctionedAmount());
        // 6. Total outstanding balance
        assertEquals(80000.0, rahul.getTotalOutstandingBalance());
        // 7. Branch
        assertEquals("MAIN", rahul.getBranch());
        // 8. Contact info
        assertNotNull(rahul.getContactInfo());
        // 9. Loan status
        assertEquals("ACTIVE", rahul.getStatus());

        // Verify Amit Patel (PENDING loan only) is NOT in active borrowers
        assertFalse(borrowers.stream().anyMatch(b -> "Amit Patel".equals(b.getBorrowerName())),
                "Borrower with only pending loan must not appear in active borrowers");
    }

    @Test
    @DisplayName("4. Active Borrowers Page: Branch isolation restricts Loan Officer to assigned branch")
    void testActiveBorrowersBranchIsolationLoanOfficer() throws Exception {
        MvcResult result = mockMvc.perform(get("/active-borrowers")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andExpect(view().name("active-borrowers"))
                .andExpect(model().attribute("activeBorrowersCount", 1)) // Only Rahul in MAIN
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ActiveBorrowerDto> borrowers = (List<ActiveBorrowerDto>) result.getModelAndView().getModel().get("activeBorrowers");
        assertEquals(1, borrowers.size());
        assertEquals("Rahul Verma", borrowers.get(0).getBorrowerName());
    }

    @Test
    @DisplayName("5. Dashboard Count Consistency: totalLoansCount and activeBorrowersCount match between dashboard and detail pages")
    void testDashboardCountsConsistentWithDetailPages() throws Exception {
        // Admin Dashboard
        MvcResult adminDash = mockMvc.perform(get("/dashboard")
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalLoansCount", 4))
                .andExpect(model().attribute("activeBorrowersCount", 2))
                .andReturn();

        int adminTotalLoans = (int) adminDash.getModelAndView().getModel().get("totalLoansCount");
        int adminActiveBorrowers = (int) adminDash.getModelAndView().getModel().get("activeBorrowersCount");

        // Admin Total Loans Page
        MvcResult totalLoansPage = mockMvc.perform(get("/total-loans")
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<StaffDashboardLoanDto> loansList = (List<StaffDashboardLoanDto>) totalLoansPage.getModelAndView().getModel().get("loans");
        assertEquals(adminTotalLoans, loansList.size(), "Dashboard totalLoansCount must match /total-loans list size");

        // Admin Active Borrowers Page
        MvcResult activeBorrowersPage = mockMvc.perform(get("/active-borrowers")
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ActiveBorrowerDto> borrowersList = (List<ActiveBorrowerDto>) activeBorrowersPage.getModelAndView().getModel().get("activeBorrowers");
        assertEquals(adminActiveBorrowers, borrowersList.size(), "Dashboard activeBorrowersCount must match /active-borrowers list size");
    }

    @Test
    @DisplayName("6. Role-Based Contact Masking: Admin and Loan Officer see full contact; Collections Agent sees masked contact")
    void testRoleBasedContactMasking() throws Exception {
        // Loan Officer (MAIN branch) - full contact
        MvcResult officerResult = mockMvc.perform(get("/active-borrowers")
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ActiveBorrowerDto> officerBorrowers = (List<ActiveBorrowerDto>) officerResult.getModelAndView().getModel().get("activeBorrowers");
        ActiveBorrowerDto rahulOfficer = officerBorrowers.get(0);
        assertEquals("rahul.verma@example.com", rahulOfficer.getContactEmail());
        assertEquals("9876543210", rahulOfficer.getContactPhone());

        // Collections Agent (MAIN branch) - masked contact
        MvcResult agentResult = mockMvc.perform(get("/active-borrowers")
                .with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ActiveBorrowerDto> agentBorrowers = (List<ActiveBorrowerDto>) agentResult.getModelAndView().getModel().get("activeBorrowers");
        ActiveBorrowerDto rahulAgent = agentBorrowers.get(0);
        assertTrue(rahulAgent.getContactEmail().contains("***"), "Agent must see masked email");
        assertTrue(rahulAgent.getContactPhone().contains("***"), "Agent must see masked phone");
    }

    @Test
    @DisplayName("7. RBAC Protection: Customer is denied access to staff total-loans and active-borrowers endpoints")
    void testCustomerDeniedAccessToStaffEndpoints() throws Exception {
        // Customer GET /total-loans is forbidden by Spring Security
        mockMvc.perform(get("/total-loans")
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());

        // Customer GET /active-borrowers is forbidden by Spring Security
        mockMvc.perform(get("/active-borrowers")
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());

        // Customer GET /api/staff/total-loans is forbidden
        mockMvc.perform(get("/api/staff/total-loans")
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());

        // Customer GET /api/staff/active-borrowers is forbidden
        mockMvc.perform(get("/api/staff/active-borrowers")
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. REST API Endpoints: Staff roles can retrieve JSON data from /api/staff/total-loans and /api/staff/active-borrowers")
    void testStaffRestApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/staff/total-loans")
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4));

        mockMvc.perform(get("/api/staff/active-borrowers")
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].borrowerName").exists());
    }

    @Test
    @DisplayName("9. Dashboard JSP Verification: Clickable cards, accessibility, navigation links, and Profile buttons")
    void testDashboardJspStructureAndAccessibility() throws Exception {
        Path jspPath = Paths.get("src/main/webapp/WEB-INF/pages/dashboard.jsp");
        String content = Files.readString(jspPath);

        // Clickable card classes and role attributes
        assertTrue(content.contains("clickable-card"), "dashboard.jsp must include clickable-card class");
        assertTrue(content.contains("role=\"button\""), "dashboard.jsp must include role=\"button\" for accessibility");
        assertTrue(content.contains("tabindex=\"0\""), "dashboard.jsp must include tabindex=\"0\" for keyboard accessibility");

        // Links to /total-loans and /active-borrowers
        assertTrue(content.contains("window.location.href='/total-loans'"), "Total Loans card must navigate to /total-loans");
        assertTrue(content.contains("window.location.href='/active-borrowers'"), "Active Borrowers card must navigate to /active-borrowers");
        assertTrue(content.contains("<a href=\"/total-loans\""), "Quick action bar must link to /total-loans");
        assertTrue(content.contains("<a href=\"/active-borrowers\""), "Quick action bar must link to /active-borrowers");

        // Ensure Profile button to /loan-details is preserved for existing tests
        assertTrue(content.contains("<a href=\"/loan-details?loanId=${l.loanId}\""), "Staff loan portfolio must link to /loan-details");
        assertTrue(content.contains("btn-profile"), "Must preserve btn-profile class");
        assertFalse(content.contains(">360°<"), "Separate 360° button must not be present");
    }

    @Test
    @DisplayName("10. Total Loans and Active Borrowers JSPs: Include View Details (Profile) actions linking to /loan-details")
    void testTotalLoansAndActiveBorrowersPagesStructure() throws Exception {
        Path totalLoansJsp = Paths.get("src/main/webapp/WEB-INF/pages/total-loans.jsp");
        String tlContent = Files.readString(totalLoansJsp);

        assertTrue(tlContent.contains("href=\"/loan-details?loanId="), "total-loans.jsp must link to /loan-details");
        assertTrue(tlContent.contains("class=\"btn-profile\""), "total-loans.jsp must use btn-profile for View Details");
        assertTrue(tlContent.contains("loansSearchInput"), "total-loans.jsp must contain search input");
        assertTrue(tlContent.contains("branchFilterSelect"), "total-loans.jsp must contain branch filter");
        assertTrue(tlContent.contains("statusFilterSelect"), "total-loans.jsp must contain status filter");
        assertTrue(tlContent.contains("pageSizeSelect"), "total-loans.jsp must contain pagination page size selector");

        Path activeBorrowersJsp = Paths.get("src/main/webapp/WEB-INF/pages/active-borrowers.jsp");
        String abContent = Files.readString(activeBorrowersJsp);

        assertTrue(abContent.contains("href=\"/loan-details?loanId="), "active-borrowers.jsp must link to /loan-details");
        assertTrue(abContent.contains("class=\"btn-profile\""), "active-borrowers.jsp must use btn-profile for View Details");
        assertTrue(abContent.contains("borrowersSearchInput"), "active-borrowers.jsp must contain search input");
        assertTrue(abContent.contains("borrowerBranchSelect"), "active-borrowers.jsp must contain branch filter");
        assertTrue(abContent.contains("borrowerPageSize"), "active-borrowers.jsp must contain pagination page size selector");
    }
}
