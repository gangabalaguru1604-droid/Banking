package com.loanproject.loan_app.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.loanproject.loan_app.bean.PasswordResetToken;
import com.loanproject.loan_app.bean.User;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    default void deleteExpiredTokens(Instant now) {
        deleteAll(findAll().stream().filter(t -> t.getExpiryTime() != null && t.getExpiryTime().isBefore(now)).toList());
    }
}
