package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Guarantor;
import com.loanproject.loan_app.bean.Loan;

@Repository
public interface GuarantorRepository extends MongoRepository<Guarantor, Long> {

    List<Guarantor> findByLoan(Loan loan);

    default List<Guarantor> findByLoan_LoanId(Long loanId) {
        return findAll().stream().filter(g -> g.getLoan() != null && loanId.equals(g.getLoan().getLoanId())).toList();
    }
}
