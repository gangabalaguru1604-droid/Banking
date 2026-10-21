package com.loanproject.loan_app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.LinkedAccount;
import com.loanproject.loan_app.bean.Loan;

@Repository
public interface LinkedAccountRepository extends MongoRepository<LinkedAccount, Long> {

    List<LinkedAccount> findByLoan(Loan loan);

    Optional<LinkedAccount> findFirstByLoan(Loan loan);

    default Optional<LinkedAccount> findFirstByLoan_LoanId(Long loanId) {
        return findAll().stream().filter(a -> a.getLoan() != null && loanId.equals(a.getLoan().getLoanId())).findFirst();
    }
}
