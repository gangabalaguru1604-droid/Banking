package com.loanproject.loan_app.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.MfaChallenge;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.MfaChallengeRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final int MAX_MFA_ATTEMPTS = 3;
    private static final Duration CHALLENGE_VALIDITY = Duration.ofMinutes(5);

    private final MfaChallengeRepository challengeRepo;
    private final AuthAuditService auditService;
    private final Map<String, MfaProvider> providers;

    // Rate-limiting challenge creation per user ID (timestamp of last creation)
    private final Map<Long, Instant> lastCreationTimeByUser = new ConcurrentHashMap<>();

    public MfaService(MfaChallengeRepository challengeRepo,
                      AuthAuditService auditService,
                      List<MfaProvider> providerList) {
        this.challengeRepo = challengeRepo;
        this.auditService = auditService;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(MfaProvider::getType, Function.identity()));
    }

    @Transactional
    public MfaChallenge createChallenge(User user, String preferredType, HttpServletRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for MFA challenge");
        }

        // Rate limiting: prevent spamming challenge creation (minimum 5 seconds between creations)
        Instant lastCreation = lastCreationTimeByUser.get(user.getId());
        if (lastCreation != null && Duration.between(lastCreation, Instant.now()).getSeconds() < 5) {
            throw new IllegalStateException("Please wait before requesting another authentication challenge");
        }
        lastCreationTimeByUser.put(user.getId(), Instant.now());

        String type = preferredType != null ? preferredType : user.getMfaType();
        if (type == null || !providers.containsKey(type)) {
            type = "TOTP";
        }

        MfaProvider provider = providers.get(type);
        if (provider == null || !provider.isAvailable()) {
            // Fallback to TOTP if requested provider is unavailable
            type = "TOTP";
            provider = providers.get(type);
        }

        String challengeToken = UUID.randomUUID().toString();
        Instant expiryTime = Instant.now().plus(CHALLENGE_VALIDITY);

        MfaChallenge challenge = new MfaChallenge(user, challengeToken, type, expiryTime);
        challengeRepo.save(challenge);

        if (provider != null) {
            provider.initiate(user, challengeToken, null);
        }

        auditService.logMfaChallengeCreated(user.getEmail(), request);
        return challenge;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public MfaVerificationResult verifyChallenge(String challengeToken, String inputCode, HttpServletRequest request) {
        if (challengeToken == null || challengeToken.isBlank()) {
            return MfaVerificationResult.failure("INVALID_CHALLENGE", "Invalid or missing challenge", 0);
        }

        Optional<MfaChallenge> optionalChallenge = challengeRepo.findByChallengeToken(challengeToken);
        if (optionalChallenge.isEmpty()) {
            return MfaVerificationResult.failure("CHALLENGE_NOT_FOUND", "Authentication challenge not found", 0);
        }

        MfaChallenge challenge = optionalChallenge.get();
        User user = challenge.getUser();
        String username = user != null ? user.getEmail() : "UNKNOWN";

        // 1. Check if already locked
        if (challenge.isLocked() || challenge.getAttemptCount() >= MAX_MFA_ATTEMPTS) {
            challenge.setLocked(true);
            challengeRepo.save(challenge);
            auditService.logMfaLocked(username, request);
            return MfaVerificationResult.failure("CHALLENGE_LOCKED", "Challenge locked due to excessive failed attempts. Please login again.", 0);
        }

        // 2. Check if already verified (prevent double consumption / replay)
        if (challenge.isVerified()) {
            auditService.logMfaFailure(username, request, "CHALLENGE_ALREADY_USED");
            return MfaVerificationResult.failure("CHALLENGE_ALREADY_USED", "Challenge has already been used. Please login again.", 0);
        }

        // 3. Check expiry (5 minutes)
        if (challenge.isExpired()) {
            auditService.logMfaFailure(username, request, "CHALLENGE_EXPIRED");
            return MfaVerificationResult.failure("CHALLENGE_EXPIRED", "Authentication challenge has expired. Please login again.", 0);
        }

        // 4. Verify code using corresponding provider
        MfaProvider provider = providers.get(challenge.getMfaType());
        boolean codeValid = false;
        if (provider != null && inputCode != null) {
            codeValid = provider.verify(user, inputCode.trim());
        }

        if (codeValid) {
            // Atomically mark verified and consumed
            challenge.setVerified(true);
            challengeRepo.save(challenge);
            auditService.logMfaSuccess(username, request);
            return MfaVerificationResult.success(user);
        } else {
            int newAttempts = challenge.getAttemptCount() + 1;
            challenge.setAttemptCount(newAttempts);

            if (newAttempts >= MAX_MFA_ATTEMPTS) {
                challenge.setLocked(true);
                challengeRepo.save(challenge);
                auditService.logMfaLocked(username, request);
                return MfaVerificationResult.failure("CHALLENGE_LOCKED", "Challenge locked due to 3 incorrect attempts. Please login again.", 0);
            }

            challengeRepo.save(challenge);
            int remaining = MAX_MFA_ATTEMPTS - newAttempts;
            auditService.logMfaFailure(username, request, "INVALID_CODE");
            return MfaVerificationResult.failure("INVALID_CODE", "Invalid authentication code. " + remaining + " attempts remaining.", remaining);
        }
    }
}
