package com.loanproject.loan_app.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.loanproject.loan_app.bean.MfaChallenge;
import com.loanproject.loan_app.bean.User;

public interface MfaChallengeRepository extends MongoRepository<MfaChallenge, Long> {

    Optional<MfaChallenge> findByChallengeToken(String challengeToken);

    Optional<MfaChallenge> findTopByUserAndVerifiedFalseAndLockedFalseOrderByCreatedAtDesc(User user);

    default void deleteExpiredChallenges(Instant now) {
        deleteAll(findAll().stream().filter(c -> c.getExpiryTime() != null && c.getExpiryTime().isBefore(now)).toList());
    }
}
