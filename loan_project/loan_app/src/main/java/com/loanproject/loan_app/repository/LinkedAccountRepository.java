package com.loanproject.loan_app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.LinkedAccount;
import com.loanproject.loan_app.bean.Loan;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

    List<LinkedAccount> findByLoan(Loan loan);

    Optional<LinkedAccount> findFirstByLoan(Loan loan);

    Optional<LinkedAccount> findFirstByLoan_LoanId(Long loanId);
}
