package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.LoanApprovalHistory;

@Repository
public interface LoanApprovalHistoryRepository extends JpaRepository<LoanApprovalHistory, Long> {

    List<LoanApprovalHistory> findByLoanOrderByDecisionTimestampDesc(Loan loan);

    List<LoanApprovalHistory> findByLoan_LoanIdOrderByDecisionTimestampDesc(Long loanId);

    List<LoanApprovalHistory> findByReviewerEmailOrderByDecisionTimestampDesc(String reviewerEmail);
}
