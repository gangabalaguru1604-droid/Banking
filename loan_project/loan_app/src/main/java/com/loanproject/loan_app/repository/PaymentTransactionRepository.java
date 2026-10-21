package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.PaymentTransaction;

@Repository
public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByLoanOrderByPaymentDateDesc(Loan loan);

    default List<PaymentTransaction> findByLoan_LoanIdOrderByPaymentDateDesc(Long loanId) {
        return findAll().stream().filter(p -> p.getLoan() != null && loanId.equals(p.getLoan().getLoanId()))
                .sorted(java.util.Comparator.comparing(PaymentTransaction::getPaymentDate).reversed()).toList();
    }

    default List<PaymentTransaction> findByLoanInOrderByPaymentDateDesc(List<Loan> loans) {
        java.util.Set<Long> ids = loans.stream().map(Loan::getLoanId).collect(java.util.stream.Collectors.toSet());
        return findAll().stream().filter(p -> p.getLoan() != null && ids.contains(p.getLoan().getLoanId()))
                .sorted(java.util.Comparator.comparing(PaymentTransaction::getPaymentDate).reversed()).toList();
    }

    default List<PaymentTransaction> findTop5ByLoanInOrderByPaymentDateDesc(List<Loan> loans) {
        return findByLoanInOrderByPaymentDateDesc(loans).stream().limit(5).toList();
    }

    java.util.Optional<PaymentTransaction> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);
}
