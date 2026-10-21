package com.loanproject.loan_app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanReminderLog;
import com.loanproject.loan_app.bean.RepaymentInstallment;

@Repository
public interface LoanReminderLogRepository extends MongoRepository<LoanReminderLog, Long> {

    Optional<LoanReminderLog> findByInstallmentAndDpdDayAndChannelAndReminderCycle(
            RepaymentInstallment installment, int dpdDay, String channel, String reminderCycle);

    List<LoanReminderLog> findByInstallmentAndDpdDayAndReminderCycle(
            RepaymentInstallment installment, int dpdDay, String reminderCycle);

    Optional<LoanReminderLog> findByProviderMessageId(String providerMessageId);

    List<LoanReminderLog> findByLoanAndStatusIn(Loan loan, Collection<String> statuses);

    List<LoanReminderLog> findByInstallmentAndStatusIn(RepaymentInstallment installment, Collection<String> statuses);

    default List<LoanReminderLog> findByLoan_LoanIdOrderByCreatedAtDesc(Long loanId) {
        return findAll().stream().filter(r -> r.getLoan() != null && loanId.equals(r.getLoan().getLoanId()))
                .sorted(java.util.Comparator.comparing(LoanReminderLog::getCreatedAt).reversed()).toList();
    }

    default List<LoanReminderLog> findSuccessfulReminders(Loan loan, RepaymentInstallment installment, int dpdDay) {
        return findAll().stream().filter(r -> r.getInstallment() != null && installment.getId().equals(r.getInstallment().getId()) && r.getDpdDay() == dpdDay &&
                        r.getLoan() != null && loan.getLoanId().equals(r.getLoan().getLoanId()) &&
                        List.of("SENT", "ACCEPTED", "DELIVERED", "OPENED").contains(r.getStatus())).toList();
    }

    default List<LoanReminderLog> findActiveRemindersByLoanId(Long loanId) {
        return findByLoan_LoanIdOrderByCreatedAtDesc(loanId).stream()
                .filter(r -> List.of("CLAIMED", "SENT", "ACCEPTED").contains(r.getStatus())).toList();
    }
}
