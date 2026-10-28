package com.loanproject.loan_app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUser(User user);

    List<Loan> findByUserOrderByCreatedAtDesc(User user);

    Optional<Loan> findByAccountNumber(String accountNumber);

    @Query("SELECT l FROM Loan l JOIN FETCH l.user WHERE l.accountNumber = :accountNumber AND (:branch = 'ALL' OR l.branch = :branch)")
    Optional<Loan> findByAccountNumberAndBranchFilter(@Param("accountNumber") String accountNumber, @Param("branch") String branch);

    @Query("SELECT l FROM Loan l JOIN FETCH l.user WHERE l.loanId = :loanId AND (:branch = 'ALL' OR l.branch = :branch)")
    Optional<Loan> findByLoanIdAndBranchFilter(@Param("loanId") Long loanId, @Param("branch") String branch);

    // Customer ID search
    @Query(value = "SELECT l FROM Loan l JOIN FETCH l.user u WHERE (LOWER(u.customerId) = LOWER(:customerId) OR CAST(u.id AS string) = :customerId) AND (:branch = 'ALL' OR l.branch = :branch)",
           countQuery = "SELECT COUNT(l) FROM Loan l JOIN l.user u WHERE (LOWER(u.customerId) = LOWER(:customerId) OR CAST(u.id AS string) = :customerId) AND (:branch = 'ALL' OR l.branch = :branch)")
    Page<Loan> searchByCustomerId(@Param("customerId") String customerId, @Param("branch") String branch, Pageable pageable);

    // Phone / Mobile search
    @Query(value = "SELECT l FROM Loan l JOIN FETCH l.user u WHERE u.phoneNumber = :phone AND (:branch = 'ALL' OR l.branch = :branch)",
           countQuery = "SELECT COUNT(l) FROM Loan l JOIN l.user u WHERE u.phoneNumber = :phone AND (:branch = 'ALL' OR l.branch = :branch)")
    Page<Loan> searchByPhone(@Param("phone") String phone, @Param("branch") String branch, Pageable pageable);

    // Customer Name search (safe, normalized, case-insensitive)
    @Query(value = "SELECT l FROM Loan l JOIN FETCH l.user u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')) AND (:branch = 'ALL' OR l.branch = :branch)",
           countQuery = "SELECT COUNT(l) FROM Loan l JOIN l.user u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')) AND (:branch = 'ALL' OR l.branch = :branch)")
    Page<Loan> searchByName(@Param("name") String name, @Param("branch") String branch, Pageable pageable);

    // Pending applications query for loan approval workflow
    @Query("SELECT l FROM Loan l JOIN FETCH l.user u WHERE l.status IN ('PENDING', 'SUBMITTED', 'IN_REVIEW', 'APPLIED') AND (:branch = 'ALL' OR l.branch = :branch) ORDER BY l.createdAt DESC")
    List<Loan> findPendingLoansByBranch(@Param("branch") String branch);

    @Query("SELECT l FROM Loan l JOIN FETCH l.user u WHERE l.loanId = :loanId")
    Optional<Loan> findWithUserById(@Param("loanId") Long loanId);
}
