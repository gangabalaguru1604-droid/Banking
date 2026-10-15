package com.loanproject.loan_app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loanproject.loan_app.bean.AuthAuditLog;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    List<AuthAuditLog> findTop100ByOrderByTimestampDesc();

    List<AuthAuditLog> findByUsernameOrderByTimestampDesc(String username);

    List<AuthAuditLog> findByEventTypeOrderByTimestampDesc(String eventType);
}
