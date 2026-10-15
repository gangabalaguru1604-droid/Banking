package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.PaymentTransaction;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByLoanOrderByPaymentDateDesc(Loan loan);

    List<PaymentTransaction> findByLoan_LoanIdOrderByPaymentDateDesc(Long loanId);

    List<PaymentTransaction> findByLoanInOrderByPaymentDateDesc(List<Loan> loans);

    java.util.Optional<PaymentTransaction> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);
}
