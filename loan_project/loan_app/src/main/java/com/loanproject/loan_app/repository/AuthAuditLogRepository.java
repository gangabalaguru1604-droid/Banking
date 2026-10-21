package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.loanproject.loan_app.bean.AuthAuditLog;

public interface AuthAuditLogRepository extends MongoRepository<AuthAuditLog, Long> {

    List<AuthAuditLog> findTop100ByOrderByTimestampDesc();

    List<AuthAuditLog> findByUsernameOrderByTimestampDesc(String username);

    List<AuthAuditLog> findByEventTypeOrderByTimestampDesc(String eventType);
}
