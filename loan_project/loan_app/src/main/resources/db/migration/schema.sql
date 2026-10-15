-- ============================================================================
-- Banking Loan Management System - Security & Authentication DDL Migration
-- PostgreSQL 14+ / Railway / Render Deployment
-- ============================================================================

-- 1. Users Table (Customer & Staff Identities)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    user_type VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    failed_attempt_count INT NOT NULL DEFAULT 0,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    lock_time TIMESTAMP NULL,
    mfa_enrolled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(512) NULL,
    mfa_type VARCHAR(20) NOT NULL DEFAULT 'TOTP',
    phone_number VARCHAR(50) NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_type ON users(user_type);

-- 2. Loans Table
CREATE TABLE IF NOT EXISTS loan (
    loan_id BIGSERIAL PRIMARY KEY,
    amount DOUBLE PRECISION NOT NULL,
    interest DOUBLE PRECISION NOT NULL,
    months INT NOT NULL,
    emi DOUBLE PRECISION NOT NULL,
    remaining DOUBLE PRECISION NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_loan_user_id ON loan(user_id);

-- 3. MFA Challenges Table (Two-Stage Verification)
CREATE TABLE IF NOT EXISTS mfa_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    challenge_token VARCHAR(64) NOT NULL UNIQUE,
    hashed_otp_code VARCHAR(256) NULL,
    mfa_type VARCHAR(20) NOT NULL DEFAULT 'TOTP',
    expiry_time TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mfa_challenge_token ON mfa_challenges(challenge_token);
CREATE INDEX IF NOT EXISTS idx_mfa_user_active ON mfa_challenges(user_id, verified, locked);

-- 4. Password Reset Tokens Table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expiry_time TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_hash ON password_reset_tokens(token_hash);

-- 5. Authentication Audit Logs Table
CREATE TABLE IF NOT EXISTS auth_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_ip VARCHAR(64) NOT NULL,
    user_agent VARCHAR(256) NULL,
    event_type VARCHAR(50) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    failure_category VARCHAR(50) NULL,
    details VARCHAR(500) NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_username ON auth_audit_logs(username);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON auth_audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON auth_audit_logs(event_type);
