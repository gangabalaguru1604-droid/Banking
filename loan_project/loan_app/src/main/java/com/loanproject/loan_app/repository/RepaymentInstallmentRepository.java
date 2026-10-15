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

    @org.springframework.data.jpa.repository.Query("SELECT r FROM RepaymentInstallment r WHERE r.dueDate BETWEEN :startDate AND :endDate AND r.status <> 'PAID' AND r.paidAmount < r.emiAmount AND r.loan.status NOT IN ('CLOSED', 'REJECTED', 'CANCELLED') ORDER BY r.dueDate ASC, r.id ASC")
    org.springframework.data.domain.Page<RepaymentInstallment> findEligibleOverdueInstallments(
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDate endDate,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM RepaymentInstallment r WHERE r.dueDate BETWEEN :startDate AND :endDate AND r.status <> 'PAID' AND r.paidAmount < r.emiAmount AND r.loan.status NOT IN ('CLOSED', 'REJECTED', 'CANCELLED') ORDER BY r.dueDate ASC, r.id ASC")
    List<RepaymentInstallment> findEligibleOverdueInstallmentsList(
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDate endDate);

}
