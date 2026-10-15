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

    public Loan apply(User user, double amount, double interest, int months) {
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
            saved.setAccountNumber(String.format("LN%010d", Math.abs(saved.getLoanId())));
            saved = repo.save(saved);
        }
        repaymentScheduleService.generateAndSaveAmortizationSchedule(saved);
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

        return savedTxn;
    }
}
