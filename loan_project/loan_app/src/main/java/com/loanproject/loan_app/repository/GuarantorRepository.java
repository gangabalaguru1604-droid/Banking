package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Guarantor;
import com.loanproject.loan_app.bean.Loan;

@Repository
public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {

    List<Guarantor> findByLoan(Loan loan);

    List<Guarantor> findByLoan_LoanId(Long loanId);
}
