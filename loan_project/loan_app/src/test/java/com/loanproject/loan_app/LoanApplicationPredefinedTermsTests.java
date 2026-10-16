package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.File;
import java.nio.file.Files;
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
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
public class LoanApplicationPredefinedTermsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    private User testCustomer;
    private User testLoanOfficer;

    @BeforeEach
    void setUp() {
        testCustomer = userRepository.findByEmail("borrower.predefined@bank.com").orElseGet(() -> {
            User u = new User();
            u.setName("Predefined Terms Borrower");
            u.setEmail("borrower.predefined@bank.com");
            u.setPassword("Password123!");
            u.setRole(AppRole.ROLE_CUSTOMER);
            u.setUserType(UserType.CUSTOMER);
            u.setPhoneNumber("9811223344");
            u.setBranch("MAIN");
            u.setCustomerId("CUST-PREDEF-01");
            return userRepository.save(u);
        });

        testLoanOfficer = userRepository.findByEmail("officer.predefined@bank.com").orElseGet(() -> {
            User u = new User();
            u.setName("Predefined Terms Officer");
            u.setEmail("officer.predefined@bank.com");
            u.setPassword("Password123!");
            u.setRole(AppRole.ROLE_LOAN_OFFICER);
            u.setUserType(UserType.STAFF);
            u.setPhoneNumber("9811223355");
            u.setBranch("MAIN");
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("1. UI Verification: applyLoan.jsp contains only Loan Amount input and NO interest or months inputs")
    void testApplyLoanPageRendersOnlyAmountField() throws Exception {
        // Verify controller GET endpoint populates predefined model attributes
        mockMvc.perform(get("/applyLoan")
                .with(user(new CustomUserDetails(testCustomer))))
                .andExpect(status().isOk())
                .andExpect(view().name("applyLoan"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("defaultInterestRate"))
                .andExpect(model().attributeExists("defaultTenureMonths"))
                .andExpect(model().attributeExists("minLoanAmount"))
                .andExpect(model().attributeExists("maxLoanAmount"));

        // Verify JSP file content strictly has NO interest or tenure input fields
        File jspFile = new File("src/main/webapp/WEB-INF/pages/applyLoan.jsp");
        assertTrue(jspFile.exists(), "applyLoan.jsp must exist");
        String jspContent = Files.readString(jspFile.toPath());

        // Must have loan amount input
        assertTrue(jspContent.contains("name=\"amount\""), "applyLoan.jsp must have name='amount' input");
        assertTrue(jspContent.contains("Submit Loan Application"), "applyLoan.jsp must preserve Submit Loan Application button");
        assertTrue(jspContent.contains("Cancel & Back to Dashboard"), "applyLoan.jsp must preserve Cancel link");

        // Must NOT have editable interest or tenure input fields
        assertFalse(jspContent.contains("name=\"interest\""), "applyLoan.jsp must NOT contain name='interest' input");
        assertFalse(jspContent.contains("name=\"months\""), "applyLoan.jsp must NOT contain name='months' input");
    }

    @Test
    @DisplayName("2. Successful application uses predefined server interest rate and tenure")
    void testCustomerSubmitsValidAmountUsesPredefinedTerms() throws Exception {
        double applyAmount = 48000.0;
        int initialLoanCount = loanRepository.findByUserOrderByCreatedAtDesc(testCustomer).size();

        mockMvc.perform(post("/applyLoan")
                .param("amount", String.valueOf(applyAmount))
                .with(user(new CustomUserDetails(testCustomer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        List<Loan> loans = loanRepository.findByUserOrderByCreatedAtDesc(testCustomer);
        assertEquals(initialLoanCount + 1, loans.size(), "Loan should have been created");

        Loan latestLoan = loans.get(0);
        assertEquals(applyAmount, latestLoan.getAmount(), 0.001);
        assertEquals(loanService.getDefaultInterestRate(), latestLoan.getInterest(), 0.001,
                "Loan must use server predefined interest rate: " + loanService.getDefaultInterestRate());
        assertEquals(loanService.getDefaultTenureMonths(), latestLoan.getMonths(),
                "Loan must use server predefined tenure: " + loanService.getDefaultTenureMonths());

        // Validate server EMI calculation formula matches predefined terms
        double r = latestLoan.getInterest() / (12 * 100);
        double expectedEmi = (applyAmount * r * Math.pow(1 + r, latestLoan.getMonths())) /
                             (Math.pow(1 + r, latestLoan.getMonths()) - 1);
        assertEquals(expectedEmi, latestLoan.getEmi(), 0.01, "Server must compute accurate EMI with predefined terms");
    }

    @Test
    @DisplayName("3. Security: Crafted HTTP request with custom interest and months is IGNORED for customers")
    void testCraftedHttpRequestCannotOverrideServerPredefinedTerms() throws Exception {
        double applyAmount = 65000.0;
        double maliciousInterest = 1.5; // Attacker tries to get a 1.5% interest rate
        int maliciousTenure = 120;       // Attacker tries to get a 120-month tenure

        mockMvc.perform(post("/applyLoan")
                .param("amount", String.valueOf(applyAmount))
                .param("interest", String.valueOf(maliciousInterest))
                .param("months", String.valueOf(maliciousTenure))
                .with(user(new CustomUserDetails(testCustomer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        List<Loan> loans = loanRepository.findByUserOrderByCreatedAtDesc(testCustomer);
        Loan latestLoan = loans.get(0);

        // Verify attacker's parameters were completely ignored
        assertNotEquals(maliciousInterest, latestLoan.getInterest(), "Attacker's interest rate must be rejected");
        assertNotEquals(maliciousTenure, latestLoan.getMonths(), "Attacker's tenure must be rejected");

        // Verify server-side predefined terms were strictly enforced
        assertEquals(loanService.getDefaultInterestRate(), latestLoan.getInterest(), 0.001);
        assertEquals(loanService.getDefaultTenureMonths(), latestLoan.getMonths());
    }

    @Test
    @DisplayName("4. Validation: Invalid loan amounts are rejected")
    void testInvalidLoanAmountsAreRejected() {
        // Below minimum amount
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/applyLoan")
                    .param("amount", "10.0") // Less than min limit (100.0)
                    .with(user(new CustomUserDetails(testCustomer)))
                    .with(csrf()));
        });

        // Negative amount
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/applyLoan")
                    .param("amount", "-5000.0")
                    .with(user(new CustomUserDetails(testCustomer)))
                    .with(csrf()));
        });

        // Zero amount
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/applyLoan")
                    .param("amount", "0")
                    .with(user(new CustomUserDetails(testCustomer)))
                    .with(csrf()));
        });

        // Exceeding maximum limit
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/applyLoan")
                    .param("amount", "9999999999.0")
                    .with(user(new CustomUserDetails(testCustomer)))
                    .with(csrf()));
        });
    }

    @Test
    @DisplayName("5. Staff origination without interest/tenure uses predefined defaults")
    void testStaffOriginationDefaultsToPredefinedTermsWhenOmitted() throws Exception {
        double staffAmount = 80000.0;

        mockMvc.perform(post("/applyLoan")
                .param("customerEmail", testCustomer.getEmail())
                .param("amount", String.valueOf(staffAmount))
                .with(user(new CustomUserDetails(testLoanOfficer)))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        List<Loan> loans = loanRepository.findByUserOrderByCreatedAtDesc(testCustomer);
        Loan latestLoan = loans.get(0);

        assertEquals(staffAmount, latestLoan.getAmount(), 0.001);
        assertEquals(loanService.getDefaultInterestRate(), latestLoan.getInterest(), 0.001);
        assertEquals(loanService.getDefaultTenureMonths(), latestLoan.getMonths());
    }
}
