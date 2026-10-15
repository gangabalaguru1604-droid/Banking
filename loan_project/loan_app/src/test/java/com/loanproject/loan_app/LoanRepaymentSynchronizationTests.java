package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
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
import com.loanproject.loan_app.bean.PaymentTransaction;
import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.dto.LoanAccountDetailsDTO;
import com.loanproject.loan_app.dto.RepaymentInstallmentDTO;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.CollateralRepository;
import com.loanproject.loan_app.repository.GuarantorRepository;
import com.loanproject.loan_app.repository.LinkedAccountRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.MfaChallengeRepository;
import com.loanproject.loan_app.repository.PasswordResetTokenRepository;
import com.loanproject.loan_app.repository.PaymentTransactionRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanAccountDetailsService;
import com.loanproject.loan_app.services.LoanService;
import com.loanproject.loan_app.services.RepaymentScheduleService;

@SpringBootTest
@AutoConfigureMockMvc
public class LoanRepaymentSynchronizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanService loanService;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Autowired
    private LoanAccountDetailsService loanAccountDetailsService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepaymentInstallmentRepository installmentRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private AuthAuditLogRepository auditRepo;

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LinkedAccountRepository linkedAccountRepository;

    @Autowired
    private MfaChallengeRepository challengeRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User borrower;
    private User officer;
    private Loan testLoan;

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

        borrower = new User("Siddharth Patel", "siddharth@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        borrower.setBranch("MAIN");
        borrower = userRepository.save(borrower);

        officer = new User("Pooja Nair", "pooja.officer@bank.com", "Password123!", AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        officer.setBranch("MAIN");
        officer = userRepository.save(officer);

        // Sanction loan: 120,000 for 12 months at 12% interest
        testLoan = loanService.apply(borrower, 120000.0, 12.0, 12);
        testLoan.setBranch("MAIN");
        testLoan.setDisbursedAmount(120000.0);
        testLoan.setDisbursementDate(LocalDate.now());
        testLoan = loanRepository.save(testLoan);
    }

    @Test
    @DisplayName("1. No payment: installment remains DUE and paidAmount is 0")
    void test1_NoPaymentInstallmentRemainsDue() {
        List<RepaymentInstallment> installments = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(testLoan.getLoanId());
        assertFalse(installments.isEmpty(), "Installments must be pre-generated upon loan origination");

        for (RepaymentInstallment inst : installments) {
            assertEquals(BigDecimal.ZERO.setScale(2), inst.getPaidAmount().setScale(2));
            assertEquals("DUE", inst.getStatus());
            assertEquals(0, inst.getDpd());
        }

        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officer, testLoan.getLoanId(), null);
        assertNotNull(details.getRepaymentSchedule().getData());
        List<RepaymentInstallmentDTO> dtos = details.getRepaymentSchedule().getData().getInstallments();
        assertEquals(12, dtos.size());
        assertEquals("DUE", dtos.get(0).getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), dtos.get(0).getPaidAmount().setScale(2));
    }

    @Test
    @DisplayName("2. Partial payment: paid amount increases and status becomes PARTIALLY_PAID")
    void test2_PartialPaymentStatusPartiallyPaid() throws Exception {
        BigDecimal emi = BigDecimal.valueOf(testLoan.getEmi()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal partialAmount = emi.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        loanService.pay(testLoan.getLoanId(), partialAmount.doubleValue());

        // Check database persistence directly
        List<RepaymentInstallment> installments = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(testLoan.getLoanId());
        RepaymentInstallment inst1 = installments.get(0);
        assertEquals(partialAmount, inst1.getPaidAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals("PARTIALLY_PAID", inst1.getStatus());

        // Installment 2 must remain untouched
        RepaymentInstallment inst2 = installments.get(1);
        assertEquals(BigDecimal.ZERO.setScale(2), inst2.getPaidAmount().setScale(2));
        assertEquals("DUE", inst2.getStatus());

        // Check 360 View DTO
        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officer, testLoan.getLoanId(), null);
        RepaymentInstallmentDTO instDto = details.getRepaymentSchedule().getData().getInstallments().get(0);
        assertEquals("PARTIALLY_PAID", instDto.getStatus());
        assertEquals(partialAmount, instDto.getPaidAmount().setScale(2, RoundingMode.HALF_UP));

        // Check 360 View via MockMvc
        MvcResult mvcRes = mockMvc.perform(get("/loan-details")
                        .param("loanId", testLoan.getLoanId().toString())
                        .with(user(new CustomUserDetails(officer))))
                .andExpect(status().isOk())
                .andExpect(view().name("loan-account-details"))
                .andExpect(model().attributeExists("loanDetails"))
                .andReturn();

        LoanAccountDetailsDTO mvcDetails = (LoanAccountDetailsDTO) mvcRes.getModelAndView().getModel().get("loanDetails");
        assertEquals("PARTIALLY_PAID", mvcDetails.getRepaymentSchedule().getData().getInstallments().get(0).getStatus());
        assertEquals(partialAmount, mvcDetails.getRepaymentSchedule().getData().getInstallments().get(0).getPaidAmount());
    }

    @Test
    @DisplayName("3. Full payment: installment becomes PAID / COMPLETED")
    void test3_FullPaymentStatusCompletedOrPaid() {
        BigDecimal emi = BigDecimal.valueOf(testLoan.getEmi()).setScale(2, RoundingMode.HALF_UP);

        loanService.pay(testLoan.getLoanId(), emi.doubleValue());

        List<RepaymentInstallment> installments = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(testLoan.getLoanId());
        RepaymentInstallment inst1 = installments.get(0);
        assertEquals(emi, inst1.getPaidAmount().setScale(2, RoundingMode.HALF_UP));
        assertTrue("PAID".equalsIgnoreCase(inst1.getStatus()) || "COMPLETED".equalsIgnoreCase(inst1.getStatus()));
        assertEquals(0, inst1.getDpd());

        RepaymentInstallment inst2 = installments.get(1);
        assertEquals(BigDecimal.ZERO.setScale(2), inst2.getPaidAmount().setScale(2));
        assertEquals("DUE", inst2.getStatus());

        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officer, testLoan.getLoanId(), null);
        RepaymentInstallmentDTO instDto = details.getRepaymentSchedule().getData().getInstallments().get(0);
        assertTrue("PAID".equalsIgnoreCase(instDto.getStatus()) || "COMPLETED".equalsIgnoreCase(instDto.getStatus()));
    }

    @Test
    @DisplayName("4. Successful payment: balance and transaction history update correctly")
    void test4_SuccessfulPaymentUpdatesBalanceAndHistory() {
        double initialRemaining = testLoan.getRemaining();
        double payAmount = 15000.0;

        PaymentTransaction txn = loanService.pay(testLoan.getLoanId(), payAmount, "NET_BANKING", "TXN-TEST-SUCCESS-1");
        assertNotNull(txn);
        assertEquals("SUCCESS", txn.getStatus());

        Loan updatedLoan = loanRepository.findById(testLoan.getLoanId()).orElseThrow();
        assertEquals(initialRemaining - payAmount, updatedLoan.getRemaining(), 0.01);

        LoanAccountDetailsDTO details = loanAccountDetailsService.getLoanAccount360Details(officer, testLoan.getLoanId(), null);
        // Verified Payment Transactions
        List<PaymentTransaction> txns = paymentTransactionRepository.findByLoan_LoanIdOrderByPaymentDateDesc(testLoan.getLoanId());
        assertEquals(1, txns.size());
        assertEquals(BigDecimal.valueOf(payAmount).setScale(2, RoundingMode.HALF_UP), txns.get(0).getAmount());
        assertEquals("TXN-TEST-SUCCESS-1", txns.get(0).getTransactionReference());

        // Outstanding Summary on 360 View
        BigDecimal expectedPrincipal = BigDecimal.valueOf(initialRemaining - payAmount).setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedPrincipal, details.getOutstandingSummary().getData().getPrincipalOutstanding());
    }

    @Test
    @DisplayName("5. Failed payment: balance and installment remain unchanged")
    void test5_FailedPaymentBalanceAndInstallmentUnchanged() {
        double initialRemaining = testLoan.getRemaining();

        // 5a. Invalid amount should fail and roll back
        assertThrows(IllegalArgumentException.class, () -> loanService.pay(testLoan.getLoanId(), -500.0));
        assertThrows(IllegalArgumentException.class, () -> loanService.pay(testLoan.getLoanId(), 0.0));

        Loan loanAfterError = loanRepository.findById(testLoan.getLoanId()).orElseThrow();
        assertEquals(initialRemaining, loanAfterError.getRemaining(), 0.01);

        List<RepaymentInstallment> instsAfterError = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(testLoan.getLoanId());
        assertEquals("DUE", instsAfterError.get(0).getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), instsAfterError.get(0).getPaidAmount().setScale(2));

        // 5b. A PaymentTransaction with FAILED status in ledger does NOT reduce installment due
        PaymentTransaction failedTxn = new PaymentTransaction(testLoan, BigDecimal.valueOf(10000.00), Instant.now(), "UPI", "TXN-FAIL-001", "FAILED");
        paymentTransactionRepository.save(failedTxn);

        repaymentScheduleService.synchronizeInstallmentsWithPayments(testLoan, LocalDate.now());
        List<RepaymentInstallment> instsAfterFailed = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(testLoan.getLoanId());
        assertEquals("DUE", instsAfterFailed.get(0).getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), instsAfterFailed.get(0).getPaidAmount().setScale(2));
    }

    @Test
    @DisplayName("6. Repeated request: the same payment reference is not deducted twice (idempotency)")
    void test6_RepeatedRequestNotDeductedTwiceIdempotency() {
        double initialRemaining = testLoan.getRemaining();
        String idempotencyRef = "TXN-IDEMP-9999";
        double amount = 5000.0;

        // First payment request
        PaymentTransaction firstTxn = loanService.pay(testLoan.getLoanId(), amount, "UPI", idempotencyRef);
        assertNotNull(firstTxn);

        Loan afterFirst = loanRepository.findById(testLoan.getLoanId()).orElseThrow();
        assertEquals(initialRemaining - amount, afterFirst.getRemaining(), 0.01);

        // Repeated payment request with same reference
        PaymentTransaction secondTxn = loanService.pay(testLoan.getLoanId(), amount, "UPI", idempotencyRef);
        assertNotNull(secondTxn);
        assertEquals(firstTxn.getId(), secondTxn.getId(), "Must return existing transaction");

        // Verify remaining balance was NOT deducted a second time
        Loan afterSecond = loanRepository.findById(testLoan.getLoanId()).orElseThrow();
        assertEquals(initialRemaining - amount, afterSecond.getRemaining(), 0.01, "Balance must not be deducted twice");

        // Verify only 1 payment transaction in repository
        List<PaymentTransaction> allTxns = paymentTransactionRepository.findByLoan_LoanIdOrderByPaymentDateDesc(testLoan.getLoanId());
        assertEquals(1, allTxns.size());

        // Verify installment paid amount was NOT doubled
        List<RepaymentInstallment> installments = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(testLoan.getLoanId());
        assertEquals(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP), installments.get(0).getPaidAmount().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("7. Multiple installments: payment is applied sequentially to the correct installments")
    void test7_MultipleInstallmentsPaymentAllocation() {
        BigDecimal emi = BigDecimal.valueOf(testLoan.getEmi()).setScale(2, RoundingMode.HALF_UP);
        // Pay 2.5 EMIs
        BigDecimal totalToPay = emi.multiply(BigDecimal.valueOf(2.5)).setScale(2, RoundingMode.HALF_UP);

        loanService.pay(testLoan.getLoanId(), totalToPay.doubleValue());

        List<RepaymentInstallment> installments = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(testLoan.getLoanId());

        // Installment 1: Fully settled
        RepaymentInstallment inst1 = installments.get(0);
        assertEquals(emi, inst1.getPaidAmount().setScale(2, RoundingMode.HALF_UP));
        assertTrue("PAID".equalsIgnoreCase(inst1.getStatus()) || "COMPLETED".equalsIgnoreCase(inst1.getStatus()));

        // Installment 2: Fully settled
        RepaymentInstallment inst2 = installments.get(1);
        assertEquals(emi, inst2.getPaidAmount().setScale(2, RoundingMode.HALF_UP));
        assertTrue("PAID".equalsIgnoreCase(inst2.getStatus()) || "COMPLETED".equalsIgnoreCase(inst2.getStatus()));

        // Installment 3: Partially paid (0.5 EMI)
        RepaymentInstallment inst3 = installments.get(2);
        BigDecimal expectedInst3 = totalToPay.subtract(emi.multiply(BigDecimal.valueOf(2))).setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedInst3, inst3.getPaidAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals("PARTIALLY_PAID", inst3.getStatus());

        // Installment 4: Unpaid
        RepaymentInstallment inst4 = installments.get(3);
        assertEquals(BigDecimal.ZERO.setScale(2), inst4.getPaidAmount().setScale(2));
        assertEquals("DUE", inst4.getStatus());
    }

    @Test
    @DisplayName("8. Refreshing or reopening the 360° view: values remain consistent with database")
    void test8_Refreshing360ViewRemainsConsistent() throws Exception {
        BigDecimal emi = BigDecimal.valueOf(testLoan.getEmi()).setScale(2, RoundingMode.HALF_UP);
        loanService.pay(testLoan.getLoanId(), emi.doubleValue(), "UPI", "TXN-REFRESH-TEST");

        // Simulate multiple visits and refresh=true parameter
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(get("/loan-details")
                            .param("loanId", testLoan.getLoanId().toString())
                            .param("refresh", "true")
                            .with(user(new CustomUserDetails(officer))))
                    .andExpect(status().isOk())
                    .andExpect(view().name("loan-account-details"))
                    .andReturn();

            LoanAccountDetailsDTO details = (LoanAccountDetailsDTO) result.getModelAndView().getModel().get("loanDetails");
            assertNotNull(details);

            // Installment status & paid amount
            RepaymentInstallmentDTO firstInst = details.getRepaymentSchedule().getData().getInstallments().get(0);
            assertTrue("PAID".equalsIgnoreCase(firstInst.getStatus()) || "COMPLETED".equalsIgnoreCase(firstInst.getStatus()));
            assertEquals(emi, firstInst.getPaidAmount().setScale(2, RoundingMode.HALF_UP));

            // Outstanding balance
            Loan persistedLoan = loanRepository.findById(testLoan.getLoanId()).orElseThrow();
            assertEquals(BigDecimal.valueOf(persistedLoan.getRemaining()).setScale(2, RoundingMode.HALF_UP),
                    details.getOutstandingSummary().getData().getPrincipalOutstanding());

            // Transactions
            assertEquals(1, details.getPaymentHistory().getData().getTransactions().size());
            assertEquals("TXN-REFRESH-TEST", details.getPaymentHistory().getData().getTransactions().get(0).getTransactionReference());
        }
    }
}
