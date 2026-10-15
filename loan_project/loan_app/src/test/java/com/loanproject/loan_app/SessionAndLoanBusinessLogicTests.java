package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.PaymentTransactionRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;

@SpringBootTest
@AutoConfigureMockMvc
public class SessionAndLoanBusinessLogicTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Test
    @DisplayName("Test 13: Server-side session inactivity timeout is configured to exactly ten minutes")
    void testSessionInactivityTimeoutConfiguredToTenMinutes() {
        Duration timeout = serverProperties.getServlet().getSession().getTimeout();
        assertNotNull(timeout, "Session timeout must be configured");
        assertEquals(10, timeout.toMinutes(), "Server-side session timeout must be exactly 10 minutes");
        assertEquals(600, timeout.toSeconds(), "Server-side session timeout must be 600 seconds");

        // Verify cookie security configurations
        assertTrue(serverProperties.getServlet().getSession().getCookie().getHttpOnly(),
                "Session cookies must be HttpOnly");
    }

    @Test
    @DisplayName("Test 18: Existing loan application and repayment business calculations pass")
    void testExistingLoanCalculationsAndRepaymentPass() {
        User borrower = new User("John Borrower", "john.borrower@bank.com", "Password123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        borrower = userRepository.save(borrower);

        // Apply for loan: amount = 100,000, interest = 12% annual, months = 12
        double amount = 100000;
        double interest = 12.0;
        int months = 12;

        Loan loan = loanService.apply(borrower, amount, interest, months);
        assertNotNull(loan.getLoanId());
        assertEquals(amount, loan.getAmount(), 0.001);
        assertEquals(amount, loan.getRemaining(), 0.001, "Initial remaining balance must equal principal");

        // Verify reducing balance formula:
        // r = 12 / (12 * 100) = 0.01 monthly rate
        // emi = 100000 * 0.01 * (1.01)^12 / ((1.01)^12 - 1) = 8884.878867...
        double expectedMonthlyRate = 12.0 / (12 * 100);
        double expectedEmi = (amount * expectedMonthlyRate * Math.pow(1 + expectedMonthlyRate, months))
                / (Math.pow(1 + expectedMonthlyRate, months) - 1);

        assertEquals(expectedEmi, loan.getEmi(), 0.01, "Calculated EMI must match standard formula");

        // Repayment of 1 EMI
        loanService.pay(loan.getLoanId(), loan.getEmi());

        List<Loan> updatedList = loanService.get(borrower);
        assertFalse(updatedList.isEmpty(), "Loan list must not be empty");
        Loan updated = updatedList.get(0);
        assertEquals(amount - loan.getEmi(), updated.getRemaining(), 0.01,
                "Remaining balance must be reduced by payment amount");

        // Cleanup
        paymentTransactionRepository.deleteAll();
        loanRepository.delete(updated);
        userRepository.delete(borrower);
    }

    @Test
    @DisplayName("Customer with zero loans renders dashboard with empty list and zero aggregates")
    void testZeroLoansForCustomer() throws Exception {
        User userZero = new User("Zero Loan User", "zeroloan@bank.com", "Password123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userZero = userRepository.save(userZero);

        List<Loan> loans = loanService.get(userZero);
        assertNotNull(loans);
        assertTrue(loans.isEmpty(), "User with no loans must return an empty list");

        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(userZero))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalLoanCount", 0))
                .andExpect(model().attribute("activeLoanCount", 0))
                .andExpect(model().attribute("totalPrincipal", 0.0))
                .andExpect(model().attribute("totalRemaining", 0.0));

        userRepository.delete(userZero);
    }

    @Test
    @DisplayName("Customer with single loan renders dashboard with one loan and accurate aggregates")
    void testSingleLoanForCustomer() throws Exception {
        User userOne = new User("Single Loan User", "singleloan@bank.com", "Password123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userOne = userRepository.save(userOne);

        Loan singleLoan = loanService.apply(userOne, 75000, 10.5, 24);
        assertNotNull(singleLoan.getLoanId());

        List<Loan> loans = loanService.get(userOne);
        assertEquals(1, loans.size(), "User must have exactly 1 loan");
        assertEquals(singleLoan.getLoanId(), loans.get(0).getLoanId());

        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(userOne))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalLoanCount", 1))
                .andExpect(model().attribute("activeLoanCount", 1))
                .andExpect(model().attribute("totalPrincipal", 75000.0))
                .andExpect(model().attribute("totalRemaining", 75000.0))
                .andExpect(model().attributeExists("loan"));

        // Direct payPage access for single loan customer works when loanId is omitted or provided
        org.springframework.test.web.servlet.MvcResult singleRes = mockMvc.perform(get("/payPage").with(user(new CustomUserDetails(userOne))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loan"))
                .andReturn();
        Loan singleReturned = (Loan) singleRes.getModelAndView().getModel().get("loan");
        assertEquals(singleLoan.getLoanId(), singleReturned.getLoanId());

        loanRepository.delete(singleLoan);
        userRepository.delete(userOne);
    }

    @Test
    @DisplayName("Customer with 3 loans resolves IncorrectResultSizeDataAccessException and renders all loans")
    void testMultipleLoansForCustomerResolvesIncorrectResultSizeException() throws Exception {
        User userMulti = new User("Multi Loan User", "multiloan@bank.com", "Password123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userMulti = userRepository.save(userMulti);

        // Apply for 3 loans (matching the exact scenario that threw IncorrectResultSizeDataAccessException: 3 results were returned)
        Loan loanA = loanService.apply(userMulti, 50000, 11.0, 12);
        Loan loanB = loanService.apply(userMulti, 100000, 12.0, 24);
        Loan loanC = loanService.apply(userMulti, 150000, 10.0, 36);

        // 1. Verify repository directly returns List<Loan> of size 3 without throwing IncorrectResultSizeDataAccessException
        List<Loan> repoLoans = loanRepository.findByUser(userMulti);
        assertEquals(3, repoLoans.size(), "Repository findByUser must return all 3 loans");

        // 2. Verify service returns all 3 loans
        List<Loan> serviceLoans = loanService.get(userMulti);
        assertEquals(3, serviceLoans.size(), "LoanService.get must return all 3 loans");

        // 3. Verify dashboard renders all 3 loans with combined aggregates
        double expectedTotalPrincipal = loanA.getAmount() + loanB.getAmount() + loanC.getAmount();
        double expectedTotalRemaining = loanA.getRemaining() + loanB.getRemaining() + loanC.getRemaining();
        double expectedTotalEmi = loanA.getEmi() + loanB.getEmi() + loanC.getEmi();

        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(userMulti))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalLoanCount", 3))
                .andExpect(model().attribute("activeLoanCount", 3))
                .andExpect(model().attribute("totalPrincipal", expectedTotalPrincipal))
                .andExpect(model().attribute("totalRemaining", expectedTotalRemaining))
                .andExpect(model().attribute("totalEmi", expectedTotalEmi));

        // 4. Verify payPage without loanId redirects to dashboard with selectLoan prompt (does NOT use findFirst())
        mockMvc.perform(get("/payPage").with(user(new CustomUserDetails(userMulti))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?selectLoan=true"));

        // 5. Verify payPage with specific loanId loads the targeted loan
        org.springframework.test.web.servlet.MvcResult multiRes = mockMvc.perform(get("/payPage")
                .param("loanId", loanB.getLoanId().toString())
                .with(user(new CustomUserDetails(userMulti))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loan"))
                .andReturn();
        Loan multiReturned = (Loan) multiRes.getModelAndView().getModel().get("loan");
        assertEquals(loanB.getLoanId(), multiReturned.getLoanId());

        // Cleanup
        loanRepository.delete(loanA);
        loanRepository.delete(loanB);
        loanRepository.delete(loanC);
        userRepository.delete(userMulti);
    }

    @Test
    @DisplayName("Verify unauthorized user cannot view or pay another customer's loan")
    void testUnauthorizedUserCannotAccessOtherCustomerLoan() throws Exception {
        User customerA = new User("Customer A", "cust.a@bank.com", "Password123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerA = userRepository.save(customerA);
        Loan loanA = loanService.apply(customerA, 60000, 10.0, 12);

        User customerB = new User("Customer B", "cust.b@bank.com", "Password123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customerB = userRepository.save(customerB);

        // Customer B attempting to view Customer A's payPage must be 403 Forbidden
        mockMvc.perform(get("/payPage")
                .param("loanId", loanA.getLoanId().toString())
                .with(user(new CustomUserDetails(customerB))))
                .andExpect(status().isForbidden());

        // Customer B attempting to execute payment on Customer A's loan must be 403 Forbidden
        mockMvc.perform(post("/pay")
                .param("loanId", loanA.getLoanId().toString())
                .param("amount", "1000")
                .with(user(new CustomUserDetails(customerB)))
                .with(csrf()))
                .andExpect(status().isForbidden());

        // Cleanup
        loanRepository.delete(loanA);
        userRepository.delete(customerA);
        userRepository.delete(customerB);
    }

    @Test
    @DisplayName("Verify Patil Capitals customer dashboard metrics, BigDecimal balance and recent activity")
    void testPatilCapitalsCustomerDashboardMetricsAndActivity() throws Exception {
        User customer = new User("Rajesh Patil", "rajesh.patil@bank.com", "Password123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer = userRepository.save(customer);

        // 1. Active Loan
        Loan activeLoan = new Loan();
        activeLoan.setUser(customer);
        activeLoan.setAmount(100000.0);
        activeLoan.setRemaining(80000.0);
        activeLoan.setInterest(10.5);
        activeLoan.setMonths(24);
        activeLoan.setEmi(4637.0);
        activeLoan.setStatus("ACTIVE");
        activeLoan.setAccountNumber("LN1000000001");
        activeLoan.setBranch("MAIN");
        activeLoan.setCreatedAt(java.time.Instant.now().minusSeconds(86400 * 10));
        activeLoan = loanRepository.save(activeLoan);

        // 2. Pending Loan Application
        Loan pendingLoan = new Loan();
        pendingLoan.setUser(customer);
        pendingLoan.setAmount(50000.0);
        pendingLoan.setRemaining(50000.0);
        pendingLoan.setInterest(12.0);
        pendingLoan.setMonths(12);
        pendingLoan.setEmi(4442.0);
        pendingLoan.setStatus("PENDING");
        pendingLoan.setAccountNumber("LN1000000002");
        pendingLoan.setBranch("MAIN");
        pendingLoan.setCreatedAt(java.time.Instant.now().minusSeconds(86400 * 5));
        pendingLoan = loanRepository.save(pendingLoan);

        // 3. Closed Loan
        Loan closedLoan = new Loan();
        closedLoan.setUser(customer);
        closedLoan.setAmount(25000.0);
        closedLoan.setRemaining(0.0);
        closedLoan.setInterest(11.0);
        closedLoan.setMonths(6);
        closedLoan.setEmi(4300.0);
        closedLoan.setStatus("CLOSED");
        closedLoan.setAccountNumber("LN1000000003");
        closedLoan.setBranch("MAIN");
        closedLoan.setCreatedAt(java.time.Instant.now().minusSeconds(86400 * 60));
        closedLoan = loanRepository.save(closedLoan);

        // Add a real persisted PaymentTransaction on the active loan
        com.loanproject.loan_app.bean.PaymentTransaction txn = new com.loanproject.loan_app.bean.PaymentTransaction(
                activeLoan,
                java.math.BigDecimal.valueOf(4637.00),
                java.time.Instant.now().minusSeconds(3600),
                "UPI",
                "TXN-PATIL-001",
                "SUCCESS"
        );
        paymentTransactionRepository.save(txn);

        // Call /dashboard as customer
        mockMvc.perform(get("/dashboard").with(user(new CustomUserDetails(customer))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalLoanCount", 3))
                .andExpect(model().attribute("activeLoanCount", 1))
                .andExpect(model().attribute("pendingLoanCount", 1))
                .andExpect(model().attribute("outstandingBalance", java.math.BigDecimal.valueOf(130000.00).setScale(2, java.math.RoundingMode.HALF_UP)))
                .andExpect(model().attribute("totalPrincipal", 175000.0))
                .andExpect(model().attribute("totalRemaining", 130000.0))
                .andExpect(model().attributeExists("recentActivities"))
                .andExpect(model().attribute("isCustomer", true));

        // Verify Apply Loan page loads for authenticated customer
        mockMvc.perform(get("/applyLoan").with(user(new CustomUserDetails(customer))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("currentUser"));

        // Cleanup
        paymentTransactionRepository.delete(txn);
        loanRepository.delete(activeLoan);
        loanRepository.delete(pendingLoan);
        loanRepository.delete(closedLoan);
        userRepository.delete(customer);
    }
}
