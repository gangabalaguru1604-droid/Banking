package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.dto.CustomerProfileDto;
import com.loanproject.loan_app.dto.LoanSearchResultDto;
import com.loanproject.loan_app.dto.RevealRequestDto;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
public class LoanSearchAndProfileTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanService loanService;

    @Autowired
    private AuthAuditLogRepository auditRepo;

    @Autowired
    private com.loanproject.loan_app.repository.MfaChallengeRepository challengeRepository;

    @Autowired
    private com.loanproject.loan_app.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private com.loanproject.loan_app.repository.LoanReminderLogRepository reminderLogRepository;

    @Autowired
    private com.loanproject.loan_app.repository.RepaymentInstallmentRepository installmentRepository;

    @Autowired
    private com.loanproject.loan_app.repository.PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private com.loanproject.loan_app.repository.CollateralRepository collateralRepository;

    @Autowired
    private com.loanproject.loan_app.repository.GuarantorRepository guarantorRepository;

    @Autowired
    private com.loanproject.loan_app.repository.LinkedAccountRepository linkedAccountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customerMain1;
    private User customerMain2;
    private User customerNorth;

    private Loan loanMain1;
    private Loan loanMain1b;
    private Loan loanMain2;
    private Loan loanNorth;

    private User officerMain;
    private User officerNorth;
    private User collectionsAgentMain;
    private User collectionsManagerMain;
    private User adminUser;

    @BeforeEach
    void setUp() {
        auditRepo.deleteAll();
        reminderLogRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        installmentRepository.deleteAll();
        collateralRepository.deleteAll();
        guarantorRepository.deleteAll();
        linkedAccountRepository.deleteAll();
        loanRepository.deleteAll();
        challengeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Customers
        customerMain1 = new User("Rahul Sharma", "rahul.sharma@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerMain1.setBranch("MAIN");
        customerMain1.setCustomerId("CUST000101");
        customerMain1.setPhoneNumber("9876543210");
        customerMain1.setPanNumber("ABCDE1234F");
        customerMain1.setAadhaarNumber("234567890123");
        customerMain1 = userRepository.save(customerMain1);

        loanMain1 = loanService.apply(customerMain1, 500000, 10, 24);
        loanMain1.setBranch("MAIN");
        loanMain1 = loanRepository.save(loanMain1);

        loanMain1b = loanService.apply(customerMain1, 150000, 11, 12);
        loanMain1b.setBranch("MAIN");
        loanMain1b = loanRepository.save(loanMain1b);

        customerMain2 = new User("Priya Sharma", "priya.sharma@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerMain2.setBranch("MAIN");
        customerMain2.setCustomerId("CUST000102");
        customerMain2.setPhoneNumber("9876543211");
        customerMain2.setPanNumber("BCDEF2345G");
        customerMain2.setAadhaarNumber("345678901234");
        customerMain2 = userRepository.save(customerMain2);

        loanMain2 = loanService.apply(customerMain2, 300000, 10, 18);
        loanMain2.setBranch("MAIN");
        loanMain2 = loanRepository.save(loanMain2);

        customerNorth = new User("Vikram Singh", "vikram.singh@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerNorth.setBranch("NORTH");
        customerNorth.setCustomerId("CUST000201");
        customerNorth.setPhoneNumber("9811223344");
        customerNorth.setPanNumber("CDEFG3456H");
        customerNorth.setAadhaarNumber("456789012345");
        customerNorth = userRepository.save(customerNorth);

        loanNorth = loanService.apply(customerNorth, 750000, 9, 36);
        loanNorth.setBranch("NORTH");
        loanNorth = loanRepository.save(loanNorth);

        // 2. Staff
        officerMain = new User("Officer John", "officer.main@bank.com", "Password123!", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        officerMain.setBranch("MAIN");
        officerMain = userRepository.save(officerMain);

        officerNorth = new User("Officer Lisa", "officer.north@bank.com", "Password123!", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        officerNorth.setBranch("NORTH");
        officerNorth = userRepository.save(officerNorth);

        collectionsAgentMain = new User("Agent Carol", "agent.main@bank.com", "Password123!", AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgentMain.setBranch("MAIN");
        collectionsAgentMain = userRepository.save(collectionsAgentMain);

        collectionsManagerMain = new User("Manager Dave", "manager.main@bank.com", "Password123!", AppRole.ROLE_COLLECTIONS_MANAGER, UserType.STAFF);
        collectionsManagerMain.setBranch("MAIN");
        collectionsManagerMain = userRepository.save(collectionsManagerMain);

        adminUser = new User("Admin Emma", "admin@bank.com", "Password123!", AppRole.ROLE_ADMIN, UserType.STAFF);
        adminUser.setBranch("ALL");
        adminUser = userRepository.save(adminUser);
    }

    @Test
    @DisplayName("1. Valid loan account number returns the authorized profile")
    void testValidLoanAccountNumberReturnsAuthorizedProfile() throws Exception {
        MvcResult searchResult = mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", loanMain1.getAccountNumber())
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-search"))
                .andExpect(model().attributeExists("results"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> results = (Page<LoanSearchResultDto>) searchResult.getModelAndView().getModel().get("results");
        assertEquals(1, results.getTotalElements());
        assertEquals(loanMain1.getAccountNumber(), results.getContent().get(0).getAccountNumber());

        // Access Customer Profile
        MvcResult profileResult = mockMvc.perform(get("/customer-profile")
                .param("loanId", loanMain1.getLoanId().toString())
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-profile"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(model().attributeExists("profile"))
                .andReturn();

        CustomerProfileDto profile = (CustomerProfileDto) profileResult.getModelAndView().getModel().get("profile");
        assertEquals("Rahul Sharma", profile.getCustomerName());
        assertEquals(customerMain1.getCustomerId(), profile.getCustomerId());
        assertEquals(2, profile.getLoans().size(), "Should list both authorized loans for this customer in MAIN branch");
    }

    @Test
    @DisplayName("2. Invalid account-number format is rejected")
    void testInvalidAccountNumberFormatIsRejected() throws Exception {
        // Query with invalid length and format (less than 12 chars, not LN + 10 digits)
        mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", "LN123")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Account not found"));

        mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", "INVALID_ACCOUNT")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Account not found"));

        List<AuthAuditLog> auditLogs = auditRepo.findByEventTypeOrderByTimestampDesc("LOAN_SEARCH_NOT_FOUND");
        assertFalse(auditLogs.isEmpty(), "Audit log should record invalid format search attempt");
    }

    @Test
    @DisplayName("3. Nonexistent account shows 'Account not found'")
    void testNonexistentAccountShowsAccountNotFound() throws Exception {
        // Valid 12-char format but non-existent
        mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", "LN9999999999")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Account not found"));

        List<AuthAuditLog> auditLogs = auditRepo.findByEventTypeOrderByTimestampDesc("LOAN_SEARCH_NOT_FOUND");
        assertTrue(auditLogs.stream().anyMatch(l -> "ACCOUNT_NUMBER".equals(l.getDetails()) || l.getDetails().contains("ACCOUNT_NUMBER")));
    }

    @Test
    @DisplayName("4. Customer ID, mobile and name searches work")
    void testCustomerIdMobileAndNameSearchesWork() throws Exception {
        // 4a. Customer ID search
        MvcResult custIdResult = mockMvc.perform(get("/loan-search")
                .param("searchType", "CUSTOMER_ID")
                .param("query", "CUST000101")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> custIdPage = (Page<LoanSearchResultDto>) custIdResult.getModelAndView().getModel().get("results");
        assertEquals(2, custIdPage.getTotalElements());

        // 4b. Mobile search
        MvcResult phoneResult = mockMvc.perform(get("/loan-search")
                .param("searchType", "PHONE")
                .param("query", "9876543210")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> phonePage = (Page<LoanSearchResultDto>) phoneResult.getModelAndView().getModel().get("results");
        assertEquals(2, phonePage.getTotalElements());

        // 4c. Name search (case-insensitive substring)
        MvcResult nameResult = mockMvc.perform(get("/loan-search")
                .param("searchType", "NAME")
                .param("query", "rahul")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> namePage = (Page<LoanSearchResultDto>) nameResult.getModelAndView().getModel().get("results");
        assertEquals(2, namePage.getTotalElements());
    }

    @Test
    @DisplayName("5. Multiple secondary matches appear in a selectable list")
    void testMultipleSecondaryMatchesAppearInSelectableList() throws Exception {
        // Searching for last name "sharma" matches both Rahul (2 loans) and Priya (1 loan) -> 3 total loan records
        MvcResult result = mockMvc.perform(get("/loan-search")
                .param("searchType", "NAME")
                .param("query", "sharma")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> page = (Page<LoanSearchResultDto>) result.getModelAndView().getModel().get("results");
        assertEquals(3, page.getTotalElements());
        assertTrue(page.getContent().stream().anyMatch(l -> "Rahul Sharma".equals(l.getCustomerName())));
        assertTrue(page.getContent().stream().anyMatch(l -> "Priya Sharma".equals(l.getCustomerName())));
    }

    @Test
    @DisplayName("6. Results are limited to the officer's authorized branches")
    void testResultsLimitedToOfficerAuthorizedBranches() throws Exception {
        // Officer Main searches for "singh" (Customer Vikram Singh is in NORTH branch)
        MvcResult mainOfficerResult = mockMvc.perform(get("/loan-search")
                .param("searchType", "NAME")
                .param("query", "singh")
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> mainOfficerPage = (Page<LoanSearchResultDto>) mainOfficerResult.getModelAndView().getModel().get("results");
        assertEquals(0, mainOfficerPage.getTotalElements(), "Main officer must not see North branch records");

        // Officer North searches for "singh" -> finds Vikram Singh's loan
        MvcResult northOfficerResult = mockMvc.perform(get("/loan-search")
                .param("searchType", "NAME")
                .param("query", "singh")
                .with(user(new CustomUserDetails(officerNorth))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> northOfficerPage = (Page<LoanSearchResultDto>) northOfficerResult.getModelAndView().getModel().get("results");
        assertEquals(1, northOfficerPage.getTotalElements());
        assertEquals("NORTH", northOfficerPage.getContent().get(0).getBranch());

        // Admin has ALL access and sees both
        MvcResult adminResult = mockMvc.perform(get("/loan-search")
                .param("searchType", "NAME")
                .param("query", "sharma")
                .with(user(new CustomUserDetails(adminUser))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        Page<LoanSearchResultDto> adminPage = (Page<LoanSearchResultDto>) adminResult.getModelAndView().getModel().get("results");
        assertEquals(3, adminPage.getTotalElements());
    }

    @Test
    @DisplayName("7. Unauthorized branch and role access is denied and audited")
    void testUnauthorizedBranchAndRoleAccessDeniedAndAudited() throws Exception {
        // 7a. Customer attempting search is denied (403 Forbidden)
        mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", loanMain1.getAccountNumber())
                .with(user(new CustomUserDetails(customerMain1))))
                .andExpect(status().isForbidden());

        // 7b. Customer attempting customer profile is denied (403 Forbidden)
        mockMvc.perform(get("/customer-profile")
                .param("loanId", loanMain1.getLoanId().toString())
                .with(user(new CustomUserDetails(customerMain1))))
                .andExpect(status().isForbidden());

        // 7c. Officer Main attempting to view customer profile of loan in NORTH branch is redirected with error and audited
        mockMvc.perform(get("/customer-profile")
                .param("loanId", loanNorth.getLoanId().toString())
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loan-search?error=unauthorized_branch"));

        List<AuthAuditLog> deniedLogs = auditRepo.findByEventTypeOrderByTimestampDesc("PROFILE_ACCESS_DENIED");
        assertFalse(deniedLogs.isEmpty(), "Unauthorized cross-branch profile access must be audited");
        assertEquals(officerMain.getEmail(), deniedLogs.get(0).getUsername());
    }

    @Test
    @DisplayName("8. Sensitive fields are masked by default")
    void testSensitiveFieldsMaskedByDefault() throws Exception {
        MvcResult result = mockMvc.perform(get("/customer-profile")
                .param("loanId", loanMain1.getLoanId().toString())
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andReturn();

        CustomerProfileDto profile = (CustomerProfileDto) result.getModelAndView().getModel().get("profile");

        // Aadhaar: "234567890123" -> "XXXX-XXXX-0123"
        assertEquals("XXXX-XXXX-0123", profile.getMaskedAadhaar());
        assertFalse(profile.getMaskedAadhaar().contains("23456789"));

        // PAN: "ABCDE1234F" -> "ABCDE****F"
        assertEquals("ABCDE****F", profile.getMaskedPan());
        assertFalse(profile.getMaskedPan().contains("1234"));

        // Phone: "9876543210" -> "******3210"
        assertEquals("******3210", profile.getMaskedPhone());
        assertFalse(profile.getMaskedPhone().startsWith("987654"));

        // Email: "rahul.sharma@example.com" -> "r***a@example.com"
        assertTrue(profile.getMaskedEmail().contains("***"));
        assertFalse(profile.getMaskedEmail().equals(customerMain1.getEmail()));
    }

    @Test
    @DisplayName("9. Authorized reveal returns only permitted fields and creates an audit record")
    void testAuthorizedRevealReturnsOnlyPermittedFieldsAndCreatesAuditRecord() throws Exception {
        RevealRequestDto requestDto = new RevealRequestDto(
                loanMain1.getLoanId(),
                "AADHAAR",
                "Subpoena compliance verification requirement"
        );

        MvcResult revealResult = mockMvc.perform(post("/loan-search/reveal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf())
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.field").value("AADHAAR"))
                .andExpect(jsonPath("$.unmaskedValue").value("234567890123"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andReturn();

        // Verify audit log entry
        List<AuthAuditLog> auditLogs = auditRepo.findByEventTypeOrderByTimestampDesc("SENSITIVE_DATA_REVEAL_SUCCESS");
        assertFalse(auditLogs.isEmpty());
        AuthAuditLog logEntry = auditLogs.get(0);
        assertEquals(officerMain.getEmail(), logEntry.getUsername());
        assertTrue(logEntry.getDetails().contains("AADHAAR"));
        assertTrue(logEntry.getDetails().contains("Subpoena compliance"));
        // Never log plaintext sensitive value
        assertFalse(logEntry.getDetails().contains("234567890123"), "Audit log must NOT contain raw unmasked Aadhaar value");
    }

    @Test
    @DisplayName("10. Unauthorized reveal is denied and audited")
    void testUnauthorizedRevealDeniedAndAudited() throws Exception {
        // 10a. Officer Main attempts to reveal loan in NORTH branch
        RevealRequestDto crossBranchRequest = new RevealRequestDto(
                loanNorth.getLoanId(),
                "PAN",
                "Investigation on behalf of branch"
        );

        mockMvc.perform(post("/loan-search/reveal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crossBranchRequest))
                .with(csrf())
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        List<AuthAuditLog> auditLogs = auditRepo.findByEventTypeOrderByTimestampDesc("SENSITIVE_DATA_REVEAL_DENIED");
        assertFalse(auditLogs.isEmpty(), "Unauthorized reveal attempt must be audited");

        // 10b. Reveal with insufficient reason (less than 8 chars) is rejected with 400
        RevealRequestDto shortReasonRequest = new RevealRequestDto(
                loanMain1.getLoanId(),
                "PAN",
                "short"
        );

        mockMvc.perform(post("/loan-search/reveal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortReasonRequest))
                .with(csrf())
                .with(user(new CustomUserDetails(officerMain))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 10c. Customer attempting reveal is rejected with 403
        mockMvc.perform(post("/loan-search/reveal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crossBranchRequest))
                .with(csrf())
                .with(user(new CustomUserDetails(customerMain1))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Changing URL parameters cannot access another customer's loan")
    void testChangingUrlParametersCannotAccessAnotherCustomerLoan() throws Exception {
        // Officer in NORTH branch tries to tamper loanId in URL to access MAIN branch loan
        mockMvc.perform(get("/customer-profile")
                .param("loanId", loanMain1.getLoanId().toString())
                .with(user(new CustomUserDetails(officerNorth))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loan-search?error=unauthorized_branch"));

        // Accessing nonexistent loanId is rejected
        mockMvc.perform(get("/customer-profile")
                .param("loanId", "9999999")
                .with(user(new CustomUserDetails(officerNorth))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/loan-search?error=not_found"));
    }

    @Test
    @DisplayName("12. Search response performance meets the under-three-second target under a representative test workload")
    void testSearchResponsePerformanceUnderThreeSeconds() throws Exception {
        long startTime = System.currentTimeMillis();

        // Execute representative batch of 20 searches (primary and secondary across categories)
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/loan-search")
                    .param("searchType", "ACCOUNT_NUMBER")
                    .param("query", loanMain1.getAccountNumber())
                    .with(user(new CustomUserDetails(officerMain))))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/loan-search")
                    .param("searchType", "PHONE")
                    .param("query", "9876543210")
                    .with(user(new CustomUserDetails(officerMain))))
                    .andExpect(status().isOk());
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        long averagePerSearch = totalElapsed / 40;

        assertTrue(averagePerSearch < 3000, "Average search time must be under 3000ms. Actual: " + averagePerSearch + "ms");
        assertTrue(totalElapsed < 5000, "Total batch elapsed time must be under 5s. Actual: " + totalElapsed + "ms");
    }

    @Test
    @DisplayName("13. Role matrix: Collections Agent and Collections Manager can search and view profiles in their branch")
    void testCollectionsRolesCanSearchAndAccessProfiles() throws Exception {
        // Collections Agent
        mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", loanMain1.getAccountNumber())
                .with(user(new CustomUserDetails(collectionsAgentMain))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customer-profile")
                .param("loanId", loanMain1.getLoanId().toString())
                .with(user(new CustomUserDetails(collectionsAgentMain))))
                .andExpect(status().isOk());

        // Collections Manager
        mockMvc.perform(get("/loan-search")
                .param("searchType", "ACCOUNT_NUMBER")
                .param("query", loanMain1.getAccountNumber())
                .with(user(new CustomUserDetails(collectionsManagerMain))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customer-profile")
                .param("loanId", loanMain1.getLoanId().toString())
                .with(user(new CustomUserDetails(collectionsManagerMain))))
                .andExpect(status().isOk());
    }
}
