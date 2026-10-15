package com.loanproject.loan_app.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanReminderLog;
import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.dto.ReminderMessageRequest;
import com.loanproject.loan_app.dto.ReminderSendResult;
import com.loanproject.loan_app.repository.LoanReminderLogRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.DataMaskingUtils;

@Service
public class DpdReminderService {

    private static final Logger log = LoggerFactory.getLogger(DpdReminderService.class);

    private final LoanReminderLogRepository reminderLogRepository;
    private final RepaymentInstallmentRepository installmentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final EmailReminderProvider emailReminderProvider;
    private final SmsReminderProvider smsReminderProvider;

    @Value("${reminder.penalty.dpd1:0.00}")
    private BigDecimal penaltyDpd1 = BigDecimal.ZERO;

    @Value("${reminder.penalty.dpd2:150.00}")
    private BigDecimal penaltyDpd2 = new BigDecimal("150.00");

    @Value("${reminder.penalty.dpd3:300.00}")
    private BigDecimal penaltyDpd3 = new BigDecimal("300.00");

    @Value("${reminder.helpdesk.phone:1800-200-8899}")
    private String helpdeskPhone;

    @Value("${reminder.payment.base-url:http://localhost:8081/payPage}")
    private String paymentBaseUrl;

    @Value("${reminder.retry.max-attempts:3}")
    private int maxRetryAttempts = 3;

    @Value("${reminder.retry.delay-ms:1000}")
    private long retryDelayMs = 1000L;

    @Autowired
    public DpdReminderService(LoanReminderLogRepository reminderLogRepository,
                              RepaymentInstallmentRepository installmentRepository,
                              LoanRepository loanRepository,
                              UserRepository userRepository,
                              EmailReminderProvider emailReminderProvider,
                              SmsReminderProvider smsReminderProvider) {
        this.reminderLogRepository = reminderLogRepository;
        this.installmentRepository = installmentRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.emailReminderProvider = emailReminderProvider;
        this.smsReminderProvider = smsReminderProvider;
    }

    /**
     * Calculates Days Past Due (DPD) from the installment due date to the given business date.
     */
    public int calculateDpd(LocalDate dueDate, LocalDate businessDate) {
        if (dueDate == null || businessDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(dueDate, businessDate);
        return (int) Math.max(0, days);
    }

    /**
     * Determines if an installment is currently eligible for reminder processing.
     */
    public boolean isInstallmentEligibleForReminder(RepaymentInstallment installment, LocalDate businessDate) {
        if (installment == null || installment.getLoan() == null) {
            return false;
        }
        Loan loan = installment.getLoan();
        String loanStatus = loan.getStatus();
        if ("CLOSED".equalsIgnoreCase(loanStatus) || "REJECTED".equalsIgnoreCase(loanStatus) || "CANCELLED".equalsIgnoreCase(loanStatus)) {
            return false;
        }
        if ("PAID".equalsIgnoreCase(installment.getStatus())) {
            return false;
        }
        BigDecimal emi = installment.getEmiAmount();
        BigDecimal paid = installment.getPaidAmount() != null ? installment.getPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(emi) >= 0) {
            return false;
        }
        int dpd = calculateDpd(installment.getDueDate(), businessDate);
        return dpd >= 1 && dpd <= 3;
    }

    /**
     * Process an overdue installment on the specified business date.
     * Uses claim/send/result pattern where external HTTP/SMTP sends are performed outside DB transactions.
     */
    public void processInstallmentReminder(Long installmentId, LocalDate businessDate) {
        RepaymentInstallment installment = installmentRepository.findById(installmentId).orElse(null);
        if (installment == null || !isInstallmentEligibleForReminder(installment, businessDate)) {
            return;
        }

        int dpd = calculateDpd(installment.getDueDate(), businessDate);
        String cycle = installment.getDueDate().toString();
        User customer = installment.getLoan().getUser();

        boolean emailAvailable = customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()
                && !customer.isContactDetailUpdateRequired();

        if (emailAvailable) {
            processChannelReminder(installmentId, dpd, cycle, "EMAIL", businessDate);
        } else {
            // Missing email or previously flagged contact issue: direct SMS fallback
            log.info("Customer for installment {} has no valid email or requires contact update; falling back to SMS", installmentId);
            if (customer != null && !customer.isContactDetailUpdateRequired()) {
                flagCustomerContactDetailUpdate(customer.getId());
            }
            processChannelReminder(installmentId, dpd, cycle, "SMS", businessDate);
        }
    }

    /**
     * Executes the safe claim -> send -> record workflow for a specific channel.
     */
    public boolean processChannelReminder(Long installmentId, int dpdDay, String reminderCycle, String channel, LocalDate businessDate) {
        // 1. Claim slot in database (REQUIRES_NEW to isolate lock and commit quickly)
        Long logId = claimReminderLog(installmentId, dpdDay, channel, reminderCycle);
        if (logId == null) {
            log.debug("Reminder already claimed or completed for installment {} day {} channel {}", installmentId, dpdDay, channel);
            return false;
        }

        // 2. Prepare message outside DB transaction
        RepaymentInstallment installment = installmentRepository.findById(installmentId).orElse(null);
        if (installment == null) {
            cancelClaim(logId, "Installment no longer exists");
            return false;
        }

        // Recheck payment status before sending
        if ("PAID".equalsIgnoreCase(installment.getStatus()) || installment.getPaidAmount().compareTo(installment.getEmiAmount()) >= 0) {
            cancelClaim(logId, "Installment fully settled before external dispatch");
            return false;
        }

        User user = installment.getLoan().getUser();
        String customerName = user != null && user.getName() != null ? user.getName() : "Valued Customer";
        String email = user != null ? user.getEmail() : null;
        String phone = user != null ? user.getPhoneNumber() : null;
        String maskedAccount = DataMaskingUtils.maskAccountNumber(installment.getLoan().getAccountNumber());
        BigDecimal unpaid = installment.getEmiAmount().subtract(installment.getPaidAmount() != null ? installment.getPaidAmount() : BigDecimal.ZERO);
        BigDecimal penalty = getPenaltyForDpd(dpdDay);
        String paymentUrl = paymentBaseUrl + "?account=" + (installment.getLoan().getAccountNumber() != null ? installment.getLoan().getAccountNumber() : "");

        ReminderMessageRequest request = new ReminderMessageRequest(
                installment.getLoan().getLoanId(),
                installment.getId(),
                installment.getInstallmentNumber(),
                customerName,
                email,
                phone,
                maskedAccount,
                unpaid,
                installment.getDueDate(),
                dpdDay,
                penalty,
                paymentUrl,
                helpdeskPhone,
                reminderCycle
        );

        // 3. Dispatch to external provider with retries (OUTSIDE DB transaction)
        ReminderSendResult result = null;
        int attempts = 0;
        while (attempts < maxRetryAttempts) {
            attempts++;
            if ("EMAIL".equalsIgnoreCase(channel)) {
                result = emailReminderProvider.sendReminderEmail(request);
            } else {
                result = smsReminderProvider.sendReminderSms(request);
            }

            if (result != null && result.isSuccess()) {
                break;
            }

            if (result != null && result.isTransientFailure() && attempts < maxRetryAttempts) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                break; // Non-transient or retries exhausted
            }
        }

        // 4. Record result in database (REQUIRES_NEW)
        boolean sendSuccess = recordSendResult(logId, result, attempts);

        // 5. Handle fallback if email failed completely
        if (!sendSuccess && "EMAIL".equalsIgnoreCase(channel)) {
            log.warn("Email dispatch failed for installment {}. Initiating SMS fallback.", installmentId);
            if (user != null) {
                flagCustomerContactDetailUpdate(user.getId());
            }
            // Trigger SMS fallback
            processChannelReminder(installmentId, dpdDay, reminderCycle, "SMS", businessDate);
        }

        return sendSuccess;
    }

    /**
     * Atomically claims a reminder slot in the DB.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long claimReminderLog(Long installmentId, int dpdDay, String channel, String reminderCycle) {
        RepaymentInstallment installment = installmentRepository.findById(installmentId).orElse(null);
        if (installment == null) {
            return null;
        }

        Optional<LoanReminderLog> existingOpt = reminderLogRepository.findByInstallmentAndDpdDayAndChannelAndReminderCycle(
                installment, dpdDay, channel, reminderCycle);

        if (existingOpt.isPresent()) {
            LoanReminderLog existing = existingOpt.get();
            String st = existing.getStatus();
            if ("SENT".equalsIgnoreCase(st) || "DELIVERED".equalsIgnoreCase(st) || "OPENED".equalsIgnoreCase(st)
                    || "CLAIMED".equalsIgnoreCase(st)) {
                return null; // Already processed or in-progress
            }
            if ("FAILED".equalsIgnoreCase(st) && existing.getRetryCount() < maxRetryAttempts) {
                existing.setStatus("CLAIMED");
                existing.setUpdatedAt(Instant.now());
                return reminderLogRepository.save(existing).getId();
            }
            return null;
        }

        LoanReminderLog logEntry = new LoanReminderLog(
                installment.getLoan(),
                installment,
                dpdDay,
                reminderCycle,
                channel,
                "CLAIMED",
                "EMAIL".equalsIgnoreCase(channel) ? (installment.getLoan().getUser() != null ? installment.getLoan().getUser().getEmail() : null)
                        : (installment.getLoan().getUser() != null ? installment.getLoan().getUser().getPhoneNumber() : null)
        );

        try {
            LoanReminderLog saved = reminderLogRepository.saveAndFlush(logEntry);
            return saved.getId();
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent claim prevented by DB unique constraint for installment {} day {}", installmentId, dpdDay);
            return null;
        }
    }

    /**
     * Cancels an in-flight claimed reminder.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelClaim(Long logId, String reason) {
        reminderLogRepository.findById(logId).ifPresent(logEntry -> {
            logEntry.setStatus("CANCELLED");
            logEntry.setFailureDetails(reason);
            logEntry.setUpdatedAt(Instant.now());
            reminderLogRepository.save(logEntry);
        });
    }

    /**
     * Records the provider dispatch result in a dedicated transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordSendResult(Long logId, ReminderSendResult result, int attempts) {
        Optional<LoanReminderLog> opt = reminderLogRepository.findById(logId);
        if (opt.isEmpty()) {
            return false;
        }
        LoanReminderLog logEntry = opt.get();
        logEntry.setRetryCount(attempts);
        logEntry.setUpdatedAt(Instant.now());

        if (result != null && result.isSuccess()) {
            logEntry.setStatus("SENT");
            logEntry.setProviderMessageId(result.getProviderMessageId());
            logEntry.setSentTimestamp(Instant.now());
            logEntry.setFailureDetails(null);
            reminderLogRepository.save(logEntry);
            return true;
        } else {
            logEntry.setStatus("FAILED");
            String err = result != null ? result.getErrorMessage() : "Provider returned null result";
            logEntry.setFailureDetails(err);
            reminderLogRepository.save(logEntry);
            return false;
        }
    }

    /**
     * Webhook event handler for delivery, bounce, and open events.
     * Prevents duplicate status corruption and validates provider evidence.
     */
    @Transactional
    public boolean processProviderWebhook(String providerMessageId, String eventType, String failureReason) {
        if (providerMessageId == null || providerMessageId.isBlank() || eventType == null) {
            return false;
        }

        Optional<LoanReminderLog> opt = reminderLogRepository.findByProviderMessageId(providerMessageId);
        if (opt.isEmpty()) {
            log.warn("Webhook received for unknown providerMessageId: {}", providerMessageId);
            return false;
        }

        LoanReminderLog reminderLog = opt.get();
        String currentStatus = reminderLog.getStatus();
        String event = eventType.trim().toUpperCase();

        if ("DELIVERED".equals(event)) {
            // Can transition from SENT to DELIVERED. Do not overwrite if already OPENED.
            if (!"OPENED".equalsIgnoreCase(currentStatus) && !"DELIVERED".equalsIgnoreCase(currentStatus)) {
                reminderLog.setStatus("DELIVERED");
                reminderLog.setDeliveredTimestamp(Instant.now());
                reminderLog.setUpdatedAt(Instant.now());
                reminderLogRepository.save(reminderLog);
                log.info("Reminder {} updated to DELIVERED via webhook", providerMessageId);
            }
            return true;
        } else if ("OPENED".equals(event)) {
            // Can transition to OPENED
            if (!"OPENED".equalsIgnoreCase(currentStatus)) {
                reminderLog.setStatus("OPENED");
                reminderLog.setOpenedTimestamp(Instant.now());
                if (reminderLog.getDeliveredTimestamp() == null) {
                    reminderLog.setDeliveredTimestamp(Instant.now());
                }
                reminderLog.setUpdatedAt(Instant.now());
                reminderLogRepository.save(reminderLog);
                log.info("Reminder {} updated to OPENED via webhook", providerMessageId);
            }
            return true;
        } else if ("BOUNCED".equals(event)) {
            // Cannot un-bounce once recorded; prevent duplicate handling
            if (!"BOUNCED".equalsIgnoreCase(currentStatus)) {
                reminderLog.setStatus("BOUNCED");
                reminderLog.setBouncedTimestamp(Instant.now());
                reminderLog.setContactDetailUpdateRequired(true);
                reminderLog.setFailureDetails(failureReason != null ? failureReason : "Email hard bounce confirmed by provider");
                reminderLog.setUpdatedAt(Instant.now());
                reminderLogRepository.save(reminderLog);

                // Flag customer contact details as requiring update
                if (reminderLog.getLoan() != null && reminderLog.getLoan().getUser() != null) {
                    flagCustomerContactDetailUpdate(reminderLog.getLoan().getUser().getId());
                }

                // Immediately trigger SMS fallback for this installment and DPD day
                triggerSmsFallbackForBounce(reminderLog);
                log.info("Reminder {} marked BOUNCED and SMS fallback triggered", providerMessageId);
            }
            return true;
        }

        return false;
    }

    private void triggerSmsFallbackForBounce(LoanReminderLog bouncedLog) {
        if (bouncedLog.getInstallment() == null) return;
        Long installmentId = bouncedLog.getInstallment().getId();
        int dpdDay = bouncedLog.getDpdDay();
        String cycle = bouncedLog.getReminderCycle();
        LocalDate dueDate = bouncedLog.getInstallment().getDueDate();
        LocalDate businessDate = dueDate.plusDays(dpdDay);

        // Async or immediate call to SMS fallback
        processChannelReminder(installmentId, dpdDay, cycle, "SMS", businessDate);
    }

    /**
     * Flags a customer as requiring a contact detail update (visible to staff).
     */
    @Transactional
    public void flagCustomerContactDetailUpdate(Long userId) {
        if (userId == null) return;
        userRepository.findById(userId).ifPresent(user -> {
            user.setContactDetailUpdateRequired(true);
            userRepository.save(user);
            log.warn("Customer {} flagged with contactDetailUpdateRequired = true", user.getEmail());
        });
    }

    /**
     * Payment hook: Cancel in-flight or unsent reminders and ensure paid installments receive no further reminders.
     */
    @Transactional
    public void onPaymentConfirmed(Loan loan) {
        if (loan == null || loan.getLoanId() == null) return;
        List<LoanReminderLog> activeLogs = reminderLogRepository.findActiveRemindersByLoanId(loan.getLoanId());
        for (LoanReminderLog activeLog : activeLogs) {
            if ("CLAIMED".equalsIgnoreCase(activeLog.getStatus())) {
                activeLog.setStatus("CANCELLED");
                activeLog.setFailureDetails("Cancelled due to confirmed payment");
                activeLog.setUpdatedAt(Instant.now());
                reminderLogRepository.save(activeLog);
                log.info("Cancelled claimed reminder {} for loan {} due to payment", activeLog.getId(), loan.getLoanId());
            }
        }
    }

    public BigDecimal getPenaltyForDpd(int dpd) {
        switch (dpd) {
            case 1:
                return penaltyDpd1;
            case 2:
                return penaltyDpd2;
            case 3:
                return penaltyDpd3;
            default:
                return BigDecimal.ZERO;
        }
    }
}
