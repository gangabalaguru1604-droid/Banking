package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Collateral;
import com.loanproject.loan_app.bean.Loan;

@Repository
public interface CollateralRepository extends MongoRepository<Collateral, Long> {

    List<Collateral> findByLoan(Loan loan);

    default List<Collateral> findByLoan_LoanId(Long loanId) {
        return findAll().stream().filter(c -> c.getLoan() != null && loanId.equals(c.getLoan().getLoanId())).toList();
    }
}
