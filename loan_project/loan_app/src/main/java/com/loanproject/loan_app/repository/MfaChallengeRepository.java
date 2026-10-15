package com.loanproject.loan_app.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.loanproject.loan_app.bean.MfaChallenge;
import com.loanproject.loan_app.bean.User;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, Long> {

    Optional<MfaChallenge> findByChallengeToken(String challengeToken);

    Optional<MfaChallenge> findTopByUserAndVerifiedFalseAndLockedFalseOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("DELETE FROM MfaChallenge c WHERE c.expiryTime < :now")
    void deleteExpiredChallenges(Instant now);
}
