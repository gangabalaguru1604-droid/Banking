package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.RepaymentInstallment;

@Repository
public interface RepaymentInstallmentRepository extends MongoRepository<RepaymentInstallment, Long> {

    List<RepaymentInstallment> findByLoanOrderByInstallmentNumberAsc(Loan loan);

    default List<RepaymentInstallment> findByLoan_LoanIdOrderByInstallmentNumberAsc(Long loanId) {
        return findAll().stream().filter(r -> r.getLoan() != null && loanId.equals(r.getLoan().getLoanId()))
                .sorted(java.util.Comparator.comparingInt(RepaymentInstallment::getInstallmentNumber)).toList();
    }

    default void deleteByLoan(Loan loan) {
        deleteAll(findAll().stream().filter(r -> r.getLoan() != null && loan.getLoanId().equals(r.getLoan().getLoanId())).toList());
    }

    default org.springframework.data.domain.Page<RepaymentInstallment> findEligibleOverdueInstallments(
            java.time.LocalDate startDate, java.time.LocalDate endDate, org.springframework.data.domain.Pageable pageable) {
        List<RepaymentInstallment> rows = findEligibleOverdueInstallmentsList(startDate, endDate);
        int start = (int) Math.min(pageable.getOffset(), rows.size());
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        return new org.springframework.data.domain.PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }

    default List<RepaymentInstallment> findEligibleOverdueInstallmentsList(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return findAll().stream().filter(r -> r.getDueDate() != null && !r.getDueDate().isBefore(startDate) && !r.getDueDate().isAfter(endDate))
                .filter(r -> !"PAID".equalsIgnoreCase(r.getStatus()) && r.getPaidAmount().compareTo(r.getEmiAmount()) < 0)
                .filter(r -> r.getLoan() != null && !List.of("CLOSED", "REJECTED", "CANCELLED").contains(r.getLoan().getStatus()))
                .sorted(java.util.Comparator.comparing(RepaymentInstallment::getDueDate).thenComparing(RepaymentInstallment::getId)).toList();
    }

}
