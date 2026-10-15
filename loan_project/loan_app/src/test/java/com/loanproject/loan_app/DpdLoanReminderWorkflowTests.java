package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanReminderLog;
import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.controller.DpdReminderWebhookController;
import com.loanproject.loan_app.dto.ReminderMessageRequest;
import com.loanproject.loan_app.dto.ReminderSendResult;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.LoanReminderLogRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.PaymentTransactionRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.services.DpdReminderScheduler;
import com.loanproject.loan_app.services.DpdReminderService;
import com.loanproject.loan_app.services.EmailReminderProvider;
import com.loanproject.loan_app.services.LoanService;
import com.loanproject.loan_app.services.SmsReminderProvider;

@SpringBootTest
@AutoConfigureMockMvc
public class DpdLoanReminderWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DpdReminderScheduler scheduler;

    @Autowired
    private DpdReminderService reminderService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private DpdReminderWebhookController webhookController;

    @Autowired
    private LoanReminderLogRepository reminderLogRepository;

    @Autowired
    private RepaymentInstallmentRepository installmentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private AuthAuditLogRepository authAuditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailReminderProvider emailReminderProvider;

    @MockBean
    private SmsReminderProvider smsReminderProvider;

    private User testCustomer;
    private Loan testLoan;

    @BeforeEach
    public void setup() {
        reminderLogRepository.deleteAll();
        installmentRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        loanRepository.deleteAll();
        authAuditLogRepository.deleteAll();
        userRepository.deleteAll();

        testCustomer = new User("Rajesh Sharma", "rajesh.sharma@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        testCustomer.setPhoneNumber("9876543210");
        testCustomer.setBranch("MUMBAI_MAIN");
        testCustomer.setCustomerId("CUST-9871");
        testCustomer = userRepository.save(testCustomer);

        testLoan = new Loan();
        testLoan.setUser(testCustomer);
        testLoan.setAmount(50000.0);
        testLoan.setInterest(12.0);
        testLoan.setMonths(12);
        testLoan.setEmi(4442.44);
        testLoan.setRemaining(50000.0);
        testLoan.setAccountNumber("LN0009871001");
        testLoan.setBranch("MUMBAI_MAIN");
        testLoan.setStatus("ACTIVE");
        testLoan.setProductType("Personal Loan");
        testLoan = loanRepository.save(testLoan);

        reset(emailReminderProvider, smsReminderProvider);
        when(emailReminderProvider.sendReminderEmail(any())).thenReturn(ReminderSendResult.success("EML-TEST-" + UUID.randomUUID()));
        when(smsReminderProvider.sendReminderSms(any())).thenReturn(ReminderSendResult.success("SMS-TEST-" + UUID.randomUUID()));
    }

    private RepaymentInstallment createInstallment(int instNum, LocalDate dueDate, BigDecimal emi, BigDecimal paid, String status) {
        RepaymentInstallment inst = new RepaymentInstallment(
                testLoan,
                instNum,
                dueDate,
                emi,
                emi.multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP),
                emi.multiply(new BigDecimal("0.2")).setScale(2, RoundingMode.HALF_UP),
                paid,
                status,
                0
        );
        return installmentRepository.save(inst);
    }

    @Test
    @DisplayName("1. DPD 1 sends the friendly email once with zero penalty and correct details")
    public void testDpd1SendsFriendlyEmailOnce() {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1); // Exactly 1 day overdue -> DPD 1
        RepaymentInstallment inst = createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        int processed = scheduler.runDailyReminderJob(businessDate);
        assertEquals(1, processed);

        ArgumentCaptor<ReminderMessageRequest> captor = ArgumentCaptor.forClass(ReminderMessageRequest.class);
        verify(emailReminderProvider, times(1)).sendReminderEmail(captor.capture());

        ReminderMessageRequest req = captor.getValue();
        assertEquals("Rajesh Sharma", req.getCustomerName());
        assertEquals("LN******1001", req.getMaskedAccountNumber());
        assertEquals(new BigDecimal("4442.44"), req.getOverdueAmount());
        assertEquals(1, req.getDpdDay());
        assertEquals(new BigDecimal("0.00"), req.getPenaltyAmount());
        assertNotNull(req.getPaymentUrl());
        assertEquals("1800-200-8899", req.getHelpdeskNumber());

        List<LoanReminderLog> logs = reminderLogRepository.findAll();
        assertEquals(1, logs.size());
        assertEquals("SENT", logs.get(0).getStatus());
        assertEquals(1, logs.get(0).getDpdDay());
        assertEquals("EMAIL", logs.get(0).getChannel());
    }

    @Test
    @DisplayName("2. DPD 2 sends the second notice once with Rs. 150 penalty")
    public void testDpd2SendsSecondNoticeOnce() {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(2); // Exactly 2 days overdue -> DPD 2
        RepaymentInstallment inst = createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        scheduler.runDailyReminderJob(businessDate);

        ArgumentCaptor<ReminderMessageRequest> captor = ArgumentCaptor.forClass(ReminderMessageRequest.class);
        verify(emailReminderProvider, times(1)).sendReminderEmail(captor.capture());

        ReminderMessageRequest req = captor.getValue();
        assertEquals(2, req.getDpdDay());
        assertEquals(new BigDecimal("150.00"), req.getPenaltyAmount());
        assertEquals(new BigDecimal("4442.44"), req.getOverdueAmount());
    }

    @Test
    @DisplayName("3. DPD 3 sends the final email notice once with Rs. 300 penalty")
    public void testDpd3SendsFinalNoticeOnce() {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(3); // Exactly 3 days overdue -> DPD 3
        RepaymentInstallment inst = createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        scheduler.runDailyReminderJob(businessDate);

        ArgumentCaptor<ReminderMessageRequest> captor = ArgumentCaptor.forClass(ReminderMessageRequest.class);
        verify(emailReminderProvider, times(1)).sendReminderEmail(captor.capture());

        ReminderMessageRequest req = captor.getValue();
        assertEquals(3, req.getDpdDay());
        assertEquals(new BigDecimal("300.00"), req.getPenaltyAmount());
        assertEquals(new BigDecimal("4442.44"), req.getOverdueAmount());
    }

    @Test
    @DisplayName("4. A scheduler rerun does not duplicate reminders")
    public void testSchedulerRerunDoesNotDuplicateReminders() {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        RepaymentInstallment inst = createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        // First run
        scheduler.runDailyReminderJob(businessDate);
        verify(emailReminderProvider, times(1)).sendReminderEmail(any());

        // Second run on same business date
        scheduler.runDailyReminderJob(businessDate);
        // Provider must NOT be called again
        verify(emailReminderProvider, times(1)).sendReminderEmail(any());

        List<LoanReminderLog> logs = reminderLogRepository.findAll();
        assertEquals(1, logs.size());
    }

    @Test
    @DisplayName("5. A fully paid installment receives no further reminders")
    public void testFullyPaidInstallmentReceivesNoReminders() {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        // Fully paid installment
        createInstallment(1, dueDate, new BigDecimal("4442.44"), new BigDecimal("4442.44"), "PAID");

        scheduler.runDailyReminderJob(businessDate);

        verify(emailReminderProvider, never()).sendReminderEmail(any());
        verify(smsReminderProvider, never()).sendReminderSms(any());
        assertEquals(0, reminderLogRepository.count());
    }

    @Test
    @DisplayName("6. A partial payment follows the actual remaining installment balance")
    public void testPartialPaymentFollowsRemainingBalance() {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(2);
        // Partial payment: EMI is 4442.44, paid is 2000.00, remaining unpaid is 2442.44
        createInstallment(1, dueDate, new BigDecimal("4442.44"), new BigDecimal("2000.00"), "DUE");

        scheduler.runDailyReminderJob(businessDate);

        ArgumentCaptor<ReminderMessageRequest> captor = ArgumentCaptor.forClass(ReminderMessageRequest.class);
        verify(emailReminderProvider, times(1)).sendReminderEmail(captor.capture());

        ReminderMessageRequest req = captor.getValue();
        assertEquals(new BigDecimal("2442.44"), req.getOverdueAmount());
        assertEquals(2, req.getDpdDay());
    }

    @Test
    @DisplayName("7. Missing email triggers SMS and contact-update flagging")
    public void testMissingEmailTriggersSmsAndFlagging() {
        testCustomer.setEmail(""); // No email registered
        userRepository.save(testCustomer);

        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        scheduler.runDailyReminderJob(businessDate);

        // Email provider should NOT be called
        verify(emailReminderProvider, never()).sendReminderEmail(any());
        // SMS provider MUST be called
        verify(smsReminderProvider, times(1)).sendReminderSms(any());

        // Customer must be flagged for contact detail update
        User updatedCustomer = userRepository.findById(testCustomer.getId()).orElseThrow();
        assertTrue(updatedCustomer.isContactDetailUpdateRequired());

        List<LoanReminderLog> logs = reminderLogRepository.findAll();
        assertEquals(1, logs.size());
        assertEquals("SMS", logs.get(0).getChannel());
        assertEquals("SENT", logs.get(0).getStatus());
    }

    @Test
    @DisplayName("8. A confirmed email bounce triggers SMS fallback")
    public void testEmailBounceTriggersSmsFallback() throws Exception {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        scheduler.runDailyReminderJob(businessDate);

        List<LoanReminderLog> logs = reminderLogRepository.findAll();
        assertEquals(1, logs.size());
        LoanReminderLog emailLog = logs.get(0);
        assertEquals("SENT", emailLog.getStatus());
        String msgId = emailLog.getProviderMessageId();

        // Webhook arrives indicating hard bounce
        String signature = webhookController.generateSignature(msgId, "BOUNCED");
        DpdReminderWebhookController.WebhookPayload payload = new DpdReminderWebhookController.WebhookPayload();
        payload.setProviderMessageId(msgId);
        payload.setEventType("BOUNCED");
        payload.setFailureReason("Recipient address rejected: 550 User unknown");

        mockMvc.perform(post("/api/webhooks/reminders")
                .header("X-Webhook-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify status changed to BOUNCED
        LoanReminderLog updatedEmailLog = reminderLogRepository.findById(emailLog.getId()).orElseThrow();
        assertEquals("BOUNCED", updatedEmailLog.getStatus());
        assertTrue(updatedEmailLog.isContactDetailUpdateRequired());
        assertNotNull(updatedEmailLog.getBouncedTimestamp());

        // Verify SMS fallback was automatically dispatched
        verify(smsReminderProvider, times(1)).sendReminderSms(any());

        // Verify customer contact detail update flag is set
        User cust = userRepository.findById(testCustomer.getId()).orElseThrow();
        assertTrue(cust.isContactDetailUpdateRequired());
    }

    @Test
    @DisplayName("9. Temporary provider failure follows the retry policy")
    public void testTemporaryProviderFailureFollowsRetryPolicy() {
        // Mock email provider to fail transiently twice, then succeed on 3rd attempt
        AtomicInteger attemptCounter = new AtomicInteger(0);
        when(emailReminderProvider.sendReminderEmail(any())).thenAnswer(invocation -> {
            int attempt = attemptCounter.incrementAndGet();
            if (attempt < 3) {
                return ReminderSendResult.failure("421 4.7.0 Try again later", true);
            }
            return ReminderSendResult.success("EML-RETRY-SUCCESS");
        });

        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        scheduler.runDailyReminderJob(businessDate);

        // Verified 3 attempts were made
        verify(emailReminderProvider, times(3)).sendReminderEmail(any());

        List<LoanReminderLog> logs = reminderLogRepository.findAll();
        assertEquals(1, logs.size());
        assertEquals("SENT", logs.get(0).getStatus());
        assertEquals(3, logs.get(0).getRetryCount());
    }

    @Test
    @DisplayName("10. Delivery, bounce, and open webhooks update the correct reminder record")
    public void testWebhooksUpdateReminderRecord() throws Exception {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        scheduler.runDailyReminderJob(businessDate);

        LoanReminderLog logEntry = reminderLogRepository.findAll().get(0);
        String msgId = logEntry.getProviderMessageId();

        // 1. DELIVERED webhook
        String delivSig = webhookController.generateSignature(msgId, "DELIVERED");
        DpdReminderWebhookController.WebhookPayload delivPayload = new DpdReminderWebhookController.WebhookPayload();
        delivPayload.setProviderMessageId(msgId);
        delivPayload.setEventType("DELIVERED");

        mockMvc.perform(post("/api/webhooks/reminders")
                .header("X-Webhook-Signature", delivSig)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(delivPayload)))
                .andExpect(status().isOk());

        LoanReminderLog delivLog = reminderLogRepository.findById(logEntry.getId()).orElseThrow();
        assertEquals("DELIVERED", delivLog.getStatus());
        assertNotNull(delivLog.getDeliveredTimestamp());

        // 2. OPENED webhook
        String openSig = webhookController.generateSignature(msgId, "OPENED");
        DpdReminderWebhookController.WebhookPayload openPayload = new DpdReminderWebhookController.WebhookPayload();
        openPayload.setProviderMessageId(msgId);
        openPayload.setEventType("OPENED");

        mockMvc.perform(post("/api/webhooks/reminders")
                .header("X-Webhook-Signature", openSig)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(openPayload)))
                .andExpect(status().isOk());

        LoanReminderLog openLog = reminderLogRepository.findById(logEntry.getId()).orElseThrow();
        assertEquals("OPENED", openLog.getStatus());
        assertNotNull(openLog.getOpenedTimestamp());
    }

    @Test
    @DisplayName("11. Duplicate or invalid webhooks cannot corrupt status")
    public void testDuplicateOrInvalidWebhooksCannotCorruptStatus() throws Exception {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        scheduler.runDailyReminderJob(businessDate);
        LoanReminderLog logEntry = reminderLogRepository.findAll().get(0);
        String msgId = logEntry.getProviderMessageId();

        // Invalid signature must be rejected with 401 Unauthorized
        DpdReminderWebhookController.WebhookPayload payload = new DpdReminderWebhookController.WebhookPayload();
        payload.setProviderMessageId(msgId);
        payload.setEventType("DELIVERED");

        mockMvc.perform(post("/api/webhooks/reminders")
                .header("X-Webhook-Signature", "invalid_forged_signature_12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // Status remains unchanged
        assertEquals("SENT", reminderLogRepository.findById(logEntry.getId()).orElseThrow().getStatus());

        // Process valid DELIVERED webhook
        String validSig = webhookController.generateSignature(msgId, "DELIVERED");
        mockMvc.perform(post("/api/webhooks/reminders")
                .header("X-Webhook-Signature", validSig)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        assertEquals("DELIVERED", reminderLogRepository.findById(logEntry.getId()).orElseThrow().getStatus());

        // Duplicate delivery webhook must be idempotent and not corrupt state
        mockMvc.perform(post("/api/webhooks/reminders")
                .header("X-Webhook-Signature", validSig)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        assertEquals("DELIVERED", reminderLogRepository.findById(logEntry.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("12. Concurrent scheduler runs do not duplicate sends")
    public void testConcurrentSchedulerRunsDoNotDuplicateSends() throws InterruptedException {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        int concurrency = 4;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrency);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    scheduler.runDailyReminderJob(businessDate);
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one reminder log record should be created and sent
        List<LoanReminderLog> logs = reminderLogRepository.findAll();
        assertEquals(1, logs.size());
        assertEquals("SENT", logs.get(0).getStatus());
        verify(emailReminderProvider, times(1)).sendReminderEmail(any());
    }

    @Test
    @DisplayName("13. Customer and branch information is correctly isolated")
    public void testCustomerAndBranchInformationIsolation() {
        // Customer 1 in MUMBAI_MAIN
        // Create Customer 2 in PUNE_BRANCH with overdue loan
        User puneCustomer = new User("Suresh Patil", "suresh.patil@example.com", "Password123!", AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        puneCustomer.setPhoneNumber("9123456789");
        puneCustomer.setBranch("PUNE_BRANCH");
        puneCustomer.setCustomerId("CUST-PUNE-01");
        puneCustomer = userRepository.save(puneCustomer);

        Loan puneLoan = new Loan();
        puneLoan.setUser(puneCustomer);
        puneLoan.setAmount(100000.0);
        puneLoan.setInterest(10.0);
        puneLoan.setMonths(24);
        puneLoan.setEmi(4614.49);
        puneLoan.setRemaining(100000.0);
        puneLoan.setAccountNumber("LN000PUNE001");
        puneLoan.setBranch("PUNE_BRANCH");
        puneLoan.setStatus("ACTIVE");
        puneLoan = loanRepository.save(puneLoan);

        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);

        RepaymentInstallment mumbaiInst = createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        RepaymentInstallment puneInst = new RepaymentInstallment(
                puneLoan, 1, dueDate, new BigDecimal("4614.49"),
                new BigDecimal("3781.16"), new BigDecimal("833.33"),
                BigDecimal.ZERO, "DUE", 0
        );
        installmentRepository.save(puneInst);

        scheduler.runDailyReminderJob(businessDate);

        ArgumentCaptor<ReminderMessageRequest> captor = ArgumentCaptor.forClass(ReminderMessageRequest.class);
        verify(emailReminderProvider, times(2)).sendReminderEmail(captor.capture());

        List<ReminderMessageRequest> requests = captor.getAllValues();
        ReminderMessageRequest mumbaiReq = requests.stream().filter(r -> r.getRecipientEmail().equals("rajesh.sharma@example.com")).findFirst().orElseThrow();
        ReminderMessageRequest puneReq = requests.stream().filter(r -> r.getRecipientEmail().equals("suresh.patil@example.com")).findFirst().orElseThrow();

        assertEquals("LN******1001", mumbaiReq.getMaskedAccountNumber());
        assertEquals("LN******E001", puneReq.getMaskedAccountNumber());
        assertEquals("Rajesh Sharma", mumbaiReq.getCustomerName());
        assertEquals("Suresh Patil", puneReq.getCustomerName());
    }

    @Test
    @DisplayName("14. Payment confirmation cancels in-flight reminders and stops subsequent reminders")
    public void testPaymentStopsRemindersImmediately() {
        LocalDate businessDate = LocalDate.of(2026, 10, 15);
        LocalDate dueDate = businessDate.minusDays(1);
        RepaymentInstallment inst = createInstallment(1, dueDate, new BigDecimal("4442.44"), BigDecimal.ZERO, "DUE");

        // Customer pays loan installment fully
        loanService.pay(testLoan.getLoanId(), 4442.44, "NET_BANKING", "TXN_FULL_PAY_001");

        // Now run the reminder scheduler
        scheduler.runDailyReminderJob(businessDate);

        // Must NOT send any reminder email or SMS
        verify(emailReminderProvider, never()).sendReminderEmail(any());
        verify(smsReminderProvider, never()).sendReminderSms(any());

        // Installment status is updated to PAID
        RepaymentInstallment updatedInst = installmentRepository.findById(inst.getId()).orElseThrow();
        assertEquals("PAID", updatedInst.getStatus());
        assertEquals(new BigDecimal("4442.44"), updatedInst.getPaidAmount());
    }
}
