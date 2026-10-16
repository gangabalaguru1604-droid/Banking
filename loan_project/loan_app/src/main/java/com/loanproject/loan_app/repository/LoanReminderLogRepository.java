package com.loanproject.loan_app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanReminderLog;
import com.loanproject.loan_app.bean.RepaymentInstallment;

@Repository
public interface LoanReminderLogRepository extends JpaRepository<LoanReminderLog, Long> {

    Optional<LoanReminderLog> findByInstallmentAndDpdDayAndChannelAndReminderCycle(
            RepaymentInstallment installment, int dpdDay, String channel, String reminderCycle);

    List<LoanReminderLog> findByInstallmentAndDpdDayAndReminderCycle(
            RepaymentInstallment installment, int dpdDay, String reminderCycle);

    Optional<LoanReminderLog> findByProviderMessageId(String providerMessageId);

    List<LoanReminderLog> findByLoanAndStatusIn(Loan loan, Collection<String> statuses);

    List<LoanReminderLog> findByInstallmentAndStatusIn(RepaymentInstallment installment, Collection<String> statuses);

    List<LoanReminderLog> findByLoan_LoanIdOrderByCreatedAtDesc(Long loanId);

    @Query("SELECT r FROM LoanReminderLog r WHERE r.loan = :loan AND r.installment = :installment AND r.dpdDay = :dpdDay AND r.status IN ('SENT', 'ACCEPTED', 'DELIVERED', 'OPENED')")
    List<LoanReminderLog> findSuccessfulReminders(
            @Param("loan") Loan loan,
            @Param("installment") RepaymentInstallment installment,
            @Param("dpdDay") int dpdDay);

    @Query("SELECT r FROM LoanReminderLog r WHERE r.loan.id = :loanId AND r.status IN ('CLAIMED', 'SENT', 'ACCEPTED')")
    List<LoanReminderLog> findActiveRemindersByLoanId(@Param("loanId") Long loanId);
}
