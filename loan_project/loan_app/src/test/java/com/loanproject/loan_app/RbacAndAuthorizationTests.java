package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
public class RbacAndAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanService loanService;

    @Autowired
    private com.loanproject.loan_app.repository.MfaChallengeRepository challengeRepository;

    @Autowired
    private com.loanproject.loan_app.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    private User customer1;
    private User customer2;
    private Loan loan1;
    private Loan loan2;

    private User loanOfficer;
    private User collectionsAgent;
    private User collectionsManager;
    private User admin;

    @BeforeEach
    void setup() {
        loanRepository.deleteAll();
        challengeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Customer 1
        customer1 = new User("Alice Borrower", "alice@example.com", "Secret123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer1 = userRepository.save(customer1);
        loan1 = loanService.apply(customer1, 10000, 10, 12);

        // Customer 2
        customer2 = new User("Bob Borrower", "bob@example.com", "Secret123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer2 = userRepository.save(customer2);
        loan2 = loanService.apply(customer2, 20000, 12, 24);

        // Staff Roles
        loanOfficer = new User("Officer Dan", "officer.dan@bank.com", "Secret123!", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        loanOfficer = userRepository.save(loanOfficer);

        collectionsAgent = new User("Agent Carol", "agent.carol@bank.com", "Secret123!", AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgent = userRepository.save(collectionsAgent);

        collectionsManager = new User("Manager Dave", "manager.dave@bank.com", "Secret123!", AppRole.ROLE_COLLECTIONS_MANAGER, UserType.STAFF);
        collectionsManager = userRepository.save(collectionsManager);

        admin = new User("Admin Emma", "admin.emma@bank.com", "Secret123!", AppRole.ROLE_ADMIN, UserType.STAFF);
        admin = userRepository.save(admin);
    }

    @Test
    @DisplayName("Test 6: Authorized admin unlock works; unauthorized unlock fails")
    void testAdminUnlockAuthorization() throws Exception {
        // Lock Customer 1's account
        customer1.setAccountNonLocked(false);
        userRepository.save(customer1);

        // Non-admin (Loan Officer) attempting to unlock must be rejected with 403 Forbidden
        mockMvc.perform(post("/admin/staff/unlock")
                .with(user(new CustomUserDetails(loanOfficer)))
                .param("email", customer1.getEmail())
                .with(csrf()))
                .andExpect(status().isForbidden());

        User stillLocked = userRepository.findByEmail(customer1.getEmail()).orElseThrow();
        assertFalse(stillLocked.isAccountNonLocked(), "Account must remain locked after unauthorized attempt");

        // Authorized Admin unlocks account
        mockMvc.perform(post("/admin/staff/unlock")
                .with(user(new CustomUserDetails(admin)))
                .param("email", customer1.getEmail())
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        User unlocked = userRepository.findByEmail(customer1.getEmail()).orElseThrow();
        assertTrue(unlocked.isAccountNonLocked(), "Account must be unlocked after admin unlock");
    }

    @Test
    @DisplayName("Test 11: All four staff roles receive only their permitted access")
    void testAllFourRolesPermissions() throws Exception {
        // 1. ROLE_LOAN_OFFICER
        // Can access /applyLoan and /dashboard
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());
        // Cannot access repayment processing (/payPage, /pay)
        mockMvc.perform(get("/payPage").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isForbidden());
        // Cannot access admin
        mockMvc.perform(get("/admin/staff").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isForbidden());

        // 2. ROLE_COLLECTIONS_AGENT
        // Can access /dashboard; CANNOT access /payPage, /pay, /applyLoan, /admin/staff
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/payPage").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/staff").with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isForbidden());

        // 3. ROLE_COLLECTIONS_MANAGER
        // Can access /dashboard; CANNOT access /payPage, /pay, /applyLoan, /admin/staff
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/payPage").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/staff").with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isForbidden());

        // 4. ROLE_ADMIN
        // Can access /admin/staff, /admin/audit-logs, /applyLoan, /payPage, /dashboard
        mockMvc.perform(get("/admin/staff").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/audit-logs").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test 12: Object-level authorization prevents customer from viewing or paying another customer's loan")
    void testObjectLevelAuthorization() throws Exception {
        // Customer 1 attempting to view Customer 2's loan payment page must be forbidden
        mockMvc.perform(get("/payPage")
                .param("loanId", loan2.getLoanId().toString())
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());

        // Customer 1 attempting to pay Customer 2's loan must be forbidden
        mockMvc.perform(post("/pay")
                .param("loanId", loan2.getLoanId().toString())
                .param("amount", "500")
                .with(user(new CustomUserDetails(customer1)))
                .with(csrf()))
                .andExpect(status().isForbidden());

        // Customer 1 paying their OWN loan must succeed
        mockMvc.perform(post("/pay")
                .param("loanId", loan1.getLoanId().toString())
                .param("amount", "500")
                .with(user(new CustomUserDetails(customer1)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Verify role-based URL access control and loan application restrictions across all roles")
    void testRoleBasedNavigationAndAccessControls() throws Exception {
        // 1. ROLE_CUSTOMER
        // Customer can access customer dashboard, apply loan, pay own loan
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/payPage").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isOk());

        // Customer cannot access staff / admin endpoints
        mockMvc.perform(get("/loan-search").with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/customer-profile").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/loan-details").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/staff").with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/audit-logs").with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());

        // Customer self-service loan application
        mockMvc.perform(post("/applyLoan")
                .param("amount", "15000")
                .param("interest", "9.5")
                .param("months", "12")
                .with(user(new CustomUserDetails(customer1)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // 2. ROLE_ADMIN
        // Admin can access admin endpoints, staff dashboard, loan search, loan origination
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/staff").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/audit-logs").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/loan-search").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/customer-profile").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        // Admin accessing 360 profile via /loan-details
        mockMvc.perform(get("/loan-details").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"));
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());

        // Admin CANNOT access repayment processing (/payPage or /pay)
        mockMvc.perform(get("/payPage").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/pay").param("loanId", loan1.getLoanId().toString())
                .param("amount", "100")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf()))
                .andExpect(status().isForbidden());

        // Admin originating loan on behalf of a valid customer
        mockMvc.perform(post("/applyLoan")
                .param("customerEmail", customer1.getEmail())
                .param("amount", "25000")
                .param("interest", "10.0")
                .param("months", "18")
                .with(user(new CustomUserDetails(admin)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Admin originating loan without customer email must fail
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/applyLoan")
                    .param("customerEmail", "")
                    .param("amount", "25000")
                    .param("interest", "10.0")
                    .param("months", "18")
                    .with(user(new CustomUserDetails(admin)))
                    .with(csrf()));
        });

        // 3. ROLE_LOAN_OFFICER
        // Loan officer can access staff dashboard, loan search, and loan origination
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/loan-search").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());

        // Loan officer cannot access admin endpoints or repayment processing
        mockMvc.perform(get("/admin/staff").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/audit-logs").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/payPage").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/pay").param("loanId", loan1.getLoanId().toString())
                .param("amount", "100")
                .with(user(new CustomUserDetails(loanOfficer)))
                .with(csrf()))
                .andExpect(status().isForbidden());

        // Loan officer originating loan on behalf of a valid customer
        mockMvc.perform(post("/applyLoan")
                .param("customerEmail", customer2.getEmail())
                .param("amount", "30000")
                .param("interest", "11.0")
                .param("months", "24")
                .with(user(new CustomUserDetails(loanOfficer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // 4. ROLE_COLLECTIONS_AGENT / MANAGER
        // Cannot access loan origination (/applyLoan) or repayment processing (/payPage, /pay)
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/applyLoan")
                .param("customerEmail", customer1.getEmail())
                .param("amount", "10000")
                .param("interest", "10.0")
                .param("months", "12")
                .with(user(new CustomUserDetails(collectionsAgent)))
                .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/payPage").param("loanId", loan1.getLoanId().toString())
                .with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/pay").param("loanId", loan1.getLoanId().toString())
                .param("amount", "100")
                .with(user(new CustomUserDetails(collectionsAgent)))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 14: All four staff roles do NOT have Originate New Loan on their dashboards")
    void testStaffDashboardsDoNotHaveOriginateNewLoan() throws Exception {
        // 1. Admin dashboard
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isStaff", true));

        // 2. Loan officer dashboard
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isStaff", true));

        // 3. Collections agent dashboard
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isStaff", true));

        // 4. Collections manager dashboard
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("isStaff", true));

        // 5. Loan search is still accessible for all four staff roles
        mockMvc.perform(get("/loan-search").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/loan-search").with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/loan-search").with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/loan-search").with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isOk());

        // 6. Customer can still access Apply Loan
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isOk());

        // 7. Collections staff are denied direct access to /applyLoan
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 15: Unified Profile opens 360 view for Collections Agent, Collections Manager, Loan Officer, and Admin")
    void testUnifiedProfileOpens360ViewForStaffRoles() throws Exception {
        // 1. Admin accesses 360 loan view via /loan-details
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loan1.getLoanId().toString())
                        .with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"));

        // 2. Loan Officer accesses 360 loan view via /loan-details
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loan1.getLoanId().toString())
                        .with(user(new CustomUserDetails(loanOfficer))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"));

        // 3. Collections Agent accesses 360 loan view via /loan-details
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loan1.getLoanId().toString())
                        .with(user(new CustomUserDetails(collectionsAgent))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"));

        // 4. Collections Manager accesses 360 loan view via /loan-details
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loan1.getLoanId().toString())
                        .with(user(new CustomUserDetails(collectionsManager))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"));

        // 5. Customer is denied direct access to officer 360 view
        mockMvc.perform(get("/loan-details")
                        .param("loanId", loan1.getLoanId().toString())
                        .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isForbidden());

        // 6. Verify dashboard.jsp file structure: Actions column contains single Profile button to /loan-details and NO 360° or Pay buttons in staff table
        java.nio.file.Path jspPath = java.nio.file.Paths.get("src/main/webapp/WEB-INF/pages/dashboard.jsp");
        String jspContent = java.nio.file.Files.readString(jspPath);
        assertTrue(jspContent.contains("<a href=\"/loan-details?loanId=${l.loanId}\""), "Staff loan portfolio must link to /loan-details");
        assertTrue(jspContent.contains("Profile"), "Action button text must be Profile");
        assertFalse(jspContent.contains(">360°<"), "Separate 360° button must be removed from dashboard");
        assertFalse(jspContent.contains("360°</a>"), "Separate 360° link must be removed from dashboard");

        // Staff table must not have any pay action
        int staffSectionEnd = jspContent.indexOf("<%-- Customer Dashboard View --%>");
        String staffSection = staffSectionEnd != -1 ? jspContent.substring(0, staffSectionEnd) : jspContent;
        assertFalse(staffSection.contains("/payPage"), "Staff Loan Portfolio must not contain /payPage link");
        assertFalse(staffSection.contains(">Pay<"), "Staff Loan Portfolio must not contain Pay button");
    }

    @Test
    @DisplayName("Test 16: All four staff roles are denied access to repayment processing endpoints (/payPage, /pay)")
    void testAllStaffRolesDeniedAccessToRepaymentEndpoints() throws Exception {
        User[] staffUsers = {admin, loanOfficer, collectionsAgent, collectionsManager};

        for (User staff : staffUsers) {
            // GET /payPage is forbidden for staff
            mockMvc.perform(get("/payPage")
                            .param("loanId", loan1.getLoanId().toString())
                            .with(user(new CustomUserDetails(staff))))
                    .andExpect(status().isForbidden());

            // POST /pay is forbidden for staff
            mockMvc.perform(post("/pay")
                            .param("loanId", loan1.getLoanId().toString())
                            .param("amount", "250")
                            .with(user(new CustomUserDetails(staff)))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        // Customer can still access /payPage and process payment for their own loan
        mockMvc.perform(get("/payPage")
                        .param("loanId", loan1.getLoanId().toString())
                        .with(user(new CustomUserDetails(customer1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/pay")
                        .param("loanId", loan1.getLoanId().toString())
                        .param("amount", "250")
                        .with(user(new CustomUserDetails(customer1)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
