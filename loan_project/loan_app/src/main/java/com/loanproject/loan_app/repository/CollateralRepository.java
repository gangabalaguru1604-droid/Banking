package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Collateral;
import com.loanproject.loan_app.bean.Loan;

@Repository
public interface CollateralRepository extends JpaRepository<Collateral, Long> {

    List<Collateral> findByLoan(Loan loan);

    List<Collateral> findByLoan_LoanId(Long loanId);
}
