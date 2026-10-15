package com.loanproject.loan_app.security;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final AuthAuditService auditService;

    public LoginAttemptService(UserRepository userRepository, AuthAuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void recordFailedAttempt(String email, HttpServletRequest request) {
        if (email == null || email.isBlank()) {
            auditService.logLoginFailure("UNKNOWN", request, "EMPTY_CREDENTIALS");
            return;
        }

        Optional<User> optionalUser = userRepository.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isAccountNonLocked()) {
                int attempts = user.getFailedAttemptCount() + 1;
                user.setFailedAttemptCount(attempts);

                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    user.setAccountNonLocked(false);
                    user.setLockTime(LocalDateTime.now());
                    userRepository.save(user);
                    log.warn("Account locked for email: {} after {} failed attempts", email, attempts);
                    auditService.logAccountLocked(email, request);
                    return;
                }
                userRepository.save(user);
            }
            auditService.logLoginFailure(email, request, "INVALID_PASSWORD");
        } else {
            auditService.logLoginFailure(email, request, "USER_NOT_FOUND");
        }
    }

    @Transactional
    public void resetFailedAttempts(String email) {
        if (email == null) return;
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            if (user.getFailedAttemptCount() > 0) {
                user.setFailedAttemptCount(0);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public boolean isAccountLocked(String email) {
        if (email == null) return false;
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(user -> !user.isAccountNonLocked())
                .orElse(false);
    }

    @Transactional
    public boolean unlockAccount(String email, String adminUsername, HttpServletRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setAccountNonLocked(true);
            user.setFailedAttemptCount(0);
            user.setLockTime(null);
            userRepository.save(user);
            log.info("Admin {} unlocked account: {}", adminUsername, email);
            auditService.logAccountUnlocked(email, request, adminUsername);
            return true;
        }
        return false;
    }
}
