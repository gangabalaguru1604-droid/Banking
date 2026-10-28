package com.loanproject.loan_app.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.LoanRepository;

@Service
public class LoanService {

    @Autowired
    LoanRepository repo;

    @Autowired
    private com.loanproject.loan_app.repository.PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Autowired(required = false)
    private DpdReminderService dpdReminderService;

    @org.springframework.beans.factory.annotation.Value("${loan.product.default.interest-rate:10.5}")
    private double defaultInterestRate = 10.5;

    @org.springframework.beans.factory.annotation.Value("${loan.product.default.tenure-months:24}")
    private int defaultTenureMonths = 24;

    @org.springframework.beans.factory.annotation.Value("${loan.product.min-amount:100.0}")
    private double minAmount = 100.0;

    @org.springframework.beans.factory.annotation.Value("${loan.product.max-amount:10000000.0}")
    private double maxAmount = 10000000.0;

    public double getDefaultInterestRate() {
        return defaultInterestRate;
    }

    public void setDefaultInterestRate(double defaultInterestRate) {
        this.defaultInterestRate = defaultInterestRate;
    }

    public int getDefaultTenureMonths() {
        return defaultTenureMonths;
    }

    public void setDefaultTenureMonths(int defaultTenureMonths) {
        this.defaultTenureMonths = defaultTenureMonths;
    }

    public double getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(double minAmount) {
        this.minAmount = minAmount;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public void validateLoanAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < minAmount || amount > maxAmount) {
            throw new IllegalArgumentException(String.format("Loan amount must be between ₹%,.2f and ₹%,.2f", minAmount, maxAmount));
        }
    }

    public Loan applyWithPredefinedTerms(User user, double amount) {
        validateLoanAmount(amount);
        return apply(user, amount, defaultInterestRate, defaultTenureMonths);
    }

    public Loan apply(User user, double amount, double interest, int months) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero");
        }
        if (interest <= 0 || interest > 100) {
            throw new IllegalArgumentException("Annual interest rate must be between 0.1% and 100%");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Tenure months must be greater than zero");
        }

        double r = interest / (12 * 100);

        double emi = (amount * r * Math.pow(1 + r, months)) /
                     (Math.pow(1 + r, months) - 1);

        Loan l = new Loan();
        l.setUser(user);
        l.setAmount(amount);
        l.setInterest(interest);
        l.setMonths(months);
        l.setEmi(emi);
        l.setRemaining(amount);
        l.setBranch(user != null && user.getBranch() != null ? user.getBranch() : "MAIN");
        l.setStatus("ACTIVE");
        l.setProductType("Personal Loan");
        l.setInterestType("FIXED");
        l.setDisbursedAmount(amount);
        l.setDisbursementDate(java.time.LocalDate.now());

        Loan saved = repo.save(l);
        if (saved.getAccountNumber() == null || saved.getAccountNumber().isBlank()) {
            String accNum = String.format("LN%010d", Math.abs(saved.getLoanId()));
            if (repo.findByAccountNumber(accNum).isPresent()) {
                accNum = String.format("LN%08d%02d", Math.abs(saved.getLoanId()), (int) (Math.random() * 90 + 10));
            }
            saved.setAccountNumber(accNum);
            saved = repo.save(saved);
        }
        repaymentScheduleService.generateAndSaveAmortizationSchedule(saved);
        return saved;
    }

    @Transactional
    public Loan applyPendingLoan(User user, double amount, double interest, int months) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero");
        }
        if (interest <= 0 || interest > 100) {
            throw new IllegalArgumentException("Annual interest rate must be between 0.1% and 100%");
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Tenure months must be greater than zero");
        }

        double r = interest / (12 * 100);
        double emi = (amount * r * Math.pow(1 + r, months)) /
                     (Math.pow(1 + r, months) - 1);

        Loan l = new Loan();
        l.setUser(user);
        l.setAmount(amount);
        l.setInterest(interest);
        l.setMonths(months);
        l.setEmi(emi);
        l.setRemaining(amount);
        l.setBranch(user != null && user.getBranch() != null ? user.getBranch() : "MAIN");
        l.setStatus("PENDING");
        l.setProductType("Personal Loan");
        l.setInterestType("FIXED");
        l.setDisbursedAmount(0.0);
        l.setDisbursementDate(null);

        Loan saved = repo.save(l);
        if (saved.getAccountNumber() == null || saved.getAccountNumber().isBlank()) {
            String accNum = String.format("LN%010d", Math.abs(saved.getLoanId()));
            if (repo.findByAccountNumber(accNum).isPresent()) {
                accNum = String.format("LN%08d%02d", Math.abs(saved.getLoanId()), (int) (Math.random() * 90 + 10));
            }
            saved.setAccountNumber(accNum);
            saved = repo.save(saved);
        }
        // Do not generate amortization schedule or disburse funds until approved
        return saved;
    }

    public List<Loan> get(User user) {
        if (user == null) {
            return List.of();
        }
        return repo.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Loan> getLoansByUser(User user) {
        return get(user);
    }

    public Optional<Loan> getById(Long id) {
        if (id == null) return Optional.empty();
        return repo.findById(id);
    }

    public List<Loan> getAll() {
        return repo.findAll();
    }

    @Transactional
    public com.loanproject.loan_app.bean.PaymentTransaction pay(Long id, double amt) {
        return pay(id, amt, "UPI", null);
    }

    @Transactional
    public com.loanproject.loan_app.bean.PaymentTransaction pay(Long id, double amt, String paymentMode, String transactionReference) {
        if (id == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        if (amt <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        Loan l = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Loan not found: " + id));

        // Idempotency: Prevent duplicate payment processing and duplicate balance deductions when retried
        if (transactionReference != null && !transactionReference.isBlank()) {
            java.util.Optional<com.loanproject.loan_app.bean.PaymentTransaction> existingOpt =
                    paymentTransactionRepository.findByTransactionReference(transactionReference);
            if (existingOpt.isPresent()) {
                com.loanproject.loan_app.bean.PaymentTransaction existing = existingOpt.get();
                if ("SUCCESS".equalsIgnoreCase(existing.getStatus())) {
                    return existing;
                }
            }
        }

        String txRef = (transactionReference != null && !transactionReference.isBlank())
                ? transactionReference
                : "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000);

        String mode = (paymentMode != null && !paymentMode.isBlank()) ? paymentMode : "UPI";

        // Persist verified payment transaction
        com.loanproject.loan_app.bean.PaymentTransaction txn = new com.loanproject.loan_app.bean.PaymentTransaction(
                l,
                java.math.BigDecimal.valueOf(amt).setScale(2, java.math.RoundingMode.HALF_UP),
                java.time.Instant.now(),
                mode,
                txRef,
                "SUCCESS"
        );
        com.loanproject.loan_app.bean.PaymentTransaction savedTxn = paymentTransactionRepository.save(txn);

        // Recalculate outstanding balance using existing business rules
        double newRemaining = Math.max(0.0, java.math.BigDecimal.valueOf(l.getRemaining())
                .subtract(java.math.BigDecimal.valueOf(amt))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue());
        l.setRemaining(newRemaining);
        if (l.getRemaining() == 0) {
            l.setStatus("CLOSED");
        }
        repo.save(l);

        // Synchronize and persist repayment installments with confirmed payment
        repaymentScheduleService.synchronizeInstallmentsWithPayments(l, java.time.LocalDate.now(java.time.ZoneId.systemDefault()));

        if (dpdReminderService != null) {
            dpdReminderService.onPaymentConfirmed(l);
        }

        return savedTxn;
    }

    @Transactional
    public Loan approveLoan(Long loanId, String officerEmail, String authorizedBranch) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        Loan l = repo.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (authorizedBranch != null && !"ALL".equalsIgnoreCase(authorizedBranch)) {
            String loanBranch = l.getBranch() != null ? l.getBranch() : "MAIN";
            if (!authorizedBranch.equalsIgnoreCase(loanBranch)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Unauthorized: Loan belongs to branch " + loanBranch + " but officer is assigned to " + authorizedBranch);
            }
        }

        l.setStatus("ACTIVE");
        l.setDisbursementDate(java.time.LocalDate.now());
        l.setDisbursedAmount(l.getAmount());
        if (l.getRemaining() <= 0) {
            l.setRemaining(l.getAmount());
        }

        if (l.getAccountNumber() == null || l.getAccountNumber().isBlank()) {
            String accNum = String.format("LN%010d", Math.abs(l.getLoanId()));
            if (repo.findByAccountNumber(accNum).isPresent()) {
                accNum = String.format("LN%08d%02d", Math.abs(l.getLoanId()), (int) (Math.random() * 90 + 10));
            }
            l.setAccountNumber(accNum);
        }

        Loan saved = repo.save(l);
        repaymentScheduleService.generateAndSaveAmortizationSchedule(saved);
        return saved;
    }

    @Transactional
    public Loan rejectLoan(Long loanId, String reason, String officerEmail, String authorizedBranch) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        Loan l = repo.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (authorizedBranch != null && !"ALL".equalsIgnoreCase(authorizedBranch)) {
            String loanBranch = l.getBranch() != null ? l.getBranch() : "MAIN";
            if (!authorizedBranch.equalsIgnoreCase(loanBranch)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Unauthorized: Loan belongs to branch " + loanBranch + " but officer is assigned to " + authorizedBranch);
            }
        }

        l.setStatus("REJECTED");
        return repo.save(l);
    }
}
