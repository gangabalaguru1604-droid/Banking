package com.loanproject.loan_app.services;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;

@Component
public class DpdReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DpdReminderScheduler.class);

    private final RepaymentInstallmentRepository installmentRepository;
    private final DpdReminderService reminderService;

    @Value("${reminder.scheduler.enabled:true}")
    private boolean schedulerEnabled = true;

    @Value("${reminder.scheduler.timezone:Asia/Kolkata}")
    private String businessTimezone = "Asia/Kolkata";

    @Value("${reminder.batch.size:100}")
    private int batchSize = 100;

    @Autowired
    public DpdReminderScheduler(RepaymentInstallmentRepository installmentRepository,
                                DpdReminderService reminderService) {
        this.installmentRepository = installmentRepository;
        this.reminderService = reminderService;
    }

    /**
     * Daily scheduled cron job running at the configured local business time and timezone.
     */
    @Scheduled(cron = "${reminder.scheduler.cron:0 0 9 * * *}", zone = "${reminder.scheduler.timezone:Asia/Kolkata}")
    public void scheduleDailyReminders() {
        if (!schedulerEnabled) {
            log.info("DPD Reminder Scheduler is disabled by configuration");
            return;
        }

        ZoneId zone = ZoneId.of(businessTimezone);
        LocalDate businessDate = LocalDate.now(zone);
        log.info("Starting DPD 1-3 Reminder Scheduled Job for business date {} (timezone: {})", businessDate, businessTimezone);

        runDailyReminderJob(businessDate);
    }

    /**
     * Executes the reminder processing workflow for a specific business date in batches.
     * Can be invoked programmatically or by tests with explicit business dates.
     */
    public int runDailyReminderJob(LocalDate businessDate) {
        if (businessDate == null) {
            businessDate = LocalDate.now(ZoneId.of(businessTimezone));
        }

        // Installments due 1, 2, or 3 calendar days ago
        LocalDate minDueDate = businessDate.minusDays(3); // DPD 3
        LocalDate maxDueDate = businessDate.minusDays(1); // DPD 1

        log.info("Identifying eligible overdue installments with due date between {} and {}", minDueDate, maxDueDate);

        int processedCount = 0;
        int pageIndex = 0;
        Page<RepaymentInstallment> pageResult;

        do {
            pageResult = installmentRepository.findEligibleOverdueInstallments(
                    minDueDate, maxDueDate, PageRequest.of(pageIndex, batchSize));

            for (RepaymentInstallment inst : pageResult.getContent()) {
                try {
                    reminderService.processInstallmentReminder(inst.getId(), businessDate);
                    processedCount++;
                } catch (Exception ex) {
                    log.error("Failed to process reminder for installment {}: {}", inst.getId(), ex.getMessage(), ex);
                }
            }
            pageIndex++;
        } while (pageResult.hasNext());

        log.info("Completed DPD Reminder Job for business date {}. Processed {} eligible installments.",
                businessDate, processedCount);
        return processedCount;
    }
}
