package com.loanproject.loan_app.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.loanproject.loan_app.bean.PasswordResetToken;
import com.loanproject.loan_app.bean.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryTime < :now")
    void deleteExpiredTokens(Instant now);
}
