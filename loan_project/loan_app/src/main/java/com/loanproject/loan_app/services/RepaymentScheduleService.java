package com.loanproject.loan_app.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.PaymentTransaction;
import com.loanproject.loan_app.bean.RepaymentInstallment;
import com.loanproject.loan_app.dto.DelinquencySectionDTO;
import com.loanproject.loan_app.dto.OutstandingSummarySectionDTO;
import com.loanproject.loan_app.dto.PaymentHistorySectionDTO;
import com.loanproject.loan_app.dto.PaymentTransactionDTO;
import com.loanproject.loan_app.dto.RepaymentInstallmentDTO;
import com.loanproject.loan_app.dto.RepaymentScheduleSectionDTO;
import com.loanproject.loan_app.repository.PaymentTransactionRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;

@Service
public class RepaymentScheduleService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentScheduleService.class);

    private final RepaymentInstallmentRepository installmentRepo;
    private final PaymentTransactionRepository paymentRepo;

    public RepaymentScheduleService(RepaymentInstallmentRepository installmentRepo,
                                    PaymentTransactionRepository paymentRepo) {
        this.installmentRepo = installmentRepo;
        this.paymentRepo = paymentRepo;
    }

    /**
     * Synchronizes and persists installment paid amounts, status transitions, and DPD based on confirmed successful payments.
     */
    @Transactional
    public List<RepaymentInstallment> synchronizeInstallmentsWithPayments(Loan loan, LocalDate businessDate) {
        if (loan == null || loan.getLoanId() == null) {
            return new ArrayList<>();
        }

        LocalDate today = businessDate != null ? businessDate : LocalDate.now(ZoneId.systemDefault());
        List<RepaymentInstallment> installments = installmentRepo.findByLoan_LoanIdOrderByInstallmentNumberAsc(loan.getLoanId());

        if (installments.isEmpty()) {
            installments = generateAndSaveAmortizationSchedule(loan);
        }

        // Apply successful payment transactions
        List<PaymentTransaction> payments = paymentRepo.findByLoan_LoanIdOrderByPaymentDateDesc(loan.getLoanId());
        BigDecimal totalPaid = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Also respect any existing installment paid amounts if direct payments were recorded
        BigDecimal totalFromInstallments = installments.stream()
                .map(RepaymentInstallment::getPaidAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditToAllocate = totalPaid.max(totalFromInstallments);
        BigDecimal remainingCredit = creditToAllocate;

        for (RepaymentInstallment inst : installments) {
            BigDecimal emi = inst.getEmiAmount();
            BigDecimal paidForThis;

            if (remainingCredit.compareTo(emi) >= 0) {
                paidForThis = emi;
                remainingCredit = remainingCredit.subtract(emi);
                inst.setPaidAmount(paidForThis);
                inst.setStatus("PAID");
                inst.setDpd(0); // Paid/completed installments never show DPD
            } else if (remainingCredit.compareTo(BigDecimal.ZERO) > 0) {
                paidForThis = remainingCredit;
                remainingCredit = BigDecimal.ZERO;
                inst.setPaidAmount(paidForThis);
                inst.setStatus("PARTIALLY_PAID");
                if (inst.getDueDate().isBefore(today)) {
                    int dpd = (int) ChronoUnit.DAYS.between(inst.getDueDate(), today);
                    inst.setDpd(Math.max(0, dpd));
                } else {
                    inst.setDpd(0);
                }
            } else {
                paidForThis = BigDecimal.ZERO;
                inst.setPaidAmount(paidForThis);
                if (inst.getDueDate().isBefore(today)) {
                    inst.setStatus("OVERDUE");
                    int dpd = (int) ChronoUnit.DAYS.between(inst.getDueDate(), today);
                    inst.setDpd(Math.max(0, dpd));
                } else {
                    inst.setStatus("DUE");
                    inst.setDpd(0);
                }
            }
        }

        return installmentRepo.saveAll(installments);
    }

    /**
     * Retrieves or generates the full repayment schedule for a loan, evaluating DPD and status against business date.
     */
    @Transactional
    public RepaymentScheduleSectionDTO getRepaymentSchedule(Loan loan, LocalDate businessDate) {
        List<RepaymentInstallment> installments = synchronizeInstallmentsWithPayments(loan, businessDate);

        List<RepaymentInstallmentDTO> dtos = new ArrayList<>();
        int paidCount = 0;
        int overdueCount = 0;

        for (RepaymentInstallment inst : installments) {
            if ("PAID".equalsIgnoreCase(inst.getStatus()) || "COMPLETED".equalsIgnoreCase(inst.getStatus())) {
                paidCount++;
            } else if ("OVERDUE".equalsIgnoreCase(inst.getStatus()) ||
                    ("PARTIALLY_PAID".equalsIgnoreCase(inst.getStatus()) && inst.getDpd() > 0)) {
                overdueCount++;
            }

            dtos.add(new RepaymentInstallmentDTO(
                    inst.getInstallmentNumber(),
                    inst.getDueDate().toString(),
                    inst.getEmiAmount(),
                    inst.getPrincipalComponent(),
                    inst.getInterestComponent(),
                    inst.getPaidAmount(),
                    inst.getStatus(),
                    inst.getDpd()
            ));
        }

        return new RepaymentScheduleSectionDTO(dtos, installments.size(), paidCount, overdueCount);
    }

    /**
     * Generates a standard amortization schedule based on loan tenure, interest rate, and disbursement date.
     */
    @Transactional
    public List<RepaymentInstallment> generateAndSaveAmortizationSchedule(Loan loan) {
        List<RepaymentInstallment> generated = new ArrayList<>();
        double principal = loan.getAmount();
        double annualRate = loan.getInterest();
        int tenureMonths = Math.max(1, loan.getMonths());
        LocalDate startDate = loan.getDisbursementDate();

        BigDecimal balance = BigDecimal.valueOf(principal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = BigDecimal.valueOf(annualRate)
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        BigDecimal emi = BigDecimal.valueOf(loan.getEmi()).setScale(2, RoundingMode.HALF_UP);
        if (emi.compareTo(BigDecimal.ZERO) <= 0) {
            emi = calculateEmi(principal, annualRate, tenureMonths);
        }

        for (int i = 1; i <= tenureMonths; i++) {
            LocalDate dueDate = startDate.plusMonths(i);
            BigDecimal interestComponent = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComponent = emi.subtract(interestComponent).setScale(2, RoundingMode.HALF_UP);

            if (i == tenureMonths || principalComponent.compareTo(balance) > 0) {
                principalComponent = balance;
                emi = principalComponent.add(interestComponent).setScale(2, RoundingMode.HALF_UP);
            }

            balance = balance.subtract(principalComponent).max(BigDecimal.ZERO);

            RepaymentInstallment inst = new RepaymentInstallment(
                    loan,
                    i,
                    dueDate,
                    emi,
                    principalComponent,
                    interestComponent,
                    BigDecimal.ZERO,
                    "DUE",
                    0
            );
            generated.add(inst);
        }

        return installmentRepo.saveAll(generated);
    }

    /**
     * Calculates the overall delinquency information and account-level DPD.
     */
    public DelinquencySectionDTO calculateDelinquency(RepaymentScheduleSectionDTO schedule, Loan loan) {
        if (schedule == null || schedule.getInstallments().isEmpty()) {
            return new DelinquencySectionDTO(0, BigDecimal.ZERO, "None", "CURRENT", "STANDARD");
        }

        int maxDpd = 0;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        for (RepaymentInstallmentDTO inst : schedule.getInstallments()) {
            if ("OVERDUE".equalsIgnoreCase(inst.getStatus()) ||
                    ("PARTIALLY_PAID".equalsIgnoreCase(inst.getStatus()) && inst.getDpd() > 0)) {
                if (inst.getDpd() > maxDpd) {
                    maxDpd = inst.getDpd();
                }
                BigDecimal unpaid = inst.getEmiAmount().subtract(inst.getPaidAmount() != null ? inst.getPaidAmount() : BigDecimal.ZERO);
                if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
                    overdueAmount = overdueAmount.add(unpaid);
                }
            }
        }

        String bucket;
        String accountStatus;
        if (maxDpd == 0) {
            bucket = "CURRENT";
            accountStatus = "STANDARD";
        } else if (maxDpd <= 30) {
            bucket = "SMA-0";
            accountStatus = "SPECIAL_MENTION";
        } else if (maxDpd <= 60) {
            bucket = "SMA-1";
            accountStatus = "SPECIAL_MENTION";
        } else if (maxDpd <= 90) {
            bucket = "SMA-2";
            accountStatus = "SPECIAL_MENTION";
        } else {
            bucket = "NPA";
            accountStatus = "NON_PERFORMING";
        }

        String lastReminder = maxDpd > 0 ? "SMS & Email sent on " + LocalDate.now(ZoneId.systemDefault()).minusDays(Math.min(maxDpd, 5)) : "N/A - Account Current";

        return new DelinquencySectionDTO(maxDpd, overdueAmount, lastReminder, bucket, accountStatus);
    }

    /**
     * Calculates the authoritative outstanding balances using BigDecimal with explicit rounding.
     */
    public OutstandingSummarySectionDTO calculateOutstandingSummary(Loan loan, DelinquencySectionDTO delinquency) {
        BigDecimal principalOutstanding = BigDecimal.valueOf(loan.getRemaining()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal overdueAmount = delinquency != null ? delinquency.getOverdueAmount() : BigDecimal.ZERO;
        int dpd = delinquency != null ? delinquency.getDpd() : 0;

        BigDecimal accruedInterest = BigDecimal.ZERO;
        BigDecimal penaltiesAndLateFees = BigDecimal.ZERO;

        if (dpd > 0 && overdueAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Accrued penal interest: (Overdue * Annual Rate / 365) * DPD
            BigDecimal dailyRate = BigDecimal.valueOf(loan.getInterest())
                    .divide(BigDecimal.valueOf(36500), 8, RoundingMode.HALF_UP);
            accruedInterest = overdueAmount.multiply(dailyRate)
                    .multiply(BigDecimal.valueOf(dpd))
                    .setScale(2, RoundingMode.HALF_UP);

            // 2% late payment penalty on overdue amount (minimum 250)
            penaltiesAndLateFees = overdueAmount.multiply(BigDecimal.valueOf(0.02))
                    .max(BigDecimal.valueOf(250.00))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalPayable = principalOutstanding.add(accruedInterest).add(penaltiesAndLateFees)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal foreclosureAmount;
        boolean foreclosureAvailable = true;
        String foreclosureExplanation = null;

        if ("CLOSED".equalsIgnoreCase(loan.getStatus()) || principalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            foreclosureAmount = BigDecimal.ZERO;
            foreclosureExplanation = "Loan account is already fully settled and closed.";
        } else {
            // Foreclosure amount: Principal + Accrued Interest + 2% prepayment/foreclosure charge
            BigDecimal foreclosureCharges = principalOutstanding.multiply(BigDecimal.valueOf(0.02)).setScale(2, RoundingMode.HALF_UP);
            foreclosureAmount = principalOutstanding.add(accruedInterest).add(foreclosureCharges).setScale(2, RoundingMode.HALF_UP);
        }

        return new OutstandingSummarySectionDTO(
                principalOutstanding,
                accruedInterest,
                penaltiesAndLateFees,
                totalPayable,
                foreclosureAmount,
                foreclosureAvailable,
                foreclosureExplanation
        );
    }

    /**
     * Retrieves actual payment transactions from the repository.
     */
    public PaymentHistorySectionDTO getPaymentHistory(Loan loan) {
        List<PaymentTransaction> txns = paymentRepo.findByLoanOrderByPaymentDateDesc(loan);
        List<PaymentTransactionDTO> dtos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PaymentTransaction p : txns) {
            dtos.add(new PaymentTransactionDTO(
                    p.getPaymentDate().toString(),
                    p.getAmount(),
                    p.getPaymentMode(),
                    p.getTransactionReference(),
                    p.getStatus()
            ));
            if ("SUCCESS".equalsIgnoreCase(p.getStatus())) {
                total = total.add(p.getAmount());
            }
        }

        return new PaymentHistorySectionDTO(dtos, total.setScale(2, RoundingMode.HALF_UP));
    }

    public static BigDecimal calculateEmi(double amount, double interest, int months) {
        double r = interest / 1200.0;
        double emi = (amount * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }
}
