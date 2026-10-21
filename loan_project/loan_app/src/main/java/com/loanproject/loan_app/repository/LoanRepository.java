package com.loanproject.loan_app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.PageImpl;
import java.util.Locale;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;

public interface LoanRepository extends MongoRepository<Loan, Long> {

    List<Loan> findByUser(User user);

    List<Loan> findByUserOrderByCreatedAtDesc(User user);

    Optional<Loan> findByAccountNumber(String accountNumber);

    default Optional<Loan> findByAccountNumberAndBranchFilter(String accountNumber, String branch) {
        return findByAccountNumber(accountNumber).filter(l -> inBranch(l, branch));
    }

    default Optional<Loan> findByLoanIdAndBranchFilter(Long loanId, String branch) {
        return findById(loanId).filter(l -> inBranch(l, branch));
    }

    // Customer ID search
    default Page<Loan> searchByCustomerId(String customerId, String branch, Pageable pageable) {
        String q = customerId.toLowerCase(Locale.ROOT);
        return page(findAll().stream().filter(l -> inBranch(l, branch) && l.getUser() != null &&
                ((l.getUser().getCustomerId() != null && l.getUser().getCustomerId().toLowerCase(Locale.ROOT).equals(q)) ||
                 String.valueOf(l.getUser().getId()).equals(q))).toList(), pageable);
    }

    // Phone / Mobile search
    default Page<Loan> searchByPhone(String phone, String branch, Pageable pageable) {
        return page(findAll().stream().filter(l -> inBranch(l, branch) && l.getUser() != null && phone.equals(l.getUser().getPhoneNumber())).toList(), pageable);
    }

    // Customer Name search (safe, normalized, case-insensitive)
    default Page<Loan> searchByName(String name, String branch, Pageable pageable) {
        String q = name.toLowerCase(Locale.ROOT);
        return page(findAll().stream().filter(l -> inBranch(l, branch) && l.getUser() != null && l.getUser().getName() != null &&
                l.getUser().getName().toLowerCase(Locale.ROOT).contains(q)).toList(), pageable);
    }

    private static boolean inBranch(Loan loan, String branch) {
        return branch == null || "ALL".equalsIgnoreCase(branch) || branch.equalsIgnoreCase(loan.getBranch());
    }

    private static Page<Loan> page(List<Loan> results, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), results.size());
        int end = Math.min(start + pageable.getPageSize(), results.size());
        return new PageImpl<>(results.subList(start, end), pageable, results.size());
    }
}
