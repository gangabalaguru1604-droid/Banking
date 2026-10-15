package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.RepaymentInstallment;

@Repository
public interface RepaymentInstallmentRepository extends JpaRepository<RepaymentInstallment, Long> {

    List<RepaymentInstallment> findByLoanOrderByInstallmentNumberAsc(Loan loan);

    List<RepaymentInstallment> findByLoan_LoanIdOrderByInstallmentNumberAsc(Long loanId);

    void deleteByLoan(Loan loan);
}
